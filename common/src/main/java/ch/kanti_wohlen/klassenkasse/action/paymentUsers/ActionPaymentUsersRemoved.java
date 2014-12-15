package ch.kanti_wohlen.klassenkasse.action.paymentUsers;

import java.util.Collection;

import io.netty.buffer.ByteBuf;

import org.eclipse.jdt.annotation.NonNullByDefault;

import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.Payment;
import ch.kanti_wohlen.klassenkasse.framework.User;

@NonNullByDefault
public class ActionPaymentUsersRemoved extends ActionPaymentUsers {

	public ActionPaymentUsersRemoved(int paymentId, Collection<Integer> userIds) {
		super(paymentId, userIds, true);
	}

	public ActionPaymentUsersRemoved(Payment payment, Collection<User> users) {
		super(payment, users, true);
	}

	public ActionPaymentUsersRemoved(Payment payment, User... users) {
		super(payment, users, true);
	}

	public ActionPaymentUsersRemoved(Host host, ByteBuf buffer) {
		super(host, buffer, true);
	}
}
