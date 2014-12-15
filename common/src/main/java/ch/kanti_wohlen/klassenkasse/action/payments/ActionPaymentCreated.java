package ch.kanti_wohlen.klassenkasse.action.payments;

import io.netty.buffer.ByteBuf;

import org.eclipse.jdt.annotation.NonNullByDefault;

import ch.kanti_wohlen.klassenkasse.action.UpdateType;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.Payment;

@NonNullByDefault
public class ActionPaymentCreated extends ActionPayment {

	public ActionPaymentCreated(Payment payment) {
		super(payment, UpdateType.CREATION);
	}

	public ActionPaymentCreated(Host host, ByteBuf buffer) {
		super(host, buffer, UpdateType.CREATION);
	}
}
