package ch.kanti_wohlen.klassenkasse.action;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import io.netty.buffer.ByteBuf;
import ch.kanti_wohlen.klassenkasse.action.actions.ActionActions;
import ch.kanti_wohlen.klassenkasse.action.actions.ActionActionsUndone;
import ch.kanti_wohlen.klassenkasse.action.classes.ActionClass;
import ch.kanti_wohlen.klassenkasse.action.classes.ActionClassCreated;
import ch.kanti_wohlen.klassenkasse.action.classes.ActionClassRemoved;
import ch.kanti_wohlen.klassenkasse.action.paymentUsers.ActionPaymentUsers;
import ch.kanti_wohlen.klassenkasse.action.paymentUsers.ActionPaymentUsersAdded;
import ch.kanti_wohlen.klassenkasse.action.payments.ActionPayment;
import ch.kanti_wohlen.klassenkasse.action.payments.ActionPaymentCreated;
import ch.kanti_wohlen.klassenkasse.action.payments.ActionPaymentRemoved;
import ch.kanti_wohlen.klassenkasse.action.users.ActionUser;
import ch.kanti_wohlen.klassenkasse.action.users.ActionUserCreated;
import ch.kanti_wohlen.klassenkasse.action.users.ActionUserRemoved;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.Payment;
import ch.kanti_wohlen.klassenkasse.framework.Role;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.network.Protocol;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

@NonNullByDefault
public abstract class Action {

	public final byte getActionId() {
		return Protocol.getActionId(this.getClass());
	}

	public abstract void writeData(ByteBuf buf);

	protected abstract void update(Host host, boolean apply);

	// Helper method
	protected static final <T> T assertNotNull(@Nullable T object) {
		if (object == null) {
			throw new IllegalStateException("Parameter was null.");
		}
		return object;
	}

	protected static final void checkState(@Nullable Object current, UpdateType type) {
		switch (type) {
		case CREATION:
			// Can't check for existance because the object is not really removed
			break;
		case REMOVAL: // same as UPDATE
		case UPDATE:
			if (current == null) {
				throw new IllegalStateException("The object doesn't yet exist.");
			}
			break;
		default:
			throw new IllegalArgumentException("type");
		}
	}

	public static Action createActionByType(Host host, byte actionType, ByteBuf actionData)
			throws ActionCreationException {
		Class<? extends Action> actionClass = Protocol.getActionClassById(actionType);
		if (actionClass == null) {
			throw new ActionCreationException("Unknown action type (" + actionType + ").");
		}

		Action resultAction = null;
		try {
			Constructor<? extends Action> actionConstructor = actionClass.getConstructor(Host.class, ByteBuf.class);
			resultAction = actionConstructor.newInstance(host, actionData);
		} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
			// Unit-Tested, won't happen.
			e.printStackTrace();
		}

		if (resultAction == null) throw new IllegalStateException("Action constructor returned a null value.");
		return resultAction;
	}

	public static String getActionDescription(Host host, BaseAction... actions) {
		StringBuilder result = new StringBuilder();
		if (actions.length == 0) return assertNotNull(result.toString());

		Action firstAction = actions[0].getAction();
		int additionalActions = actions.length - 1;

		if (firstAction instanceof ActionClass) {
			ActionClass classAction = (ActionClass) firstAction;
			result.append("Klasse ").append(classAction.getStudentClassName());

			if (classAction instanceof ActionClassCreated) {
				result.append(" erstellt");
				if (actions.length > 1 && actions[1].getAction() instanceof ActionPaymentUsersAdded) {
					ActionPaymentUsersAdded users = (ActionPaymentUsersAdded) actions[1].getAction();
					result.append(" und ").append(users.getUserIds().size()).append(" Nutzer hinzugefügt");
				}
			} else if (classAction instanceof ActionClassRemoved) {
				result.append(" gelöscht");
			} else {
				result.append(" umbenannt");
			}
		} else if (firstAction instanceof ActionUser) {
			ActionUser userAction = (ActionUser) firstAction;
			Role userRole = host.getRoleById(((ActionUser) firstAction).getRoleId());
			if (userRole != null) result.append(userRole.getName()).append(" ");
			User user = new User(userAction.getUserId(), userAction.getClassId(), userAction.getRoleId(),
					userAction.getFirstName(), userAction.getLastName(), userAction.getUsername(),
					MonetaryValue.ZERO);

			result.append(user.getFullName());

			if (userAction instanceof ActionUserCreated) {
				result.append(" erstellt");
			} else if (userAction instanceof ActionUserRemoved) {
				result.append(" gelöscht");
			} else {
				result.append(" verändert");
			}
		} else if (firstAction instanceof ActionPayment) {
			ActionPayment paymentAction = (ActionPayment) firstAction;
			result.append("Zahlung \"").append(paymentAction.getPaymentDescription()).append("\"");

			if (paymentAction instanceof ActionPaymentCreated) {
				if (additionalActions > 0) result.append(" für ").append(additionalActions).append(" Nutzer");
				result.append(" erstellt");
			} else if (paymentAction instanceof ActionPaymentRemoved) {
				result.append(" gelöscht");
			} else {
				result.append(" verändert");
			}
		} else if (firstAction instanceof ActionPaymentUsers) {
			ActionPaymentUsers paymentUsersAction = (ActionPaymentUsers) firstAction;
			Payment payment = host.getPaymentById(paymentUsersAction.getPaymentId());
			result.append(paymentUsersAction.getUserIds().size()).append(" Nutzer");

			if (paymentUsersAction instanceof ActionPaymentUsersAdded) {
				if (payment == null) {
					result.append(" zu einer Zahlung");
				} else {
					result.append(" zur Zahlung \"").append(payment.getDescription()).append("\"");
				}
				result.append(" hinzugefügt");
			} else {
				if (payment == null) {
					result.append(" von einer Zahlung");
				} else {
					result.append(" von Zahlung \"").append(payment.getDescription()).append("\"");
				}
				result.append(" entfernt");
			}
		} else if (firstAction instanceof ActionActions) {
			ActionActions actionActions = (ActionActions) firstAction;
			result.append(actionActions.getActionIds().size()).append(" Aktionen");

			if (actionActions instanceof ActionActionsUndone) {
				result.append(" rückgängig gemacht");
			} else {
				result.append(" wiederhergestellt");
			}
		}

		return assertNotNull(result.toString());
	}
}
