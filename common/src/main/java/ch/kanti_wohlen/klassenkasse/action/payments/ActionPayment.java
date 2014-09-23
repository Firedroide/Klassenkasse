package ch.kanti_wohlen.klassenkasse.action.payments;

import java.util.Date;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import io.netty.buffer.ByteBuf;
import ch.kanti_wohlen.klassenkasse.action.Action;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.Payment;
import ch.kanti_wohlen.klassenkasse.framework.User;
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

	public ActionPayment(long id, User creator, @NonNull Date date) {
		super(id, creator, date);
	}

	public Payment getPayment() {
		return payment;
	}

	@Override
	public void writeData(ByteBuf buf) {
		Payment payment = assertNotNull(this.payment);

		buf.writeInt(payment.getLocalId());
		buf.writeLong(payment.getDate().getTime());
		buf.writeLong(payment.getValue().getCentValue());
		BufferUtil.writeString(buf, payment.getDescription());
	}
}
