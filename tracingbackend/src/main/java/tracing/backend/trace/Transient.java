package tracing.backend.trace;

/**
 * Marker interface for trace events that shouldn't be included in the final output (but are required for technical reasons or debugging).
 */
public interface Transient { }
