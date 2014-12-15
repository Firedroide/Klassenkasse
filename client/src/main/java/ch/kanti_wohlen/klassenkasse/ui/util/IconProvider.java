package ch.kanti_wohlen.klassenkasse.ui.util;

import javax.swing.ImageIcon;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import ch.kanti_wohlen.klassenkasse.action.Action;
import ch.kanti_wohlen.klassenkasse.action.actions.ActionActions;
import ch.kanti_wohlen.klassenkasse.action.actions.ActionActionsRedone;
import ch.kanti_wohlen.klassenkasse.action.actions.ActionActionsUndone;
import ch.kanti_wohlen.klassenkasse.action.classes.ActionClass;
import ch.kanti_wohlen.klassenkasse.action.classes.ActionClassCreated;
import ch.kanti_wohlen.klassenkasse.action.classes.ActionClassRemoved;
import ch.kanti_wohlen.klassenkasse.action.classes.ActionClassUpdated;
import ch.kanti_wohlen.klassenkasse.action.paymentUsers.ActionPaymentUsers;
import ch.kanti_wohlen.klassenkasse.action.payments.ActionPayment;
import ch.kanti_wohlen.klassenkasse.action.payments.ActionPaymentCreated;
import ch.kanti_wohlen.klassenkasse.action.payments.ActionPaymentRemoved;
import ch.kanti_wohlen.klassenkasse.action.payments.ActionPaymentUpdated;
import ch.kanti_wohlen.klassenkasse.action.users.ActionUser;
import ch.kanti_wohlen.klassenkasse.action.users.ActionUserCreated;
import ch.kanti_wohlen.klassenkasse.action.users.ActionUserRemoved;
import ch.kanti_wohlen.klassenkasse.action.users.ActionUserUpdated;
import ch.kanti_wohlen.klassenkasse.framework.User;

import com.jgoodies.validation.ValidationResult;

@NonNullByDefault
public final class IconProvider {

	private static final Class<IconProvider> clazz = IconProvider.class;

	public static final ImageIcon APPLICATION = new ImageIcon(clazz.getResource("/icons/silk/report.png"));

	public static final ImageIcon HOME = new ImageIcon(clazz.getResource("/icons/silk/house.png"));
	public static final ImageIcon LOADING = new ImageIcon(clazz.getResource("/icons/loading.gif"));
	public static final ImageIcon WRENCH = new ImageIcon(clazz.getResource("/icons/silk/wrench.png"));
	public static final ImageIcon SETTINGS = new ImageIcon(clazz.getResource("/icons/fatcow/gear_in.png"));
	public static final ImageIcon LOG = new ImageIcon(clazz.getResource("/icons/silk/report.png"));
	public static final ImageIcon DISK = new ImageIcon(clazz.getResource("/icons/silk/disk.png"));
	public static final ImageIcon PRINT = new ImageIcon(clazz.getResource("/icons/silk/printer.png"));
	public static final ImageIcon PRINT_LARGE = new ImageIcon(clazz.getResource("/icons/fatcow/printer.png"));
	public static final ImageIcon COLOR_PICKER = new ImageIcon(clazz.getResource("/icons/silk/color_wheel.png"));

	public static final ImageIcon OK = new ImageIcon(clazz.getResource("/icons/silk/tick.png"));
	public static final ImageIcon INFO = new ImageIcon(clazz.getResource("/icons/silk/information.png"));
	public static final ImageIcon WARNING = new ImageIcon(clazz.getResource("/icons/silk/error.png"));
	public static final ImageIcon ERROR = new ImageIcon(clazz.getResource("/icons/silk/exclamation.png"));

	public static final ImageIcon DETAILS = new ImageIcon(clazz.getResource("/icons/silk/magnifier.png"));
	public static final ImageIcon EDIT = new ImageIcon(clazz.getResource("/icons/silk/pencil.png"));
	public static final ImageIcon EDIT_LARGE = new ImageIcon(clazz.getResource("/icons/fatcow/pencil.png"));
	public static final ImageIcon DELETE = new ImageIcon(clazz.getResource("/icons/silk/cross.png"));
	public static final ImageIcon REMOVE = new ImageIcon(clazz.getResource("/icons/silk/delete.png"));

	public static final ImageIcon UNDO = new ImageIcon(clazz.getResource("/icons/silk/arrow_undo.png"));
	public static final ImageIcon REDO = new ImageIcon(clazz.getResource("/icons/silk/arrow_redo.png"));

	public static final ImageIcon CLASS = new ImageIcon(clazz.getResource("/icons/silk/group.png"));
	public static final ImageIcon CLASS_ADD = new ImageIcon(clazz.getResource("/icons/silk/group_add.png"));
	public static final ImageIcon CLASS_EDIT = new ImageIcon(clazz.getResource("/icons/silk/group_edit.png"));
	public static final ImageIcon CLASS_REMOVE = new ImageIcon(clazz.getResource("/icons/silk/group_delete.png"));
	public static final ImageIcon CLASS_EMPTY = new ImageIcon(clazz.getResource("/icons/silk/group.png"));

