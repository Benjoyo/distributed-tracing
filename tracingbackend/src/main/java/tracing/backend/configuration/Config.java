package tracing.backend.configuration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON config definition.
 */
public class Config {

    private final List<TargetConfig> targetConfigs = new ArrayList<>();

    public List<TargetConfig> getTargetConfigs() {
        return targetConfigs;
    }

    public static class TargetConfig {

        private String name;
        private String elf_path;
        private final List<String> watched_vars = new ArrayList<>();
        private final List<MemoryWatchpoint> watchpoints = new ArrayList<>();

        public String getElfPathString() {
            return elf_path;
        }

        public Path getElfPath() {
            return Path.of(elf_path);
        }

        public List<String> getWatchedVars() {
            return watched_vars;
        }

        public void addMemoryWatchpoint(Long address, Long size) {
            this.watchpoints.add(new MemoryWatchpoint(address, size));
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return "TargetConfig{" +
                    "name='" + name + '\'' +
                    ", watched_vars=" + watched_vars +
                    ", watchpoints=" + watchpoints +
                    '}';
        }

        public static class MemoryWatchpoint {
            private final Long address;
            private final Long size;

            public MemoryWatchpoint(Long address, Long size) {
                this.address = address;
                this.size = size;
            }
        }
    }
}
