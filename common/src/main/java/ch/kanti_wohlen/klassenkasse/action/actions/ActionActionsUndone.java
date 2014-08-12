package ch.kanti_wohlen.klassenkasse.action.actions;

import org.eclipse.jdt.annotation.NonNull;

import ch.kanti_wohlen.klassenkasse.action.Action;
import ch.kanti_wohlen.klassenkasse.framework.Host;

public class ActionActionsUndone extends ActionActions {

	public ActionActionsUndone(@NonNull Host host, Action... actions) {
		super(host, actions);
	}

	public ActionActionsUndone(@NonNull Host host) {
		super(host);
	}

	public ActionActionsUndone(long id) {
		super(id);
	}

	@Override
	public void apply(Host host) {
		checkState(false);
		setActionsUndone(host, true);
		applied = true;
	}

	@Override
	public void undo(Host host) {
		checkState(true);
		setActionsUndone(host, false);
		applied = false;
	}
}
