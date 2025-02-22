package io.split.service;

import io.split.client.utils.Utils;
import io.split.telemetry.domain.enums.HTTPLatenciesEnum;
import io.split.telemetry.domain.enums.LastSynchronizationRecordsEnum;
import io.split.telemetry.domain.enums.ResourceEnum;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hc.client5.http.classic.methods.HttpPost;

import java.net.URI;

import static com.google.common.base.Preconditions.checkNotNull;

public class HttpPostImp {
    private static final Logger _logger = LoggerFactory.getLogger(HttpPostImp.class);
    private CloseableHttpClient _client;
    private final TelemetryRuntimeProducer _telemetryRuntimeProducer;

    public HttpPostImp(CloseableHttpClient client, TelemetryRuntimeProducer telemetryRuntimeProducer) {
        _client = client;
        _telemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
    }

    public void post(URI uri, Object object, String posted, HTTPLatenciesEnum httpLatenciesEnum, LastSynchronizationRecordsEnum lastSynchronizationRecordsEnum, ResourceEnum resourceEnum) {
        long initTime = System.currentTimeMillis();
        HttpEntity entity = Utils.toJsonEntity(object);
        HttpPost request = new HttpPost(uri);
        request.setEntity(entity);

        try (CloseableHttpResponse response = _client.execute(request)) {

            int status = response.getCode();

            if (status < HttpStatus.SC_OK || status >= HttpStatus.SC_MULTIPLE_CHOICES) {
                _telemetryRuntimeProducer.recordSyncError(resourceEnum, status);
                _logger.warn("Response status was: " + status);
                return;
            }
            _telemetryRuntimeProducer.recordSyncLatency(httpLatenciesEnum, System.currentTimeMillis() - initTime);
            _telemetryRuntimeProducer.recordSuccessfulSync(lastSynchronizationRecordsEnum, System.currentTimeMillis());
        } catch (Throwable t) {
            _logger.warn("Exception when posting " + posted + object, t);
        }
    }
}
