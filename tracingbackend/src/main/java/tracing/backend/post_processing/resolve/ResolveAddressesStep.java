package tracing.backend.post_processing.resolve;

import tracing.backend.configuration.JsonTargetProvider;
import tracing.backend.post_processing.PostProcessingStep;
import tracing.backend.trace.FunctionEvent;
import tracing.backend.trace.MemoryEvent;
import tracing.backend.trace.TraceEvent;

/**
 * Resolves addresses of functions and variables and adds information from the symbol table to the event.
 */
public class ResolveAddressesStep implements PostProcessingStep {

    @Override
    public void accept(TraceEvent traceEvent) {
        this.resolveAddresses(traceEvent);
    }

    /**
     * Resolves function and memory addresses.
     * @param traceEvent the event to process
     */
    private void resolveAddresses(TraceEvent traceEvent) {
        var elfParser = JsonTargetProvider.getInstance().getElfParserByTargetId(traceEvent.getTargetId());
        if (traceEvent instanceof FunctionEvent) {
            var t = (FunctionEvent) traceEvent;
            elfParser.getFunction(t.getFunctionAddress()).ifPresent(t::setCalleeFunction);
            elfParser.getFunction(t.getCallSiteAddress()).ifPresent(t::setCallerFunction);
        } else if (traceEvent instanceof MemoryEvent) {
            var t = (MemoryEvent) traceEvent;
            elfParser.getSymbol(t.getAddress()).ifPresent(t::setSymbol);
        }
    }
}
