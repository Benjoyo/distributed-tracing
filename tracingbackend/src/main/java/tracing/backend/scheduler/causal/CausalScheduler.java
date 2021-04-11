package tracing.backend.scheduler.causal;

import tracing.backend.Target;
import tracing.backend.TraceQueue;
import tracing.backend.scheduler.EvictionService;
import tracing.backend.scheduler.QueueSpliterator;
import tracing.backend.scheduler.Scheduler;
import tracing.backend.trace.EventType;
import tracing.backend.trace.MessageEvent;
import tracing.backend.trace.TraceEvent;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Schedules an event order based on the causality of events, that is, following the happened-before relation.
 */
public class CausalScheduler implements Scheduler {

    public static final int EVICTION_INTERVAL = 5000;

    // all targets of the system
    private Collection<Target> targets;
    // maps message IDs to message events to remember send events
    private final HashMap<String, MessageEvent> sendMsgMap = new HashMap<>();
    // output queue
    public final BlockingQueue<TraceEvent> resultQueue = new LinkedBlockingQueue<>();
    // still running?
    private boolean running = true;
    // maps target IDs to input queues
    private final Map<String, TraceQueue> queueMap = new HashMap<>();
    // global sequence number assigned to events
    private final AtomicLong globalSeq = new AtomicLong(0);

    @Override
    public void setTargets(Collection<Target> targets) {
        this.targets = targets;
        targets.forEach(target -> queueMap.put(target.getTargetId(), target.getTraceQueue()));
    }

    @Override
    public void run() {

        AtomicLong operations = new AtomicLong(0);

        // while not stopped...
        while (running) {

            // try getting the oldest TraceEvent element from every trace, blocking if one trace is empty
            var candidates = this.targets.stream()
                    .map(t -> t.getTraceQueue().blockingPeek())
                    .sorted(Comparator.comparing(TraceEvent::getLocalTimestamp)) // sort by local timestamps to get initial order
                    .collect(Collectors.toList());

            // process candidates in order, breaking as soon as the first event can be processed
            for (var event : candidates) {
                if (event.getEventType() == EventType.SEND) {

                    var send = (MessageEvent) event;

                    // set vector clock
                    var clock = event.getTarget().incrementVectorClock();
                    event.setVectorClock(clock);

                    // add to send map to remember for receive event(s)
                    this.sendMsgMap.put(send.getMsgId(), send);

                } else if (event.getEventType() == EventType.RECEIVE) {

                    var receiveMsg = (MessageEvent) event;
                    var msgId = receiveMsg.getMsgId();

                    var sendMsg = this.sendMsgMap.get(msgId);
                    if (sendMsg == null) {
                        System.out.println("no send event to this receive event");

                        continue; // don't process receive yet
                    }

                    // receive event depends on send event
                    event.setDependency(sendMsg.getGlobalEventId());

                    sendMsg.addParticipant(event.getTargetId()); // set sender
                    receiveMsg.addParticipant(sendMsg.getTargetId()); // add receiver

                    // set vector clock
                    var senderClock = sendMsg.getVectorClock();
                    var clock = event.getTarget().merge(senderClock);
                    event.setVectorClock(clock);

                } else { // internal

                    // set vector clock
                    var clock = event.getTarget().incrementVectorClock();
                    event.setVectorClock(clock);
                }

                // remove processed event from input queue
                this.queueMap.get(event.getTargetId()).remove();

                // set global sequence number
                event.setGlobalEventId(globalSeq.incrementAndGet());

                // add to output
                this.resultQueue.add(event);

                // remember last event on target
                event.getTarget().setLastTraceEvent(event);

                if (operations.incrementAndGet() == EVICTION_INTERVAL) {
                    operations.set(0);
                    new EvictionService(targets, sendMsgMap).start();
                }

                // stop here, continue with next set of candidates
                break;
            }
        }
    }

    public Thread start() {
        var t = new Thread(this);
        t.start();
        return t;
    }

    @Override
    public void stop() {
        this.running = false;
    }

    /**
     * Create result/output stream from queue.
     * @return output stream
     */
    @Override
    public Stream<TraceEvent> resultStream() {
        return StreamSupport.stream(new QueueSpliterator<>(resultQueue), false);
    }
}




















