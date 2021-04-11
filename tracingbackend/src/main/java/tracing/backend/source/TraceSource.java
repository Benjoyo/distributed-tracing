package tracing.backend.source;

import tracing.backend.configuration.TargetProvider;

/**
 * A TraceSource provides a way to receive trace events from targets.
 * For this, it gives a TargetProvider that creates the target definitions.
 * Each target contains a queue of events.
 */
public interface TraceSource {

    void init();
    void close();

    TargetProvider getTargetProvider();
}
