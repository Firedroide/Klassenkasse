package ch.kanti_wohlen.klassenkasse.action.payments.users;

import java.util.Collection;
import java.util.Date;

import org.eclipse.jdt.annotation.NonNull;

import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.Payment;
import ch.kanti_wohlen.klassenkasse.framework.User;

public class ActionPaymentUsersAdded extends ActionPaymentUsers {

	public ActionPaymentUsersAdded(@NonNull Host host, Payment payment, Collection<User> users) {
		super(host, payment, users);
	}

	public ActionPaymentUsersAdded(@NonNull Host host) {
		super(host);
	}

	public ActionPaymentUsersAdded(long id, User creator, @NonNull Date date) {
		super(id, creator, date);
	}

	@Override
	public void apply(Host host) {
		Payment payment = assertNotNull(this.payment);
		Collection<User> users = assertNotNull(this.users);
		checkState(false);

		host.addUsersToPayment(payment, users);
		applied = true;
	}

	@Override
	public void undo(Host host) {
		Payment payment = assertNotNull(this.payment);
		Collection<User> users = assertNotNull(this.users);
		checkState(true);

		host.removeUsersFromPayment(payment, users);
		applied = false;
	}
}
