package ch.kanti_wohlen.klassenkasse.action;

import java.util.Date;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import io.netty.buffer.ByteBuf;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.LocallyIdentifiable;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.network.Protocol;

@NonNullByDefault
public abstract class Action implements LocallyIdentifiable<Long> {

	protected final long id;

	protected @Nullable User creator;
	protected Date date;
	protected boolean applied;

	public Action(Host host) {
		id = host.getIdProvider().generateActionId();
		date = new Date();
	}

	public Action(long id, @Nullable User creator, Date date) {
		this.id = id;
		this.creator = creator;
		this.date = date;
	}

	@SuppressWarnings("null")
	public Long getLocalId() {
		return id;
	}

	public @Nullable User getCreator() {
		return creator;
	}

	public Date getCreationDate() {
		return date;
	}

	public boolean isApplied() {
		return applied;
	}

	public boolean isUndone() {
		return !applied;
	}

	protected void checkState(boolean wantedState) {
		if (applied != wantedState) {
			if (applied) {
				throw new IllegalStateException("Action was already applied.");
			} else {
				throw new IllegalStateException("Action has not been applied yet.");
			}
		}
	}

	protected <T> T assertNotNull(@Nullable T object) {
		if (object == null) {
			throw new IllegalStateException("Parameter of " + getClass().getSimpleName() + " was null.");
		}
		return object;
	}

	public final byte getActionId() {
		return Protocol.getActionId(this.getClass());
	}

	public abstract void apply(Host host);

	public abstract void undo(Host host);

	public abstract void readData(ByteBuf buf, Host host) throws ActionCreationException;

	public abstract void writeData(ByteBuf buf);
}
