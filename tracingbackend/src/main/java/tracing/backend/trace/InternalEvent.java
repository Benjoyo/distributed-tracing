package tracing.backend.trace;

import tracing.backend.Target;

/**
 * A generic internal event, used in simulation.
 */
public class InternalEvent extends TraceEvent {

    public InternalEvent(Target target, long timestamp) {
        super(target, EventType.INTERNAL, timestamp);
    }

    @Override
    public String toString() {
        return "[" + getTargetId() + "] " + getGlobalEventId() + " LOCAL";
    }
}
