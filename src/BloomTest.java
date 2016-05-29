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

	private void checkNoFalseNegatives(CaffeinBloomFilter filter, Random random, int count, Predicate<Long> predicate) {
		while (count>0) {
			final long x = random.nextLong();
			if (!predicate.apply(x)) continue;
			assertTrue(filter.mightContain(x));
			--count;
		}
	}

	public void testBloomFilter1() {
		printStats(false);
	}

	public void testBloomFilter3() {
		printStats(true);
	}

	private void printStats(boolean variant) {
		System.out.println("variant\tlog2\tInsertions\tFalse positives\t(%)");
		for (int log2=10; log2<=28; log2+=3) {
			final int capacity = 1<<log2;

			final int randomSeed = 0x23456789;

			final CaffeinBloomFilter bf = variant ? new CaffeinBloomFilter3(capacity, randomSeed)
			: new CaffeinBloomFilter1(capacity, randomSeed);

			fill(bf, newRandom(), capacity, new MyPredicate());
			//			checkNoFalseNegatives(bf, newRandom(), capacity, new MyPredicate());
			final int falsePositives = falsePositives(bf, newRandom(), capacity, new MyPredicate());

			System.out.format("%6s\t%3d\t%9d\t%7d\t(%6.3f%%)\n",
					variant, log2, capacity, falsePositives, 100.0 * falsePositives / capacity);
		}
	}

	private Random newRandom() {
		return new Random(7547390);
	}
}
