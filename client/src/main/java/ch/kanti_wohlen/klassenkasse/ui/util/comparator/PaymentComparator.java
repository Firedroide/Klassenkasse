package ch.kanti_wohlen.klassenkasse.ui.util.comparator;

import java.util.Comparator;

import ch.kanti_wohlen.klassenkasse.framework.Payment;

public class PaymentComparator implements Comparator<Payment> {

	@Override
	public int compare(Payment o1, Payment o2) {
		int date = o1.getDate().compareTo(o2.getDate());

		if (date != 0) {
			return date;
		} else {
			return o1.getDescription().compareTo(o2.getDescription());
		}
	}
}
