package tracing.trace;

/**
 * Defines a memory watchpoint.
 */
public class MemoryWatchpoint {

    private final long address;
    private final long size;

    public MemoryWatchpoint(long address, long size) {
        this.address = address;
        this.size = size;
    }

    public long getAddress() {
        return address;
    }

    public long getSize() {
        return size;
    }
}
