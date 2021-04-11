package tracing.backend.scheduler;

import tracing.backend.Target;
import tracing.backend.trace.MessageEvent;

import java.util.HashMap;

/**
 * Removes unnecessary events from the send message map. If events from all targets have vector clocks that are after
 * a send event in the map, then that event is not required anymore.
 */
public class EvictionService implements Runnable {

    private final Iterable<Target> targets;
    private final HashMap<String, MessageEvent> sendMsgMap;

    public EvictionService(Iterable<Target> targets, HashMap<String, MessageEvent> sendMsgMap) {
        this.targets = targets;
        this.sendMsgMap = sendMsgMap;
    }

    @Override
    public void run() {
        System.err.println("Before eviction: " + sendMsgMap.size());

        this.sendMsgMap.entrySet().removeIf(entry -> {
            for (Target target : targets) {
                if (!target.getLastTraceEvent().vectorClockIsAfter(entry.getValue())) {
                    return false;
                }
            }
            return true;
        });

        System.err.println("After eviction: " + sendMsgMap.size());
    }

    public void start() {
        new Thread(this).start();
    }
}
