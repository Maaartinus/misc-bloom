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
 * @author ben.manes@gmail.com (Ben Manes)
 */
@NotThreadSafe
public final class CaffeinBloomFilter3 implements CaffeinBloomFilter {
	static final int BITS_PER_LONG_SHIFT = 6; // 64-bits
	static final int INDEX_MASK = Long.SIZE - 1;

	static final double FPP = 0.03; // false positive probability
	static final double LOG_OF_2 = Math.log(2);
	static final double OPTIMAL_BITS_FACTOR = -Math.log(FPP) / (LOG_OF_2 * LOG_OF_2);

	final int randomSeed;

	int tableMask;
	int tableShift;
	long[] table;

	/**
	 * Creates a lazily initialized frequency sketch, requiring {@link #ensureCapacity} be called
	 * when the expected number of insertions is determined.
	 *
	 * @param expectedInsertions the number of expected insertions
	 * @param randomSeed the smear to protect against hash flooding
	 */
	public CaffeinBloomFilter3(@Nonnegative long expectedInsertions, int randomSeed) {
		this.randomSeed = 2*randomSeed + 1;
		checkArgument(randomSeed != 0);
		ensureCapacity(expectedInsertions);
	}

	/**
	 * Initializes and increases the capacity of this <tt>BloomFilter</tt> instance, if necessary,
	 * to ensure that it can accurately estimate the membership of elements given the expected
	 * number of insertions.
	 *
	 * @param expectedInsertions the number of expected insertions
	 */
	public void ensureCapacity(@Nonnegative long expectedInsertions) {
		checkArgument(expectedInsertions >= 0);

		final int optimalNumberOfBits = (int) (expectedInsertions * OPTIMAL_BITS_FACTOR);
		final int optimalSize = Math.max(optimalNumberOfBits >>> BITS_PER_LONG_SHIFT, 2);
		if ((table != null) && (table.length >= optimalSize)) {
			return;
		}

		table = new long[ceilingPowerOfTwo(optimalSize)];
		tableMask = table.length - 1;
		tableShift = 32 + Integer.numberOfLeadingZeros(tableMask);
	}

	/**
	 * Returns if the element <i>might</i> have been put in this Bloom filter, {@code false} if this
	 * is <i>definitely</i> not the case.
	 *
	 * @param e the element whose presence is to be tested
	 * @return if the element might be present
	 */
	@Override
	public boolean mightContain(long e) {
		e = spread(e);
		if (!getTwo(e)) return false;
		e = respread(e);
		if (!getOne(e)) return false;
		e = respread(e);
		if (!getOne(e)) return false;
		//		e = respread(e);
		//		if (!getOne(e)) return false;
		//		e = respread(e);
		//		if (!getOne(e)) return false;
		return true;
	}

	/** Removes all of the elements from this collection. */
	public void clear() {
		Arrays.fill(table, 0L);
	}

	/**
	 * Puts an element into this collection so that subsequent queries with the same element will
	 * return {@code true}.
	 *
	 * @param e the element to add
	 */
	@Override
	public void put(long e) {
		e = spread(e);
		setTwo(e);
		e = respread(e);
		setOne(e);
		e = respread(e);
		setOne(e);
		//		e = respread(e);
		//		setOne(e);
		//		e = respread(e);
		//		setOne(e);
	}

	private void setOne(long e) {
		final int index = (int) (e >>> tableShift);
		table[index] |= bitmaskOne(e);
	}

	private boolean getOne(long e) {
		final int index = (int) (e >>> tableShift);
		return (table[index] & bitmaskOne(e)) != 0;
	}

	private void setTwo(long e) {
		final int index = (int) (e >>> tableShift);
		table[index] |= bitmaskOne(e) | bitmaskTwo(e);
	}

	private boolean getTwo(long e) {
		final int index = (int) (e >>> tableShift);
		final long entry = table[index];
		return (entry & bitmaskOne(e)) != 0 && (entry & bitmaskTwo(e)) != 0;
	}

	private long bitmaskOne(long e) {
		return 1L << e;
	}

	private long bitmaskTwo(long e) {
		return 1L << (e >> BITS_PER_LONG_SHIFT);
	}

	/**
	 * Applies a supplemental hash function to a given hashCode, which defends against poor quality
	 * hash functions.
	 */
	long spread(long e) {
		e ^= (e >>> 21) ^ (e >>> 41);
		e *= 0xc3a5c85c97cb3127L;
		e ^= (e >>> 21) ^ (e >>> 41);
		e *= randomSeed;
		e ^= (e >>> 21) ^ (e >>> 41);
		return e;
	}

	private long respread(long e) {
		e *= 0xb492b66fbe98f273L;
		e ^= (e >>> 21) ^ (e >>> 41);
		return e;
	}

	static int ceilingPowerOfTwo(int x) {
		// From Hacker's Delight, Chapter 3, Harry S. Warren Jr.
		return 1 << -Integer.numberOfLeadingZeros(x - 1);
	}
}
