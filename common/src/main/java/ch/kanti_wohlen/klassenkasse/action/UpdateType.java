package ch.kanti_wohlen.klassenkasse.action;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public enum UpdateType {
	CREATION,
	REMOVAL,
	UPDATE;

	public UpdateType reverse() {
		if (this == CREATION) {
			return REMOVAL;
		} else if (this == REMOVAL) {
			return CREATION;
		} else {
			return this;
		}
	}
}