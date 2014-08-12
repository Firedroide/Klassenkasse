package ch.kanti_wohlen.klassenkasse.action.payments;

import java.util.Date;

import org.eclipse.jdt.annotation.NonNull;

import io.netty.buffer.ByteBuf;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.Payment;
import ch.kanti_wohlen.klassenkasse.framework.id.IdMapper;
import ch.kanti_wohlen.klassenkasse.util.BufferUtil;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

public class ActionPaymentCreated extends ActionPayment {

	private boolean isRestore = false;

	public ActionPaymentCreated(@NonNull Host host, Payment payment) {
		super(host, payment);
	}

	public ActionPaymentCreated(@NonNull Host host) {
		super(host);
	}

	public ActionPaymentCreated(long id) {
		super(id);
		isRestore = true;
	}

	@Override
	public void readData(ByteBuf buf, Host host, IdMapper idMapper) {
		int clientPaymentId = buf.readInt();
		Date date = new Date(buf.readLong());
		MonetaryValue value = new MonetaryValue(buf.readLong());
		String description = BufferUtil.readString(buf);

		if (isRestore) {
			payment = new Payment(clientPaymentId, date, description, value);
		} else {
			payment = new Payment(host, date, description, value);
			idMapper.mapPayment(clientPaymentId, payment.getLocalId());
		}
	}

	@Override
	public void apply(Host host) {
		Payment payment = assertNotNull(this.payment);
		checkState(false);

		host.updatePayment(payment, false);
		applied = true;
	}

	@Override
	public void undo(Host host) {
		Payment payment = assertNotNull(this.payment);
		checkState(true);

		host.updatePayment(payment, true);
		applied = false;
	}
}
