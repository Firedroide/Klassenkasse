package ch.kanti_wohlen.klassenkasse.action.payments;

import org.eclipse.jdt.annotation.NonNull;

import io.netty.buffer.ByteBuf;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.Payment;
import ch.kanti_wohlen.klassenkasse.framework.id.IdMapper;

public class ActionPaymentRemoved extends ActionPayment {

	public ActionPaymentRemoved(@NonNull Host host, Payment payment) {
		super(host, payment);
	}

	public ActionPaymentRemoved(@NonNull Host host) {
		super(host);
	}

	public ActionPaymentRemoved(long id) {
		super(id);
	}

	@Override
	public void readData(ByteBuf buf, Host host, IdMapper idMapper) {
		int paymentId = idMapper.getPaymentMapping(buf.readInt());
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
