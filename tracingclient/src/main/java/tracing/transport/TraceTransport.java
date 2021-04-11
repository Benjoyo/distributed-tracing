package tracing.transport;

/**
 * Interface for a transport implementation that connects to the server and allows sending of events.
 */
public interface TraceTransport extends Runnable {

    void close();
}
