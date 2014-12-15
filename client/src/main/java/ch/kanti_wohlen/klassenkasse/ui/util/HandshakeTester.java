package ch.kanti_wohlen.klassenkasse.ui.util;

import javax.swing.JLabel;

import ch.kanti_wohlen.klassenkasse.network.HandshakeSocket;

public class HandshakeTester {

	private final JLabel label;
	private HandshakeSocket runnable;

	public HandshakeTester(JLabel resultLabel) {
		label = resultLabel;
	}

	public void setTarget(String hostName, int port) {
		cancel();

		label.setIcon(IconProvider.LOADING);
		label.setText("Es wird versucht, eine Verbindung herzustellen...");

		runnable = new HandshakeSocket(label, hostName, port);
		new Thread(runnable).start();
	}

	public void cancel() {
		if (runnable != null) {
			runnable.cancel();
		}
	}
}
