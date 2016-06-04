package frequency;

import java.util.concurrent.ThreadLocalRandom;

import lombok.Getter;
import lombok.Setter;


@Getter @Setter public class MyFrequencyFactory {
	private int expectedInsertions;
	private long randomSeed = ThreadLocalRandom.current().nextLong();
	private boolean fullReset;
	private boolean conservative;
	private double countersMultiplier;

	private boolean bloomFullReset;
	private int bloomExpectedInsertions;
	private double bloomOccupancyRatio;

	private boolean cmFullReset;
	private int cmExpectedInsertions;
	private double cmOccupancyRatio;
	private double cmCountersMultiplier;
	private boolean cmConservative;

	public Frequency newFrequency() {
		final MyBloomFilter filter = new MyBloomFilter(this);
		final MyCountMin4Frequency simpleFrequency = new MyCountMin4Frequency(this);
		final BatchingFrequency batchingFrequency = new BatchingFrequency(simpleFrequency);
		final FilteredFrequency filteredFrequency = new FilteredFrequency(batchingFrequency, filter);
		return new SpreadingFrequency(filteredFrequency, randomSeed);
	}
}
