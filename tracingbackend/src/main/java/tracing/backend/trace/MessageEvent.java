package tracing.backend.trace;

import tracing.backend.Target;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A message send or receive event.
 */
public class MessageEvent extends TraceEvent {

    // the id of this message
    private final String msgId;

    // either a sender or (one or multiple) receivers, depending on the type
    private final Set<String> participants = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Creates a new message event (send or receive) on the given target from the trace packet.
     * @param target the target the event happened on
     * @param tracePacket the raw trace packet
     */
    public MessageEvent(Target target, TracePacket tracePacket) {
        super(target, (tracePacket.getSubType() == TracePacket.SUBTYPE_SEND) ? EventType.SEND : EventType.RECEIVE, tracePacket.getTimestamp());

        this.msgId = tracePacket.getM();
    }

    public String getMsgId() {
        return msgId;
    }

    /**
     * Add a target that (depending on whether this is a send or receive) is a receiver or sender of this message.
     * Used to track senders and recipients of messages.
     * @param targetId participating target ID
     */
    public void addParticipant(String targetId) {
        this.participants.add(targetId);
    }

    public Set<String> getParticipants() {
        return participants;
    }

    @Override
    public void serialize(Map<String, Object> jsonMap) {
        super.serialize(jsonMap);
        jsonMap.put("type", getEventType() == EventType.SEND ? "MESSAGE_SEND" : "MESSAGE_RECEIVE");
        jsonMap.put("msg_id", getMsgId());
        jsonMap.put("participants", getParticipants());
    }

    @Override
    public String toString() {
        return "[" + getTargetId() + "] " + getString();

    }

    public String getString() {
        return (getGlobalEventId() != null ? getGlobalEventId() + " " : "")
                + (getEventType() == EventType.SEND ? "SEND  " : "RCV   ")
                + "message " + msgId
                + (getEventType() == EventType.SEND ? " to " : " from ")
                + participants;
    }
}
