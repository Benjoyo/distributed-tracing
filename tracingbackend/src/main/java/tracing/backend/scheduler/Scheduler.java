package tracing.backend.scheduler;

import tracing.backend.Target;
import tracing.backend.trace.TraceEvent;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * An event scheduler gets a collection of targets that each have an event queue and outputs a single result stream
 * of ordered event.
 */
public interface Scheduler extends Runnable {

    /**
     * Sets the collection of targets. Each target has an event queue that is an input to the scheduler.
     * @param targets the targets
     */
    void setTargets(Collection<Target> targets);

    /**
     * Stop the scheduler.
     */
    void stop();

    /**
     * The output stream of ordered events.
     * @return the stream
     */
    Stream<TraceEvent> resultStream();
}
