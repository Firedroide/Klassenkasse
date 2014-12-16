package ch.kanti_wohlen.klassenkasse.framework;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Represents a permissions role that many {@linkplain User Users} can be a part of.
 * <p>
 * Roles have an ID, a name and a {@link String} of permissions, which can be expanded to a {@link Set}.
 * </p>
 */
public final class Role implements LocallyIdentifiable<Integer> {

	private final int id;
	private final String name;
	private final String permString;
	private final Set<String> perms;

	/**
	 * Creates a new {@link Role} with a given ID.
	 * 
	 * @param id
	 *            the ID to be given which should be unique
	 * @param name
	 *            its name, not {@code null} or empty
	 * @param permissions
	 *            the permissions this role should grant its users, not {@code null} or empty
	 */
	public Role(int id, String name, String permissions) {
		this.id = id;
		this.name = name;
		this.permString = permissions;
		perms = new HashSet<String>(Arrays.asList(permissions.split(",")));
	}

	@SuppressWarnings("null")
	@Override
	public Integer getLocalId() {
		return id;
	}

	/**
	 * Gets the name of this {@link Role}.
	 * 
	 * @return the name, never {@code null}
	 */
	public String getName() {
		return name;
	}

	/**
	 * Tests whether a {@link Role} has a permission or a permission that is superior to the one supplied.
	 * 
	 * @param permission
	 *            the permission to be tested
	 * @return true if the user has this permission
	 */
	public boolean hasPermission(String permission) {
		if (permission.isEmpty() || permission.startsWith(".") || permission.endsWith(".")) {
			throw new IllegalArgumentException("Illegal permission " + String.valueOf(permission));
		}

		if (perms.contains("*")) return true;
		for (String perm : perms) {
			if (permission.startsWith(perm)) {
				if (permission.length() == perm.length()) {
					return true;
				} else if (permission.charAt(perm.length()) == '.') {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Checks if the {@link Role} has any permissions that are inferior to the one supplied
	 * 
	 * @param permission
	 *            the root permission
	 * @return true if this user has any subpermissions of the supplied permission
	 */
	public boolean hasAnySubpermission(String permission) {
		if (hasPermission(permission)) return true;

		for (String perm : perms) {
			if (perm.startsWith(permission)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Gets a {@link Set} of all permissions this {@link Role} will grant its users.
	 * 
	 * @return the set of permissions.
	 */
	@SuppressWarnings("null")
	public Set<String> getPermissions() {
		return Collections.unmodifiableSet(perms);
	}

	/**
	 * Gets the string of comma separated permissions that can be stored in a database.
	 * 
	 * @return the permissions string
	 */
	public String getPermissionsString() {
		return permString;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (obj instanceof Role) {
			return id == ((Role) obj).id;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return id;
	}
}
