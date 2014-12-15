package ch.kanti_wohlen.klassenkasse.framework;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;

import ch.kanti_wohlen.klassenkasse.action.ActionSearchQuery;
import ch.kanti_wohlen.klassenkasse.action.BaseAction;
import ch.kanti_wohlen.klassenkasse.action.UpdateType;
import ch.kanti_wohlen.klassenkasse.framework.id.IdMapper;
import ch.kanti_wohlen.klassenkasse.framework.id.IdProvider;
import ch.kanti_wohlen.klassenkasse.framework.id.LocalIdProvider;
import ch.kanti_wohlen.klassenkasse.login.LoginProvider;
import ch.kanti_wohlen.klassenkasse.login.LoginServerException;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

@SuppressWarnings("null")
public class TestHost implements Host {

	private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.GERMANY);

	private final IdProvider idProvider = new LocalIdProvider();
	private final LoginProvider loginProvider = new LoginProvider() {

		@Override
		public Collection<String> getUsernames() {
			return Collections.unmodifiableList(eMails);
		}

		@Override
		public @Nullable String logIn(Host host, String eMailAddress, char[] password, boolean isToken)
				throws LoginServerException {
			return null;
		}
	};

	private final List<String> eMails = new ArrayList<>();
	private final Map<Integer, StudentClass> studentClasses = new HashMap<>();
	private final Map<Integer, Role> roles = new HashMap<>();
	private final Map<Integer, User> users = new HashMap<>();
	private final Map<String, User> eMailUsers = new HashMap<>();
	private final Map<Integer, Payment> payments = new HashMap<>();
	private final Map<Integer, List<Integer>> paymentUsers = new HashMap<>();
	private final Map<Integer, List<Integer>> userPayments = new HashMap<>();

	public TestHost() {
		setUp();
	}

	private void putClass(int id, String name) {
		studentClasses.put(id, new StudentClass(id, name, MonetaryValue.ZERO, MonetaryValue.ZERO));
	}

	private void putRole(int id, String name, String perms) {
		roles.put(id, new Role(id, name, perms));
	}

	private void putUser(int id, String name, int roleId, int classId) {
		String[] names = name.split("\\s");
		String eMail = eMails.get(id);

		User user = new User(id, classId, roleId, names[0], names[1], eMail, MonetaryValue.ZERO);
		users.put(id, user);
		eMailUsers.put(eMail, user);
	}

	private void putPayment(int id, String date, String description, long value, int... userIds) {
		Payment payment = null;
		try {
			payment = new Payment(id, DATE_FORMAT.parse(date), description, new MonetaryValue(value),
					MonetaryValue.ZERO);
		} catch (ParseException e) {
			e.printStackTrace();
			return;
		}

		int paymentId = payment.getLocalId();
		payments.put(paymentId, payment);
		for (int userId : userIds) {
			List<Integer> pu = paymentUsers.get(paymentId);
			if (pu == null) {
				pu = new ArrayList<>();
				paymentUsers.put(paymentId, pu);
			}
			pu.add(userId);

			List<Integer> up = userPayments.get(userId);
			if (up == null) {
				up = new ArrayList<>();
				userPayments.put(userId, up);
			}
			up.add(paymentId);
		}
	}

	private void setUp() {
		eMails.add("super.user@localhost.com");
		eMails.add("stu.dent@x.com");
		eMails.add("well.well.well@welcome.to");
		eMails.add("my.lair.chell@and.a");
		eMails.add("potato.oh.you@got.me");
		eMails.add("scared.so@you.re");

		putClass(0, "No-mans land");
		putClass(1, "2014A");
		putClass(2, "2014B");
		putClass(-1, "2016C");
		idProvider.generateClassId();

		putRole(0, "SuperUserRole", "*");
		putRole(1, "Schüler", "");
		putRole(2, "Klassenkassier", "");
		putRole(3, "Lehrer", "");
		putRole(4, "Administator", "");

		putUser(0, "Super User", 0, 0);
		putUser(1, "Stu Dänt", 1, 1);
		putUser(2, "Chell Aperture", 3, 1);
		putUser(3, "Fancy Pants Man", 2, 1);

		putPayment(1, "1.1.1970", "UNIX", 20000, 1, 2);
		putPayment(2, "12.08.2014", "Development continues.", -12345, 1, 3);
		// putPayment(3, "11.09.2001", "Strange transaction...", Long.MIN_VALUE, 2);

		// for (User user : users.values()) {
		// user.recalculateBalance(this, false);
		// }
		// for (StudentClass studentClass : studentClasses.values()) {
		// studentClass.recalculateBalance(this);
		// }
	}

	@Override
	public String getName() {
		return "TestHost";
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
	public IdMapper getIdMapper() {
		return IdMapper.SELF_MAPPER;
	}

	@Override
	public Map<Integer, StudentClass> getClasses() {
		return Collections.unmodifiableMap(studentClasses);
	}

	@Override
	public @Nullable StudentClass getClassById(int classId) {
		return studentClasses.get(classId);
	}

	@Override
	public void updateClass(StudentClass studentClass, UpdateType updateType) {
		if (updateType == UpdateType.REMOVAL) {
			studentClasses.remove(studentClass.getLocalId());
		} else {
			studentClasses.put(studentClass.getLocalId(), studentClass);
		}
	}

	@Override
	public Map<String, String> getPrintingVariablesForClass(int classId) {
		return Collections.emptyMap();
	}

	@Override
	public void updatePrintingVariablesForClass(int classId, Map<String, String> variables) {
		// Not implemented
	}

	@Override
	public Map<Integer, User> getUsers() {
		return Collections.unmodifiableMap(users);
	}

	@Override
	public Map<Integer, User> getUsersByClass(int classId) {
		Map<Integer, User> classUsers = new HashMap<>();
		for (User user : users.values()) {
			if (user == null) continue;
			if (user.getStudentClassId() == classId) {
				classUsers.put(user.getLocalId(), user);
			}
		}
		return classUsers;
	}

	@Override
	public Map<Integer, User> getUsersWithPayment(int paymentId) {
		Map<Integer, User> result = new HashMap<>();
		List<Integer> userIds = paymentUsers.get(paymentId);
		if (userIds == null) return Collections.emptyMap();

		for (int userId : userIds) {
			User user = users.get(userId);
			if (user == null) continue;
			result.put(user.getLocalId(), user);
		}

		return result;
	}

	@Override
	public @Nullable User getUserById(int userId) {
		return users.get(userId);
	}

	@Override
	public @Nullable User getUserByUsername(String eMailAddress) {
		return eMailUsers.get(eMailAddress);
	}

	@Override
	public @Nullable User getLoggedInUser() {
		return getUserById(0);
	}

	public void setLoggedInUser(User user) {
		throw new IllegalStateException("Already logged in");
	}

	@Override
	public void updateUser(User user, UpdateType updateType) {
		if (updateType == UpdateType.REMOVAL) {
			users.remove(user.getLocalId());
		} else {
			users.put(user.getLocalId(), user);
		}
	}

	@Override
	public Map<Integer, Payment> getPayments() {
		return Collections.unmodifiableMap(payments);
	}

	@Override
	public Map<Integer, Payment> getPaymentsByUser(int userId) {
		Map<Integer, Payment> result = new HashMap<>();
		List<Integer> paymentIds = userPayments.get(userId);
		if (paymentIds == null) return Collections.emptyMap();

		for (int paymentId : paymentIds) {
			Payment payment = payments.get(paymentId);
			if (payment == null) continue;
			result.put(payment.getLocalId(), payment);
		}

		return result;
	}

	@Override
	public @Nullable Payment getPaymentById(int paymentId) {
		return payments.get(paymentId);
	}

	@Override
	public void updatePayment(Payment payment, UpdateType updateType) {
		if (updateType == UpdateType.REMOVAL) {
			payments.remove(payment.getLocalId());
		} else {
			payments.put(payment.getLocalId(), payment);
		}

		recalculateBalances(getUsersWithPayment(payment.getLocalId()).values());
	}

	@Override
	public void addUsersToPayment(Payment payment, Collection<User> users) {
		List<Integer> pu = paymentUsers.get(payment);
		if (pu == null) {
			pu = new ArrayList<>();
			paymentUsers.put(payment.getLocalId(), pu);
		}

		for (User user : users) {
			List<Integer> up = userPayments.get(user.getLocalId());
			if (up == null) {
				up = new ArrayList<>();
				userPayments.put(user.getLocalId(), up);
			}

			up.add(payment.getLocalId());
			pu.add(user.getLocalId());
		}

		recalculateBalances(users);
	}

	@Override
	public void removeUsersFromPayment(Payment payment, Collection<User> users) {
		List<Integer> pu = paymentUsers.get(payment.getLocalId());
		if (pu == null) return;

		for (User user : users) {
			List<Integer> up = userPayments.get(user.getLocalId());
			if (up == null) continue;

			up.remove(payment.getLocalId());
			pu.remove(user.getLocalId());
		}

		recalculateBalances(users);

		if (pu.isEmpty()) {
			updatePayment(payment, UpdateType.UPDATE);
		}
	}

	@Override
	public Map<Integer, Role> getRoles() {
		return Collections.unmodifiableMap(roles);
	}

	@Override
	public @Nullable Role getRoleById(int roleId) {
		return roles.get(roleId);
	}

	@Override
	public @Nullable BaseAction getActionById(long actionId) {
		return null;
	}

	@Override
	public Map<Long, BaseAction> searchActions(ActionSearchQuery searchQuery) {
		return Collections.emptyMap();
	}

	@Override
	public void addActions(BaseAction... actions) {
		for (BaseAction base : actions) {
			base.apply(this);
		}
	}

	@Override
	public void setActionsUndone(boolean undone, BaseAction... actions) {
		for (BaseAction baseAction : actions) {
			if (undone) {
				baseAction.apply(this);
			} else {
				baseAction.undo(this);
			}
		}
	}

	private void recalculateBalances(Collection<User> users) {
//		Set<StudentClass> classes = new HashSet<>();
//		for (User user : users) {
//			classes.add(user.getStudentClass(this));
//			user.recalculateBalance(this, false);
//		}
//
//		for (StudentClass studentClass : classes) {
//			studentClass.recalculateBalance(this);
//		}
	}
}
