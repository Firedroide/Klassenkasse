package ch.kanti_wohlen.klassenkasse.action.paymentUsers;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;

import ch.kanti_wohlen.klassenkasse.action.Action;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.Payment;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.framework.id.IdMapper;

@NonNullByDefault
public abstract class ActionPaymentUsers extends Action {

	protected final int paymentId;
	protected final Set<Integer> userIds;
	protected final boolean remove;

	public ActionPaymentUsers(int paymentId, Collection<Integer> userIds, boolean remove) {
		this.paymentId = paymentId;
		this.userIds = new HashSet<>(userIds);
		this.remove = remove;
	}

	public ActionPaymentUsers(Payment payment, Collection<User> users, boolean remove) {
		this.paymentId = payment.getLocalId();
		this.userIds = new HashSet<>();
		this.remove = remove;

		for (User user : users) {
			userIds.add(user.getLocalId());
		}
	}

	public ActionPaymentUsers(Payment payment, User[] users, boolean remove) {
		this.paymentId = payment.getLocalId();
		this.userIds = new HashSet<>();
		this.remove = remove;

		for (User user : users) {
			userIds.add(user.getLocalId());
		}
	}

	public ActionPaymentUsers(Host host, ByteBuf buffer, boolean remove) {
		IdMapper idMapper = host.getIdMapper();
		this.paymentId = idMapper.getPaymentMapping(buffer.readInt());
		this.userIds = new HashSet<>();
		this.remove = remove;

		while (buffer.isReadable(4)) {
			int userId = idMapper.getUserMapping(buffer.readInt());
			userIds.add(userId);
		}
	}

	public int getPaymentId() {
		return paymentId;
	}

	@SuppressWarnings("null")
	public Set<Integer> getUserIds() {
		return Collections.unmodifiableSet(userIds);
	}

	@Override
	public void writeData(ByteBuf buf) {
		buf.writeInt(paymentId);
		for (int userId : userIds) {
			buf.writeInt(userId);
		}
	}

	@Override
	protected void update(Host host, boolean apply) {
		boolean remove = (this.remove == apply);

		// First retrieve all users and payments
		Payment payment = assertNotNull(host.getPaymentById(paymentId));
		List<User> users = new ArrayList<>();

		for (int userId : userIds) {
			User user = host.getUserById(userId);
			if (user == null) {
				throw new IllegalStateException("User with the ID of " + userId + " did not exist!");
			}

			users.add(user);
		}

		// Add the users to / remove the users from the payment
		if (remove) {
			host.removeUsersFromPayment(payment, users);
		} else {
			host.addUsersToPayment(payment, users);
		}
	}
}
