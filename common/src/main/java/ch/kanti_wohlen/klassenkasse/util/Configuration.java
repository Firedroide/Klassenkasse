package ch.kanti_wohlen.klassenkasse.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

public abstract class Configuration {

	public abstract @NonNull String getPath();

	public abstract <T> T getValue(@NonNull String key, @NonNull Class<T> clazz);

	public abstract @NonNull Map<String, Object> getValues(boolean deep);

	public abstract @NonNull Configuration getSubsection(@NonNull String key);

	public abstract @NonNull Set<String> getSubsectionKeys();

	public abstract void set(@NonNull String key, @Nullable Object value);

	public Object get(@NonNull String key) {
		return getValue(key, Object.class);
	}

	public boolean getBoolean(@NonNull String key) {
		return getValue(key, Boolean.class).booleanValue();
	}

	public String getString(@NonNull String key) {
		return getValue(key, String.class);
	}

	public int getInteger(@NonNull String key) {
		return getValue(key, Number.class).intValue();
	}

	public long getLong(@NonNull String key) {
		return getValue(key, Number.class).longValue();
	}

	public double getDouble(@NonNull String key) {
		return getValue(key, Number.class).doubleValue();
	}

	public @NonNull Map<String, Configuration> getSubsections() {
		Set<String> keys = getSubsectionKeys();
		Map<String, Configuration> subsections = new HashMap<>();

		for (String key : keys) {
			if (key == null) continue;
			subsections.put(key, getSubsection(key));
		}

		@SuppressWarnings("null")
		@NonNull
		Map<String, Configuration> unmodifiable = Collections.unmodifiableMap(subsections);
		return unmodifiable;
	}

	public boolean containsKey(@NonNull String key) {
		return get(key) != null;
	}

	public void remove(@NonNull String key) {
		set(key, null);
	}
}
