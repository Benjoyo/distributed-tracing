package tracing.backend.trace;

import tracing.backend.Target;
import tracing.backend.source.elf.Function;

import java.util.Map;

/**
 * A function call event.
 */
public class FunctionEvent extends TraceEvent {

    // address of the called function
    private final Long functionAddress;
    // address of the calling function
    private final Long callSiteAddress;
    // whether this is the enter or exit event
    private final Boolean isEnter;

    // called function
    private Function calleeFunction;
    // calling function
    private Function callerFunction;

    /**
     * Creates a new function event on the given target from the trace packet.
     * @param target the target the event happened on
     * @param tracePacket the raw trace packet
     */
    public FunctionEvent(Target target, TracePacket tracePacket) {
        super(target, EventType.INTERNAL, tracePacket.getTimestamp());

        this.functionAddress = tracePacket.getF();
        this.callSiteAddress = tracePacket.getC();
        this.isEnter = (tracePacket.getSubType() == TracePacket.SUBTYPE_ENTER);
    }

    public Function getCalleeFunction() {
        return calleeFunction;
    }

    public void setCalleeFunction(Function calleeFunction) {
        this.calleeFunction = calleeFunction;
    }

    public Function getCallerFunction() {
        return callerFunction;
    }

    public void setCallerFunction(Function callerFunction) {
        this.callerFunction = callerFunction;
    }

    public long getFunctionAddress() {
        return functionAddress;
    }

    public long getCallSiteAddress() {
        return callSiteAddress;
    }

    public boolean isEnter() {
        return isEnter;
    }

    @Override
    public void serialize(Map<String, Object> jsonMap) {
        super.serialize(jsonMap);
        jsonMap.put("type", isEnter() ? "FUNCTION_ENTER" : "FUNCTION_EXIT");
        jsonMap.put("enter", isEnter());
        jsonMap.put("function", getCalleeFunction().getName());
        jsonMap.put("file", getCalleeFunction().getFile());
        jsonMap.put("line", getCalleeFunction().getLineNumber());
        jsonMap.put("address", getCalleeFunction().getAddress());
    }

    @Override
    public String toString() {
        return "[" + getTargetId() + "] " + getString();
    }

    public String getString() {
        var callee = calleeFunction.getFileAndLineNumber().split("/");
        var caller = callerFunction.getFileAndLineNumber().split("/");
        return (isEnter ? "ENTER " : "EXIT  ") + "function " + calleeFunction.getName() + " (" + callee[callee.length - 1] + "), called from " + callerFunction.getName() + " (" + caller[caller.length - 1] + ")";
    }
}
