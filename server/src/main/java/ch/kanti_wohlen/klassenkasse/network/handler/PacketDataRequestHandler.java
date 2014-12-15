package ch.kanti_wohlen.klassenkasse.network.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import ch.kanti_wohlen.klassenkasse.action.ActionSearchQuery;
import ch.kanti_wohlen.klassenkasse.action.BaseAction;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.Payment;
import ch.kanti_wohlen.klassenkasse.framework.Role;
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.network.Protocol.NetworkError;
import ch.kanti_wohlen.klassenkasse.network.packet.Packet;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketActions;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketClassVariables;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketDataRequest;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketDataRequest.RequestType;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketErrorEncountered;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketPayments;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketPrintingInformation;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketRoles;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketStudentClasses;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketUsernames;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketUsers;
import ch.kanti_wohlen.klassenkasse.server.Server;

public class PacketDataRequestHandler extends SimpleChannelInboundHandler<PacketDataRequest> {

	private static final Logger LOGGER = Logger.getLogger(PacketDataRequestHandler.class.getSimpleName());

	private final Host host;

	public PacketDataRequestHandler(Host host) {
		this.host = host;
	}

	// TODO: Long IO operation, extract
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, PacketDataRequest msg) throws Exception {
		Packet answerPacket;
		Object argument = msg.getArgument();
		RequestType request = msg.getRequestedDataType();
		if (!request.getArgumentClass().equals(Void.class) && argument == null) {
			LOGGER.warning("User requested " + request.name() + " but the argument was null!");
			ctx.writeAndFlush(new PacketErrorEncountered(NetworkError.INVALID_DATA_REQUEST, "Argument was null"));
			return;
		}

		// TODO: Change level?
		LOGGER.info("Requested " + request.name() + " with argument " + String.valueOf(argument));

		switch (request) {
		case STUDENT_CLASSES:
			answerPacket = new PacketStudentClasses(host.getClasses());
			break;
		case USERS:
			answerPacket = new PacketUsers(host.getUsers());
			break;
		case PAYMENTS:
			answerPacket = new PacketPayments(host.getPayments());
			break;
		case ROLES:
			answerPacket = new PacketRoles(host.getRoles());
			break;
		case USERNAMES:
			try {
				Collection<String> usernames = host.getLoginProvider().getUsernames();
				answerPacket = new PacketUsernames(usernames);
			} catch (Exception e) {
				answerPacket = new PacketUsernames();
			}
			break;
		case LOGGED_IN_USER:
			Map<Integer, User> loggedIn = new HashMap<>();
			User loggedInUser = host.getLoggedInUser();
			if (loggedInUser != null) loggedIn.put(loggedInUser.getLocalId(), loggedInUser);

			answerPacket = new PacketUsers(loggedIn);
			break;
		case PRINTING_INFORMATION:
			BufferedImage img = getLogo();
			String format = Server.INSTANCE.getConfiguration().getString("printingFooterFormat");
			answerPacket = new PacketPrintingInformation(format, img);
			break;

		case STUDENT_CLASS_BY_ID:
			StudentClass studentClass = host.getClassById((int) argument);
			answerPacket = new PacketStudentClasses(Collections.singletonMap((int) argument, studentClass));
			break;
		case CLASS_VARIABLES_BY_ID:
			int classId = (int) argument;
			Map<String, String> variables = host.getPrintingVariablesForClass(classId);
			answerPacket = new PacketClassVariables(classId, variables);
			break;
		case USER_BY_ID:
			User user = host.getUserById((int) argument);
			answerPacket = new PacketUsers(Collections.singletonMap((int) argument, user));
			break;
		case USERS_BY_STUDENT_CLASS:
			Map<Integer, User> classUsers = host.getUsersByClass((int) argument);
			answerPacket = new PacketUsers(classUsers);
			break;
		case USERS_WITH_PAYMENT:
			Map<Integer, User> paymentUsers = host.getUsersWithPayment((int) argument);
			answerPacket = new PacketUsers(paymentUsers);
			break;
		case PAYMENT_BY_ID:
			Payment payment = host.getPaymentById((int) argument);
			answerPacket = new PacketPayments(Collections.singletonMap((int) argument, payment));
			break;
		case PAYMENTS_BY_USER:
			Map<Integer, Payment> userPayments = host.getPaymentsByUser((int) argument);
			answerPacket = new PacketPayments(userPayments);
			break;
		case ROLE_BY_ID:
			Role role = host.getRoleById((int) argument);
			answerPacket = new PacketRoles(Collections.singletonMap((int) argument, role));
			break;

		case ACTION_BY_ID:
			BaseAction baseAction = host.getActionById((long) argument);
			answerPacket = new PacketActions(Collections.singletonMap((long) argument, baseAction));
			break;

		case USER_BY_USERNAME:
			@SuppressWarnings("null")
			User userE = host.getUserByUsername((String) argument);
			if (userE == null) {
				answerPacket = new PacketUsers();
			} else {
				answerPacket = new PacketUsers(Collections.singletonMap(userE.getLocalId(), userE));
			}
			break;

		case SEARCH_ACTIONS:
			ActionSearchQuery query = (ActionSearchQuery) argument;
			@SuppressWarnings("null")
			Map<Long, BaseAction> actions = host.searchActions(query);
			answerPacket = new PacketActions(actions);
			break;

		default:
			throw new IllegalStateException("Unknown data request type " + String.valueOf(request));
		}

		ctx.writeAndFlush(answerPacket);
	}

	private BufferedImage getLogo() {
		File logoFile = new File("logo.png");
		if (!logoFile.isFile()) return null;

		try {
			return ImageIO.read(logoFile);
		} catch (IOException e) {
			return null;
		}
	}
}
