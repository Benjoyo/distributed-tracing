package tracing.source.orbuculum;

import tracing.Application;
import tracing.Config;
import tracing.trace.MemoryWatchpoint;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Creates a gdb init file or start shell script from templates.
 */
public class InitFileHelper {

    private static final String GDB_INIT_FILE = "swo.gdbinit";
    private static final String GDB_INIT_TEMPLATE = "swo.gdbinit.template";
    public static final String DWT_CONFIG_PLACEHOLDER = "{{ DWT_CONFIG }}";

    public static final String TRACE_INIT_SH_TEMPLATE = "trace-init.sh.template";
    public static final String TRACE_INIT_SH = "trace-init.sh";

    /**
     * Creates a gdb init file for the given configuration (variables to watch) from the template.
     * @param workingDir dir to write to
     * @param config config
     */
    public static void writeGDBInitFile(File workingDir, Config config) {

        var dwtConfig = config.getWatchpointMap()
                .entrySet().stream()
                .map(InitFileHelper::toDWTConfig)
                .collect(Collectors.joining("\n"));

        try {
            var bytes = Application.class.getResourceAsStream("/" + GDB_INIT_TEMPLATE).readAllBytes();
            var content = new String(bytes).replace(DWT_CONFIG_PLACEHOLDER, dwtConfig);
            var p = Paths.get(workingDir.getPath(), GDB_INIT_FILE);
            Files.write(p, content.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns a gdb command for the memory watchpoint.
     * @param entry
     * @return gdb DWT config command
     */
    private static String toDWTConfig(Map.Entry<Integer, MemoryWatchpoint> entry) {
        return "dwtComp " + entry.getKey() + " 0x" + String.format("%08x", entry.getValue().getAddress()) + " " + (entry.getValue().getSize() / 2);
    }

    /**
     * Creates a shell script to start tracing from the template.
     * @param workingDir dir to write to
     * @param fpga whether to use FPGA tracing
     */
    public static void writeTraceInitFile(File workingDir, boolean fpga) {
        try {
            var bytes = Application.class.getResourceAsStream("/" + TRACE_INIT_SH_TEMPLATE).readAllBytes();
            var content = new String(bytes);

            if (fpga) {
                // 4 bit FPGA tracing
                content = content.replace("-s localhost:2332", "-p /dev/ttyUSB1 -o 4");
            }

            var p = Paths.get(workingDir.getPath(), TRACE_INIT_SH);
            Files.write(p, content.getBytes());
            var res = p.toFile().setExecutable(true);
            if (!res) {
                System.err.println("Failed to make " + TRACE_INIT_SH + " executable!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
