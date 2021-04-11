package tracing.backend.source.elf;

/**
 * A symbol in the symbol table.
 */
public class Symbol {

    private final long address;
    private final long size;
    private final String name;

    public Symbol(long address, long size, String name) {
        this.address = address;
        this.size = size;
        this.name = name;
    }

    public long getAddress() {
        return address;
    }

    public long getSize() {
        return size;
    }

    public String getName() {
        return name;
    }
}
