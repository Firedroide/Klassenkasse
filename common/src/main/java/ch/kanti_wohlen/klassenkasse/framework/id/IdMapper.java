package ch.kanti_wohlen.klassenkasse.framework.id;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public abstract class IdMapper {

	public abstract int getClassMapping(int localId);

	public abstract int getUserMapping(int localId);

	public abstract int getPaymentMapping(int localId);

	public abstract long getActionMapping(long localId);

	public abstract void mapClass(int localId, int serverId);

	public abstract void mapUser(int localId, int serverId);

	public abstract void mapPayment(int localId, int serverId);

	public abstract void mapAction(long localId, long serverId);

	public static final IdMapper NULL_MAPPER = new IdMapper() {

		@Override
		public void mapUser(int localId, int serverId) {}

		@Override
		public void mapPayment(int localId, int serverId) {}

		@Override
		public void mapClass(int localId, int serverId) {}

		@Override
		public void mapAction(long localId, long serverId) {}

		@Override
		public int getUserMapping(int localId) {
			return localId;
		}

		@Override
		public int getPaymentMapping(int localId) {
			return localId;
		}

		@Override
		public int getClassMapping(int localId) {
			return localId;
		}

		@Override
		public long getActionMapping(long localId) {
			return localId;
		}
	};
}
