package ch.kanti_wohlen.klassenkasse.action.payments;

import java.util.Date;

import org.eclipse.jdt.annotation.NonNull;

import io.netty.buffer.ByteBuf;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.Payment;
import ch.kanti_wohlen.klassenkasse.framework.User;

public class ActionPaymentRemoved extends ActionPayment {

	public ActionPaymentRemoved(@NonNull Host host, Payment payment) {
		super(host, payment);
	}

	public ActionPaymentRemoved(@NonNull Host host) {
		super(host);
	}

	public ActionPaymentRemoved(long id, User creator, @NonNull Date date) {
		super(id, creator, date);
	}

	@Override
	public void readData(ByteBuf buf, Host host) {
		int paymentId = host.getIdMapper().getPaymentMapping(buf.readInt());
		payment = host.getPayments().get(paymentId);
	}

	@Override
	public void writeData(ByteBuf buf) {
		Payment payment = assertNotNull(this.payment);
		buf.writeInt(payment.getLocalId());
	}

	@Override
	public void apply(Host host) {
		Payment payment = assertNotNull(this.payment);
		checkState(false);

		host.updatePayment(payment, true);
		applied = true;
	}

	@Override
	public void undo(Host host) {
		Payment payment = assertNotNull(this.payment);
		checkState(true);

		host.updatePayment(payment, false);
		applied = false;
	}
}
