package ch.kanti_wohlen.klassenkasse.ui.util.comparator;

import java.util.Comparator;

import ch.kanti_wohlen.klassenkasse.framework.StudentClass;

public class StudentClassComparator implements Comparator<StudentClass> {

	@Override
	public int compare(StudentClass o1, StudentClass o2) {
		// Sort null values to the top
		if (o1 == null) return -1;
		if (o2 == null) return 1;

		return o1.getName().compareTo(o2.getName());
	}
}
