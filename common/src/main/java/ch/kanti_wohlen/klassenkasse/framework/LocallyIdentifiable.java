package ch.kanti_wohlen.klassenkasse.framework;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public interface LocallyIdentifiable<K extends Number> {

	public K getLocalId();
}
