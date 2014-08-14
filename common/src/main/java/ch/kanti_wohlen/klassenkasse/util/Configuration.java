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
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.ScalarStyle;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;

import ch.kanti_wohlen.klassenkasse.util.IllegalConfigurationAccessException;

@NonNullByDefault
public class Configuration {

	private final String name;
	private final boolean defaultsBacked;
	private Map<String, Object> properties;
	private Map<String, Object> defaults;

	private final Yaml yaml;

	public Configuration(String name, boolean defaultsBacked) {
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
					defaults = new HashMap<String, Object>();
					return;
				}
			}

			@SuppressWarnings({"null", "unchecked"})
			@NonNull
			Map<String, Object> rawMap = (Map<String, Object>) yaml.loadAs(input, Map.class);
			defaults = resolveNestedMaps(rawMap);
		} catch (IOException closeError) {
			closeError.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public void reload() {
		File file = new File(name);
		if (!file.exists()) {
			createDefaults();
		} else if (file.isDirectory()) {
			file.delete();
			createDefaults();
		}

		try (FileInputStream input = new FileInputStream(file)) {
			Map<String, Object> loaded = (Map<String, Object>) yaml.loadAs(input, Map.class);
			if (loaded == null) {
				properties = new HashMap<String, Object>();
			} else {
				properties = resolveNestedMaps(loaded);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			// File system error (exist and directory was checked)
		} catch (IOException closeError) {
			closeError.printStackTrace();
		}
	}

	private HashMap<String, Object> resolveNestedMaps(Map<String, Object> map) {
		HashMap<String, Object> result = new HashMap<String, Object>();
		resolveNestedMaps(result, "", map);
		return result;
	}

	@SuppressWarnings("unchecked")
	private void resolveNestedMaps(Map<String, Object> root, String path, Map<String, Object> element) {
		for (Entry<String, Object> entry : element.entrySet()) {
			String newPath = (path.isEmpty() ? entry.getKey() : path + "." + entry.getKey());
			if (newPath == null) continue;

			Object value = entry.getValue();
			if (value instanceof Map) {
				resolveNestedMaps(root, newPath, (Map<String, Object>) value);
			} else if (value != null) {
				root.put(newPath, value);
			}
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

	public void save() {
		try (FileWriter writer = new FileWriter(name)) {
			yaml.dump(properties, writer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean containsKey(String key) {
		if (defaultsBacked) {
			return defaults.containsKey(key);
		} else {
			return properties.containsKey(key);
		}
	}

	@SuppressWarnings("unchecked")
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

		return (T) obj;
	}

	@SuppressWarnings("unchecked")
	private <T> T getDefault(String key, Class<T> clazz) {
		Object def = defaults.get(key);
		if (def == null) {
			throw new IllegalConfigurationAccessException("Inexistant property: " + String.valueOf(key));
		} else if (!clazz.isAssignableFrom(def.getClass())) {
			throw new IllegalConfigurationAccessException("Wrong type for key \"" + String.valueOf(key)
					+ "\", requested " + clazz.getSimpleName() + ", was " + def.getClass().getSimpleName());
		}
		return (T) def;
	}

	public Object get(String key) {
		return getValue(key, Object.class);
	}

	public String getString(String key) {
		return getValue(key, String.class);
	}

	public int getInteger(String key) {
		return getValue(key, Integer.class).intValue();
	}

	public boolean getBoolean(String key) {
		return getValue(key, Boolean.class).booleanValue();
	}

	public double getDouble(String key) {
		return getValue(key, Double.class).doubleValue();
	}

	@SuppressWarnings("null")
	public Set<Map.Entry<String, Object>> getValues(boolean deep) {
		if (deep) {
			HashSet<Map.Entry<String, Object>> values = new HashSet<>(properties.entrySet());
			values.addAll(defaults.entrySet());
			return Collections.unmodifiableSet(values);
		} else {
			HashSet<Map.Entry<String, Object>> values = new HashSet<>();
			for (Map.Entry<String, Object> value : properties.entrySet()) {
				if (!value.getKey().contains(".")) {
					values.add(value);
				}
			}
			for (Map.Entry<String, Object> def : defaults.entrySet()) {
				if (!def.getKey().contains(".")) {
					values.add(def);
				}
			}
			return Collections.unmodifiableSet(values);
		}
	}

	public @Nullable Object set(String key, @Nullable Object value) {
		if (defaultsBacked && !defaults.containsKey(key)) {
			throw new IllegalConfigurationAccessException("");
		}
		if (value instanceof Map<?, ?>) {
			throw new IllegalConfigurationAccessException("Cannot set map with #set. Use #setMap");
		}

		Object previous = remove(key);
		if (value != null) properties.put(key, value);

		return previous;
	}

	public @Nullable Object setMap(String key, @Nullable Map<String, Object> value) {
		Object previous = remove(key);

		if (value != null) {
			properties.put(key, value);
			resolveNestedMaps(properties, key, value);
		}

		return previous;
	}

	private @Nullable Object remove(String key) {
		Object previous = properties.remove(key);
		if (previous instanceof Map) {
			Map<?, ?> map = (Map<?, ?>) previous;
			for (Object entry : map.keySet()) {
				if (entry instanceof String) {
					properties.remove(key + "." + entry);
				}
			}
		}
		return previous;
	}
}
