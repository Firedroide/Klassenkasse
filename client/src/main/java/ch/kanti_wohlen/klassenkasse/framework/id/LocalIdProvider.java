package ch.kanti_wohlen.klassenkasse.framework.id;

/**
 * Creates new local IDs, starting at -1 and decreasing.<br>
 * Classes, users, payments and actions all have separate IDs.<br>
 * This means that there can be a user with an ID of -1, as well as a payment with an ID of -1.<br>
 * <br>
 * Classes, users and payments have IDs of type Integer; actions have IDs of type Long.
 * 
 * @author Roger Baumgartner
 */
public class LocalIdProvider implements IdProvider {

	private int classId;
	private int userId;
	private int paymentId;
	private long actionId;

	public LocalIdProvider() {
		classId = 0;
		userId = 0;
		paymentId = 0;
		actionId = 0;
	}

	@Override
	public int generateClassId() {
		return --classId;
	}

	@Override
	public int generateUserId() {
		return --userId;
	}

	@Override
	public int generatePaymentId() {
		return --paymentId;
	}

	@Override
	public long generateActionId() {
		return --actionId;
	}
}
