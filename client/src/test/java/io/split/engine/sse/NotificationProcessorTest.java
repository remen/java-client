package io.split.engine.sse;

import io.split.engine.sse.dtos.*;
import io.split.engine.sse.workers.SplitsWorker;
import org.junit.Test;
import org.mockito.Mockito;

public class NotificationProcessorTest {

    @Test
    public void processSplitUpdateAddToQueueInWorker() {
        long changeNumber = 1585867723838L;
        String channel = "splits";
        GenericNotificationData genericNotificationData = new GenericNotificationData(changeNumber, null, null, null, null, null, null, channel);

        SplitsWorker splitsWorker = Mockito.mock(SplitsWorker.class);
        SplitChangeNotification splitChangeNotification = new SplitChangeNotification(genericNotificationData);

        NotificationProcessor notificationProcessor = new NotificationProcessorImp(splitsWorker);
        notificationProcessor.process(splitChangeNotification);

        Mockito.verify(splitsWorker, Mockito.times(1)).addToQueue(splitChangeNotification.getChangeNumber());
    }

    @Test
    public void processSplitKillAddToQueueInWorker() {
        long changeNumber = 1585867723838L;
        String defaultTreatment = "off";
        String splitName = "test-split";
        String channel = "splits";
        GenericNotificationData genericNotificationData = new GenericNotificationData(changeNumber, defaultTreatment, splitName, null, null, null, null, channel);

        SplitsWorker splitsWorker = Mockito.mock(SplitsWorker.class);
        SplitKillNotification splitKillNotification = new SplitKillNotification(genericNotificationData);

        NotificationProcessor notificationProcessor = new NotificationProcessorImp(splitsWorker);
        notificationProcessor.process(splitKillNotification);

        Mockito.verify(splitsWorker, Mockito.times(1)).killSplit(splitKillNotification.getChangeNumber(), splitKillNotification.getSplitName(), splitKillNotification.getDefaultTreatment());
    }
}