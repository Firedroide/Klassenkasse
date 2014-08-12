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
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.ScalarStyle;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;

import ch.kanti_wohlen.klassenkasse.util.IllegalConfigurationAccessException;

public class Configuration {

	private final String name;
	private final boolean defaultsBacked;
	private Map<String, Object> properties;
	private Map<String, Object> defaults;

	private final Yaml yaml;

	public Configuration(String name, boolean defaultsBacked) {
		if (name == null || name.isEmpty()) throw new IllegalArgumentException("name was null or empty");

		this.name = name;
		this.defaultsBacked = defaultsBacked;

		DumperOptions options = new DumperOptions();
		options.setIndent(2);
		options.setDefaultFlowStyle(FlowStyle.BLOCK);
		options.setDefaultScalarStyle(ScalarStyle.PLAIN);
		yaml = new Yaml(options);

		loadDefaults();
		reload();
	}

	@SuppressWarnings("unchecked")
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

			defaults = (Map<String, Object>) yaml.loadAs(input, Map.class);
			defaults = resolveNestedMaps(defaults);
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
			if (entry.getValue() instanceof Map) {
				resolveNestedMaps(root, newPath, (Map<String, Object>) entry.getValue());
			} else {
				root.put(newPath, entry.getValue());
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

	public boolean containsKey(@NonNull String key) {
		if (defaultsBacked) {
			return defaults.containsKey(key);
		} else {
			return properties.containsKey(key);
		}
	}

	@SuppressWarnings("unchecked")
	@NonNullByDefault
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
	@NonNullByDefault
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

	@NonNullByDefault
	public Object get(String key) {
		return getValue(key, Object.class);
	}

	@NonNullByDefault
	public String getString(String key) {
		return getValue(key, String.class);
	}

	@NonNullByDefault
	public int getInteger(String key) {
		return getValue(key, Integer.class).intValue();
	}

	@NonNullByDefault
	public boolean getBoolean(String key) {
		return getValue(key, Boolean.class).booleanValue();
	}

	@NonNullByDefault
	public double getDouble(String key) {
		return getValue(key, Double.class).doubleValue();
	}

	public Set<Map.Entry<String, Object>> getValues() {
		HashSet<Map.Entry<String, Object>> values = new HashSet<>(defaults.entrySet());
		values.addAll(properties.entrySet());
		return Collections.unmodifiableSet(values);
	}

	public Object set(@NonNull String key, @NonNull Object value) {
		if (defaultsBacked && !defaults.containsKey(key)) {
			throw new IllegalConfigurationAccessException("");
		}
		if (value instanceof Map<?, ?>) {
			throw new IllegalConfigurationAccessException("Cannot set map with #set. Use #setMap");
		}

		Object previous = remove(key);
		properties.put(key, value);

		return previous;
	}

	public Object setMap(@NonNull String key, @NonNull Map<String, Object> value) {
		Object previous = remove(key);
		properties.put(key, value);
		resolveNestedMaps(properties, key, value);

		return previous;
	}

	private Object remove(@NonNull String key) {
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
