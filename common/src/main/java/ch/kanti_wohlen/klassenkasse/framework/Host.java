package ch.kanti_wohlen.klassenkasse.framework;

import java.util.Collection;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import ch.kanti_wohlen.klassenkasse.action.Action;
import ch.kanti_wohlen.klassenkasse.framework.id.IdMapper;
import ch.kanti_wohlen.klassenkasse.framework.id.IdProvider;
import ch.kanti_wohlen.klassenkasse.login.LoginProvider;

// TODO: JavaDoc of methods
/**
 * The {@code Host} is the middleman between the stored data and any object
 * wanting to retrieve or modify the data.<br>
 * <p>
 * The methods offered by {@code Host} are synchronous and can take a long time
 * to complete, as they may rely on database access or network activity. It is
 * therefore not advisable to run these methods in threads that need to complete
 * quickly, e.g. a UI thread.
 * </p>
 * <p>
 * A {@code Host} also provides methods to get the {@link IdProvider} for this
 * set of data, which provides unique client or server IDs for different kinds
 * of objects.<br>
 * Furthermore it is able to create {@link IdMapper}s for each connection which
 * map these clientIDs to the appropriate serverIDs. A client implementation
 * should return an {@code IdMapper} that maps the IDs to themselves.
 * </p>
 * 
 * @author Roger Baumgartner
 */
@NonNullByDefault
public interface Host {

	String getName();

	LoginProvider getLoginProvider();

	IdProvider getIdProvider();

	IdMapper getIdMapper();

	// Classes
	Map<Integer, StudentClass> getClasses();

	@Nullable StudentClass getClassById(int classId);

	void updateClass(StudentClass studentClass, boolean removed);

	// Users
	Map<Integer, User> getUsers();

	Map<Integer, User> getUsersByClass(int classId);

	Map<Integer, User> getUsersWithPayment(int paymentId);

	@Nullable User getUserById(int userId);

	void updateUser(User user, boolean removed);

	// Payments
	Map<Integer, Payment> getPayments();

	Map<Integer, Payment> getPaymentsByUser(int userId);

	@Nullable Payment getPaymentById(int paymentId);

	void updatePayment(Payment payment, boolean removed);

	// Payment-Users
	void addUsersToPayment(Payment payment, Collection<User> users);

	void removeUsersFromPayment(Payment payment, Collection<User> users);

	// Roles
	Map<Integer, Role> getRoles();

	@Nullable Role getRoleById(int roleId);

	// Actions
	@Nullable Action getActionById(long actionId);

	void addAction(Action action);

	void setActionUndone(Action action, boolean undone);
}
