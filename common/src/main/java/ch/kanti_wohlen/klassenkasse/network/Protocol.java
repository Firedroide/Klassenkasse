package ch.kanti_wohlen.klassenkasse.network;

import ch.kanti_wohlen.klassenkasse.action.Action;
import ch.kanti_wohlen.klassenkasse.action.actions.ActionActionsRedone;
import ch.kanti_wohlen.klassenkasse.action.actions.ActionActionsUndone;
import ch.kanti_wohlen.klassenkasse.action.classes.ActionClassCreated;
import ch.kanti_wohlen.klassenkasse.action.classes.ActionClassRemoved;
import ch.kanti_wohlen.klassenkasse.action.classes.ActionClassUpdated;
import ch.kanti_wohlen.klassenkasse.action.paymentUsers.ActionPaymentUsersAdded;
import ch.kanti_wohlen.klassenkasse.action.paymentUsers.ActionPaymentUsersRemoved;
import ch.kanti_wohlen.klassenkasse.action.payments.ActionPaymentCreated;
import ch.kanti_wohlen.klassenkasse.action.payments.ActionPaymentRemoved;
import ch.kanti_wohlen.klassenkasse.action.payments.ActionPaymentUpdated;
import ch.kanti_wohlen.klassenkasse.action.users.ActionUserCreated;
import ch.kanti_wohlen.klassenkasse.action.users.ActionUserRemoved;
import ch.kanti_wohlen.klassenkasse.action.users.ActionUserUpdated;
import ch.kanti_wohlen.klassenkasse.network.packet.Packet;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketAccepted;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketActionCommitted;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketActions;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketClassVariables;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketDataRequest;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketDisconnect;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketErrorEncountered;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketHandshake;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketLogin;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketLoginResponse;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketPayments;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketPrintingInformation;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketRoles;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketStudentClasses;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketUsernames;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketUsers;
import ch.kanti_wohlen.klassenkasse.util.BiMap;

/**
 * The protocol of this application, which maps IDs to classes of {@code Object}s transmitted over the network.
 * 
 * @author Roger Baumgartner
 */
public final class Protocol {

	/**
	 * The current protocol version.
	 * <p>
	 * Will change when versions of the program become incompatible. Is transmitted in {@link PacketHandshake}.
	 * </p>
	 */
	public static final short VERSION = 1;

	private static BiMap<Byte, Class<? extends Packet>> packets = new BiMap<>();
	private static BiMap<Byte, Class<? extends Action>> actions = new BiMap<>();

	private static void packet(int id, Class<? extends Packet> packet) {
		packets.put((byte) id, packet);
	}

	private static void action(int id, Class<? extends Action> action) {
		actions.put((byte) id, action);
	}

	static {
		// Both ways
		packet(0, PacketHandshake.class);
		packet(1, PacketClassVariables.class);

		// Client --> Server
		// State change
		packet(16, PacketLogin.class);
		packet(17, PacketDisconnect.class);
		// Data
		packet(24, PacketActionCommitted.class);
		packet(25, PacketDataRequest.class);

		// Server -> Client
		// Data
		packet(32, PacketStudentClasses.class);
		packet(33, PacketUsers.class);
		packet(34, PacketPayments.class);
		packet(35, PacketRoles.class);
		packet(36, PacketActions.class);
		packet(37, PacketUsernames.class);
		packet(38, PacketPrintingInformation.class);
		// Responses, errors
		packet(48, PacketAccepted.class);
		packet(49, PacketErrorEncountered.class);
		packet(50, PacketLoginResponse.class);

		// Class actions
		action(0, ActionClassCreated.class);
		action(1, ActionClassUpdated.class);
		action(2, ActionClassRemoved.class);
		// User actions
		action(4, ActionUserCreated.class);
		action(5, ActionUserUpdated.class);
		action(6, ActionUserRemoved.class);
		// Payment actions
		action(8, ActionPaymentCreated.class);
		action(9, ActionPaymentUpdated.class);
		action(10, ActionPaymentRemoved.class);
		// PaymentUser actions
		action(12, ActionPaymentUsersAdded.class);
		action(13, ActionPaymentUsersRemoved.class);
		// Action actions
		action(16, ActionActionsUndone.class);
		action(17, ActionActionsRedone.class);
	}

	public static enum NetworkError {
		UNKNOWN_ERROR,
		INCORRECT_PROTOCOL_VERSION,
		INVALID_LOGIN,
		INVALID_DATA_REQUEST;
	}

	public static Class<? extends Packet> getPacketClassById(byte id) {
		return packets.get(id);
	}

	public static byte getPacketId(Class<? extends Packet> packetClass) {
		return packets.inverse().get(packetClass);
	}

	public static Class<? extends Action> getActionClassById(byte id) {
		return actions.get(id);
	}

	public static byte getActionId(Class<? extends Action> actionClass) {
		return actions.inverse().get(actionClass);
	}
}
