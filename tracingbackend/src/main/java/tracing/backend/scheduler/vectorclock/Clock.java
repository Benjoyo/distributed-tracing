package tracing.backend.scheduler.vectorclock;

/**
 * An immutable logical (scalar) clock.
 */
public class Clock implements Comparable<Clock> {

    private final int value;

    /**
     * Initializes the clock.
     * @param value initial value
     */
    public Clock(int value) {
        this.value = value;
    }

    /**
     * Returns the current clock value.
     * @return the value
     */
    public int getValue() {
        return value;
    }

    /**
     * Whether the clock is zero.
     * @return true if zero
     */
    public boolean isEmpty() {
        return value == 0;
    }

    /**
     * Returns a new clock instance incremented by one.
     * @return the incremented clock
     */
    public Clock increment() {
        return new Clock(value + 1);
    }

    /**
     * Returns the bigger of this or the given clock.
     * @param other clock
     * @return the bigger clock
     */
    public Clock max(Clock other) {
        if (value < other.value) {
            return other;
        }
        return this;
    }

    @Override
    public int compareTo(Clock o) {
        return Integer.compare(value, o.value);
    }

    /**
     * Returns true if the given clock is smaller than this clock.
     */
    public boolean isBefore(Clock clock) {
        if (clock == null) {
            return false;
        }
        return compareTo(clock) < 0;
    }

    /**
     * Returns true if the given clock is bigger than this clock.
     */
    public boolean isAfter(Clock clock) {
        if (clock == null) {
            return true;
        }
        return compareTo(clock) > 0;
    }


    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof Clock)) {
            return false;
        }

        return compareTo((Clock) o) == 0;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}