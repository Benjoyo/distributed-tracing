package tracing.backend.source.elf;

/**
 * A function in the symbol table.
 */
public class Function {

    // name of the function
    private final String name;
    // file path of the function
    private final String file;
    // line number of function
    private final int lineNumber;
    // raw address
    private final long address;

    public Function(String name, String file, int lineNumber, long address) {
        this.name = name;
        this.file = file;
        this.lineNumber = lineNumber;
        this.address = address;
    }

    public long getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }

    public String getFile() {
        return file;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getFileAndLineNumber() {
        return file + ":" + lineNumber;
    }
}
