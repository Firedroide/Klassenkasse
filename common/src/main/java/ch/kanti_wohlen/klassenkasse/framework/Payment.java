package ch.kanti_wohlen.klassenkasse.framework;

import java.util.Date;

import org.eclipse.jdt.annotation.NonNullByDefault;

import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

@NonNullByDefault
public class Payment {

	private final int id;

	private Date paymentDate;
	private String description;
	private MonetaryValue value;

	public Payment(Host host, Date date, String description, MonetaryValue value) {
		id = host.getIdProvider().generatePaymentId();
		this.paymentDate = date;
		this.description = description;
		this.value = value;
	}

	public Payment(int id, Date date, String description, MonetaryValue value) {
		this.id = id;
		this.paymentDate = date;
		this.description = description;
		this.value = value;
	}

	public int getLocalId() {
		return id;
	}

	public Date getPaymentDate() {
		return paymentDate;
	}

	public void setPaymentDate(Date newPaymentDate) {
		paymentDate = newPaymentDate;
	}

	public String getPaymentDescription() {
		return description;
	}

	public void setPaymentDescription(String newDescription) {
		if (newDescription.isEmpty()) {
			throw new IllegalArgumentException("Description cannot be an empty String.");
		}
		description = newDescription;
	}

	public MonetaryValue getMonetaryValue() {
		return value;
	}

	public void setMonetaryValue(MonetaryValue newValue) {
		value = newValue;
	}
}
