package ch.kanti_wohlen.klassenkasse.action.actions;

import java.util.Date;

import org.eclipse.jdt.annotation.NonNull;

import ch.kanti_wohlen.klassenkasse.action.Action;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.User;

public class ActionActionsRedone extends ActionActions {

	public ActionActionsRedone(@NonNull Host host, Action... actions) {
		super(host, actions);
	}

	public ActionActionsRedone(@NonNull Host host) {
		super(host);
	}

	public ActionActionsRedone(long id, User creator, @NonNull Date date) {
		super(id, creator, date);
	}

	@Override
	public void apply(Host host) {
		checkState(false);
		setActionsUndone(host, false);
		applied = true;
	}

	@Override
	public void undo(Host host) {
		checkState(true);
		setActionsUndone(host, true);
		applied = false;
	}
}
