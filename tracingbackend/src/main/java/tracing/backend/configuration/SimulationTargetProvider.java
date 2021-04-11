package tracing.backend.configuration;

import tracing.backend.Target;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Creates a number of virtual target definitions.
 */
public class SimulationTargetProvider implements TargetProvider {

    private final ArrayList<Target> targets = new ArrayList<>();

    public SimulationTargetProvider(int numTargets) {

        var targetIds = IntStream.range(0, numTargets).mapToObj(String::valueOf).collect(Collectors.toList());

        targetIds.forEach(targetId -> {
            var target = new Target(targetId, targetIds);
            this.targets.add(target);
        });
    }

    @Override
    public Collection<Target> getTargets() {
        return this.targets;
    }
}
