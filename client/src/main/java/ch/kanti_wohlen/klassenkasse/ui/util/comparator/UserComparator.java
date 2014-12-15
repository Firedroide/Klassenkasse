package ch.kanti_wohlen.klassenkasse.ui.util.comparator;

import java.util.Comparator;

import ch.kanti_wohlen.klassenkasse.framework.User;

public class UserComparator implements Comparator<User> {

	@Override
	public int compare(User o1, User o2) {
		// Sort null values to the top
		if (o1 == null) return -1;
		if (o2 == null) return 1;

		int group = Integer.compare(o1.getRoleId(), o2.getRoleId());
		if (group == 0) {
			return o1.getFullName().compareTo(o2.getFullName());
		} else {
			return group;
		}
	}
}
