package tracing;

import tracing.trace.MemoryWatchpoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Config as sent by the server.
 */
public class Config {

    private String name;
    private final List<String> watched_vars = new ArrayList<>();

    private final List<MemoryWatchpoint> watchpoints = new ArrayList<>();

    private final transient Map<Integer, MemoryWatchpoint> watchpointMap = new HashMap<>();

    public MemoryWatchpoint getWatchpoint(int comparator) {
        return this.watchpointMap.get(comparator);
    }

    public List<String> getWatchedVars() {
        return watched_vars;
    }

    public String getName() {
        return name;
    }

    public List<MemoryWatchpoint> getWatchpoints() {
        return watchpoints;
    }

    public Map<Integer, MemoryWatchpoint> getWatchpointMap() {
        return watchpointMap;
    }
}