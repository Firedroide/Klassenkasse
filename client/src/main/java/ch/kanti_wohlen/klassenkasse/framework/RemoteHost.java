package ch.kanti_wohlen.klassenkasse.framework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import ch.kanti_wohlen.klassenkasse.action.ActionSearchQuery;
import ch.kanti_wohlen.klassenkasse.action.BaseAction;
import ch.kanti_wohlen.klassenkasse.action.UpdateType;
import ch.kanti_wohlen.klassenkasse.action.actions.ActionActions;
import ch.kanti_wohlen.klassenkasse.action.actions.ActionActionsRedone;
import ch.kanti_wohlen.klassenkasse.action.actions.ActionActionsUndone;
import ch.kanti_wohlen.klassenkasse.framework.id.IdMapper;
import ch.kanti_wohlen.klassenkasse.framework.id.IdProvider;
import ch.kanti_wohlen.klassenkasse.framework.id.LocalIdProvider;
import ch.kanti_wohlen.klassenkasse.login.LoginProvider;
import ch.kanti_wohlen.klassenkasse.network.ClientSocket;
import ch.kanti_wohlen.klassenkasse.network.packet.Packet;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketActions;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketClassVariables;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketDataRequest;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketDataRequest.RequestType;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketPayments;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketRoles;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketStudentClasses;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketUsers;

// TODO: ActionAppliedExceptions?
@SuppressWarnings("null")
public class RemoteHost implements Host {

	private static final Logger LOGGER = Logger.getLogger(RemoteHost.class.getSimpleName());

	private final @NonNull String hostName;
	private final @NonNull ClientSocket socket;
	private final @NonNull IdProvider idProvider;
	private final @NonNull RemoteLoginProvider loginProvider;
	private final @NonNull ActionHistory actionHistory;

	private final @NonNull Map<Integer, Role> roles;

	private @Nullable User loggedInUser;

	@NonNullByDefault
	public RemoteHost(String name, String hostname, int port) {
		hostName = name;

		socket = new ClientSocket(hostname, port, this);
		idProvider = new LocalIdProvider();
		loginProvider = new RemoteLoginProvider(socket);
		actionHistory = new ActionHistory();

		roles = new HashMap<>();

		loggedInUser = null;
	}

	@Override
	public String getName() {
		return hostName;
	}

	public void start() {
		new Thread(socket).start();
	}

