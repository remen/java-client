package io.split.client;

import com.google.common.annotations.VisibleForTesting;
import io.split.client.dtos.Event;
import io.split.client.utils.GenericClientUtil;
import io.split.client.utils.Utils;
import io.split.telemetry.domain.enums.EventsDataRecordsEnum;
import io.split.telemetry.domain.enums.HTTPLatenciesEnum;
import io.split.telemetry.domain.enums.LastSynchronizationRecordsEnum;
import io.split.telemetry.storage.TelemetryEvaluationProducer;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.MIN_PRIORITY;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Responsible for sending events added via .track() to Split collection services
 */
public class EventClientImpl implements EventClient {

    public static final Long MAX_SIZE_BYTES = 5 * 1024 * 1024L;

    private final BlockingQueue<WrappedEvent> _eventQueue;
    private final int _maxQueueSize;
    private final long _flushIntervalMillis;

    private final ExecutorService _senderExecutor;
    private final ExecutorService _consumerExecutor;

    private final ScheduledExecutorService _flushScheduler;

    static final Event CENTINEL = new Event();
    private static final Logger _log = LoggerFactory.getLogger(EventClientImpl.class);
    private final CloseableHttpClient _httpclient;
    private final URI _target;
    private final int _waitBeforeShutdown;
    private final TelemetryRuntimeProducer _telemetryRuntimeProducer;

    ThreadFactory eventClientThreadFactory(final String name) {
        return r -> new Thread(() -> {
            Thread.currentThread().setPriority(MIN_PRIORITY);
            r.run();
        }, name);
    }


    public static EventClientImpl create(CloseableHttpClient httpclient, URI eventsRootTarget, int maxQueueSize, long flushIntervalMillis, int waitBeforeShutdown, TelemetryRuntimeProducer telemetryRuntimeProducer) throws URISyntaxException {
        return new EventClientImpl(new LinkedBlockingQueue<>(maxQueueSize),
                httpclient,
                Utils.appendPath(eventsRootTarget, "api/events/bulk"),
                maxQueueSize,
                flushIntervalMillis,
                waitBeforeShutdown,
                telemetryRuntimeProducer);
    }

    EventClientImpl(BlockingQueue<WrappedEvent> eventQueue, CloseableHttpClient httpclient, URI target, int maxQueueSize,
                    long flushIntervalMillis, int waitBeforeShutdown, TelemetryRuntimeProducer telemetryRuntimeProducer) throws URISyntaxException {

        _httpclient = httpclient;

        _target = target;

        _eventQueue = eventQueue;
        _waitBeforeShutdown = waitBeforeShutdown;

        _maxQueueSize = maxQueueSize;
        _flushIntervalMillis = flushIntervalMillis;
        _telemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);

        _senderExecutor = new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(50),
                eventClientThreadFactory("eventclient-sender"),
                new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        _log.warn("Executor queue full. Dropping events.");
                    }
                });

        _consumerExecutor = Executors.newSingleThreadExecutor(eventClientThreadFactory("eventclient-consumer"));
        _consumerExecutor.submit(new Consumer());

        _flushScheduler = Executors.newScheduledThreadPool(1, eventClientThreadFactory("eventclient-flush"));
        _flushScheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                flush();
            }
        }, _flushIntervalMillis, _flushIntervalMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * the existence of this message in the queue triggers a send event in the consumer thread.
     */
    public void flush() {
        track(CENTINEL, 0);
    }  // CENTINEL event won't be queued, so no size needed.

    public boolean track(Event event, int eventSize) {
        try {
            if (event == null) {
                return false;
            }
            if(_eventQueue.offer(new WrappedEvent(event, eventSize))) {
                _telemetryRuntimeProducer.recordEventStats(EventsDataRecordsEnum.EVENTS_QUEUED, 1);
            }
            else {
                _log.warn("Event dropped.");
                _telemetryRuntimeProducer.recordEventStats(EventsDataRecordsEnum.EVENTS_DROPPED, 1);
            }

        } catch (ClassCastException | NullPointerException | IllegalArgumentException e) {
            _telemetryRuntimeProducer.recordEventStats(EventsDataRecordsEnum.EVENTS_DROPPED, 1);
            _log.warn("Interruption when adding event withed while adding message %s.", event);
            return false;
        }
        return true;
    }

    public void close() {
        try {
            _consumerExecutor.shutdownNow();
            _flushScheduler.shutdownNow();
            _senderExecutor.awaitTermination(_waitBeforeShutdown, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            _log.warn("Error when shutting down EventClientImpl", e);
        }
    }

    /**
     * Infinite loop that listens to event from the event queue, dequeue them and send them over once:
     *  - a CENTINEL message has arrived, or
     *  - the queue reached a specific size
     *
     */
    class Consumer implements Runnable {
        @Override
        public void run() {
            List<Event> events = new ArrayList<>();
            long accumulated = 0;
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    WrappedEvent data = _eventQueue.take();
                    Event event = data.event();
                    Long size = data.size();

                    if (event != CENTINEL) {
                        events.add(event);
                        accumulated += size;
                    } else if (events.size() < 1) {

                        if (_log.isDebugEnabled()) {
                            _log.debug("No messages to publish.");
                        }

                        continue;
                    }
                    long initTime = System.currentTimeMillis();
                    if (events.size() >= _maxQueueSize ||  accumulated >= MAX_SIZE_BYTES || event == CENTINEL) {

                        // Send over the network
                        if (_log.isDebugEnabled()) {
                            _log.debug(String.format("Sending %d events", events.size()));
                        }

                        // Dispatch
                        _senderExecutor.submit(EventSenderTask.create(_httpclient, _target, events));

                        // Clear the queue of events for the next batch.
                        events = new ArrayList<>();
                        accumulated = 0;
                        _telemetryRuntimeProducer.recordSyncLatency(HTTPLatenciesEnum.EVENTS, System.currentTimeMillis()-initTime);
                        _telemetryRuntimeProducer.recordSuccessfulSync(LastSynchronizationRecordsEnum.EVENTS, System.currentTimeMillis());
                    }
                }
            } catch (InterruptedException e) {
                _log.debug("Consumer thread was interrupted. Exiting...");
            }
        }
    }

    static class WrappedEvent {
        private final Event _event;
        private final long _size;

        public WrappedEvent(Event event, long size) {
            _event = event;
            _size = size;
        }

        public Event event() {
            return _event;
        }

        public long size() {
            return _size;
        }
    }

    static class EventSenderTask implements Runnable {

        private final List<Event> _data;
        private final URI _endpoint;
        private final CloseableHttpClient _client;

        static EventSenderTask create(CloseableHttpClient httpclient, URI eventsTarget, List<Event> events) {
            return new EventSenderTask(httpclient, eventsTarget, events);
        }

        EventSenderTask(CloseableHttpClient httpclient, URI eventsTarget, List<Event> events) {
            _client = httpclient;
            _data = events;
            _endpoint = eventsTarget;
        }

        @Override
        public void run() {
            GenericClientUtil.process(_data, _endpoint, _client);
        }
    }

    @VisibleForTesting
    URI getTarget() {
        return _target  ;
    }
}