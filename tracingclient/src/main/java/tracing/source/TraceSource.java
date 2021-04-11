package tracing.source;

import tracing.Config;

/**
 * Source of trace data. Has to follow the given config (e.g. in terms of traced variables).
 */
public interface TraceSource {

    void start(Config config);
    void close();
}
