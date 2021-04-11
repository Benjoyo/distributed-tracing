package tracing;

import org.apache.commons.cli.*;
import tracing.metrics.Metrics;
import tracing.source.TraceSource;
import tracing.source.orbuculum.OrbuculumITMSource;
import tracing.transport.TracePacket;
import tracing.transport.TraceTransport;
import tracing.transport.mqtt.MQTTTransport;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Tracing observer. The client part to the tracing server.
 */
public class TraceClient {

    // the source of trace events
    private TraceSource traceSource;
    // the transport implementation
    private TraceTransport traceTransport;
    // queue between source and transport
    private final BlockingQueue<TracePacket> traceQueue = new LinkedBlockingQueue<>();

    private CommandLine cmd;
    private boolean metrics;

    /**
     * Entry point.
     */
    public void start(String[] args) {
        this.parseOptions(args);

        // unique name of the target/observer pair, must match with config on server
        String deviceName;
        if (cmd.hasOption("n")) {
            deviceName = cmd.getOptionValue("n");
        } else {
            deviceName = "unnamed_device_" + UUID.randomUUID();
        }
        // server host/IP
        String serverHost;
        if (cmd.hasOption("s")) {
            serverHost = cmd.getOptionValue("s");
        } else {
            serverHost = "localhost";
        }
        // device type
        String device;
        if (cmd.hasOption("d")) {
            device = cmd.getOptionValue("d");
        } else {
            device = "NRF52840_XXAA";
        }
        metrics = cmd.hasOption("m");

        // whether to use FPGA tracing instead of SWO
        boolean fpga = cmd.hasOption("fpga");

        // use orbuculum and the ITM as trace source
        this.traceSource = new OrbuculumITMSource(traceQueue, device, fpga);

        // use MQTT for transport
        this.traceTransport = new MQTTTransport(traceQueue, deviceName, serverHost, this::onTraceStarted);
        new Thread(this.traceTransport).start();

        // make sure to close/stop everything on exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            this.traceSource.close();
            this.traceTransport.close();
        }));
    }

    /**
     * Called by trace transport as soon as it got a "go" message from the server.
     * @param config the config sent by the server
     */
    private void onTraceStarted(Config config) {
        // create watchpoints according to config
        for (int i = 0; i < config.getWatchpoints().size(); i++) {
            config.getWatchpointMap().put(i, config.getWatchpoints().get(i));
        }

        // start source with config
        this.traceSource.start(config);

        if (metrics) {
            Metrics.startReport();
        }
    }

    /**
     * Parse command line options
     * @param args
     */
    private void parseOptions(String[] args) {
        Options options = new Options();
        options.addOption("n", true, "device name (same as in server config)");
        options.addOption("fpga", false, "use FPGA for trace");
        options.addOption("s", true, "server IP address");
        options.addOption("m", false, "collect runtime metrics");
        options.addRequiredOption("d", "device", true, "device");
        CommandLineParser parser = new DefaultParser();
        try {
            cmd = parser.parse( options, args);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
