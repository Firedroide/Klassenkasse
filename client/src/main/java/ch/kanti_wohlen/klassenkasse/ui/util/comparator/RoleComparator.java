package ch.kanti_wohlen.klassenkasse.ui.util.comparator;

import java.util.Comparator;

import ch.kanti_wohlen.klassenkasse.framework.Role;

public class RoleComparator implements Comparator<Role> {

	@Override
	public int compare(Role o1, Role o2) {
		return Integer.compare(o1.getLocalId(), o2.getLocalId());
	}
}
