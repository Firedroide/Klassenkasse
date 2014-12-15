package ch.kanti_wohlen.klassenkasse.action.payments;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.netty.buffer.ByteBuf;
import ch.kanti_wohlen.klassenkasse.action.UpdateType;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.Payment;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

@NonNullByDefault
public class ActionPaymentRemoved extends ActionPayment {

	public ActionPaymentRemoved(Payment payment) {
		super(payment, UpdateType.REMOVAL);
		value = MonetaryValue.ZERO;
		rounding = MonetaryValue.ZERO;
	}

	public ActionPaymentRemoved(Host host, ByteBuf buffer) {
		super(host, buffer, UpdateType.REMOVAL);
		value = MonetaryValue.ZERO;
		rounding = MonetaryValue.ZERO;
	}
}
