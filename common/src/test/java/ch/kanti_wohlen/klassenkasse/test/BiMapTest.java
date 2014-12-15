package ch.kanti_wohlen.klassenkasse.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import ch.kanti_wohlen.klassenkasse.util.BiMap;

public class BiMapTest {

	private final BiMap<Integer, String> biMap;

	public BiMapTest() {
		biMap = new BiMap<>();
	}

	@Test
	public void testInverse() {
		// Twice the inverse should return the original map
		assertEquals(biMap, biMap.inverse().inverse());
	}

	@Test
	public void testPut() {
		// Put values in the map
		biMap.put(1, "Test 1");
		assertEquals("Test 1", biMap.get(1));
		assertEquals((Integer) 1, biMap.inverse().get("Test 1"));
	}

	@Test
	public void testUpdateSame() {
		// Put values in the map
		biMap.put(1, "Test 1");

		// Updating a key with the same value should not throw errors
		biMap.put(1, "Test 1");
	}

	@Test
	public void testUpdateOther() {
		// Put values in the map
		biMap.put(1, "Test 1");

		// Update value with existing key
		biMap.put(1, "Test 2");
		assertEquals("Test 2", biMap.get(1));
		assertEquals((Integer) 1, biMap.inverse().get("Test 2"));
		assertNull(biMap.inverse().get("Test 1"));
	}

	@Test
	public void testRemove() {
		biMap.put(1, "Test 1");

		// Remove a key
		biMap.remove(1);
		assertNull(biMap.get(1));
		assertNull(biMap.inverse().get("Test 1"));
	}

	@Test
	public void testPutNullKey() {
		// Put null key
		biMap.put(null, "testNullKey");
		assertTrue(biMap.containsKey(null));
		assertTrue(biMap.inverse().containsKey("testNullKey"));
		assertEquals("testNullKey", biMap.get(null));
		assertEquals(null, biMap.inverse().get("testNullKey"));
	}

	@Test
	public void testRemoveNullKey() {
		// Put null key
		biMap.put(null, "testNullKey");

		// Remove null key
		biMap.remove(null);
		assertFalse(biMap.containsKey(null));
		assertFalse(biMap.inverse().containsKey("testNullKey"));
		assertNull(biMap.get(null));
		assertNull(biMap.inverse().get("testNullKey"));
	}

	@Test
	public void testUpdateNullKey() {
		// Put null key
		biMap.put(null, "Test 1");

		// Update value with existing key
		biMap.put(null, "Test 2");
		assertEquals("Test 2", biMap.get(null));
		assertNull(biMap.inverse().get("Test 2"));
		assertNull(biMap.inverse().get("Test 1"));
	}

	@Test
	public void testPutNullValue() {
		// Put null value
		biMap.put(1, null);
		assertTrue(biMap.containsKey(1));
		assertTrue(biMap.inverse().containsKey(null));
		assertEquals(null, biMap.get(1));
		assertEquals((Integer) 1, biMap.inverse().get(null));
	}

	@Test
	public void testRemoveNullValue() {
		// Put null value
		biMap.put(1, null);

		biMap.remove(1);
		assertFalse(biMap.containsKey(1));
		assertFalse(biMap.inverse().containsKey(null));
		assertNull(biMap.get(1));
		assertNull(biMap.inverse().get(null));
	}

	@Test
	public void testPutNullKeyWithNullValue() {
		// Put null key with null value
		biMap.put(null, null);
		assertTrue(biMap.containsKey(null));
		assertTrue(biMap.inverse().containsKey(null));
		assertNull(biMap.get(null));
		assertNull(biMap.inverse().get(null));
	}

	@Test
	public void testRemoveNullKeyWithNullValue() {
		// Put null key with null value
		biMap.put(null, null);

		biMap.remove(null);
		assertFalse(biMap.containsKey(null));
		assertFalse(biMap.inverse().containsKey(null));
		assertNull(biMap.get(null));
		assertNull(biMap.inverse().get(null));
	}

	@Test
	public void testPutException() {
		// No two keys should be able to point at the same object
		biMap.put(1, "Test 1");
		try {
			biMap.put(2, "Test 1");
			// No exception --> fail
			fail("Having two keys pointing to the same value did not cause an exception.");
		} catch (IllegalArgumentException e) {
			// Expected
		}
		assertEquals("Test 1", biMap.get(1));
		assertEquals((Integer) 1, biMap.inverse().get("Test 1"));
		assertNull(biMap.get(2));

		// Same goes for the inverse
		biMap.inverse().put("Test 2-1", 3);
		try {
			biMap.inverse().put("Test 2-2", 3);
			fail("Having two keys pointing to the same value in the inverse did not cause an exception.");
		} catch (IllegalArgumentException e) {
			// Expected
		}
		assertEquals("Test 2-1", biMap.get(3));
		assertEquals((Integer) 3, biMap.inverse().get("Test 2-1"));
		assertNull(biMap.inverse().get("Test 2-2"));
	}

	@Test
	public void testPutForce() {
		// Preparation
		biMap.put(1, "Test 1");
		biMap.put(2, "Test 2");

		biMap.putForce(2, "Test 1");
		assertEquals("Test 1", biMap.get(2));
		assertEquals((Integer) 2, biMap.inverse().get("Test 1"));
		assertNull(biMap.get(1));
		assertNull(biMap.inverse().get("Test 2"));
	}

	@Test
	public void testClear() {
		biMap.put(1, "1");
		biMap.put(2, "2");
		assertEquals(2, biMap.size());
		assertEquals(2, biMap.inverse().size());

		biMap.clear();
		assertEquals(0, biMap.size());
		assertEquals(0, biMap.inverse().size());
		assertNull(biMap.get(1));
		assertNull(biMap.inverse().get("2"));
	}
}
