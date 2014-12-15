package ch.kanti_wohlen.klassenkasse.framework;

import java.util.Date;

import org.eclipse.jdt.annotation.Nullable;

import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

public final class Payment implements LocallyIdentifiable<Integer> {

	private final int id;

	private Date date;
	private String description;
	private MonetaryValue value;
	private MonetaryValue rounding;

	public Payment(Host host, Date date, String description, MonetaryValue value, MonetaryValue rounding) {
		id = host.getIdProvider().generatePaymentId();
		this.date = date;
		this.description = description;
		this.value = value;
		this.rounding = rounding;
	}

	public Payment(int id, Date date, String description, MonetaryValue value, MonetaryValue rounding) {
		this.id = id;
		this.date = date;
		this.description = description;
		this.value = value;
		this.rounding = rounding;
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

	public MonetaryValue getRoundingValue() {
		return rounding;
	}

	public void setRoundingValue(MonetaryValue newRoundingValue) {
		rounding = newRoundingValue;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (obj instanceof Payment) {
			return id == ((Payment) obj).id;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return id;
	}
}
