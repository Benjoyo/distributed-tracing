package tracing.backend.output.shiviz;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import tracing.backend.output.TraceOutput;
import tracing.backend.trace.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * TraceOutput that writes events to a file shiviz_trace.txt in a format that the ShiViz visualizer understands.
 *
 * @see <a href="https://bestchai.bitbucket.io/shiviz/">https://bestchai.bitbucket.io/shiviz/</a>
 */
public class ShiVizOutput implements TraceOutput {

    // the regex of our output format, ShiViz needs it to understand the format
    private static final String REGEX = "(?<host>\\w+)\\|(?<event>.*)\\|(?<clock>\\{.*\\})";

    private Gson gson;
    private PrintWriter writer;

    @Override
    public void init() {

        GsonBuilder builder = new GsonBuilder();
        builder.serializeNulls();
        this.gson = builder.create();

        File output = new File("shiviz_trace.txt");
        try {
            writer = new PrintWriter(new FileWriter(output));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        writer.println(REGEX);
        writer.println();
    }

    @Override
    public void close() {
        this.writer.close();
    }

    /**
     * Writes an event as a line into the output file.
     * @param traceEvent the event
     */
    @Override
    public void put(TraceEvent traceEvent) {

        var host = traceEvent.getTargetId();
        var clock = gson.toJson(traceEvent.getVectorClock().toMap());
        var eventDesc = "";

        if (traceEvent instanceof LogEvent) {
            eventDesc = ((LogEvent) traceEvent).getLogMessage();
        } else if (traceEvent instanceof MessageEvent) {
            eventDesc = ((MessageEvent) traceEvent).getString();
        } else if (traceEvent instanceof MemoryEvent) {
            eventDesc = ((MemoryEvent) traceEvent).getString();
        } else if (traceEvent instanceof FunctionEvent) {
            eventDesc = ((FunctionEvent) traceEvent).getString();
        } else {
            eventDesc = traceEvent.toString();
        }

        writer.println(host + "|" + eventDesc + "|" + clock);
    }

    @Override
    public void putRaw(TracePacket tracePacket) { }
}
