package ch.kanti_wohlen.klassenkasse.framework;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.Nullable;

import ch.kanti_wohlen.klassenkasse.login.LoginProvider;
import ch.kanti_wohlen.klassenkasse.login.LoginServerException;
import ch.kanti_wohlen.klassenkasse.network.ClientSocket;
import ch.kanti_wohlen.klassenkasse.network.packet.Packet;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketDataRequest;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketDataRequest.RequestType;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketErrorEncountered;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketLogin;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketLoginResponse;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketUsernames;

public class RemoteLoginProvider implements LoginProvider {

	private final ClientSocket socket;

	public RemoteLoginProvider(ClientSocket clientSocket) {
		socket = clientSocket;
	}

	@SuppressWarnings("null")
	@Override
	public Collection<String> getUsernames() {
		PacketDataRequest request = new PacketDataRequest(RequestType.USERNAMES);
		Future<? extends Packet> future = socket.transmitPacket(request);
		try {
			PacketUsernames response = (PacketUsernames) future.get(30, TimeUnit.SECONDS);
			return response.getUsernames();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		} catch (ClassCastException e) {
			e.printStackTrace();
		}

		return Collections.emptyList();
	}

	@Override
	public @Nullable String logIn(Host host, String username, char[] password, boolean isToken) throws LoginServerException {
		PacketLogin logIn = new PacketLogin(username, password, isToken);

		Future<? extends Packet> future = socket.transmitPacket(logIn);
		try {
			Packet response = future.get(5, TimeUnit.SECONDS);
			if (response instanceof PacketLoginResponse) {
				User loggedIn = host.getLoggedInUser();
				if (loggedIn == null) throw new IllegalStateException("Logged in, but no user logged in?");
				host.setLoggedInUser(loggedIn);
				return ((PacketLoginResponse) response).getToken();
			} else if (response instanceof PacketErrorEncountered) {
				throw new LoginServerException();
			} else {
				throw new IllegalStateException("Unexpected packet type.");
			}
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			throw new LoginServerException(e);
		} catch (TimeoutException e) {
			throw new LoginServerException(e);
		}
	}
}
