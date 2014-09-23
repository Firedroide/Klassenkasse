package ch.kanti_wohlen.klassenkasse.framework;

import java.util.Date;

import org.eclipse.jdt.annotation.NonNullByDefault;

import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

@NonNullByDefault
public class Payment implements LocallyIdentifiable<Integer> {

	private final int id;

	private Date date;
	private String description;
	private MonetaryValue value;

	public Payment(Host host, Date date, String description, MonetaryValue value) {
		id = host.getIdProvider().generatePaymentId();
		this.date = date;
		this.description = description;
		this.value = value;
	}

	public Payment(int id, Date date, String description, MonetaryValue value) {
		this.id = id;
		this.date = date;
		this.description = description;
		this.value = value;
	}

	@SuppressWarnings("null")
	@Override
	public Integer getLocalId() {
		return id;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date newDate) {
		date = newDate;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String newDescription) {
		if (newDescription.isEmpty()) {
			throw new IllegalArgumentException("Description cannot be an empty String.");
		}
		description = newDescription;
	}

	public MonetaryValue getValue() {
		return value;
	}

	public void setValue(MonetaryValue newValue) {
		value = newValue;
	}
}
