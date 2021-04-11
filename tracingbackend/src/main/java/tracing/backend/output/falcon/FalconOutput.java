package tracing.backend.output.falcon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import tracing.backend.output.TraceOutput;
import tracing.backend.trace.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * TraceOutput that writes events to a file falcon_trace.json in a format that the Falcon visualizer understands.
 *
 * @see <a href="https://github.com/fntneves/falcon">https://github.com/fntneves/falcon</a>
 */
public class FalconOutput implements TraceOutput {

    public static final int WEBSOCKET_PORT = 9889;

    private final List<FalconEvent> events = new ArrayList<>();
    private final boolean websocket;
    private Gson gson;
    private PrintWriter writer;
    private WebSocketServer webSocketServer;

    /**
     * Creates a FalconOutput that either writes to a WebSocket or a json file.
     * @param websocket whether output should be written to a WebSocket
     */
    public FalconOutput(boolean websocket) {
        this.websocket = websocket;
    }

    /**
     * Creates a WebSocket server or a json file, depending on the constructor argument.
     */
    @Override
    public void init() {

        GsonBuilder builder = new GsonBuilder();
        builder.serializeNulls();
        this.gson = builder.create();

        if (websocket) { // websocket output
            this.webSocketServer = new WebSocketServer(WEBSOCKET_PORT);
            this.webSocketServer.start();

            this.webSocketServer.setCloseListener(() -> {
                this.webSocketServer = new WebSocketServer(WEBSOCKET_PORT);
                this.webSocketServer.start();
            });

        } else { // json file output
            File output = new File("falcon_trace.json");
            try {
                writer = new PrintWriter(new FileWriter(output));
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }

    @Override
    public void close() {
        if (websocket) {
            try {
                this.webSocketServer.stop();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            var json = this.gson.toJson(this.events);
            this.writer.write(json);
            this.writer.close();
        }
    }

    /**
     * Serializes an event to the falcon format and puts it into a WebSocket connection or file.
     * @param traceEvent the event to output
     */
    @Override
    public void put(TraceEvent traceEvent) {
        var event = new FalconEvent();

        event.type = this.getFalconEventType(traceEvent);

        if (traceEvent instanceof LogEvent) {
            event.data = new Data(((LogEvent) traceEvent).getLogMessage());
        } else if (traceEvent instanceof MessageEvent) {
            event.data = new Data(((MessageEvent) traceEvent).getMsgId());
        } else if (traceEvent instanceof MemoryEvent) {
            event.data = new Data(((MemoryEvent) traceEvent).getString());
        } else if (traceEvent instanceof FunctionEvent) {
            event.data = new Data(((FunctionEvent) traceEvent).getString());
        } else {
            event.data = new Data((traceEvent.toString()));
        }

        if (traceEvent.getEventType() == EventType.SEND && traceEvent instanceof MessageEvent) {
            var s = (MessageEvent) traceEvent;
            event.src = s.getTargetId();
            event.dst = s.getParticipants().toString();
        }

        if (traceEvent.getEventType() == EventType.RECEIVE && traceEvent instanceof MessageEvent) {
            var s = (MessageEvent) traceEvent;
            event.dst = s.getTargetId();
            event.src = s.getParticipants().toString();
        }

        event.thread = "0@" + traceEvent.getTargetId();
        event.loc = "";
        event.order = traceEvent.getGlobalEventId();
        event.id = traceEvent.getGlobalEventId();
        event.timestamp = traceEvent.getLocalTimestamp().toString();
        event.dependency = traceEvent.getDependencies() != null ? traceEvent.getDependencies().toString() : null;

        if (websocket) {
            this.webSocketServer.send(this.gson.toJson(event));
        } else {
            this.events.add(event);
        }
    }

    /**
     * Returns an event type that falcon understands.
     * @param event the event
     * @return the falcon event type
     */
    private String getFalconEventType(TraceEvent event) {
        switch (event.getEventType()) {
            case SEND:
                return "SND";
            case RECEIVE:
                return "RCV";
            default:
                return event.getClass().getSimpleName().replaceAll("Event", "").toUpperCase();
        }
    }

    /**
     * Structure of a falcon event.
     */
    private static class FalconEvent {
        String type;
        String thread;
        String loc = "";
        long order;
        long id;
        String src;
        String dst;
        String timestamp;
        String dependency;
        Data data;
    }

    /**
     * Optional data field.
     */
    private static class Data {
        String message;
        public Data(String message) {
            this.message = message;
        }
    }

    @Override
    public void putRaw(TracePacket tracePacket) { }
}
