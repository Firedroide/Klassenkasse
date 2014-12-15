package ch.kanti_wohlen.klassenkasse.framework;

import java.util.Collection;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;

import ch.kanti_wohlen.klassenkasse.action.Action;
import ch.kanti_wohlen.klassenkasse.action.ActionSearchQuery;
import ch.kanti_wohlen.klassenkasse.action.BaseAction;
import ch.kanti_wohlen.klassenkasse.action.UpdateType;
import ch.kanti_wohlen.klassenkasse.framework.id.IdMapper;
import ch.kanti_wohlen.klassenkasse.framework.id.IdProvider;
import ch.kanti_wohlen.klassenkasse.login.LoginProvider;

/**
 * The {@code Host} is the middleman between the stored data and any object
 * wanting to retrieve or modify the data.
 * <p>
 * The methods offered by {@code Host} are synchronous and can take a long time to complete, as they may rely on
 * database access or network activity. It is therefore not advisable to run these methods in threads that need to
 * complete quickly, e.g. a UI thread.
 * </p>
 * <p>
 * A {@code Host} also provides methods to get the {@link IdProvider} for this set of data, which provides unique client
 * or server IDs for different kinds of objects.<br>
 * Furthermore it is able to create {@link IdMapper}s for each connection which map these clientIDs to the appropriate
 * serverIDs. A client implementation should return an {@code IdMapper} that maps the IDs to themselves.
 * </p>
 * <p>
 * There will usually be one host for one connection and one logged in {@link User}, so a client usually has one
 * {@code Host}, while a server side implementation will have several {@code Host}s.
 * </p>
 * 
 * @author Roger Baumgartner
 */
public interface Host {

	/**
	 * Gets the name of this {@link Host}, which shall never be {@code null}.
	 * 
	 * @return the name of this {@code Host}
	 */
	String getName();

	/**
	 * Gets this {@link Host}'s {@link LoginProvider}, which can be shared between different instances.
	 * 
	 * @return this {@code Host}'s {@code LoginProvider}
	 */
	LoginProvider getLoginProvider();

	/**
	 * Gets this {@link Host}'s {@link IdProvider}, which can be shared between different instances.
	 * <p>
	 * The {@code IdProvider} is used to assign IDs to newly created objects.
	 * </p>
	 * 
	 * @return this {@code Host}'s {@code IdProvider}
	 */
	IdProvider getIdProvider();

	/**
	 * Gets this {@link Host}'s {@link IdMapper}, which is used to map client side IDs to server side IDs.
	 * <p>
	 * Each instance of {@code Host} shall return its own {@code IdMapper}.
	 * </p>
	 * 
	 * @return this {@code Host}'s {@link IdMapper}
	 */
	IdMapper getIdMapper();

	/*         */
	/* Classes */
	/*         */

	/**
	 * Returns all {@linkplain StudentClass StudentClasses} the currently logged in {@link User} can view.
	 * 
	 * @return a {@link Map} mapping the local IDs to their {@code StudentClass} instances, never {@code null}
	 */
	Map<Integer, StudentClass> getClasses();

	/**
	 * Returns the {@link StudentClass} with the given local ID, if it exists and the currently logged in {@link User}
	 * can view it.
	 * 
	 * @param classId
	 *            the local ID of the {@code StudentClass}
	 * @return the {@code StudentClass} with the ID of {@code classId}, or {@code null}
	 */
	@Nullable
	StudentClass getClassById(int classId);

	/**
	 * Replaces the {@link StudentClass} with the local ID of {@code studentClass} with {@code studentClass}.
	 * 
	 * @param studentClass
	 *            the {@code StudentClass} to update, not {@code null}
	 * @param updateType
	 *            what kind of {@link UpdateType} this update is
	 */
	void updateClass(StudentClass studentClass, UpdateType updateType);

	/*                          */
	/* Class Printing Variables */
	/*                          */

