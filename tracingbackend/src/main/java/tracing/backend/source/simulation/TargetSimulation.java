package tracing.backend.source.simulation;

import tracing.backend.Target;
import tracing.backend.metrics.Metrics;
import tracing.backend.trace.InternalEvent;
import tracing.backend.trace.MessageEvent;
import tracing.backend.trace.TracePacket;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulates a target.
 */
public class TargetSimulation implements Runnable {

    private static final int SIMULATION_SLEEP_MS = 1000;

    private final Target target;
    private List<TargetSimulation> targetSimulations;
    private final BlockingQueue<Message> inbox = new LinkedBlockingQueue<>();
    private long localMsgSeqNum = 0;

    /**
     * Creates a simulation for a target.
     *
     * @param target the target
     */
    public TargetSimulation(Target target) {
        this.target = target;
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                // see if message arrived
                Message msg = inbox.poll();
                if (msg != null) {
                    // receive event
                    var r = new TracePacket();
                    r.setSubType(TracePacket.SUBTYPE_RECEIVE);
                    r.setMessageId(msg.msgId);
                    r.setTimestamp(System.currentTimeMillis());

                    var receiveEvent = new MessageEvent(target, r);
                    this.target.getTraceQueue().add(receiveEvent);

                    Metrics.markInputEvent(target.getTargetId(), receiveEvent.uuid);
                    Metrics.updateQueueSize(target.getTargetId(), target.getTraceQueue().size());

                    // decide if immediate reply should be sent
                    var reply = rand(0, 3);
                    if (reply == 1) {
                        this.sendMessageTo(msg.sender);
                    }
                }

                // decide if should send msg to random target or perform a local action
                var action = rand(0, 3);
                if (action == 0) { // send message to random target
                    var r = rand(0, this.targetSimulations.size());
                    var receiver = this.targetSimulations.get(r);
                    if (receiver != this) {
                        this.sendMessageTo(receiver);
                    }

                } else { // local action
                    var event = new InternalEvent(this.target, System.currentTimeMillis());
                    this.target.getTraceQueue().add(event);

                    Metrics.markInputEvent(target.getTargetId(), event.uuid);
                    Metrics.updateQueueSize(target.getTargetId(), target.getTraceQueue().size());
                }

                // wait random time
                Thread.sleep(rand(SIMULATION_SLEEP_MS / 4, SIMULATION_SLEEP_MS));

            } catch (InterruptedException e) {
                break; // stop
            }
        }
    }

    /**
     * Called by other targets to put their message into our inbox.
     *
     * @param msg the message to receive
     */
    void receiveMessage(Message msg) {
        this.inbox.add(msg);
    }

    /**
     * Send new message to another target and log a corresponding send event.
     *
     * @param receiver receiving target
     */
    private void sendMessageTo(TargetSimulation receiver) {
        var id = this.getCurrentMsgID();
        // send event
        var r = new TracePacket();
        r.setSubType(TracePacket.SUBTYPE_SEND);
        r.setMessageId(id);
        r.setTimestamp(System.currentTimeMillis());

        var sendEvent = new MessageEvent(target, r);
        this.target.getTraceQueue().add(sendEvent);

        Metrics.markInputEvent(target.getTargetId(), sendEvent.uuid);
        Metrics.updateQueueSize(target.getTargetId(), target.getTraceQueue().size());

        receiver.receiveMessage(new Message(this, id));
    }

    /**
     * Increment message sequence number and return new string message id.
     *
     * @return new string message id
     */
    private String getCurrentMsgID() {
        return this.target.getTargetId() + "_" + localMsgSeqNum++;
    }

    /**
     * Random number
     */
    private int rand(int from, int to) {
        return ThreadLocalRandom.current().nextInt(from, to);
    }

    public void setSimulatedTargets(List<TargetSimulation> targetSimulations) {
        this.targetSimulations = targetSimulations;
    }

    static class Message {
        final TargetSimulation sender;
        final String msgId;

        Message(TargetSimulation sender, String msgId) {
            this.sender = sender;
            this.msgId = msgId;
        }
    }
}
