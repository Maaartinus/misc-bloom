package frequency;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor public class FilteredFrequency implements Frequency {
	private final Frequency delegate;
	private final BloomFilter filter;

	@Override public void increment(long e, int count) {
		final boolean wasAbsent = filter.put(e);
		if (wasAbsent) --count;
		if (count == 0) return;
		delegate.increment(e, count);
	}

	@Override public int frequency(long e) {
		// Adding one to account for the events lost in the filter.
		return filter.mightContain(e) ? delegate.frequency(e) + 1 : 0;
	}
}
