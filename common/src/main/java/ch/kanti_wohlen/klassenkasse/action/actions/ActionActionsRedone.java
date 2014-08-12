package ch.kanti_wohlen.klassenkasse.action.actions;

import org.eclipse.jdt.annotation.NonNull;

import ch.kanti_wohlen.klassenkasse.action.Action;
import ch.kanti_wohlen.klassenkasse.framework.Host;

public class ActionActionsRedone extends ActionActions {

	public ActionActionsRedone(@NonNull Host host, Action... actions) {
		super(host, actions);
	}

	public ActionActionsRedone(@NonNull Host host) {
		super(host);
	}

	public ActionActionsRedone(long id) {
		super(id);
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
