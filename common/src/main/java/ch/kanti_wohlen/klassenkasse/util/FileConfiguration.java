package ch.kanti_wohlen.klassenkasse.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.ScalarStyle;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;

import ch.kanti_wohlen.klassenkasse.util.IllegalConfigurationAccessException;

public class FileConfiguration extends Configuration {

	private final Yaml yaml;
	private final String name;
	private final boolean defaultsBacked;
	private Map<String, Object> properties;
	private Map<String, Object> defaults;

	public FileConfiguration(String name, boolean defaultsBacked) {
		if (name.isEmpty()) throw new IllegalArgumentException("name was null or empty");

		this.name = name;
		this.defaultsBacked = defaultsBacked;

		DumperOptions options = new DumperOptions();
		options.setIndent(2);
		options.setDefaultFlowStyle(FlowStyle.BLOCK);
		options.setDefaultScalarStyle(ScalarStyle.PLAIN);
		yaml = new Yaml(options);

		defaults = new HashMap<>();
		properties = new HashMap<>();

		loadDefaults();
		reload();
	}

	private void loadDefaults() {
		try (InputStream input = getClass().getResourceAsStream("/" + name)) {
			if (input == null) {
				if (defaultsBacked) {
					throw new IllegalStateException("Defaults file was not found in the classpath");
				} else {
					return;
				}
			}

			@SuppressWarnings({"unchecked"})
			Map<String, Object> rawMap = (Map<String, Object>) yaml.loadAs(input, Map.class);

			if (rawMap != null) {
				defaults.clear();
				unpackMap(defaults, null, rawMap);
			}
		} catch (IOException closeError) {
			closeError.printStackTrace();
		}
	}

