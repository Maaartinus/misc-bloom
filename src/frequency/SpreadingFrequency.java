package frequency;


public class SpreadingFrequency implements Frequency {
	private final Frequency delegate;
	private final long randomSeed;

	public SpreadingFrequency(Frequency delegate, long randomSeed) {
		this.delegate = delegate;
		this.randomSeed = randomSeed | 1;
	}

	@Override public void increment(long e, int count) {
		delegate.increment(spread(e), count);
	}

	@Override public int frequency(long e) {
		return delegate.frequency(spread(e));
	}

	private long spread(long e) {
		e *= 0xc3a5c85c97cb3127L;
		e = Long.reverseBytes(e); // A single instruction on amd64.
		e *= randomSeed;
		e ^= (e >>> 21) ^ (e >>> 41); // On a typical superscalar CPU it doesn't take any longer than xoring with a single shift.
		return e;
	}
}
