package tracing.backend;

import org.apache.commons.cli.*;
import tracing.backend.metrics.Metrics;
import tracing.backend.output.TraceOutput;
import tracing.backend.output.elk.ElasticsearchOutput;
import tracing.backend.output.falcon.FalconOutput;
import tracing.backend.output.other.NullOutput;
import tracing.backend.output.other.PrintOutput;
import tracing.backend.output.shiviz.ShiVizOutput;
import tracing.backend.post_processing.PostProcessingStep;
import tracing.backend.post_processing.print.PrintStep;
import tracing.backend.post_processing.resolve.ResolveAddressesStep;
import tracing.backend.scheduler.causal.CausalScheduler;
import tracing.backend.source.TraceSource;
import tracing.backend.source.mqtt.MQTTSource;
import tracing.backend.source.simulation.SimulationSource;

import java.util.ArrayList;
import java.util.List;

public class TraceServer {

    private TraceSource traceSource;
    private final List<PostProcessingStep> postProcessingSteps = new ArrayList<>();
    private TraceOutput traceOutput;
    private CommandLine cmd;

    /**
     * Entry point
     */
    public void start(String[] args) {
        this.parseOptions(args);

        // trace source selection, default: MQTTSource
        if (cmd.hasOption("s")) {
            switch (cmd.getOptionValue("s")) {
                case "sim":
                    System.out.println("Trace source: Simulation");
                    traceSource = new SimulationSource(4);
                    break;
                case "mqtt":
                    System.out.println("Trace source: MQTT");
                    traceSource = new MQTTSource(traceOutput);
                    break;
                default:
                    throw new IllegalArgumentException(cmd.getOptionValue("s"));
            }
        } else {
            System.out.println("Default trace source: MQTTSource");
            traceSource = new MQTTSource(traceOutput);
        }

        // trace post-processing steps selection, default: ResolveAddressesStep and PrintStep
        if (cmd.hasOption("p")) {
            for (String optionValue : cmd.getOptionValues("p")) {
                switch (optionValue) {
                    case "print":
                        postProcessingSteps.add(new PrintStep());
                        break;
                    case "resolve":
                        postProcessingSteps.add(new ResolveAddressesStep());
                        break;
                    default:
                        throw new IllegalArgumentException(optionValue);
                }
            }
        }

        // trace output selection, default: NullOutput (no output)
        if (cmd.hasOption("o")) {
            switch (cmd.getOptionValue("o")) {
                case "null":
                    System.out.println("Trace output: none");
                    traceOutput = new NullOutput();
                    break;
                case "print":
                    System.out.println("Trace output: print");
                    traceOutput = new PrintOutput();
                    break;
                case "elk":
                    System.out.println("Trace output: ELK");
                    traceOutput = new ElasticsearchOutput(true);
                    break;
                case "falcon":
                    System.out.println("Trace output: Falcon");
                    traceOutput = new FalconOutput(true);
                    break;
                case "shiviz":
                    System.out.println("Trace output: ShiViz");
                    traceOutput = new ShiVizOutput();
                    break;
                default:
                    throw new IllegalArgumentException(cmd.getOptionValue("o"));
            }
        } else {
            traceOutput = new NullOutput();
        }

        // initialize trace source and output
        traceSource.init();
        traceOutput.init();

        // get the trace configuration provider from the source
        // different source implementations may have different ways to define the traced targets
        var configurationProvider = traceSource.getTargetProvider();

        // start the causal scheduler
        var scheduler = new CausalScheduler();
        // get list of targets from the configuration provider of the source
        scheduler.setTargets(configurationProvider.getTargets());
        scheduler.start();

        // make sure to close/stop the trace source and output on exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            traceSource.close();
            traceOutput.close();
        }));

        // collect metrics at scheduler output
        var stream = scheduler.resultStream()
                .peek(t -> {
                    Metrics.markSchedulerEvent();
                    Metrics.updateSchedulerQueueSize(scheduler.resultQueue.size());
                });

        // execute all post-processing steps on events that passed the scheduler
        for (var postProcessingStep : postProcessingSteps) {
            stream = stream.peek(postProcessingStep);
        }

        // output
        stream.peek(t -> Metrics.markSinkEvent(t.uuid))
                .forEach(traceOutput::put);
    }

    /**
     * Parse command line options.
     * @param args
     */
    private void parseOptions(String[] args) {
        Options options = new Options();
        options.addOption("s", "source", true, "trace source (sim/mqtt)");
        var interOption = new Option("p", "post", true, "post-processing (print/resolve)");
        interOption.setArgs(Option.UNLIMITED_VALUES);
        options.addOption(interOption);
        options.addOption("o", "output", true, "trace sink/output (null/print/elk/falcon/shiviz)");
        CommandLineParser parser = new DefaultParser();
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
