package ch.kanti_wohlen.klassenkasse.action.payments.users;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import io.netty.buffer.ByteBuf;
import ch.kanti_wohlen.klassenkasse.action.payments.ActionPayment;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.Payment;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.framework.id.IdMapper;

public abstract class ActionPaymentUsers extends ActionPayment {

	protected @Nullable Collection<User> users;

	public ActionPaymentUsers(@NonNull Host host, Payment payment, Collection<User> users) {
		super(host, payment);
		this.users = Collections.unmodifiableCollection(users);
	}

	public ActionPaymentUsers(@NonNull Host host) {
		super(host);
	}

	public ActionPaymentUsers(long id, User creator, @NonNull Date date) {
		super(id, creator, date);
	}

	public Collection<User> getUsers() {
		return users;
	}

	@Override
	public void readData(ByteBuf buf, Host host) {
		IdMapper idMapper = host.getIdMapper();
		int paymentId = idMapper.getPaymentMapping(buf.readInt());
		List<User> userList = new ArrayList<User>(buf.readableBytes() / 4);
		while (buf.isReadable(4)) {
			int userId = idMapper.getUserMapping(buf.readInt());
			userList.add(host.getUserById(userId));
		}

		payment = host.getPayments().get(paymentId);
		this.users = Collections.unmodifiableCollection(userList);
	}

	@Override
	public void writeData(ByteBuf buf) {
		Payment payment = assertNotNull(this.payment);
		Collection<User> users = assertNotNull(this.users);

		buf.writeInt(payment.getLocalId());
		for (User user : users) {
			buf.writeInt(user.getLocalId());
		}
	}
}