	public static final ImageIcon USER = new ImageIcon(clazz.getResource("/icons/silk/user.png"));
	public static final ImageIcon USER_ADD = new ImageIcon(clazz.getResource("/icons/silk/user_add.png"));
	public static final ImageIcon USER_EDIT = new ImageIcon(clazz.getResource("/icons/silk/user_edit.png"));
	public static final ImageIcon USER_REMOVE = new ImageIcon(clazz.getResource("/icons/silk/user_delete.png"));
	public static final ImageIcon USER_GREEN = new ImageIcon(clazz.getResource("/icons/silk/user_green.png"));
	public static final ImageIcon USER_ORANGE = new ImageIcon(clazz.getResource("/icons/silk/user_orange.png"));
	public static final ImageIcon USER_RED = new ImageIcon(clazz.getResource("/icons/silk/user_red.png"));
	public static final ImageIcon USER_GREY = new ImageIcon(clazz.getResource("/icons/silk/user_gray.png"));
	public static final ImageIcon USER_SUIT = new ImageIcon(clazz.getResource("/icons/silk/user_suit.png"));

	public static final ImageIcon PAYMENT = new ImageIcon(clazz.getResource("/icons/silk/money.png"));
	public static final ImageIcon PAYMENT_ADD = new ImageIcon(clazz.getResource("/icons/silk/money_add.png"));
	public static final ImageIcon PAYMENT_EDIT = new ImageIcon(clazz.getResource("/icons/silk/money_edit.png"));
	public static final ImageIcon PAYMENT_REMOVE = new ImageIcon(clazz.getResource("/icons/silk/money_delete.png"));

	private IconProvider() {}

	public static final ImageIcon forStudentClass(boolean hasUsers) {
		return hasUsers ? CLASS : CLASS_EMPTY;
	}

	public static final ImageIcon forUser(User user) {
		return forRole(user.getRoleId());
	}

	public static final ImageIcon forRole(int roleId) {
		switch (roleId) {
		case 0:
			return USER_ORANGE;
		case 1: // Administrator
			return USER_GREY;
		case 2: // Teacher
			return USER_SUIT;
		case 3: // Cashier
			return USER_GREEN;
		case 4: // User
			return USER;
		default: // Fallback for custom roles
			return USER_RED;
		}
	}

	public static final @Nullable ImageIcon forValidationResult(ValidationResult result) {
		switch (result.getSeverity()) {
		case ERROR:
			return ERROR;
		case INFO:
			return INFO;
		case WARNING:
			return WARNING;
		case OK: // Fall-through
		default:
			return null;
		}
	}

	public static final @Nullable ImageIcon forAction(Action action) {
		if (action instanceof ActionActions) {
			return forActionActions((ActionActions) action);
		} else if (action instanceof ActionClass) {
			return forActionClass((ActionClass) action);
		} else if (action instanceof ActionUser) {
			return forActionUser((ActionUser) action);
		} else if (action instanceof ActionPayment) {
			return forActionPayment((ActionPayment) action);
		} else if (action instanceof ActionPaymentUsers) {
			return PAYMENT_EDIT;
		}
		return null;
	}

	public static final @Nullable ImageIcon forActionActions(ActionActions action) {
		if (action instanceof ActionActionsUndone) {
			return UNDO;
		} else if (action instanceof ActionActionsRedone) {
			return REDO;
		}
		return null;
	}

	public static final @Nullable ImageIcon forActionClass(ActionClass action) {
		if (action instanceof ActionClassCreated) {
			return CLASS_ADD;
		} else if (action instanceof ActionClassUpdated) {
			return CLASS_EDIT;
		} else if (action instanceof ActionClassRemoved) {
			return CLASS_REMOVE;
		}
		return null;
	}

	public static final @Nullable ImageIcon forActionUser(ActionUser action) {
		if (action instanceof ActionUserCreated) {
			return USER_ADD;
		} else if (action instanceof ActionUserUpdated) {
			return USER_EDIT;
		} else if (action instanceof ActionUserRemoved) {
			return USER_REMOVE;
		}
		return null;
	}

	public static final @Nullable ImageIcon forActionPayment(ActionPayment action) {
		if (action instanceof ActionPaymentCreated) {
			return PAYMENT_ADD;
		} else if (action instanceof ActionPaymentUpdated) {
			return PAYMENT_EDIT;
		} else if (action instanceof ActionPaymentRemoved) {
			return PAYMENT_REMOVE;
		}
		return null;
	}
}
