package tracing.backend.configuration;

import com.google.gson.Gson;
import tracing.backend.Target;
import tracing.backend.source.elf.ElfParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Parses target definitions from a JSON file.
 */
public class JsonTargetProvider implements TargetProvider {

    public static final String TRACE_CONFIG_JSON = "trace_config.json";

    // singleton instance
    private static JsonTargetProvider instance;

    /**
     * Singleton accessor.
     * @return the instance
     */
    public static JsonTargetProvider getInstance() {
        if (instance == null) {
            instance = new JsonTargetProvider();
        }
        return instance;
    }

    /**
     * Private constructor that loads the configuration when the singleton instance is first requested.
     */
    private JsonTargetProvider() {
        try {
            this.loadConfiguration();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Config config;
    private final Gson gson = new Gson();
    // maps elf file names to elf parser instances (often many or all targets have the same binaries, so we cache the elf parsers)
    private final Map<String, ElfParser> elfMap = new HashMap<>();
    // maps target IDs to elf parser instances
    private final Map<String, ElfParser> targetIdToElfParserMap = new HashMap<>();
    // maps target IDs to targets
    private final Map<String, Target> targetIdToTargetMap = new HashMap<>();

    public void loadConfiguration() throws IOException {

        var path = Paths.get(".", TRACE_CONFIG_JSON);
        if (!path.toFile().exists()) {
            System.err.println("Config file not found!");
            System.exit(0);
        }

        // parse JSON config file
        config = this.gson.fromJson(Files.readString(path), Config.class);
        List<String> targetNames = config.getTargetConfigs().stream().map(Config.TargetConfig::getName).collect(Collectors.toList());

        // process all defined targets
        config.getTargetConfigs().forEach(targetConfig -> {

            // create a new target
            var name = targetConfig.getName();
            var target = new Target(name, targetNames);
            targetIdToTargetMap.put(name, target);

            // get an elf parser for the target's binary
            var elfPath = targetConfig.getElfPathString();
            ElfParser elfParser;
            if (elfMap.containsKey(elfPath)) {
                elfParser = elfMap.get(elfPath);
            } else {
                elfParser = new ElfParser(targetConfig.getElfPath());
                elfMap.put(elfPath, elfParser);
            }
            targetIdToElfParserMap.put(targetConfig.getName(), elfParser);

            // resolve variable names to addresses and sizes to create watchpoints
            targetConfig.getWatchedVars().forEach(var -> {
                var symbolOpt = elfParser.getSymbol(var);
                symbolOpt.ifPresent(symbol -> {
                    System.out.println("[Config] Watch variable " + var + " on target " + targetConfig.getName());
                    targetConfig.addMemoryWatchpoint(symbol.getAddress(), symbol.getSize());
                });
            });
        });
    }

    @Override
    public Collection<Target> getTargets() {
        return targetIdToTargetMap.values();
    }

    public boolean hasTarget(String targetId) {
        return targetIdToTargetMap.containsKey(targetId);
    }

    public Target getTarget(String targetId) {
        return targetIdToTargetMap.get(targetId);
    }

    public Collection<Config.TargetConfig> getTargetConfigs() {
        return config.getTargetConfigs();
    }

    public ElfParser getElfParserByTargetId(String targetId) {
        return targetIdToElfParserMap.get(targetId);
    }
}