	/**
	 * Gets a {@link Map} of variables that are to be used when printing a {@link StudentClass}.
	 * Returns an empty {@code Map} if the class doesn't exist or has no data.
	 * <ul>
	 * <li>The key is a {@code String} with less or equal to 32 characters and is never {@code null}.</li>
	 * <li>The value is a {@code String} which might be empty, but is never {@code null}.</li>
	 * </ul>
	 * 
	 * @param classId
	 *            the ID of the {@code StudentClass}
	 * @return a {@code Map} containing the variables for printing
	 */
	Map<String, String> getPrintingVariablesForClass(int classId);

	/**
	 * Sets the variables to be used when printing a {@link StudentClass}.
	 * 
	 * <ul>
	 * <li>The keys must be {@code String}s with less or equal to 32 characters and never {@code null}.</li>
	 * <li>The values must be {@code String}s which might be empty, but s never {@code null}.</li>
	 * </ul>
	 * 
	 * @param classId
	 *            the ID of the {@code StudentClass}
	 * @param variables
	 *            a {@code Map} containing the variables for printing
	 */
	void updatePrintingVariablesForClass(int classId, Map<String, String> variables);

	/*       */
	/* Users */
	/*       */

	/**
	 * Returns all {@linkplain User Users} the currently logged in {@code User} can view.
	 * 
	 * @return a {@link Map} mapping the local IDs to their {@code User} instances, never {@code null}
	 */
	Map<Integer, User> getUsers();

	/**
	 * Returns all {@linkplain User Users} in the {@link StudentClass} with the local ID of {@code classId} and which
	 * the currently logged in {@code User} can view.
	 * 
	 * @return a {@link Map} mapping the local IDs to their {@code User} instances, never {@code null}
	 */
	Map<Integer, User> getUsersByClass(int classId);

	/**
	 * Returns all {@linkplain User Users} that have a {@link Payment} with the local ID of {@code paymentId} and which
	 * the currently logged in {@code User} can view.
	 * 
	 * @return a {@link Map} mapping the local IDs to their {@code User} instances, never {@code null}
	 */
	Map<Integer, User> getUsersWithPayment(int paymentId);

	/**
	 * Returns the {@link User} with the given local ID, if it exists and the currently logged in {@code User} can view
	 * it.
	 * 
	 * @param userId
	 *            the local ID of the {@code User}, not {@code null}
	 * @return the {@code User} with the ID of {@code userId}, or {@code null}
	 */
	@Nullable
	User getUserById(int userId);

	/**
	 * Returns the {@link User} with the given user name, if it exists and the currently logged in {@code User} can
	 * view it.
	 * 
	 * @param username
	 *            the {@code User}'s unique user name, provided by the login system, not {@code null}
	 * @return the {@code User} with the user name {@code username}, or {@code null}
	 */
	@Nullable
	User getUserByUsername(String username);

	/**
	 * Returns the currently logged in {@link User}, or {@code null} if no {@code User} is logged in.
	 * 
	 * @return the currently logged in {@code User}, or {@code null}
	 */
	@Nullable
	User getLoggedInUser();

	/**
	 * Sets the currently logged in {@link User}.
	 * 
	 * @param user
	 *            the {@code User} to set as logged in, not {@code null}.
	 * @throws IllegalStateException
	 *             if the {@code User} is already logged in.
	 */
	void setLoggedInUser(User user);

	/**
	 * Replaces the {@link User} with the local ID of {@code user} with {@code user}.
	 * 
	 * @param user
	 *            the {@code User} to update, not {@code null}
	 * @param updateType
	 *            what kind of {@link UpdateType} this update is
	 */
	void updateUser(User user, UpdateType updateType);

	/*          */
	/* Payments */
	/*          */

	/**
	 * Returns all {@linkplain Payment Payments} the currently logged in {@link User} can view.
	 * 
	 * @return a {@link Map} mapping the local IDs to their {@code Payment} instances, never {@code null}
	 */
	Map<Integer, Payment> getPayments();

