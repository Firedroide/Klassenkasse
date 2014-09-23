package ch.kanti_wohlen.klassenkasse.action.payments.users;

import java.util.Collection;
import java.util.Date;

import org.eclipse.jdt.annotation.NonNull;

import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.Payment;
import ch.kanti_wohlen.klassenkasse.framework.User;

public class ActionPaymentUsersRemoved extends ActionPaymentUsers {

	public ActionPaymentUsersRemoved(@NonNull Host host, Payment payment, Collection<User> users) {
		super(host, payment, users);
	}

	public ActionPaymentUsersRemoved(@NonNull Host host) {
		super(host);
	}

	public ActionPaymentUsersRemoved(long id, User creator, @NonNull Date date) {
		super(id, creator, date);
	}

	@Override
	public void apply(Host host) {
		Payment payment = assertNotNull(this.payment);
		Collection<User> users = assertNotNull(this.users);
		checkState(false);

		host.removeUsersFromPayment(payment, users);
		applied = true;
	}

	@Override
	public void undo(Host host) {
		Payment payment = assertNotNull(this.payment);
		Collection<User> users = assertNotNull(this.users);
		checkState(true);

		host.addUsersToPayment(payment, users);
		applied = false;
	}
}
