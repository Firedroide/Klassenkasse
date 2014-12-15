package ch.kanti_wohlen.klassenkasse.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;

import ch.kanti_wohlen.klassenkasse.framework.LocallyIdentifiable;

//TODO: Handle all SQLExceptions
@NonNullByDefault
public abstract class LinkerSection<K1 extends Number, V1 extends LocallyIdentifiable<K1>, K2 extends Number, V2 extends LocallyIdentifiable<K2>> {

	protected final Database database;
	protected final DatabaseSection<K1, V1> leftSection;
	protected final DatabaseSection<K2, V2> rightSection;

	public LinkerSection(Database database, String tableName, String createSql, DatabaseSection<K1, V1> left,
			DatabaseSection<K2, V2> right) throws SQLException {
		this.database = database;
		database.createTable(database.openConnection(), tableName, createSql);

		leftSection = left;
		rightSection = right;
	}

	public Map<K2, V2> getLeft(K1 id) {
		try {
			return loadByLeft(id);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return new HashMap<>();
	}

	public Map<K1, V1> getRight(K2 id) {
		try {
			return loadByRight(id);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return new HashMap<>();
	}

	@SuppressWarnings("null")
	public void updateSingle(K1 left, K2 right, boolean removed) {
		updateLeftToRight(left, Arrays.asList(right), removed);
	}

	@SuppressWarnings("null")
	public void updateSingle(V1 left, V2 right, boolean removed) {
		updateLeftToRight(left.getLocalId(), Arrays.asList(right.getLocalId()), removed);
	}

	public void updateLeftToRight(K1 left, Collection<K2> rights, boolean removed) {
		try {
			storeLeftToRight(left, rights, removed);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void updateLeftToRight(V1 left, Collection<V2> rights, boolean removed) {
		K1 k1 = left.getLocalId();
		List<K2> k2s = new ArrayList<>();
		for (V2 right : rights) {
			k2s.add(right.getLocalId());
		}
		updateLeftToRight(k1, k2s, removed);
	}

	public void updateRightToLeft(K2 right, Collection<K1> lefts, boolean removed) {
		try {
			storeRightToLeft(right, lefts, removed);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void updateRightToLeft(V2 right, Collection<V1> lefts, boolean removed) {
		K2 k2 = right.getLocalId();
		List<K1> k1s = new ArrayList<>();
		for (V1 left : lefts) {
			k1s.add(left.getLocalId());
		}
		updateRightToLeft(k2, k1s, removed);
	}

	protected abstract Map<K2, V2> loadByLeft(K1 id) throws SQLException;

	protected abstract Map<K1, V1> loadByRight(K2 id) throws SQLException;

	protected abstract void storeLeftToRight(K1 left, Collection<K2> rights, boolean removed) throws SQLException;

	protected abstract void storeRightToLeft(K2 right, Collection<K1> lefts, boolean removed) throws SQLException;
}
