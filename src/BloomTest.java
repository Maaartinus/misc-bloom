import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

import com.google.common.base.Predicate;

import junit.framework.TestCase;

@SuppressWarnings("boxing")
public class BloomTest extends TestCase {
	private static class MyPredicate implements Predicate<Long> {
		@Override public boolean apply(Long input) {
			long x = input.longValue();
			long y = x;
			x *= 0xce4b3f671f428ed7L;
			y *= 0xe7ca32857b0c3681L;
			x = Long.reverseBytes(x);
			y = Long.rotateLeft(y, 32);
			x *= 0x3C696485625DA219L;
			y *= 0x24C06DEDDD5975D1L;
			return x+y < 0;
		}
	}

	private void fill(CaffeinBloomFilter filter, Random random, int count, Predicate<Long> predicate) {
		while (count>0) {
			final long x = random.nextLong();
			if (!predicate.apply(x)) continue;
			filter.put(x);
			--count;
		}
	}

	private int falsePositives(CaffeinBloomFilter filter, Random random, int count, Predicate<Long> predicate) {
		int result = 0;
		while (count>0) {
			final long x = random.nextLong();
			if (predicate.apply(x)) continue;
			if (filter.mightContain(x)) ++result;
			--count;
		}
		return result;
	}

	public void testBloomFilter1() {
		printStats(false);
	}

	public void testBloomFilter2() {
		printStats(true);
	}

	private void printStats(boolean variant) {
		System.out.println("variant\tlog2\tInsertions\tFalse positives\t(%)");
		for (int log2=20; log2<=24; log2++) {
			final int capacity = 1<<log2;

			final int randomSeed = 0x23456789;

			final CaffeinBloomFilter bf = variant ? new CaffeinBloomFilter3(capacity, randomSeed)
			: new CaffeinBloomFilter1(capacity, randomSeed);

			fill(bf, newRandom(), capacity, new MyPredicate());
			final int falsePositives = falsePositives(bf, newRandom(), capacity, new MyPredicate());

			System.out.format("%6s\t%3d\t%9d\t%7d\t(%6.3f%%)\n",
					variant, log2, capacity, falsePositives,
					((float) falsePositives / capacity) * 100);
		}
	}

	private Random newRandom() {
		return new Random(1);
	}

	private long[] newRandomLongArray(int length, Random random) {
		final long[] result = new long[length];
		for (int i=0; i<length; ++i) {
			result[i] = random.nextLong();
		}

		// Reduce duplicates.
		Arrays.sort(result);
		for (int i=1; i<length; ++i) {
			// Replace duplicates by new values, probably non-duplicates.
			if (result[i] == result[i-1]) result[i] = random.nextLong();
		}
		for (int i=length-1; i>0; i--) {
			final int j = random.nextInt(i+1);
			final long temp = result[i];
			result[i] = result[j];
			result[j] = temp;
		}
		return result;
	}
}
