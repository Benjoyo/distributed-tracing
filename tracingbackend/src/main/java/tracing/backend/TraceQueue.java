package tracing.backend;

import tracing.backend.trace.TraceEvent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * Queue of trace events that allows a blocking peek operation.
 */
public class TraceQueue {

    private final Semaphore available = new Semaphore(0, false);
    private final BlockingQueue<TraceEvent> queue = new LinkedBlockingQueue<>();

    /**
     * Retrieves, but does not remove, the head of this queue, or blocks if the queue is empty.
     * @return the head of this queue
     */
    public TraceEvent blockingPeek() {
        try {
            // potentially block until at least one element is in the queue, counts semaphore down by one
            this.available.acquire();
            // count back up because element is not yet removed
            this.available.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
        return this.queue.peek();
    }

    /**
     * Removes the head of the queue (oldest event in the trace of a specific target)
     */
    public void remove() {
        try {
            // count down by one because element is removed. Should not block here, as blockingPeek() should have been called first
            this.available.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // remove head of queue (oldest event in the trace of a specific target)
        this.queue.remove();
    }

    /**
     * Inserts the specified event into this queue.
     * @param traceEvent the event to add
     */
    public void add(TraceEvent traceEvent) {
        this.queue.add(traceEvent);
        // indicate that there is a an element in the queue, potentially unblocking a waiting thread
        this.available.release();
    }

    /**
     * Returns the number of events in this queue.
     * @return the number of events in this queue
     */
    public int size() {
        return this.queue.size();
    }
}
