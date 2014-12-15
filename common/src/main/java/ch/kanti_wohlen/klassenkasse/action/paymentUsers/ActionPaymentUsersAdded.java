package ch.kanti_wohlen.klassenkasse.action.paymentUsers;

import io.netty.buffer.ByteBuf;

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;

import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.Payment;
import ch.kanti_wohlen.klassenkasse.framework.User;

@NonNullByDefault
public class ActionPaymentUsersAdded extends ActionPaymentUsers {

	public ActionPaymentUsersAdded(int paymentId, Collection<Integer> userIds) {
		super(paymentId, userIds, false);
	}

	public ActionPaymentUsersAdded(Payment payment, Collection<User> users) {
		super(payment, users, false);
	}

	public ActionPaymentUsersAdded(Payment payment, User... users) {
		super(payment, users, false);
	}

	public ActionPaymentUsersAdded(Host host, ByteBuf buffer) {
		super(host, buffer, false);
	}
}
