package ch.kanti_wohlen.klassenkasse.framework.id;

import ch.kanti_wohlen.klassenkasse.util.BiMap;

public class ServerIdMapper extends IdMapper {

	// Maps a local ID to a server-side ID
	private final BiMap<Integer, Integer> classes;
	private final BiMap<Integer, Integer> users;
	private final BiMap<Integer, Integer> payments;
	private final BiMap<Long, Long> actions;

	public ServerIdMapper() {
		classes = new BiMap<>();
		users = new BiMap<>();
		payments = new BiMap<>();
		actions = new BiMap<>();
	}

	@Override
	public void mapClass(int localId, int serverId) {
		if (localId >= 0) return;
		classes.put(localId, serverId);
	}

	@Override
	public void mapUser(int localId, int serverId) {
		if (localId >= 0) return;
		users.put(localId, serverId);
	}

	@Override
	public void mapPayment(int localId, int serverId) {
		if (localId >= 0) return;
		payments.put(localId, serverId);
	}

	@Override
	public void mapAction(long localId, long serverId) {
		if (localId >= 0) return;
		actions.put(localId, serverId);
	}

	@Override
	public int getClassMapping(int localId) {
		Integer classId = classes.get(localId);
		return classId == null ? localId : classId;
	}

	@Override
	public int getUserMapping(int localId) {
		Integer userId = users.get(localId);
		return userId == null ? localId : userId;
	}

	@Override
	public int getPaymentMapping(int localId) {
		Integer paymentId = payments.get(localId);
		return paymentId == null ? localId : paymentId;
	}

	@Override
	public long getActionMapping(long localId) {
		Long actionId = actions.get(localId);
		return actionId == null ? localId : actionId;
	}
}
