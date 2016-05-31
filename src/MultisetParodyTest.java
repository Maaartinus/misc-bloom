import junit.framework.TestCase;


public class MultisetParodyTest extends TestCase {
	private static class Consumer<E> implements MultisetParody.MultisetParodyConsumer<E> {
		@Override public void accept(E e, int count) {
			System.out.println(e + " -> " + count);
		}
	}

	MultisetParody<String> parody = new MultisetParody<>(1);

	public void testAdd1() {
		System.out.println("\n*testAdd1");
		for (int i = 0; i < 5; i++) {
			parody.add("a");
		}
		parody.add("b");
		for (int i = 0; i < 25; i++) {
			parody.add("a");
		}
		parody.drainTo(new Consumer<String>());
	}

	public void testAdd2() {
		System.out.println("\n*testAdd2");
		for (int n = 0; n < 10; n++) {
			parody.add("a");
			parody.add("b");
		}
		parody.drainTo(new Consumer<String>());
	}

	public void testAdd3() {
		System.out.println("\n*testAdd3");
		for (int n = 0; n < 10; n++) {
			parody.add("a");
			parody.add("b");
			parody.add("c");
		}
		parody.drainTo(new Consumer<String>());
	}

	public void testAdd4() {
		System.out.println("\n*testAdd4");
		for (int n = 0; n < 10; n++) {
			parody.add("a");
			parody.add("b");
			parody.add("a");
			parody.add("c");
		}
		parody.drainTo(new Consumer<String>());
	}
}


