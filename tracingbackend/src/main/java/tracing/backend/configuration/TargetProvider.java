package tracing.backend.configuration;

import tracing.backend.Target;

import java.util.Collection;

/**
 * A target provider provides the definitions of all traced targets.
 * The implementation can be specific for the trace source.
 */
public interface TargetProvider {

    Collection<Target> getTargets();
}
