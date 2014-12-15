package ch.kanti_wohlen.klassenkasse.database;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import ch.kanti_wohlen.klassenkasse.action.UpdateType;
import ch.kanti_wohlen.klassenkasse.framework.LocallyIdentifiable;

// TODO: Handle all SQLExceptions
@NonNullByDefault
public abstract class CachedDatabaseSection<K extends Number, V extends LocallyIdentifiable<K>> extends DatabaseSection<K, V> {

	private final Map<K, V> cache;
	private boolean allLoaded;

	public CachedDatabaseSection(Database database, String tableName, String createSql) throws SQLException {
		super(database, tableName, createSql);
		cache = new HashMap<>();
		allLoaded = false;
	}

	@Override
	public Map<K, V> getAll() {
		if (!allLoaded) {
			try {
				Map<K, V> result = loadAll();
				cache.putAll(result);
				allLoaded = true;
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return cache;
	}

	@Override
	public @Nullable V getById(K id) {
		V value = cache.get(id);
		if (value != null) return value;

		return super.getById(id);
	}

	@Override
	public void update(V value, UpdateType updateType) {
		try {
			save(value, updateType);

			boolean remove;
			switch (updateType) {
			case CREATION:
				remove = false;
				break;
			case REMOVAL:
				remove = true;
				break;
			case UPDATE:
				remove = !cache.containsKey(value.getLocalId());
				break;
			default:
				throw new IllegalArgumentException("Unknown UpdateType.");
			}

			if (remove) {
				cache.remove(value.getLocalId());
			} else {
				cache.put(value.getLocalId(), value);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
