package tracing.backend.post_processing;

import tracing.backend.trace.TraceEvent;

import java.util.function.Consumer;

/**
 * A post-processing step that consumes events after they exit the scheduler.
 */
public interface PostProcessingStep extends Consumer<TraceEvent> {
}
