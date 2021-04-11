package tracing.backend.trace;

import tracing.backend.Target;

/**
 * Heartbeat event to indicate that the observer is still alive.
 */
public class Heartbeat extends TraceEvent implements Transient {

    public Heartbeat(Target target, long timestamp) {
        super(target, EventType.INTERNAL, timestamp);
    }

    @Override
    public String toString() {
        return "[" + getTargetId() + "] " + getGlobalEventId() + " HEARTBEAT"; //+ "\n" + getVectorClock() + " - " + getLocalTimestamp();
    }
}
