package ch.kanti_wohlen.klassenkasse.action.payments;

import io.netty.buffer.ByteBuf;

import java.util.Date;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import ch.kanti_wohlen.klassenkasse.action.ActionCreationException;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.Payment;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.util.BufferUtil;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

public class ActionPaymentUpdated extends ActionPayment {

	private @Nullable Date date;
	private @Nullable String description;
	private @Nullable MonetaryValue value;

	public ActionPaymentUpdated(@NonNull Host host, Payment payment, Date newDate, String newDescription, MonetaryValue newValue) {
		super(host, payment);

		date = (newDate != null) ? newDate : payment.getDate();
		description = (newDescription != null) ? newDescription : payment.getDescription();
		value = (newValue != null) ? newValue : payment.getValue();
	}

	public ActionPaymentUpdated(@NonNull Host host) {
		super(host);
	}

	public ActionPaymentUpdated(long id, User creator, @NonNull Date date) {
		super(id, creator, date);
	}

	@Override
	public void readData(ByteBuf buf, Host host) throws ActionCreationException {
		int paymentId = host.getIdMapper().getPaymentMapping(buf.readInt());
		payment = host.getPayments().get(paymentId);
		if (payment == null) {
			throw new ActionCreationException("Inexistant payment (id = " + paymentId + ")");
		}

		date = new Date(buf.readLong());
		value = new MonetaryValue(buf.readLong());
		description = BufferUtil.readString(buf);
	}

	@Override
	public void apply(@NonNull Host host) {
		Payment payment = assertNotNull(this.payment);
		checkState(false);

		swap(payment);
		host.updatePayment(payment, false);
	}

	@Override
	public void undo(Host host) {
		Payment payment = assertNotNull(this.payment);
		checkState(true);

		swap(payment);
		host.updatePayment(payment, false);
	}

	public @NonNull Payment getUpdatedPayment() {
		Payment payment = assertNotNull(this.payment);
		Date date = assertNotNull(this.date);
		String description = assertNotNull(this.description);
		MonetaryValue value = assertNotNull(this.value);

		return new Payment(payment.getLocalId(), date, description, value);
	}

	private void swap(@NonNull Payment payment) {
		applied = !applied;

		Date oldDate = payment.getDate();
		String oldDescription = payment.getDescription();
		MonetaryValue oldValue = payment.getValue();

		Date date = assertNotNull(this.date);
		String description = assertNotNull(this.description);
		MonetaryValue value = assertNotNull(this.value);

		payment.setDate(date);
		payment.setDescription(description);
		payment.setValue(value);

		this.date = oldDate;
		this.description = oldDescription;
		this.value = oldValue;
	}
}