	public void reload() {
		File file = new File(name);
		if (!file.exists()) {
			createDefaults();
		} else if (file.isDirectory()) {
			file.delete();
			createDefaults();
		}

		try (FileInputStream input = new FileInputStream(file)) {
			@SuppressWarnings("unchecked")
			Map<String, Object> rawMap = (Map<String, Object>) yaml.loadAs(input, Map.class);

			if (rawMap != null) {
				properties.clear();
				unpackMap(properties, null, rawMap);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			// File system error (exist and directory was checked)
		} catch (IOException closeError) {
			closeError.printStackTrace();
		}
	}

	private void createDefaults() {
		try (InputStream in = getClass().getResourceAsStream("/" + name)) {
			if (defaultsBacked && in == null) {
				throw new IllegalStateException("Defaults file was not found in the classpath");
			}

			try (OutputStream out = new FileOutputStream(name)) {
				if (in != null) {
					// Copy file from JAR to the properties file
					byte[] buf = new byte[4096];
					int len = 0;
					while ((len = in.read(buf)) >= 0) {
						out.write(buf, 0, len);
					}
				}
			} catch (FileNotFoundException isDirectory) {
				// We checked isDirectory earlier, won't happen.
				isDirectory.printStackTrace();
			} catch (IOException outCloseError) {
				outCloseError.printStackTrace();
			}

		} catch (IOException inCloseError) {
			inCloseError.printStackTrace();
		}
	}

	// TODO
	public void save() {
		try (FileWriter writer = new FileWriter(name)) {
			Map<String, Object> packed = packMap(this, null);
			yaml.dump(packed, writer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getPath() {
		return "";
	}

	@Override
	public <T> T getValue(String key, Class<T> clazz) {
		Object obj = properties.get(key);

		if (defaultsBacked) {
			Object def = getDefault(key, clazz);

			if (obj == null) {
				obj = def;
				properties.put(key, def);
			} else if (!clazz.isAssignableFrom(obj.getClass())) {
				obj = def;
				properties.put(key, def);
			}
		} else if (obj == null) {
			throw new IllegalConfigurationAccessException("Inexistant property: " + String.valueOf(key));
		}

		@SuppressWarnings("unchecked")
		T t = (T) obj;

		return t;
	}

	private <T> T getDefault(String key, Class<T> clazz) {
		Object def = defaults.get(key);
		if (def == null) {
			throw new IllegalConfigurationAccessException("Inexistant property: " + String.valueOf(key));
		} else if (!clazz.isAssignableFrom(def.getClass())) {
			throw new IllegalConfigurationAccessException("Wrong type for key \"" + String.valueOf(key)
					+ "\", requested " + clazz.getSimpleName() + ", was " + def.getClass().getSimpleName());
		}

		@SuppressWarnings("unchecked")
		T t = (T) def;
		return t;
	}

	@Override
	public Map<String, Object> getValues(boolean deep) {
		HashMap<String, Object> values = new HashMap<>();

		if (deep) {
			values.putAll(defaults);
			values.putAll(properties);
		} else {
			for (Map.Entry<String, Object> def : defaults.entrySet()) {
				if (!def.getKey().contains(".")) {
					values.put(def.getKey(), def.getValue());
				}
			}
			for (Map.Entry<String, Object> value : properties.entrySet()) {
				if (!value.getKey().contains(".")) {
					values.put(value.getKey(), value.getValue());
				}
			}
		}

		@SuppressWarnings("null")
		@NonNull
		Map<String, Object> unmodifiable = Collections.unmodifiableMap(values);

		return unmodifiable;
	}

	@Override
	public Configuration getSubsection(String key) {
		if (containsKey(key)) {
			throw new IllegalConfigurationAccessException("Key " + key + " has a value associated to it!");
		}
		return new SubConfiguration(this, key);
	}

	@Override
	public Set<String> getSubsectionKeys() {
		Set<String> keys = new HashSet<String>();
		Map<String, Object> values = getValues(true);

		for (String key : values.keySet()) {
			if (key == null) continue;

			int pos = key.indexOf(".");
			if (pos == -1) continue;

			String localKey = key.substring(0, pos);
			keys.add(localKey);
		}

		@SuppressWarnings({"null"})
		@NonNull
		Set<String> unmodifiable = Collections.unmodifiableSet(keys);
		return unmodifiable;
	}

	@Override
	public boolean containsKey(String key) {
		if (defaultsBacked) {
			return defaults.containsKey(key);
		} else {
			return properties.containsKey(key);
		}
	}

	@Override
	public void set(String key, Object value) {
		if (defaultsBacked && !defaults.containsKey(key)) {
			throw new IllegalConfigurationAccessException(
					"The defaults of this configuration does not contain this key.");
		}
		if (value instanceof Map<?, ?>) {
			throw new IllegalConfigurationAccessException("Cannot set map with #set. Use #setMap");
		}

		if (containsKey(key)) {
			properties.remove(key);
		} else {
			SubConfiguration sub = new SubConfiguration(this, key);
			Map<String, Object> values = sub.getValues(true);
			for (String path : values.keySet()) {
				properties.remove(path);
			}
		}

		if (value == null) {
			return;
		} else if (value instanceof Configuration) {
			Configuration config = (Configuration) value;
			properties.putAll(config.getValues(true));
		} else if (value instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) value;

			for (Entry<String, Object> entry : map.entrySet()) {
				if (entry == null) continue;
				String k = entry.getKey();
				Object v = entry.getValue();
				if (k == null || v == null) continue;

				if (v instanceof Map) {

				} else {

				}
			}
		} else {
			properties.put(key, value);
		}
	}

	// Map tools
	// vvvvvvvvv

	private static Map<String, Object> packMap(Configuration config, String path) {
		HashMap<String, Object> localValues = new HashMap<>();

		// First add own local elements to the map
		Map<String, Object> values = config.getValues(false);
		for (Entry<String, Object> entry : values.entrySet()) {
			if (entry == null) continue;
			String k = entry.getKey();
			Object v = entry.getValue();

			if (k == null || v == null) continue;
			int pos = k.lastIndexOf(".") + 1;
			String key = k.substring(pos);

			localValues.put(key, v);
		}

		// Then add any subsections
		Map<String, Configuration> subsections = config.getSubsections();
		for (Entry<String, Configuration> subsection : subsections.entrySet()) {
			if (subsection == null) continue;
			String key = subsection.getKey();
			Configuration subConfig = subsection.getValue();

			if (key == null || subConfig == null) continue;
			String newPath = key;
			if (path != null) {
				newPath = path + "." + key;
			}

			localValues.put(key, packMap(subConfig, newPath));
		}

		return localValues;
	}

	private static void unpackMap(Map<String, Object> resultMap, String path, Map<String, Object> map) {
		for (Entry<String, Object> entry : map.entrySet()) {
			if (entry == null) continue;
			String k = entry.getKey();
			Object v = entry.getValue();

			if (k == null || v == null) continue;
			if (path != null) {
				k = path + "." + k;
			}

			if (v instanceof Map) {
				@SuppressWarnings({"unchecked"})
				Map<String, Object> vMap = (Map<String, Object>) v;
				unpackMap(resultMap, k, vMap);
			} else {
				resultMap.put(k, v);
			}
		}
	}
}
