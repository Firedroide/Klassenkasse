package ch.kanti_wohlen.klassenkasse.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;

public class SubConfiguration extends Configuration {

	private final @NonNull Configuration root;
	private final @NonNull String path;

	@NonNullByDefault
	public SubConfiguration(Configuration root, String path) {
		this.root = root;
		this.path = path;
	}

	public String getPath() {
		return path;
	}

	@SuppressWarnings("null")
	public @NonNull String combinedPath(@NonNull String key) {
		StringBuilder sb = new StringBuilder();
		if (!path.isEmpty()) sb.append(path).append(".");
		sb.append(key);
		return sb.toString();
	}

	@Override
	public <T> T getValue(String key, Class<T> clazz) {
		return root.getValue(combinedPath(key), clazz);
	}

	@Override
	public Map<String, Object> getValues(boolean deep) {
		Map<String, Object> rootValues = new HashMap<>(root.getValues(true));
		String path = combinedPath("");
		int pathLength = path.length();

		for (Iterator<String> keys = rootValues.keySet().iterator(); keys.hasNext();) {
			String key = keys.next();
			if (key == null || !key.startsWith(path)) {
				keys.remove();
			} else if (!deep) {
				String localKey = key.substring(pathLength);
				if (localKey.contains(".")) keys.remove();
			}
		}

		return rootValues;
	}

	@Override
	public Set<String> getSubsectionKeys() {
		Set<String> keys = new HashSet<String>();
		Map<String, Object> values = getValues(true);
		String path = combinedPath("");
		int pathLength = path.length();

		for (String key : values.keySet()) {
			if (key == null) continue;
			if (!key.startsWith(path)) continue;

			String localKey = key.substring(pathLength);
			int pos = localKey.indexOf(".");
			if (pos == -1) continue;

			String substring = localKey.substring(0, pos);
			keys.add(substring);
		}

		@SuppressWarnings({"null"})
		@NonNull
		Set<String> unmodifiable = Collections.unmodifiableSet(keys);
		return unmodifiable;
	}

	@Override
	public Configuration getSubsection(String key) {
		if (containsKey(key)) {
			throw new IllegalConfigurationAccessException("Key " + combinedPath(key) + " has a value associated to it!");
		}
		return new SubConfiguration(root, combinedPath(key));
	}

	@Override
	public void set(String key, Object value) {
		root.set(combinedPath(key), value);
	}

	@Override
	public boolean containsKey(String key) {
		return root.containsKey(combinedPath(key));
	}
}
