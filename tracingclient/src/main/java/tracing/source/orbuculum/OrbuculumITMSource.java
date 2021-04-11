package tracing.source.orbuculum;

import tracing.Config;
import tracing.source.TraceSource;
import tracing.transport.TracePacket;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Trace source that uses the ITM, DWT and orbuculum to capture traces.
 */
public class OrbuculumITMSource implements TraceSource {

    public static final int HEARTBEAT_INTERVAL = 100;

    // output queue
    private final BlockingQueue<TracePacket> traceQueue;
    private final String device;
    private final boolean fpga;

    public OrbuculumITMSource(BlockingQueue<TracePacket> traceQueue, String device, boolean fpga) {
        this.traceQueue = traceQueue;
        this.device = device;
        this.fpga = fpga;
    }

    @Override
    public void start(Config config) {
        var workingDir = new File(System.getProperty("user.dir"));

        // create a gdb init script from a template
        System.out.println("Create swo.gdbinit...");
        InitFileHelper.writeGDBInitFile(workingDir, config);

        // create a shell script to start tracing
        System.out.println("Create trace-init.sh...");
        InitFileHelper.writeTraceInitFile(workingDir, fpga);

        // start tracing by running shell script
        System.out.println("Run trace-init.sh...");
        try {
            Runtime.getRuntime().exec("sh trace-init.sh", null, workingDir).waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        // remember last time an event happened
        var lastEventTime = new AtomicLong(System.currentTimeMillis());

        // start reading the orbuculum output
        System.out.println("Start trace capturing...");
        new OrbuculumTraceFeeder(traceQueue, lastEventTime, config, workingDir, false).start();

        System.out.println("Start log message capturing...");
        new OrbuculumTraceFeeder(traceQueue, lastEventTime, config, workingDir, true).start();

        // send heartbeats if time between events is too long
        new Thread(() -> {
            while (true) {
                if ((System.currentTimeMillis() - lastEventTime.get()) > HEARTBEAT_INTERVAL) {

                    // heartbeat packet
                    var packet = new TracePacket();
                    packet.setTimestamp(System.currentTimeMillis());

                    this.traceQueue.add(packet);

                    lastEventTime.set(System.currentTimeMillis());

                }
                try {
                    Thread.sleep(HEARTBEAT_INTERVAL / 4);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void close() {
    }
}
