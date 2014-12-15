package ch.kanti_wohlen.klassenkasse.action.payments;

import io.netty.buffer.ByteBuf;

import java.util.Date;

import org.eclipse.jdt.annotation.NonNullByDefault;

import ch.kanti_wohlen.klassenkasse.action.Action;
import ch.kanti_wohlen.klassenkasse.action.UpdateType;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.Payment;
import ch.kanti_wohlen.klassenkasse.util.BufferUtil;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;
import ch.kanti_wohlen.klassenkasse.util.PaymentHelper;

@NonNullByDefault
public abstract class ActionPayment extends Action {

	protected final UpdateType type;
	protected final int paymentId;

	protected Date date;
	protected String description;
	protected MonetaryValue value;
	protected MonetaryValue rounding;

	public ActionPayment(Payment payment, UpdateType type) {
		this.type = type;
		this.paymentId = payment.getLocalId();

		this.date = payment.getDate();
		this.description = payment.getDescription();
		this.value = payment.getValue();
		this.rounding = payment.getRoundingValue();
	}

	public ActionPayment(Host host, ByteBuf buffer, UpdateType type) {
		this.type = type;

		int clientId = buffer.readInt();
		this.date = new Date(buffer.readLong());
		this.description = BufferUtil.readString(buffer);
		this.value = new MonetaryValue(buffer.readLong());
		this.rounding = new MonetaryValue(buffer.readLong());

		if (type == UpdateType.CREATION) {
			if (clientId < 0) { // TODO: Better solution?
				this.paymentId = host.getIdProvider().generatePaymentId();
				host.getIdMapper().mapPayment(clientId, this.paymentId);
			} else {
				this.paymentId = clientId;
			}
		} else {
			this.paymentId = host.getIdMapper().getPaymentMapping(clientId);
		}
	}

	public int getPaymentId() {
		return paymentId;
	}

	public Date getPaymentDate() {
		return date;
	}

	public String getPaymentDescription() {
		return description;
	}

	public MonetaryValue getPaymentValue() {
		return value;
	}

	public MonetaryValue getPaymentRounding() {
		return rounding;
	}

	@Override
	protected void update(Host host, boolean apply) {
		Payment current = host.getPaymentById(paymentId);
		UpdateType actualType = apply ? type : type.reverse();
		checkState(current, actualType);

		Payment update = new Payment(paymentId, date, description, value, rounding);

		if (current != null) {
			PaymentHelper.updatePaymentValue(host, paymentId, current.getValue(), value);
		}

		PaymentHelper.changeRoundingValue(host, paymentId, rounding);
		host.updatePayment(update, actualType);

		if (current != null) {
			date = current.getDate();
			description = current.getDescription();
			value = current.getValue();
		}
	}

	@Override
	public void writeData(ByteBuf buf) {
		buf.writeInt(paymentId);
		buf.writeLong(date.getTime());
		BufferUtil.writeString(buf, description);
		buf.writeLong(value.getCentValue());
		buf.writeLong(rounding.getCentValue());
	}
}
