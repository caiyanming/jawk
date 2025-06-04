package org.metricshub.jawk;

import static org.junit.Assert.*;

import org.junit.Test;
import org.metricshub.jawk.jrt.AssocArray;

public class AssocArrayTest {

	@Test
	public void testRemoveNumericStringKey() {
		AssocArray array = new AssocArray(false);
		array.put(1L, "one");

		assertEquals("one", array.remove("1"));
		assertFalse(array.isIn(1L));
	}

	@Test
	public void testRemoveNumericKeyFromString() {
		AssocArray array = new AssocArray(false);
		array.put("2", "two");

		assertEquals("two", array.remove(2L));
		assertFalse(array.isIn("2"));
	}

	@Test
	public void testRemoveMissingKey() {
		AssocArray array = new AssocArray(false);
		array.put(1, "one");

		assertNull(array.remove(3L));
		assertTrue(array.isIn(1L));
	}
}
