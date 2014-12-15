package ch.kanti_wohlen.klassenkasse.action.payments;

import io.netty.buffer.ByteBuf;

import org.eclipse.jdt.annotation.NonNullByDefault;

import ch.kanti_wohlen.klassenkasse.action.UpdateType;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.Payment;

@NonNullByDefault
public class ActionPaymentUpdated extends ActionPayment {

	public ActionPaymentUpdated(Payment payment) {
		super(payment, UpdateType.UPDATE);
	}

	public ActionPaymentUpdated(Host host, ByteBuf buffer) {
		super(host, buffer, UpdateType.UPDATE);
	}
}
