package ch.kanti_wohlen.klassenkasse.action.payments.users;

import java.util.ArrayList;
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

	protected @Nullable User[] users;

	public ActionPaymentUsers(@NonNull Host host, Payment payment, User[] users) {
		super(host, payment);
		this.users = users;
	}

	public ActionPaymentUsers(@NonNull Host host) {
		super(host);
	}

	public ActionPaymentUsers(long id) {
		super(id);
	}

	public User[] getUsers() {
		return users;
	}

	@Override
	public void readData(ByteBuf buf, Host host, IdMapper idMapper) {
		int paymentId = idMapper.getPaymentMapping(buf.readInt());
		List<User> userList = new ArrayList<User>(buf.readableBytes() / 4);
		while (buf.isReadable(4)) {
			int userId = idMapper.getUserMapping(buf.readInt());
			userList.add(host.getUserById(userId));
		}

		payment = host.getPayments().get(paymentId);
		this.users = userList.toArray(new User[0]);
	}

	@Override
	public void writeData(ByteBuf buf) {
		Payment payment = assertNotNull(this.payment);
		User[] users = assertNotNull(this.users);

		buf.writeInt(payment.getLocalId());
		for (User user : users) {
			buf.writeInt(user.getLocalId());
		}
	}
}
