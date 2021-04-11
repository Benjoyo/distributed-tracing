package tracing.backend.source.elf;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Parses (lazily and cached) an ELF-file at the given path and provides information about functions and variables.
 */
public class ElfParser {

    private static final Pattern SYMBOL_PATTERN = Pattern.compile("\\s+\\d+:\\s([\\dabcdef]+)\\s+(\\d+)\\s+(\\w+)\\s+\\w+\\s+\\w+\\s+\\w+\\s(.*)");

    private final Path elfPath;

    /**
     * Parse ELF-file at the given path.
     * @param elfPath the ELF file path
     */
    public ElfParser(Path elfPath) {
        this.elfPath = elfPath;
    }

    // caches
    private final Map<Long, Function> addressToFunctionMap = new HashMap<>();
    private final Map<String, Symbol> nameToSymbolMap = new HashMap<>();
    private final Map<Long, Symbol> addressToSymbolMap = new HashMap<>();

    /**
     * Get information about the function at the address.
     * @param address the function address
     * @return function object
     */
    public Optional<Function> getFunction(long address) {
        if (addressToFunctionMap.containsKey(address)) {
            return Optional.of(addressToFunctionMap.get(address));
        }

        ProcessBuilder builder = new ProcessBuilder(
                "addr2line",
                "-f",
                "-e", elfPath.toAbsolutePath().toString(),
                "0x" + String.format("%08x", address)
        ).redirectErrorStream(true);

        try {
            Process process = builder.start();

            try (var in = process.getInputStream();
                 var scanner = new Scanner(in)) {

                if (scanner.hasNextLine()) {
                    var name = scanner.nextLine();
                    if (scanner.hasNextLine()) {
                        var s = scanner.nextLine().split(":");
                        var path = s[0];
                        var lineNum = Integer.parseInt(s[1].split(" ")[0]);
                        var function = new Function(name, path, lineNum, address);
                        addressToFunctionMap.put(address, function);
                        return Optional.of(function);
                    } else {
                        return Optional.empty();
                    }
                } else {
                    return Optional.empty();
                }
            }
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
        return Optional.empty();
    }

    /**
     * Get information about the symbol at the address.
     * @param address the symbol address
     * @return symbol object
     */
    public Optional<Symbol> getSymbol(long address) {
        if (addressToSymbolMap.isEmpty()) {
            parseSymbols();
        }

        if (addressToSymbolMap.containsKey(address)) {
            return Optional.of(addressToSymbolMap.get(address));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Get information about the symbol of the given name.
     * @param name the symbol name
     * @return symbol object
     */
    public Optional<Symbol> getSymbol(String name) {
        if (nameToSymbolMap.isEmpty()) {
            parseSymbols();
        }

        if (nameToSymbolMap.containsKey(name)) {
            return Optional.of(nameToSymbolMap.get(name));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Parse all relevant symbols using readelf.
     */
    private void parseSymbols() {
        ProcessBuilder builder = new ProcessBuilder(
                "readelf",
                "-s",
                elfPath.toAbsolutePath().toString()
        ).redirectErrorStream(true);

        try {
            Process process = builder.start();

            try (var in = process.getInputStream();
                 var scanner = new Scanner(in)) {

                while (scanner.hasNextLine()) {
                    var line = scanner.nextLine();
                    var matcher = SYMBOL_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        var name = matcher.group(4);
                        var address = Long.parseLong(matcher.group(1), 16);
                        var size = Long.parseLong(matcher.group(2));
                        var type = matcher.group(3);
                        if (type.equals("OBJECT")) {
                            var symbol = new Symbol(address, size, name);
                            nameToSymbolMap.put(name, symbol);
                            addressToSymbolMap.put(address, symbol);
                        }
                    }
                }

                System.out.println("[ElfParser] Loaded elf file with " + nameToSymbolMap.size() + " object symbols.");
            }
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }
}
