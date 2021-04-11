package tracing.backend.output;

import tracing.backend.trace.TraceEvent;
import tracing.backend.trace.TracePacket;

/**
 * Specifies an adapter or converter to output the result trace.
 */
public interface TraceOutput {

    /**
     * Initialize the output.
     */
    void init();

    /**
     * Close or stop the output.
     */
    void close();

    /**
     * Write a processed event to the output.
     * @param traceEvent the event to output
     */
    void put(TraceEvent traceEvent);

    /**
     * Write a raw, unprocessed event to the output (not supported by all implementations).
     * @param tracePacket the trace packet (raw event)
     */
    void putRaw(TracePacket tracePacket);
}
