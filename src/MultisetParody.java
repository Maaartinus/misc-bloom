import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.Arrays;

public class MultisetParody<E> {
	// Just to get rid of the red markers.
	private @interface Nonnegative {}
	private @interface Nonnull {}

	private Object[] array;
	private int[] count;

	// One index is enough, also for RunLengthConsumer.
	private int index;
	private int maxIndex = -1;

	/**
	 * @param initialCapacity must be a pwer of two.
	 */
	public MultisetParody(@Nonnegative int initialCapacity) {
		checkArgument(Integer.bitCount(initialCapacity) == 1);
		array = new Object[initialCapacity];
		count = new int[initialCapacity];
	}

	public void add(@Nonnull E e) {
		requireNonNull(e);

		// Try luck with RLE.
		if (array[index] == e) {
			count[index]++;
			return;
		}

		// Try luck with trivial hashing.
		final int hashCode = e.hashCode();
		final int i = hashCode & (array.length-1);
		if (array[i] == e) {
			count[i]++;
			return;
		}
		// This rehashing is necessary, otherwise the algorithm fails badly in MultisetParodyTest#testAdd2.
		// The failure reason is the high probability of the slot being occupied by something else,
		// especially when i falls within the first half of the array.
		// Because of that, we jump accross half the array.
		final int j = i ^ (array.length >> 1);
		if (array[j] == e) {
			count[j]++;
			return;
		}
		if (array[i] == null) {
			array[i] = e;
			count[i] = 1;
			maxIndex = Math.max(maxIndex, i);
			return;
		}
		if (array[j] == null) {
			array[j] = e;
			count[j] = 1;
			maxIndex = Math.max(maxIndex, j);
			return;
		}

		// Search sequentially. As index never decreases, the amortized cost is O(1).
		while (++index < array.length) {
			if (array[index] == e) {
				count[index]++;
				return;
			}
			if (array[index] == null) {
				array[index] = e;
				count[index] = 1;
				return;
			}
		}

		// Grow sloppily. On the average, half of the hashed entries won't be found again.
		final int capacity = array.length << 1;
		array = Arrays.copyOf(array, capacity);
		count = Arrays.copyOf(count, capacity);
		array[index] = e;
		count[index] = 1;
	}

	public void drainTo(@Nonnull MultisetParodyConsumer<E> consumer) {
		requireNonNull(consumer);
		maxIndex = Math.max(maxIndex, index);

		for (int i = 0; i <= maxIndex; i++) {
			@SuppressWarnings("unchecked")
			final E e = (E) array[i];
			if (e == null) continue;
			array[i] = null;

			final int amount = count[i];
			count[i] = 0;

			consumer.accept(e, amount);
		}

		index = 0;
		maxIndex = -1;
	}

	interface MultisetParodyConsumer<E> {
		void accept(@Nonnull E e, @Nonnegative int count);
	}
}
