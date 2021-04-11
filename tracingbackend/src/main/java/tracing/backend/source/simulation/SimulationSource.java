package tracing.backend.source.simulation;

import tracing.backend.configuration.SimulationTargetProvider;
import tracing.backend.configuration.TargetProvider;
import tracing.backend.metrics.Metrics;
import tracing.backend.source.TraceSource;

import java.util.ArrayList;

/**
 * A trace source that simulates targets and how they produce events.
 */
public class SimulationSource implements TraceSource {

    private final ArrayList<Thread> targetSimulationThreads = new ArrayList<>();
    private final ArrayList<TargetSimulation> targetSimulations = new ArrayList<>();
    private final SimulationTargetProvider configProvider;

    /**
     * Create simulation with numTargets targets.
     * @param numTargets number of targets
     */
    public SimulationSource(int numTargets) {
        this.configProvider = new SimulationTargetProvider(numTargets);
    }

    @Override
    public void init() {

        // create simulation for every target
        this.configProvider.getTargets().forEach(target -> {
            var targetSim = new TargetSimulation(target);
            this.targetSimulations.add(targetSim);
            this.targetSimulationThreads.add(new Thread(targetSim));

            Metrics.registerInputQueue(target.getTargetId(), target.getTraceQueue());
        });

        this.targetSimulations.forEach(s -> s.setSimulatedTargets(this.targetSimulations));

        this.startSimulation();

        Metrics.startReport();
    }

    @Override
    public void close() {
        this.stopSimulation();
    }

    @Override
    public TargetProvider getTargetProvider() {
        return this.configProvider;
    }

    public void startSimulation() {
        this.targetSimulationThreads.forEach(Thread::start);
    }

    public void stopSimulation() {
        this.targetSimulationThreads.forEach(Thread::interrupt);
    }
}
