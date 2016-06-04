package frequency;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Arrays;

import javax.annotation.Nonnegative;

/**
 * A probabilistic multiset for estimating the popularity of an element within a time window. The
 * maximum frequency of an element is limited to 15 (4-bits) and aging is currently implemented by clearing the data.
 * 
 * <p>This Frequency resets (fully or partially clears) itself, when a limit is reached.
 */
public class MyCountMin4Frequency implements Frequency {
	private static final long SEED = 0xcbf29ce484222325L;
	private static final long ONE_MASK = 0x1111111111111111L;
	private static final long RESET_MASK = 7 * ONE_MASK;

	private final boolean fullReset;
	private final double occupancyRatio;
	private final double countersMultiplier;
	private final boolean conservative;

	private long[] table;
	/** A value such that x >>> tableShift si a valid index for any long x. */
	private int tableShift;

	/** The sum of all counters. */
	private long occupancy;
	private long maxOccupancy;
	private int cursor;

	public MyCountMin4Frequency(MyFrequencyFactory factory) {
		fullReset = factory.getCmFullReset();
		occupancyRatio = factory.getCmOccupancyRatio();
		checkArgument(0 < occupancyRatio && occupancyRatio < 1);
		countersMultiplier = factory.getCmCountersMultiplier();
		conservative = factory.getCmConservative();
		final long counters = (long) (countersMultiplier * factory.getCmExpectedInsertions());
		ensureCapacity(counters);
	}

	@Override public void increment(long e, int count) {
		if (conservative) {
			conservativeIncrement(e, count);
		} else {
			regularIncrement(e, count);
		}
	}

	@Override public int frequency(long e) {
		int result = extract(e);
		e = respread1(e);
		result = Math.min(result, extract(e));
		e = respread2(e);
		result = Math.min(result, extract(e));
		e = respread3(e);
		result = Math.min(result, extract(e));
		return result;
	}

	/**
	 * Initializes and increases the capacity of this instance, if necessary,
	 * to ensure that it can accurately estimate the popularity of elements given the maximum size of
	 * the cache. This operation forgets all previous counts when resizing.
	 *
	 * @param maximumSize the maximum size of the cache
	 */
	public void ensureCapacity(@Nonnegative long maximumSize) {
		checkArgument(maximumSize >= 0);
		int maximum = (int) Math.min(maximumSize, Integer.MAX_VALUE >>> 1);
		if ((table != null) && (table.length >= maximum)) {
			return;
		}
		maximum = Math.max(maximum, 1);

		table = new long[ceilingNextPowerOfTwo(maximum)];
		tableShift = Long.numberOfLeadingZeros(table.length-1);
		occupancy = 0;
		final double coef = 15.0 / 4; // maximum value per counter / counter bits
		maxOccupancy = (int) (occupancyRatio * table.length * Long.SIZE * coef);
	}

	private void conservativeIncrement(long e, int count) {
		if (count > 15) count = 15;

		final int oldFrequency = frequency(e);
		if (oldFrequency == 15) return;

		final int newFrequency = Math.min(oldFrequency + count, 15);
		if (newFrequency == oldFrequency) return;

		occupancy += maximizeAt(e, newFrequency);
		e = respread1(e);
		occupancy += maximizeAt(e, newFrequency);
		e = respread2(e);
		occupancy += maximizeAt(e, newFrequency);
		e = respread3(e);
		occupancy += maximizeAt(e, newFrequency);

		if (occupancy >= maxOccupancy) reset();
	}

	private void regularIncrement(long e, @Nonnegative int count) {
		if (count > 15) count = 15;

		occupancy += incrementAt(e, count);
		e = respread1(e);
		occupancy += incrementAt(e, count);
		e = respread2(e);
		occupancy += incrementAt(e, count);
		e = respread3(e);
		occupancy += incrementAt(e, count);

		if (occupancy >= maxOccupancy) reset();
	}

	private void reset() {
		if (fullReset) {
			clear();
		} else {
			final int i = cursor++ & (table.length-1);
			final long old = table[i];
			final long neu = (old >>> 1) & RESET_MASK;
			table[i] = neu;
			occupancy -= nibbleSum(old - neu);
		}
	}

	private int nibbleSum(long value) {
		final long mask = 0x0F0F0F0F0F0F0F0FL;
		final long a = value & mask;
		final long b = (value & ~mask) >>> 4;
		final long c = a + b;
		return (int) ((0x0101010101010101L * c) >>> 56);
	}

	private void clear() {
		Arrays.fill(table, 0L);
		occupancy = 0;
	}

	private long incrementAt(long e, int count) {
		final int index = index(e);
		final int shift = shift(e);

		final long old = (table[index] >>> shift) & 15;
		final long neu = Math.min(old + count, 15);
		final long delta = neu - old;
		table[index] += delta << shift;

		return delta;
	}

	private long maximizeAt(long e, int value) {
		final int index = index(e);
		final int shift = shift(e);

		final long old = (table[index] >>> shift) & 15;
		final long neu = Math.max(old, value);
		final long delta = neu - old;
		table[index] += delta << shift;

		return delta;
	}

	private long respread1(long e) {
		// This is enough as each operation uses basically the upper half of e only.
		return Long.rotateLeft(e, 32);
	}

	private long respread2(long e) {
		return e * SEED;
	}

	private long respread3(long e) {
		// This is enough as each operation uses basically the upper half of e only.
		return Long.rotateLeft(e, 32);
	}

	private int extract(long e) {
		final int index = index(e);
		final int shift = shift(e);
		return (int) (table[index] >>> shift) & 15;
	}

	private int index(long e) {
		return (int) (e >>> tableShift);
	}

	private int shift(long e) {
		// Return a number from the set {0, 4, ..., 60}.
		return (int) e & (15 << 2);
	}

	private static int ceilingNextPowerOfTwo(int x) {
		// From Hacker's Delight, Chapter 3, Harry S. Warren Jr.
		return 1 << (Integer.SIZE - Integer.numberOfLeadingZeros(x - 1));
	}
}
