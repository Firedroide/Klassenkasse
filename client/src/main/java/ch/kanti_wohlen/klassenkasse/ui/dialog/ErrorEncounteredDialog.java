package ch.kanti_wohlen.klassenkasse.ui.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class ErrorEncounteredDialog extends JDialog implements ActionListener {

	public enum Result {
		ABORT,
		RETRY
	}

	private final JPanel contentPanel = new JPanel();

	private Result result;

	/**
	 * Create the dialog.
	 */
	public ErrorEncounteredDialog(String errorMessage) {
		setBounds(100, 100, 450, 300);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setLayout(new FlowLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("Wiederholen");
				okButton.setActionCommand("Retry");
				okButton.addActionListener(this);
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Abbrechen");
				cancelButton.setActionCommand("Abort");
				cancelButton.addActionListener(this);
				buttonPane.add(cancelButton);
			}
		}
	}

	public Result getResult() {
		return result;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if ("Retry".equals(e.getActionCommand())) {
			result = Result.RETRY;
		} else if ("Abort".equals(e.getActionCommand())) {
			result = Result.ABORT;
		}
	}
}
