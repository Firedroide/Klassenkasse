package ch.kanti_wohlen.klassenkasse.action.payments;

import io.netty.buffer.ByteBuf;

import java.util.Date;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import ch.kanti_wohlen.klassenkasse.action.ActionCreationException;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.Payment;
import ch.kanti_wohlen.klassenkasse.framework.id.IdMapper;
import ch.kanti_wohlen.klassenkasse.util.BufferUtil;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

public class ActionPaymentUpdated extends ActionPayment {

	private @Nullable Date date;
	private @Nullable String description;
	private @Nullable MonetaryValue value;

	public ActionPaymentUpdated(@NonNull Host host, Payment payment, Date newDate, String newDescription, MonetaryValue newValue) {
		super(host, payment);

		date = (newDate != null) ? newDate : payment.getPaymentDate();
		description = (newDescription != null) ? newDescription : payment.getPaymentDescription();
		value = (newValue != null) ? newValue : payment.getMonetaryValue();
	}

	public ActionPaymentUpdated(@NonNull Host host) {
		super(host);
	}

	public ActionPaymentUpdated(long id) {
		super(id);
	}

	@Override
	public void readData(ByteBuf buf, Host host, IdMapper idMapper) throws ActionCreationException {
		int paymentId = idMapper.getPaymentMapping(buf.readInt());
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
		host.updatePayment(payment, true);
	}

	@Override
	public void undo(Host host) {
		Payment payment = assertNotNull(this.payment);
		checkState(true);

		swap(payment);
		host.updatePayment(payment, true);
	}

	private void swap(@NonNull Payment payment) {
		applied = !applied;

		Date oldDate = payment.getPaymentDate();
		String oldDescription = payment.getPaymentDescription();
		MonetaryValue oldValue = payment.getMonetaryValue();

		Date date = assertNotNull(this.date);
		String description = assertNotNull(this.description);
		MonetaryValue value = assertNotNull(this.value);

		payment.setPaymentDate(date);
		payment.setPaymentDescription(description);
		payment.setMonetaryValue(value);

		this.date = oldDate;
		this.description = oldDescription;
		this.value = oldValue;
	}
}
