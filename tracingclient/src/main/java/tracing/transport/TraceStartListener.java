package tracing.transport;

import tracing.Config;

/**
 * Listener for notifying of the "go" signal from the server.
 */
public interface TraceStartListener {

    void onTraceStarted(Config config);
}