	/**
	 * Returns all {@linkplain Payment Payments} of the {@code User} with the local ID of {@code userId} and which
	 * the currently logged in {@code User} can view.
	 * 
	 * @return a {@link Map} mapping the local IDs to their {@code Payment} instances, never {@code null}
	 */
	Map<Integer, Payment> getPaymentsByUser(int userId);

	/**
	 * Returns the {@link Payment} with the given local ID, if it exists and the currently logged in {@code User} can
	 * view it.
	 * 
	 * @param paymentId
	 *            the local ID of the {@code Payment}, not {@code null}
	 * @return the {@code Payment} with the ID of {@code paymentId}, or {@code null}
	 */
	@Nullable
	Payment getPaymentById(int paymentId);

	/**
	 * Replaces the {@link Payment} with the local ID of {@code payment} with {@code payment}.
	 * 
	 * @param payment
	 *            the {@code Payment} to update, not {@code null}
	 * @param updateType
	 *            what kind of {@link UpdateType} this update is
	 */
	void updatePayment(Payment payment, UpdateType updateType);

	/*              */
	/* PaymentUsers */
	/*              */

	/**
	 * Adds multiple {@linkplain User Users} to a single {@link Payment}.
	 * 
	 * @param payment
	 *            the {@code Payment} to add the {@code Users} to, not {@code null}
	 * @param users
	 *            the {@link Collection} of {@code Users}, not {@code null}
	 */
	void addUsersToPayment(Payment payment, Collection<User> users);

	/**
	 * Removes multiple {@linkplain User Users} to a single {@link Payment}.
	 * 
	 * @param payment
	 *            the {@code Payment} to add the {@code Users} to, not {@code null}
	 * @param users
	 *            the {@link Collection} of {@code Users}, not {@code null}
	 */
	void removeUsersFromPayment(Payment payment, Collection<User> users);

	/*       */
	/* Roles */
	/*       */

	/**
	 * Returns all {@linkplain Role Roles} the currently logged in {@link User} can view.
	 * 
	 * @return a {@link Map} mapping the local IDs to their {@code Role} instances, never {@code null}
	 */
	Map<Integer, Role> getRoles();

	/**
	 * Returns the {@link Role} with the given local ID, if it exists and the currently logged in {@code User} can view
	 * it.
	 * <p>
	 * Unlike other data types, {@code Role}s are immutable and can therefore be cached.
	 * </p>
	 * 
	 * @param roleId
	 *            the local ID of the {@code Role}, not {@code null}
	 * @return the {@code Role} with the ID of {@code roleId}, or {@code null}
	 */
	@Nullable
	Role getRoleById(int roleId);

	/*         */
	/* Actions */
	/*         */

	/**
	 * Returns the {@link Action} with the given local ID, if it exists and the currently logged in {@code User} can
	 * view it.
	 * 
	 * @param actionId
	 *            the local ID of the {@code Action}, not {@code null}
	 * @return the {@code Action} with the ID of {@code actionId}, or {@code null}
	 */
	@Nullable
	BaseAction getActionById(long actionId);

	/**
	 * Searches the stored {@linkplain Action Actions} by a {@link ActionSearchQuery}.
	 * 
	 * @param searchQuery
	 *            the {@code ActionSearchQuery} containing the search parameters
	 * @return a {@link Map} containing the results of the search
	 */
	Map<Long, BaseAction> searchActions(ActionSearchQuery searchQuery);

	/**
	 * Adds and applies multiple {@linkplain BaseAction BaseActions}, changing the stored data of this {@link Host}.
	 * 
	 * @param actions
	 *            the {@code BaseAction}s to be applied, not {@code null}
	 */
	void addActions(BaseAction... actions);

	/**
	 * Undoes or redoes multiple {@linkplain BaseAction BaseActions}, depending on the state of {@code undone}.
	 * 
	 * @param undone
	 *            whether this shall undo ({@code true}) or redo ({@code false}) the actions
	 * @param actions
	 *            the {@code BaseAction}s to be undone / redone, not {@code null}
	 */
	void setActionsUndone(boolean undone, BaseAction... actions);
}
