package tracing.backend.trace;

import tracing.backend.Target;

import java.util.Map;

/**
 * An internal event that contains a log message.
 */
public class LogEvent extends TraceEvent {

    private final String logMessage;

    /**
     * Creates a new log event on the given target from the trace packet.
     * @param target the target the event happened on
     * @param tracePacket the raw trace packet
     */
    public LogEvent(Target target, TracePacket tracePacket) {
        super(target, EventType.INTERNAL, tracePacket.getTimestamp());

        if (tracePacket.getI() == TracePacket.TYPE_LOG) {
            logMessage = tracePacket.getLogMessage();
        } else if (tracePacket.getI() == TracePacket.TYPE_OVERFLOW) {
            logMessage = "ITM_OVERFLOW";
        } else {
            logMessage = "";
        }
    }

    public String getLogMessage() {
        return logMessage;
    }

    @Override
    public void serialize(Map<String, Object> jsonMap) {
        super.serialize(jsonMap);
        jsonMap.put("type", "LOG");
        jsonMap.put("log_msg", getLogMessage());
    }

    @Override
    public String toString() {
        return "[" + getTargetId() + "] INFO: " + logMessage;
    }
}
