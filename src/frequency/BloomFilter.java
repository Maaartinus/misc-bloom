package frequency;

public interface BloomFilter {
	/**
	 * @param e Already spreaded hash of the input.
	 * @return true if anything has changed, i.e., mightContain would return false before
	 */
	boolean put(long e);

	/**
	 * @param e Already spreaded hash of the input.
	 */
	boolean mightContain(long e);

	void clear();
}
