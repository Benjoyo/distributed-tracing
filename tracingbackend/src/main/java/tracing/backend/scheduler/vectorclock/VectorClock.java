package tracing.backend.scheduler.vectorclock;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * An immutable vector clock.
 */
public class VectorClock<K> {

    // the current clock values for all keys
    private final Map<K, Clock> map;

    /**
     * Creates a new vector clock for given keys with all values initialized to zero.
     * @param keys list of keys
     * @param <K> key type
     * @return new vector clock
     */
    public static <K> VectorClock<K> create(List<K> keys) {
        var map = new HashMap<K, Clock>();
        keys.forEach(k -> map.put(k, new Clock(0)));
        return VectorClock.create(map);
    }

    /**
     * Creates a vector clock from a string representation.
     * e.g. {a=0, b=12, c=3}
     */
    public static <K> VectorClock<K> fromString(String v, Function<String, K> keyFromString) {
        var map = new HashMap<K, Clock>();
        v = v.replaceAll("[{}]", "");
        Arrays.stream(v.split(","))
                .map(String::trim)
                .map(s -> s.split("="))
                .forEach(strings ->
                        map.put(keyFromString.apply(strings[0]),
                                new Clock(Integer.parseInt(strings[1]))
                        ));
        return VectorClock.create(map);
    }

    private VectorClock(Map<K, Clock> map) {
        this.map = map;
    }

    private static <K> VectorClock<K> create(Map<K, Clock> map) {
        return new VectorClock<>(map);
    }

    /**
     * Increment the clock component of the given key.
     * @param key the key
     * @return the new vector clock
     */
    public VectorClock<K> increment(K key) {
        SortedMap<K, Clock> clockTreeMap = new TreeMap<>(this.map);

        Clock clock = clockTreeMap.get(key);
        if (clock == null) {
            clock = new Clock(0);
        }

        clockTreeMap.put(key, clock.increment());
        return new VectorClock<>(clockTreeMap);
    }

    /**
     * Copies this clock.
     * @return copy
     */
    public VectorClock<K> copy() {
        SortedMap<K, Clock> dst = new TreeMap<>(this.map);
        return new VectorClock<>(dst);
    }

    public int size() {
        return this.map.size();
    }

    /**
     * Get the clock for given key.
     * @param key the key
     * @return the clock for the key or a new clock with initial value zero
     */
    public Clock get(K key) {
        if (this.map.containsKey(key)) {
            return this.map.get(key);
        } else {
            return new Clock(0);
        }
    }

    public Set<? extends Map.Entry<K, Clock>> entrySet() {
        return Collections.unmodifiableSet(this.map.entrySet());
    }

    public Set<K> keySet() {
        return Collections.unmodifiableSet(this.map.keySet());
    }

    public Collection<Clock> values() {
        return Collections.unmodifiableCollection(this.map.values());
    }

    public Map<K, Integer> toMap() {
        return map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getValue()));
    }

    /**
     * Whether this vector clock is causally after the given one.
     * @param that another vc
     * @return true if this is after
     */
    public boolean isAfter(VectorClock<K> that) {
        boolean anyClockGreater = false;

        for (K key : this.keySet()) {
            final Clock thatClock = that.get(key);
            final Clock thisClock = this.get(key);

            if (thisClock == null || thisClock.isBefore(thatClock)) {
                return false;
            } else if (thisClock.isAfter(thatClock)) {
                anyClockGreater = true;
            }
        }
        // there is at least one local timestamp greater or local vector clock has additional timestamps
        return anyClockGreater || that.entrySet().size() < entrySet().size();
    }

    /**
     * Merge this clock with the other and return the new clock.
     * @param other another vc
     * @return the merged clock
     */
    public VectorClock<K> merge(VectorClock<K> other) {
        SortedMap<K, Clock> dst = new TreeMap<>(this.map);

        for (Map.Entry<K, Clock> entry : other.entrySet()) {
            K key = entry.getKey();
            Clock clock = entry.getValue();

            Clock existing = dst.get(key);
            if (existing != null) {
                clock = existing.max(clock);
            }

            dst.put(key, clock);
        }
        return new VectorClock<K>(dst);
    }

    @Override
    public String toString() {
        return this.map.toString();
    }
}
