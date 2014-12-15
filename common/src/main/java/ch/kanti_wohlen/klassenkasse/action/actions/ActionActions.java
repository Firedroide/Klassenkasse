package ch.kanti_wohlen.klassenkasse.action.actions;

import io.netty.buffer.ByteBuf;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;

import ch.kanti_wohlen.klassenkasse.action.Action;
import ch.kanti_wohlen.klassenkasse.action.BaseAction;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.id.IdMapper;

@NonNullByDefault
public abstract class ActionActions extends Action {

	protected final Set<Long> actionIds;

	protected boolean undo;

	public ActionActions(Collection<Long> actionIds, boolean undo) {
		this.actionIds = new HashSet<>(actionIds);
		this.undo = undo;
	}

	public ActionActions(BaseAction[] actions, boolean undo) {
		this.actionIds = new HashSet<>();
		this.undo = undo;

		for (BaseAction action : actions) {
			actionIds.add(action.getLocalId());
		}
	}

	public ActionActions(Host host, ByteBuf buffer, boolean undo) {
		this.actionIds = new HashSet<>();
		this.undo = undo;

		IdMapper idMapper = host.getIdMapper();
		while (buffer.isReadable(8)) {
			long actionId = idMapper.getActionMapping(buffer.readLong());
			actionIds.add(actionId);
		}
	}

	@SuppressWarnings("null")
	public Set<Long> getActionIds() {
		return Collections.unmodifiableSet(actionIds);
	}

	@Override
	public void writeData(ByteBuf buf) {
		for (long actionId : actionIds) {
			buf.writeLong(actionId);
		}
	}

	@SuppressWarnings("null")
	@Override
	protected void update(Host host, boolean apply) {
		boolean undo = (this.undo == apply);
		List<BaseAction> actions = new ArrayList<>();

		// First of all, check if all actions exist. Reject the action otherwise
		for (long actionId : actionIds) {
			BaseAction action = host.getActionById(actionId);
			if (action == null) {
				throw new IllegalStateException("Action with the ID of " + actionId + " did not exist!");
			}

			actions.add(action);
		}

		// Then apply all actions
		host.setActionsUndone(undo, actions.toArray(new BaseAction[0]));
	}
}
