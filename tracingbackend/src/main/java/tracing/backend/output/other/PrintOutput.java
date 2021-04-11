package tracing.backend.output.other;

import tracing.backend.output.TraceOutput;
import tracing.backend.trace.TraceEvent;
import tracing.backend.trace.TracePacket;

/**
 * Simply prints a string representation of every event to the console.
 */
public class PrintOutput implements TraceOutput {

    @Override
    public void init() { }

    @Override
    public void close() { }

    @Override
    public void put(TraceEvent traceEvent) {
        traceEvent.print();
    }

    @Override
    public void putRaw(TracePacket tracePacket) { }
}
