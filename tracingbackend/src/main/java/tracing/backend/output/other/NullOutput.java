package tracing.backend.output.other;

import tracing.backend.output.TraceOutput;
import tracing.backend.trace.TraceEvent;
import tracing.backend.trace.TracePacket;

/**
 * A TraceOutput that does nothing.
 */
public class NullOutput implements TraceOutput {

    @Override
    public void init() { }

    @Override
    public void put(TraceEvent traceEvent) { }

    @Override
    public void putRaw(TracePacket tracePacket) { }

    @Override
    public void close() { }
}
