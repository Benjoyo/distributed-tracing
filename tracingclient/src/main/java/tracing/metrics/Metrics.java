package tracing.metrics;

import com.codahale.metrics.*;

import java.io.File;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Collects metrics about the observer/client in CSV files.
 */
public final class Metrics {

    private static final MetricRegistry metrics = new MetricRegistry();

    private static final Meter outputEvents = metrics.meter("outputEvents");
    private static final Meter inputEvents = metrics.meter("inputEvents");
    private static final Histogram queueSizes = new Histogram(new UniformReservoir());

    public static void startReport() {
        metrics.register("queueSizes", queueSizes);
        metrics.register("memoryUsageBytes", (Gauge<Long>) () -> {
            Runtime runtime = Runtime.getRuntime();
            return (runtime.totalMemory() - runtime.freeMemory());
        });

        var metricsDir = new File("metrics_current/");
        if (metricsDir.exists()) {
            metricsDir.renameTo(new File("metrics_" + UUID.randomUUID().toString() + "/"));
        }

        var reporter = ConsoleReporter.forRegistry(metrics)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();

        var csvReporter = CsvReporter.forRegistry(metrics)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build(new File("metrics_current/"));

        reporter.start(1, TimeUnit.SECONDS);
        csvReporter.start(1, TimeUnit.SECONDS);
    }

    public static void markOutputEvent() {
        outputEvents.mark();
    }

    public static void markInputEvent() {
        inputEvents.mark();
    }

    public static void updateQueueSize(int size) {
        queueSizes.update(size);
    }

    public static void registerQueue(Queue<?> queue) {
        metrics.register("Queue", (Gauge<Integer>) queue::size);
    }
}
