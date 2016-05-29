
public interface CaffeinBloomFilter {
	void put(long e);
	boolean mightContain(long e);
}
