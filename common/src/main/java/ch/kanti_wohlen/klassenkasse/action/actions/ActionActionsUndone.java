package ch.kanti_wohlen.klassenkasse.action.actions;

import io.netty.buffer.ByteBuf;

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;

import ch.kanti_wohlen.klassenkasse.action.BaseAction;
import ch.kanti_wohlen.klassenkasse.framework.Host;

@NonNullByDefault
public class ActionActionsUndone extends ActionActions {

	public ActionActionsUndone(Collection<Long> actionIds) {
		super(actionIds, true);
	}

	public ActionActionsUndone(BaseAction... actions) {
		super(actions, true);
	}

	public ActionActionsUndone(Host host, ByteBuf buffer) {
		super(host, buffer, true);
	}
}
