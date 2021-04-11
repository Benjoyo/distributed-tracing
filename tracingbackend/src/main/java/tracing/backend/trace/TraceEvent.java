package tracing.backend.trace;

import tracing.backend.Target;
import tracing.backend.scheduler.vectorclock.VectorClock;

import java.util.Map;
import java.util.UUID;

/**
 * Base class for all events.
 */
public abstract class TraceEvent {

    public final String uuid = UUID.randomUUID().toString();

    // the target that the event happened on
    private final Target target;
    // the target ID that the event happened on
    private final String targetId;
    // the type of the event
    private final EventType eventType;
    // the local timestamp captured at observer
    private final Long localTimestamp;

    // vector clock value of this event
    private VectorClock<String> vectorClock;
    // global event ID
    private Long globalEventId;

    // event ID of a predecessor event (used for falcon)
    private Long dependency;

    /**
     * Creates a new trace event on the given target of the given type and with the given timestamp.
     * @param target the target the event happened on
     * @param eventType the event type
     * @param localTimestamp timestamp captured at observer
     */
    protected TraceEvent(Target target, EventType eventType, long localTimestamp) {
        this.target = target;
        this.targetId = target.getTargetId();
        this.eventType = eventType;
        this.localTimestamp = localTimestamp;
    }

    /**
     * Creates a new event on the given target from the trace packet.
     * @param target the target the event happened on
     * @param tracePacket the raw trace packet
     */
    public static TraceEvent from(Target target, TracePacket tracePacket) {
        if (tracePacket.getI() == null) {
            return new Heartbeat(target, tracePacket.getTimestamp());
        }
        switch (tracePacket.getI()) {
            case TracePacket.TYPE_FUNCTION:
                return new FunctionEvent(target, tracePacket);
            case TracePacket.TYPE_MEMORY:
                return new MemoryEvent(target, tracePacket);
            case TracePacket.TYPE_MESSAGE:
                return new MessageEvent(target, tracePacket);
            case TracePacket.TYPE_LOG:
            case TracePacket.TYPE_OVERFLOW:
                return new LogEvent(target, tracePacket);
            default:
                throw new IllegalStateException("Unexpected value: " + tracePacket.getI());
        }
    }

    public EventType getEventType() {
        return eventType;
    }

    public Long getGlobalEventId() {
        return globalEventId;
    }

    public String getTargetId() {
        return targetId;
    }

    public Long getLocalTimestamp() {
        return localTimestamp;
    }

    public VectorClock<String> getVectorClock() {
        return vectorClock;
    }

    public void setVectorClock(VectorClock<String> vectorClock) {
        this.vectorClock = vectorClock;
    }

    public void setGlobalEventId(Long globalEventId) {
        this.globalEventId = globalEventId;
    }

    public Boolean vectorClockIsAfter(TraceEvent that) {
        return this.vectorClock.isAfter(that.vectorClock);
    }

    public void serialize(Map<String, Object> jsonMap) {
        jsonMap.put("targetId", getTargetId());
        if (getVectorClock() != null) {
            jsonMap.put("vector_clock", getVectorClock().toMap());
        }
        jsonMap.put("timestamp", getLocalTimestamp());
    }

    public void print() {
        System.out.println(this);
    }

    @Override
    public String toString() {
        return "{" +
                "targetId='" + targetId + '\'' +
                ", globalEventId=" + globalEventId +
                ", eventType=" + eventType +
                ", vectorClock=" + vectorClock +
                '}';
    }

    public Target getTarget() {
        return target;
    }

    public Long getDependencies() {
        return dependency;
    }

    public void setDependency(Long dependency) {
        this.dependency = dependency;
    }
}
