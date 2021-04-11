package tracing.backend;

import tracing.backend.scheduler.vectorclock.VectorClock;
import tracing.backend.trace.TraceEvent;

import java.util.List;

/**
 * Represents a tracing target (or the observer board of a target).
 */
public class Target {

    // unique name/ID of the target
    private final String targetId;

    // the queue of events from this target
    private final TraceQueue traceQueue = new TraceQueue();

    // whether the target notified the server that it is ready to start tracing
    private boolean isReady = false;

    // host name of the observer
    private String host;

    // the current vector clock value of the target
    private VectorClock<String> vectorClock;

    // last event from this target that the scheduler processed
    private TraceEvent lastTraceEvent;

    /**
     * Construct a new target instance with a unique ID.
     *
     * @param targetId unique ID
     * @param allTargetIds IDs of all other targets, needed for vector clock
     */
    public Target(String targetId, List<String> allTargetIds) {
        this.targetId = targetId;

        this.vectorClock = VectorClock.create(allTargetIds);
    }

    /**
     * Increments the vector clock of this target and returns the new value.
     * @return new value
     */
    public VectorClock<String> incrementVectorClock() {
        this.vectorClock = this.vectorClock.increment(this.getTargetId());
        return this.vectorClock;
    }

    /**
     * Increments the vector clock of this target, merges it with the given vector clock and returns the new value.
     * @return new value
     */
    public VectorClock<String> merge(VectorClock<String> other) {
        this.incrementVectorClock();
        this.vectorClock = this.vectorClock.merge(other);
        return this.vectorClock;
    }

    public TraceQueue getTraceQueue() {
        return traceQueue;
    }

    public String getTargetId() {
        return targetId;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean ready) {
        isReady = ready;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public TraceEvent getLastTraceEvent() {
        return lastTraceEvent;
    }

    public void setLastTraceEvent(TraceEvent lastTraceEvent) {
        this.lastTraceEvent = lastTraceEvent;
    }
}
