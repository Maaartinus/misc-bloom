package frequency;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor public class BatchingFrequency implements Frequency {
	private final Frequency delegate;
	private long e;
	private int count;

	@Override public void increment(long e, int count) {
		if (e == this.e) {
			this.count += count;
		} else {
			delegate.increment(this.e, this.count);
			this.e = e;
			this.count = count;
		}
	}

	@Override public int frequency(long e) {
		if (count > 0) {
			delegate.increment(this.e, this.count);
			this.count = 0;
		}
		return delegate.frequency(e);
	}
}
