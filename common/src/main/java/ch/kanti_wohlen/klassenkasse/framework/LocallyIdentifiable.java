package ch.kanti_wohlen.klassenkasse.framework;

/**
 * Represents an object which stores its own numeric ID.
 * 
 * @param <K>
 *            the {@link Number} type that is the index of this {@code LocallyIdentifiable}.
 */
public interface LocallyIdentifiable<K extends Number> {

	/**
	 * Gets this object's stored local ID.
	 * 
	 * @return the local ID
	 */
	public K getLocalId();
}
