package tracing.backend.trace;

import tracing.backend.Target;
import tracing.backend.source.elf.Symbol;

import java.util.Map;

/**
 * An internal event that marks a read or write to memory.
 */
public class MemoryEvent extends TraceEvent {

    private final Long address;
    private final Long value;
    // whether this is a write or read
    private final boolean isWrite;

    // the symbol at the address
    private Symbol symbol;

    /**
     * Creates a new memory event on the given target from the trace packet.
     * @param target the target the event happened on
     * @param tracePacket the raw trace packet
     */
    public MemoryEvent(Target target, TracePacket tracePacket) {
        super(target, EventType.INTERNAL, tracePacket.getTimestamp());

        this.address = tracePacket.getA();
        this.value = tracePacket.getV();
        this.isWrite = (tracePacket.getSubType() == TracePacket.SUBTYPE_WRITE);
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }

    public long getAddress() {
        return address;
    }

    public long getValue() {
        return value;
    }

    public boolean isWrite() {
        return isWrite;
    }

    @Override
    public void serialize(Map<String, Object> jsonMap) {
        super.serialize(jsonMap);
        jsonMap.put("type", isWrite() ? "MEMORY_WRITE" : "MEMORY_READ");
        jsonMap.put("write", isWrite());
        jsonMap.put("variable", getSymbol().getName());
        jsonMap.put("size", getSymbol().getSize());
        jsonMap.put("address", getSymbol().getAddress());
        jsonMap.put("value", getValue());
    }

    @Override
    public String toString() {
        return "[" + getTargetId() + "] " + getString();
    }

    public String getString() {
        return (isWrite ? "WRITE " : "READ  ") + "variable " + symbol.getName() + ": 0x" + String.format("%0" + symbol.getSize() * 2 + "x", value).toUpperCase();
    }
}
