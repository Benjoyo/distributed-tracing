package tracing.backend.post_processing.print;

import tracing.backend.post_processing.PostProcessingStep;
import tracing.backend.trace.TraceEvent;

/**
 * Simply prints a string representation of every event.
 * Can be used for debugging etc. if if the chosen output does not print to the console.
 */
public class PrintStep implements PostProcessingStep {

    @Override
    public void accept(TraceEvent traceEvent) {
        traceEvent.print();
    }
}
