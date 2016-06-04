package frequency;
/*
 * Copyright 2016 Ben Manes. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import static com.google.common.base.Preconditions.checkArgument;

import java.util.Arrays;

import javax.annotation.Nonnegative;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A Bloom filter is a space and time efficient probabilistic data structure that is used to test
 * whether an element is a member of a set. False positives are possible, but false negatives are
 * not. Elements can be added to the set, but not removed. The more elements that are added the
 * higher the probability of false positives. While risking false positives, Bloom filters have a
 * space advantage over other data structures for representing sets by not storing the items.
 *
 * <p>This filter resets (fully or partially clears) itself, when a limit is reached.
 * 
 * @author ben.manes@gmail.com (Ben Manes)
 */
@NotThreadSafe
public final class MyBloomFilter implements BloomFilter {
	private static final long SEED = 0xb492b66fbe98f273L;
	private static final int BITS_PER_LONG_SHIFT = 6; // 64-bits

	private static final double FPP = 0.03; // false positive probability
	private static final double LOG_OF_2 = Math.log(2);
	private static final double OPTIMAL_BITS_FACTOR = -Math.log(FPP) / (LOG_OF_2 * LOG_OF_2);

	private final boolean fullReset;
	private final double occupancyRatio;

	private long[] table;
	/** A value such that x >>> tableShift si a valid index for any long x. */
	private int tableShift;
	/** The total number of set bits. */
	private int occupancy;
	private int maxOccupancy;
	private int cursor;


	public MyBloomFilter(MyFrequencyFactory factory) {
		fullReset = factory.getBloomFullReset();
		occupancyRatio = factory.getBloomOccupancyRatio();
		checkArgument(0 < occupancyRatio && occupancyRatio < 1);
		ensureCapacity(factory.getBloomExpectedInsertions());
	}

	/**
	 * Initializes and increases the capacity of this <tt>BloomFilter</tt> instance, if necessary,
	 * to ensure that it can accurately estimate the membership of elements given the expected
	 * number of insertions.
	 *
	 * @param expectedInsertions the number of expected insertions
	 */
	public void ensureCapacity(@Nonnegative int expectedInsertions) {
		checkArgument(expectedInsertions >= 0);

		final long optimalNumberOfBits = (long) (expectedInsertions * OPTIMAL_BITS_FACTOR);
		// The minimum optimalSize is 2 in order for tableShift to work.
		final int optimalSize = (int) Math.max(optimalNumberOfBits >>> BITS_PER_LONG_SHIFT, 2);
		if ((table != null) && (table.length >= optimalSize)) {
			return;
		}

		table = new long[ceilingPowerOfTwo(optimalSize)];
		tableShift = Long.numberOfLeadingZeros(table.length - 1);
		occupancy = 0;
		maxOccupancy = (int) (occupancyRatio * table.length * Long.SIZE);
	}

	/**
	 * Returns if the element <i>might</i> have been put in this Bloom filter, {@code false} if this
	 * is <i>definitely</i> not the case.
	 *
	 * @param e the element whose presence is to be tested
	 * @return if the element might be present, assumed to be already spreaded well.
	 */
	@Override public boolean mightContain(long e) {
		if (!getTwo(e)) return false;
		e = respread(e);
		if (!getTwo(e)) return false;
		return true;
	}

	private void reset() {
		if (fullReset) {
			clear();
		} else {
			final int i = cursor++ & (table.length-1);
			final int bits = Long.bitCount(table[i]);
			table[i] = 0;
			occupancy -= bits;
		}
	}

	/** Removes all of the elements from this collection. */
	@Override public void clear() {
		Arrays.fill(table, 0L);
		occupancy = 0;
	}

	/**
	 * Puts an element into this collection so that subsequent queries with the same element will
	 * return {@code true}.
	 *
	 * @param e the element to add, assumed to be already spreaded well.
	 * @return true if the state has changed
	 */
	@Override public boolean put(long e) {
		final long bitsSet = setTwo(e) + setTwo(respread(e));
		occupancy += bitsSet;
		if (occupancy >= maxOccupancy) reset();
		return bitsSet > 0;
	}

	private int setTwo(long e) {
		final int index = index(e);
		final long old = table[index];
		final long neu = old | (Long.MIN_VALUE >>> e) | (Long.MIN_VALUE >>> altShiftDistance(e));
		table[index] = neu;
		return Long.bitCount(neu ^ old);
	}

	private boolean getTwo(long e) {
		final long entry = table[index(e)];
		final long result = (entry << e) & (entry << altShiftDistance(e));
		return result < 0;
	}

	private int index(long e) {
		return (int) (e >>> tableShift);
	}

	private long altShiftDistance(long e) {
		return e >> BITS_PER_LONG_SHIFT;
	}

	private long respread(long e) {
		e *= SEED;
		e ^= (e >>> 21) ^ (e >>> 41);
		return e;
	}

	private static int ceilingPowerOfTwo(int x) {
		// From Hacker's Delight, Chapter 3, Harry S. Warren Jr.
		return 1 << -Integer.numberOfLeadingZeros(x - 1);
	}
}
