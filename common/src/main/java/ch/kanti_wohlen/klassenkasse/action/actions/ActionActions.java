package ch.kanti_wohlen.klassenkasse.action.actions;

import java.util.Arrays;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import io.netty.buffer.ByteBuf;
import ch.kanti_wohlen.klassenkasse.action.Action;
import ch.kanti_wohlen.klassenkasse.action.ActionCreationException;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.User;

public abstract class ActionActions extends Action {

	protected @Nullable List<Action> actions;

	public ActionActions(@NonNull Host host, Action... actions) {
		super(host);
		setActions(actions);
	}

	public ActionActions(@NonNull Host host) {
		super(host);
	}

	public ActionActions(long id, User creator, @NonNull Date date) {
		super(id, creator, date);
	}

	public List<Action> getActions() {
		return Collections.unmodifiableList(actions);
	}

	public void setActions(Action... actions) {
		if (actions == null) throw new NullPointerException("Actions list cannot be null.");
		this.actions = Arrays.asList(actions);
	}

	@Override
	public void readData(ByteBuf buf, Host host) throws ActionCreationException {
		List<Action> actionList = new ArrayList<>(buf.readableBytes() / 8);
		while (buf.isReadable(8)) {
			long actionId = host.getIdMapper().getActionMapping(buf.readLong());
			Action action = host.getActionById(actionId);
			if (action == null) {
				throw new ActionCreationException("Could not resolve action with ID " + actionId + ".");
			}

			actionList.add(action);
		}
		actions = actionList;
	}

	@Override
	public void writeData(ByteBuf buf) {
		List<Action> actions = assertNotNull(this.actions);

		for (Action a : actions) {
			buf.writeLong(a.getLocalId());
		}
	}

	protected void setActionsUndone(@NonNull Host host, boolean undone) {
		List<Action> actions = assertNotNull(this.actions);

		for (Action a : actions) {
			if (a == null) throw new NullPointerException("Action was null.");
			host.setActionUndone(a, undone);
		}
	}
}
