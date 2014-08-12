package ch.kanti_wohlen.klassenkasse.util;

import java.util.HashMap;
import java.util.Map;

/**
 * A bidirectional map which provides efficient lookups for both values by keys
 * and keys by values.
 * This implies that this map can only have one-to-one mappings; it guarantees
 * the uniqueness of both keys and values.
 * 
 * <p>
 * This map implements those properties by possessing a key-value
 * {@link HashMap}, as well as a reference to the inverse of this map, which is
 * also a {@code BiMap}. When any values of the key-value map are edited, the
 * inverse map is edited as well.
 * </p>
 * 
 * @author Roger Baumgartner
 * 
 * @param <K>
 *            the type of keys of this map
 * @param <V>
 *            the type of mapped values
 */
public class BiMap<K, V> extends HashMap<K, V> {

	private final BiMap<V, K> inverse;

	/**
	 * Creates a new empty {@code BiMap} with the default initial capacity of 16
	 * and the default load factor of 0.75.
	 */
	public BiMap() {
		this(16, 0.75f);
	}

	/**
	 * Creates a new empty {@code BiMap} with the specified initial capacity
	 * and the default load factor of 0.75.
	 * 
	 * @param initialCapacity
	 *            the minimal initial size this map should have
	 */
	public BiMap(int initialCapacity) {
		this(initialCapacity, 0.75f);
	}

	/**
	 * Creates a new empty {@code BiMap} with the specified initial capacity
	 * and load factor.
	 * 
	 * @param initialCapacity
	 *            the minimal initial size this map should have
	 * @param loadFactor
	 *            the load factor for this map
	 */
	public BiMap(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
		inverse = new BiMap<>(this, initialCapacity, loadFactor);
	}

	/**
	 * Creates a {@code BiMap} with the elements of the given map and the
	 * default load factor of 0.75.
	 * 
	 * @param initialEntries
	 *            the map whose entries should be placed in this {@code BiMap}
	 * @throws IllegalArgumentException
	 *             if more than one key are mapped to a value in the map
	 */
	public BiMap(Map<K, V> initialEntries) {
		this(initialEntries.size());
		putAll(initialEntries);
	}

	/**
	 * Constructor for the inverse {@code BiMap}. Should have the same default
	 * capacity and load factor as the parent map.
	 * 
	 * @param parent
	 *            the parent {@code BiMap} for this inverse map
	 * @param initialCapacity
	 *            the initial capacity of the parent map
	 * @param loadFactor
	 *            the load factor of the inverse map
	 */
	private BiMap(BiMap<V, K> parent, int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
		this.inverse = parent;
	}

	/**
	 * Gets the inverse map of this {@code BiMap}. This getter method does not
	 * copy any values and thus has O(1) performance.
	 * 
	 * @return the inverse map or the parent map if this is the inverse map
	 */
	public BiMap<V, K> inverse() {
		return inverse;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * If the inverse map contained a mapping for the value, an exception is
	 * thrown. Use {@link #putForce(Object, Object)} to avoid this behavior.
	 * </p>
	 * 
	 * @throws IllegalArgumentException
	 *             if the value already exists in this map
	 */
	@Override
	public V put(K key, V value) {
		boolean containsKey = super.containsKey(key);
		V oldValue = super.get(key);
		if (containsKey && equals(oldValue, value)) {
			return value;
		}

		if (containsValue(value) && inverse.get(value) != key) {
			throw new IllegalArgumentException("Map already contained the value \"" + String.valueOf(value) + "\".");
		}

		// Remove old mapping for this key, if there was one
		if (containsKey) {
			inverse.remove(oldValue);
		}

		// Put new keys
		super.put(key, value);
		inverse.put(value, key);

		return oldValue;
	}

	/**
	 * Associates the specified value with the specified key in this map.
	 * If the map previously contained a mapping for the key, the old value is
	 * replaced.
	 * 
	 * <p>
	 * If the inverse map contained a mapping for the value, the mapping in the
	 * inverse map is removed.
	 * </p>
	 * 
	 * @param key
	 *            key with which the specified value is to be associated
	 * @param value
	 *            value to be associated with the specified key
	 * @return the previous value associated with key, or {@code null} if there
	 *         was no mapping for key. (A {@code null} return can also indicate
	 *         that the map previously associated {@code null} with key.)
	 */
	public V putForce(K key, V value) {
		boolean containsKey = super.containsKey(key);
		V oldValue = super.get(key);
		if (containsKey && equals(oldValue, value)) {
			return value;
		}

		super.put(key, value);
		if (containsKey) {
			inverse.remove(oldValue);
		}

		inverse.putForce(value, key);
		return oldValue;
	}

	private static boolean equals(Object o1, Object o2) {
		if (o1 == o2) {
			return true;
		} else if (o1 == null || o2 == null) {
			return false;
		} else {
			return o1.equals(o2);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * If the inverse map contained a mapping for a value in the map, an
	 * exception is thrown. Use {@link #putAllForce(Map)} to avoid this
	 * behavior.
	 * </p>
	 * 
	 * @throws IllegalArgumentException
	 *             if a duplicate value exists in this map
	 */
	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
			put(e.getKey(), e.getValue());
		}
	}

	/**
	 * Copies all of the mappings from the specified map to this map.
	 * These mappings will replace any mappings that this map had for any of the
	 * keys currently in the specified map.
	 * 
	 * <p>
	 * If the inverse map contained a mapping for a value in the map, the
	 * mapping in the inverse map is removed. This can lead to inconsistent
	 * behavior if there are multiple mappings to the value in the given map.
	 * </p>
	 * 
	 * @param m
	 *            mappings to be stored in this map
	 */
	public void putAllForce(Map<? extends K, ? extends V> m) {
		for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
			putForce(e.getKey(), e.getValue());
		}
	}

	@Override
	public void clear() {
		if (size() == 0) return;

		super.clear();
		inverse.clear();
	}

	@Override
	public V remove(Object key) {
		// Necessary check so we don't lose null-key compatibility.
		if (super.containsKey(key)) {
			V value = super.remove(key);
			inverse.remove(value);
			return value;
		}
		return null;
	}

	@Override
	public Object clone() {
		// type erasure...
		HashMap<?, ?> hashMap = (HashMap<?, ?>) super.clone();
		return new BiMap<>(hashMap);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * This implementation is faster than the original {@link HashMap}
	 * implementation and has O(1) performance.
	 * </p>
	 */
	@Override
	public boolean containsValue(Object value) {
		return inverse.containsKey(value);
	}
}
