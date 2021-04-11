package tracing.backend.metrics;

import com.codahale.metrics.*;
import tracing.backend.TraceQueue;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Collects metrics about the server.
 */
public final class Metrics {

    private static final MetricRegistry metrics = new MetricRegistry();

    // tracks the time events spend in the server
    private static final Timer timer = metrics.timer("event_pipeline");
    private static final Map<String, Timer.Context> eventTimerContexts = new HashMap<>();
    // rate of output
    private static final Meter output = metrics.meter("sink_output");
    // rate of scheduler output
    private static final Meter schedulerOutput = metrics.meter("scheduler_output");
    // input queue sizes
    private static final Map<String, Histogram> queueHistograms = new HashMap<>();
    // rate of input queues
    private static final Map<String, Meter> eventMeters = new HashMap<>();
    // scheduler output queue sizes
    private static final Histogram schedulerQueueHistogram = new Histogram(new UniformReservoir());

    /**
     * Starts metric collection and reporting. CSV files are stored in a metrics folder.
     */
    public static void startReport() {
        metrics.register("scheduler_histogram", schedulerQueueHistogram);
        metrics.register("memoryUsageBytes", (Gauge<Long>) () -> {
            Runtime runtime = Runtime.getRuntime();
            return (runtime.totalMemory() - runtime.freeMemory());
        });

        var reporter = ConsoleReporter.forRegistry(metrics)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();

        var f = new File("metrics/");
        if (!f.exists()) {
           f.mkdir();
        }

        var csvReporter = CsvReporter.forRegistry(metrics)
                .formatFor(Locale.US)
                .convertRatesTo(TimeUnit.SECONDS)
                .withSeparator(";")
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build(f);

        //reporter.start(10, TimeUnit.SECONDS);
        csvReporter.start(1, TimeUnit.SECONDS);
    }

    public static void markSchedulerEvent() {
        schedulerOutput.mark();
    }

    public static void markInputEvent(String name, String eventUUID) {
        eventMeters.get(name).mark();
        startInputEventTimer(eventUUID);
    }

    public static void markSinkEvent(String eventUUID) {
        output.mark();
        stopInputEventTimer(eventUUID);
    }

    public static void startInputEventTimer(String eventUUID) {
        synchronized (eventTimerContexts) {
            eventTimerContexts.put(eventUUID, timer.time());
        }
    }

    public static void stopInputEventTimer(String eventUUID) {
        synchronized (eventTimerContexts) {
            try {
               eventTimerContexts.get(eventUUID).stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        eventTimerContexts.remove(eventUUID);
    }

    public static void updateQueueSize(String name, int size) {
        try {
            queueHistograms.get(name).update(size);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateSchedulerQueueSize(int size) {
        schedulerQueueHistogram.update(size);
    }

    public static void registerInputQueue(String name, TraceQueue queue) {
        metrics.register(name + "_input_gauge", (Gauge<Integer>) queue::size);
        var histogram = new Histogram(new ExponentiallyDecayingReservoir(100000, 0.015));
        metrics.register(name + "_input_histogram", histogram);

        queueHistograms.put(name, histogram);
        eventMeters.put(name, metrics.meter(name + "_input_meter"));
    }
}
