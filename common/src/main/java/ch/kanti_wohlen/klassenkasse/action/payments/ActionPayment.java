package ch.kanti_wohlen.klassenkasse.action.payments;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import io.netty.buffer.ByteBuf;
import ch.kanti_wohlen.klassenkasse.action.Action;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.Payment;
import ch.kanti_wohlen.klassenkasse.util.BufferUtil;

public abstract class ActionPayment extends Action {

	protected @Nullable Payment payment;

	public ActionPayment(@NonNull Host host, Payment payment) {
		super(host);
		this.payment = payment;
	}

	public ActionPayment(@NonNull Host host) {
		super(host);
	}

	public ActionPayment(long id) {
		super(id);
	}

	public Payment getPayment() {
		return payment;
	}

	@Override
	public void writeData(ByteBuf buf) {
		Payment payment = assertNotNull(this.payment);

		buf.writeInt(payment.getLocalId());
		buf.writeLong(payment.getPaymentDate().getTime());
		buf.writeLong(payment.getMonetaryValue().getCentValue());
		BufferUtil.writeString(buf, payment.getPaymentDescription());
	}
}
