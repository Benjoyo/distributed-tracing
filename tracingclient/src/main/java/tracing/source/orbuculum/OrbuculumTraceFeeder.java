package tracing.source.orbuculum;

import tracing.Config;
import tracing.metrics.Metrics;
import tracing.transport.TracePacket;

import java.io.File;
import java.io.IOException;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Reads events from the output of a custom orbuculum tool.
 */
public class OrbuculumTraceFeeder {

    private final Queue<TracePacket> traceQueue;
    private final AtomicLong lastEventTime;
    private final Config config;
    private final File workingDir;
    private final boolean log;

    public OrbuculumTraceFeeder(Queue<TracePacket> traceQueue, AtomicLong lastEventTime, Config config, File workingDir, boolean log) {
        this.traceQueue = traceQueue;
        this.lastEventTime = lastEventTime;
        this.config = config;
        this.workingDir = workingDir;
        this.log = log; // log messages are handled differently, as they don't have a defined length and can contain new lines
    }

    public void start() {
        new Thread(() -> {
            ProcessBuilder builder = new ProcessBuilder(
                    "./trace",
                    log ? "-l" : "",
                    "-v", "1"
            ).redirectErrorStream(true);

            builder.directory(workingDir);

            try {
                // start trace tool
                Process process = builder.start();

                try (var in = process.getInputStream(); var scanner = new Scanner(in)) {

                    TracePacket funPacket = null;

                    while (scanner.hasNextLine()) {
                        // get event from program output
                        var event = scanner.nextLine();

                        Metrics.markInputEvent();

                        // create log event, if we should listen for log messages
                        if (log) {
                            var packet = new TracePacket();
                            packet.setType(TracePacket.TYPE_LOG);
                            packet.setLogMessage(event);
                            packet.setTimestamp(System.currentTimeMillis());

                            this.traceQueue.add(packet);
                            this.notifyEvent();

                            continue;
                        }

                        if (event.startsWith("ITM")) {
                            // overflow
                            var packet = new TracePacket();
                            packet.setType(TracePacket.TYPE_OVERFLOW);
                            packet.setTimestamp(System.currentTimeMillis());
                            this.traceQueue.add(packet);

                        } else {
                            if (event.startsWith("f")) { // function enter or exit
                                var parts = event.split(",");
                                var type = parts[1];
                                var fun = parts[2];

                                switch (type) {
                                    case "1": // enter #1
                                    case "3": // exit #1
                                        funPacket = new TracePacket();
                                        funPacket.setType(TracePacket.TYPE_FUNCTION);
                                        funPacket.setSubType(type.equals("1") ? TracePacket.SUBTYPE_ENTER : TracePacket.SUBTYPE_EXIT);
                                        funPacket.setTimestamp(System.currentTimeMillis());
                                        funPacket.setFunctionAddress(Long.parseLong(fun, 16));
                                        break;

                                    case "2": // enter #2
                                    case "4": // exit #2
                                        if (funPacket != null) {
                                            funPacket.setCallSiteAddress(Long.parseLong(fun, 16));
                                            this.traceQueue.add(funPacket);
                                        }
                                        break;
                                }
                            } else if (event.startsWith("d")) { // data event
                                var parts = event.split(",");
                                var comp = Integer.parseInt(parts[1]);
                                var write = parts[2].equals("w");
                                var value = parts[3];
                                var c = config.getWatchpoint(comp);

                                if (c != null) {
                                    var packet = new TracePacket();
                                    packet.setType(TracePacket.TYPE_MEMORY);
                                    packet.setSubType(write ? TracePacket.SUBTYPE_WRITE : TracePacket.SUBTYPE_READ);
                                    packet.setMemoryAddress(c.getAddress());
                                    packet.setMemoryValue(Long.parseLong(value, 16));
                                    packet.setTimestamp(System.currentTimeMillis());

                                    this.traceQueue.add(packet);
                                }

                            } else if (event.startsWith("m")) { // message event
                                var parts = event.split(",");
                                var send = parts[1].equals("1");
                                var msgId = parts[2];

                                var packet = new TracePacket();
                                packet.setType(TracePacket.TYPE_MESSAGE);
                                packet.setSubType(send ? TracePacket.SUBTYPE_SEND : TracePacket.SUBTYPE_RECEIVE);
                                packet.setMessageId(msgId);
                                packet.setTimestamp(System.currentTimeMillis());

                                this.traceQueue.add(packet);

                            } else {
                                // some other event
                                System.out.println("[!] Unknown event: " + event);
                            }
                        }
                        this.notifyEvent();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace(System.out);
            }

            System.out.println("Exit trace.");
        }).start();
    }

    /**
     * Remember last event time to decide when to send heartbeat.
     */
    private void notifyEvent() {
        this.lastEventTime.set(System.currentTimeMillis());
    }
}
