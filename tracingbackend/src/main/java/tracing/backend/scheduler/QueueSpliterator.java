package tracing.backend.scheduler;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

/**
 * Stream spliterator for a BlockingQueue.
 * @param <T> the data type in the queue
 */
public class QueueSpliterator<T> extends Spliterators.AbstractSpliterator<T> {

    static private final int DEFAULT_CHARACTERISTICS = Spliterator.CONCURRENT | Spliterator.NONNULL | Spliterator.ORDERED;

    private final BlockingQueue<T> queue;

    public QueueSpliterator(BlockingQueue<T> queue) {
        super(Long.MAX_VALUE, DEFAULT_CHARACTERISTICS);
        this.queue = queue;
    }

    @Override
    public boolean tryAdvance(final Consumer<? super T> action) {
        try {
            final T next = this.queue.take();
            action.accept(next);
            return true;
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }
}
