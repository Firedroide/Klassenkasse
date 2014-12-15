package ch.kanti_wohlen.klassenkasse.action.actions;

import io.netty.buffer.ByteBuf;

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;

import ch.kanti_wohlen.klassenkasse.action.BaseAction;
import ch.kanti_wohlen.klassenkasse.framework.Host;

@NonNullByDefault
public class ActionActionsRedone extends ActionActions {

	public ActionActionsRedone(Collection<Long> actionIds) {
		super(actionIds, false);
	}

	public ActionActionsRedone(BaseAction... actions) {
		super(actions, false);
	}

	public ActionActionsRedone(Host host, ByteBuf buffer) {
		super(host, buffer, false);
	}
}
