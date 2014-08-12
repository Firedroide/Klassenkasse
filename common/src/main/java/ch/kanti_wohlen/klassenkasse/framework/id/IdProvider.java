package ch.kanti_wohlen.klassenkasse.framework.id;

import ch.kanti_wohlen.klassenkasse.framework.Host;

/**
 * Creates new unique IDs for different types of data for this {@link Host}.<br>
 * The server's generated IDs are always positive and the client's IDs are
 * always negative.<br>
 * ID 0 should never be given out, it is reserved for special uses.<br>
 * <p>
 * This is a simple way to keep client IDs distinct from server IDs which does
 * not require any communication between both parties. This approach leaves it
 * up to the server to map a client ID to a server ID via an {@link IdMapper},
 * which in turn also solves the problem that a client could accidentally try to
 * create an object with an ID which was already used by an other client
 * beforehand.
 * </p>
 * <p>
 * The only drawback to this approach is that the possible amount of objects
 * that can be stored is reduced to half, i.e. 2.15 billion (10^9) classes,
 * users or payments and 9.22 quintillion (10^18) actions.<br>
 * As this is a crazy number of storable objects, we should never run out.
 * </p>
 * <p>
 * The implementation specific, per-host {@code IdProvider} can be obtained by
 * calling {@link Host#getIdProvider()}.
 * </p>
 * 
 * @author Roger Baumgartner
 */
public interface IdProvider {

	/**
	 * Generates a new unique ID for classes for this {@link Host}.
	 * 
	 * @return the new unique class ID
	 */
	public int generateClassId();

	/**
	 * Generates a new unique ID for users for this {@link Host}.
	 * 
	 * @return the new unique user ID
	 */
	public int generateUserId();

	/**
	 * Generates a new unique ID for payments for this {@link Host}.
	 * 
	 * @return the new unique payment ID
	 */
	public int generatePaymentId();

	/**
	 * Generates a new unique ID for actions for this {@link Host}.<br>
	 * Note that action IDs are of type {@code Long} and not {@code Integer}.
	 * 
	 * @return the new unique action ID
	 */
	public long generateActionId();
}