	public void stop() {
		socket.stop();
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

	// Classes
	@Override
	public Map<Integer, StudentClass> getClasses() {
		PacketStudentClasses packet = request(RequestType.STUDENT_CLASSES, PacketStudentClasses.class);
		return packet.getStudentClasses();
	}

	@Override
	public @Nullable StudentClass getClassById(int classId) {
		PacketStudentClasses packet = request(RequestType.STUDENT_CLASS_BY_ID, classId, PacketStudentClasses.class);
		return packet.getStudentClasses().get(classId);
	}

	@Override
	public void updateClass(StudentClass studentClass, UpdateType updateType) {
		throw new UnsupportedOperationException();
	}

	// Printing information
	@Override
	public Map<String, String> getPrintingVariablesForClass(int classId) {
		PacketClassVariables variables = request(RequestType.CLASS_VARIABLES_BY_ID, classId, PacketClassVariables.class);
		return variables.getVariables();
	}

	@Override
	public void updatePrintingVariablesForClass(int classId, Map<String, String> variables) {
		PacketClassVariables packet = new PacketClassVariables(classId, variables);
		socket.transmitPacket(packet);
	}

	// Users
	@Override
	public Map<Integer, User> getUsers() {
		PacketUsers packet = request(RequestType.USERS, PacketUsers.class);
		return packet.getUsers();
	}

	@Override
	public Map<Integer, User> getUsersByClass(int classId) {
		PacketUsers packet = request(RequestType.USERS_BY_STUDENT_CLASS, classId, PacketUsers.class);
		return packet.getUsers();
	}

	@Override
	public Map<Integer, User> getUsersWithPayment(int paymentId) {
		PacketUsers packet = request(RequestType.USERS_WITH_PAYMENT, paymentId, PacketUsers.class);
		return packet.getUsers();
	}

	@Override
	public @Nullable User getUserById(int userId) {
		PacketUsers packet = request(RequestType.USER_BY_ID, userId, PacketUsers.class);
		return packet.getUsers().get(userId);
	}

	@Override
	public @Nullable User getUserByUsername(String username) {
		PacketUsers packet = request(RequestType.USER_BY_USERNAME, username, PacketUsers.class);
		Iterator<User> users = packet.getUsers().values().iterator();
		return users.hasNext() ? users.next() : null;
	}

	@Override
	public @Nullable User getLoggedInUser() {
		if (loggedInUser != null) {
			return loggedInUser;
		}

		PacketUsers packet = request(RequestType.LOGGED_IN_USER, PacketUsers.class);
		Map<Integer, User> users = packet.getUsers();
		if (users.isEmpty()) return null;
		loggedInUser = users.values().iterator().next();
		return loggedInUser;
	}

	@Override
	public void setLoggedInUser(User user) {
		if (user != null) {
			ActionSearchQuery query = new ActionSearchQuery();
			query.setCreator(user);
			query.setApplied(true);
			query.setLimit((short) ActionHistory.MAX_ITEMS);
			PacketActions packet = request(RequestType.SEARCH_ACTIONS, query, PacketActions.class);
			List<BaseAction> actions = new ArrayList<>(packet.getActions().values());
			Collections.sort(actions, new Comparator<BaseAction>() {

				@Override
				public int compare(@Nullable BaseAction o1, @Nullable BaseAction o2) {
					if (o1 == null || o2 == null) throw new NullPointerException();
					return o1.getCreationDate().compareTo(o2.getCreationDate());
				}
			});

			// TODO: Group actions together?
			for (BaseAction action : actions) {
				if (action == null || action.getAction() == null) continue;
				if (action.getAction() instanceof ActionActions) continue;

				if (action.isApplied()) {
					actionHistory.getUndoableActions().add(new BaseAction[] {action});
				} else {
					actionHistory.getRedoableActions().add(new BaseAction[] {action});
				}
			}
		}
	}

	@Override
	public void updateUser(User user, UpdateType updateType) {
		throw new UnsupportedOperationException();
	}

	// Payments
	@Override
	public Map<Integer, Payment> getPayments() {
		PacketPayments packet = request(RequestType.PAYMENTS, PacketPayments.class);
		return packet.getPayments();
	}

	@Override
	public Map<Integer, Payment> getPaymentsByUser(int userId) {
		PacketPayments packet = request(RequestType.PAYMENTS_BY_USER, userId, PacketPayments.class);
		return packet.getPayments();
	}

	@Override
	public @Nullable Payment getPaymentById(int paymentId) {
		PacketPayments packet = request(RequestType.PAYMENT_BY_ID, paymentId, PacketPayments.class);
		return packet.getPayments().get(paymentId);
	}

	@Override
	public void updatePayment(Payment payment, UpdateType updateType) {
		throw new UnsupportedOperationException();
	}

	// PaymentUsers
	@Override
	public void addUsersToPayment(Payment payment, Collection<User> users) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeUsersFromPayment(Payment payment, Collection<User> users) {
		throw new UnsupportedOperationException();
	}

	// Roles
	@Override
	public Map<Integer, Role> getRoles() {
		if (roles.isEmpty()) {
			PacketRoles packet = request(RequestType.ROLES, PacketRoles.class);
			roles.putAll(packet.getRoles());
		}

		return Collections.unmodifiableMap(roles);
	}

	@Override
	public @Nullable Role getRoleById(int roleId) {
		return getRoles().get(roleId);
	}

	// Actions
	@Override
	public @Nullable BaseAction getActionById(long actionId) {
		PacketActions actions = request(RequestType.ACTION_BY_ID, actionId, PacketActions.class);
		return actions.getActions().get(actionId);
	}

	@Override
	public Map<Long, BaseAction> searchActions(ActionSearchQuery searchQuery) {
		PacketActions actions = request(RequestType.SEARCH_ACTIONS, searchQuery, PacketActions.class);
		return actions.getActions();
	}

	@Override
	public void addActions(BaseAction... actions) {
		if (actions == null || actions.length == 0) return;

		List<BaseAction[]> undoableActions = actionHistory.getUndoableActions();
		while (undoableActions.size() > ActionHistory.MAX_ITEMS) {
			undoableActions.remove(0);
		}
		undoableActions.add(actions);

		socket.transmitAction(actions);
	}

	@Override
	public void setActionsUndone(boolean undone, BaseAction... actions) {
		ActionActions result;
		if (undone) {
			result = new ActionActionsUndone(actions);
		} else {
			result = new ActionActionsRedone(actions);
		}

		socket.transmitAction(new BaseAction(result, this));
	}

	public ActionHistory getActionHistory() {
		return actionHistory;
	}

	// Internal
	public <T> T request(RequestType requestType, Class<T> responseClass) {
		return request(requestType, null, responseClass);
	}

	// TODO: Check for error packets
	@SuppressWarnings("unchecked")
	public <T> T request(RequestType requestType, @Nullable Object argument, Class<T> responseClass) {
		PacketDataRequest request;
		if (argument == null) {
			request = new PacketDataRequest(requestType);
		} else {
			request = new PacketDataRequest(requestType, argument);
		}

		LOGGER.info("Requesting " + request.getRequestedDataType().name()
				+ " with argument " + String.valueOf(request.getArgument()));

		Future<? extends Packet> future = socket.transmitPacket(request);

		try {
			Packet response = future.get(30, TimeUnit.SECONDS);
			LOGGER.info("Got a response packet of type " + response.getClass().getSimpleName());

			if (response != null && responseClass.isInstance(response)) {
				return (T) response;
			}
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			LOGGER.severe("Didn't get a response of type " + responseClass.getSimpleName() + " in time.");
		}

		try {
			T newInstance = responseClass.newInstance();
			return newInstance;
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
			throw new IllegalStateException("Could not instantiate packet without arguments");
		}
	}
}
