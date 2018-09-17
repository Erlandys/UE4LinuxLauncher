package launcher;

import javax.swing.*;
import java.awt.event.*;

public class TwoFactorForm extends JFrame {
	private JTextField _securityCodeTextField;
	private JButton _confirmButton;
	private JTextPane _unrealEngine4MarketplaceTextPane;
	private JTextPane errorInfo;
	private JPanel _twoFactorForm;

	public TwoFactorForm() {
		super("Two Factor Authentification");
		setContentPane(_twoFactorForm);
		setVisible(true);
		setResizable(false);
		pack();
		_securityCodeTextField.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				super.focusGained(e);
				if (_securityCodeTextField.getText().equalsIgnoreCase("Security code"))
					_securityCodeTextField.setText("");
			}

			@Override
			public void focusLost(FocusEvent e) {
				super.focusLost(e);
				if (_securityCodeTextField.getText().equalsIgnoreCase(""))
					_securityCodeTextField.setText("Security code");
			}
		});
		_confirmButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new PerformTwoFactor().start();
			}
		});
		_securityCodeTextField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent keyEvent) {
				super.keyReleased(keyEvent);
				if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
					new PerformTwoFactor().start();
				}
			}

			@Override
			public void keyPressed(KeyEvent e) {
				if (_securityCodeTextField.getText().equalsIgnoreCase("Security code"))
					_securityCodeTextField.setText("");
				super.keyPressed(e);
			}
		});
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				super.windowClosing(e);
				Main.getInstance().getLoginForm().allowActions();
			}
		});
	}

	public void showError(String error) {
		_securityCodeTextField.setText("Security code");
		errorInfo.setText("<html>\n" +
				"  <head>\n" +
				"    \n" +
				"  </head>\n" +
				"  <body>\n" +
				"    <p align=\"center\" style=\"margin-top: 10\" style=\"font-family: Lato, Helvetica, Arial, sans-serif, BrutalType; font-size: 10px;color: #dc5353; font-weight: bold;\">\n" +
				"    \t" + error + "\n" +
				"    </p>\n" +
				"  </body>\n" +
				"</html>");
		_confirmButton.setEnabled(true);
		_securityCodeTextField.setEnabled(true);
	}
	private class PerformTwoFactor extends Thread {

		@Override
		public void run() {
			errorInfo.setText("");
			_confirmButton.setEnabled(false);
			_securityCodeTextField.setEnabled(false);
			Main.getInstance().getEpicAPI().confirmTwoFactorAuth(_securityCodeTextField.getText());
		}
	}
}
