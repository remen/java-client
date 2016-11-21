package io.split.client;

import io.split.engine.experiments.SplitFetcher;
import io.split.engine.impressions.TreatmentLog;
import io.split.engine.metrics.Metrics;

/**
 * Created by adilaijaz on 7/15/16.
 */
public class SplitFactoryImpl implements SplitFactory {

    private final SplitClient _client;
    private final SplitManager _manager;

    public SplitFactoryImpl(SplitFetcher fetcher, TreatmentLog treatmentLog, Metrics metrics) {
        _client = new SplitClientImpl(fetcher, treatmentLog, metrics);
        _manager = new SplitManagerImpl(fetcher);

    }

    public SplitClient client() {
        return _client;
    }

    public SplitManager manager() {
        return _manager;
    }


}