package ch.kanti_wohlen.klassenkasse.test;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import ch.kanti_wohlen.klassenkasse.action.Action;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.Payment;
import ch.kanti_wohlen.klassenkasse.framework.Role;
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.framework.id.IdMapper;
import ch.kanti_wohlen.klassenkasse.framework.id.IdProvider;
import ch.kanti_wohlen.klassenkasse.login.LoginProvider;
import ch.kanti_wohlen.klassenkasse.login.LoginServerException;

@SuppressWarnings("null")
public class FakeHost implements Host {

	private final IdProvider idProvider;
	private final LoginProvider loginProvider;

	public FakeHost() {
		idProvider = new IdProvider() {

			@Override
			public int generateUserId() {
				return 0;
			}

			@Override
			public int generatePaymentId() {
				return 0;
			}

			@Override
			public int generateClassId() {
				return 0;
			}

			@Override
			public long generateActionId() {
				return 0;
			}
		};

		loginProvider = new LoginProvider() {

			@Override
			public Collection<String> getEMailAddresses() {
				return Collections.emptyList();
			}

			@Override
			public String logIn(String eMailAddress, String password, boolean isToken) throws LoginServerException {
				return "";
			}
		};
	}

	@Override
	public String getName() {
		return "FakeHost";
	}

	@Override
	public LoginProvider getLoginProvider() {
		return loginProvider;
	}

	@Override
	public IdProvider getIdProvider() {
		return idProvider;
	}

	@Override
	public IdMapper getNewIdMapper() {
		return IdMapper.NULL_MAPPER;
	}

	@Override
	public Map<Integer, StudentClass> getClasses() {
		return Collections.emptyMap();
	}

	@Override
	public StudentClass getClassById(int classId) {
		return null;
	}

	@Override
	public void updateClass(StudentClass studentClass, boolean removed) {
		// Do nothing
	}

	@Override
	public Map<Integer, User> getUsers() {
		return Collections.emptyMap();
	}

	@Override
	public Map<Integer, User> getUsersByClass(int classId) {
		return Collections.emptyMap();
	}

	@Override
	public User getUserById(int userId) {
		return null;
	}

	@Override
	public void updateUser(User user, boolean removed) {
		// Do nothing
	}

	@Override
	public Map<Integer, Payment> getPayments() {
		return Collections.emptyMap();
	}

	@Override
	public Map<Integer, Payment> getPaymentsByUser(int userId) {
		return Collections.emptyMap();
	}

	@Override
	public Payment getPaymentById(int paymentId) {
		return null;
	}

	@Override
	public void updatePayment(Payment payment, boolean removed) {
		// Do nothing
	}

	@Override
	public void addUsersToPayment(Payment payment, User[] users) {
		// Do nothing
	}

	@Override
	public void removeUsersFromPayment(Payment payment, User[] users) {
		// Do nothing
	}

	@Override
	public Map<Integer, Role> getRoles() {
		return Collections.emptyMap();
	}

	@Override
	public Role getRoleById(int roleId) {
		return null;
	}

	@Override
	public Action getActionById(long actionId) {
		return null;
	}

	@Override
	public void addAction(Action action, User user) {
		// Do nothing
	}

	@Override
	public void setActionUndone(Action action, boolean undone) {
		// Do nothing
	}
}
