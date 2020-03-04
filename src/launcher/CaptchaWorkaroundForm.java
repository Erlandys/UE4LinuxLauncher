package launcher;

import launcher.managers.AuthenticationManager;
import launcher.managers.SessionManager;

import javax.swing.*;
import java.awt.event.*;

public class CaptchaWorkaroundForm extends JFrame {
	private JTextPane _unrealEngine4MarketplaceTextPane;
	private JButton _confirmButton;
	private JTextPane errorInfo;
	private JTextArea _bdaTextArea;
	private JPanel _workaroundForm;
	private JTextArea _userBrowserTextArea;
	private JButton _whatAndHowButton;

	public CaptchaWorkaroundForm() {
		super("Two Factor Authentification");
		setContentPane(_workaroundForm);
		setVisible(true);
		setResizable(false);
		pack();
		_bdaTextArea.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				super.focusGained(e);
				if (_bdaTextArea.getText().equalsIgnoreCase("QmFzZTY0IGNvZGU="))
					_bdaTextArea.setText("");
			}

			@Override
			public void focusLost(FocusEvent e) {
				super.focusLost(e);
				if (_bdaTextArea.getText().equalsIgnoreCase(""))
					_bdaTextArea.setText("QmFzZTY0IGNvZGU=");
			}
		});
		_userBrowserTextArea.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				super.focusGained(e);
				if (_userBrowserTextArea.getText().equalsIgnoreCase("Browser:"))
					_userBrowserTextArea.setText("");
			}

			@Override
			public void focusLost(FocusEvent e) {
				super.focusLost(e);
				if (_userBrowserTextArea.getText().equalsIgnoreCase(""))
					_userBrowserTextArea.setText("Browser:");
			}
		});
		_confirmButton.addActionListener(e -> new PerformTwoFactor().start());
		_whatAndHowButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				String text = "To go through captcha for launcher, you have to pass captcha through your browser and then you have to connect browser data with launcher data, to do that, there's couple steps:\n";
				text += "1. Open your browser.\n";
				text += "2. Go to https://www.unrealengine.com/id/login\n";
				text += "3. Open developer tools Network tab.\n";
				text += "4. [Optional] Select to filter only XHR requests.\n";
				text += "5. Fill your credentials and press login.\n";
				text += "5.1. If captcha will be requested in browser, complete it.\n";
				text += "6. In network tab, find request to 'https://epic-games-api.arkoselabs.com/fc/gt2/public_key/37D033EB-6489-3763-2AE1-A228C04103F5' this page.\n";
				text += "7. Open sent inputs.\n";
				text += "8. Find 'bda' and 'userbrowser' parameters.\n";
				text += "9. Copy them to launcher and try to continue.\n\n";
				text += "P.s. Sorry for this inconvenience, currently I cannot find a way to make this automatic.\n";
				JOptionPane.showMessageDialog(_unrealEngine4MarketplaceTextPane, text, "Information about BDA ant User Browser.", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		_bdaTextArea.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent keyEvent) {
				if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
					_userBrowserTextArea.grabFocus();
				}
				else
					super.keyReleased(keyEvent);
			}

			@Override
			public void keyPressed(KeyEvent e) {
				if (_bdaTextArea.getText().equalsIgnoreCase("QmFzZTY0IGNvZGU="))
					_bdaTextArea.setText("");
				if (e.getKeyCode() != KeyEvent.VK_ENTER)
					super.keyPressed(e);
			}
		});
		_userBrowserTextArea.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent keyEvent) {
				if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
					new CaptchaWorkaroundForm.PerformTwoFactor().start();
				}
				else
					super.keyReleased(keyEvent);
			}

			@Override
			public void keyPressed(KeyEvent e) {
				if (_userBrowserTextArea.getText().equalsIgnoreCase("Browser:"))
					_userBrowserTextArea.setText("");
				if (e.getKeyCode() != KeyEvent.VK_ENTER)
					super.keyPressed(e);
			}
		});
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				CaptchaWorkaroundForm.super.setVisible(false);
				LoginForm.getInstance().allowActions();
			}
		});
	}

	public void showError(String error) {
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
		_bdaTextArea.setEnabled(true);
		_userBrowserTextArea.setEnabled(true);
	}

	public void start() {
		setVisible(true);
		_bdaTextArea.setText("QmFzZTY0IGNvZGU=");
		_userBrowserTextArea.setText("Browser:");
		errorInfo.setText("");
		_confirmButton.setEnabled(true);
		_bdaTextArea.setEnabled(true);
		_userBrowserTextArea.setEnabled(true);
	}

	private class PerformTwoFactor extends Thread {
		@Override
		public void run() {
			if (_bdaTextArea.getText().isEmpty() || _bdaTextArea.getText().equals("QmFzZTY0IGNvZGU=")) {
				showError("BDA parameter must be filled.");
				return;
			}
			if (_userBrowserTextArea.getText().isEmpty() || _userBrowserTextArea.getText().equals("Browser:")) {
				showError("User browser must be filled.");
				return;
			}
			errorInfo.setText("");
			_confirmButton.setEnabled(false);
			_bdaTextArea.setEnabled(false);
			_userBrowserTextArea.setEnabled(false);
			setVisible(false);
			SessionManager.getInstance().getSession().setUserBrowser(_userBrowserTextArea.getText().trim());
			SessionManager.getInstance().getSession().setBDA(_bdaTextArea.getText().trim());
			AuthenticationManager.getInstance().doLogin();
		}
	}

	public static CaptchaWorkaroundForm getInstance() {
		return CaptchaWorkaroundForm.SingletonHolder._instance;
	}

	private static class SingletonHolder {
		protected static final CaptchaWorkaroundForm _instance = new CaptchaWorkaroundForm();
	}
}
