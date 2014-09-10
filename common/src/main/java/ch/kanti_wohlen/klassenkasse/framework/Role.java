package ch.kanti_wohlen.klassenkasse.framework;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class Role implements LocallyIdentifiable<Integer> {

	private final int id;
	private final String name;
	private final String permString;
	private final Set<String> perms;

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

	public String getName() {
		return name;
	}

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

	public String getAllPermissions() {
		return permString;
	}
}
