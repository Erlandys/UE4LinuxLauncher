package launcher;

import javax.swing.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class LoginForm extends JFrame {
    private JPanel loginPanel;
    private JButton loginButton;
    private JPasswordField passwordPasswordField;
    private JTextField usernameTextField;
    private JTextPane unrealEngine4MarketplaceTextPane;
    private JTextPane errorInfo;
    private JProgressBar progressBar1;
    private boolean _changePassword = false;

    public LoginForm() {
        super("Login");
        setContentPane(loginPanel);
        setVisible(true);
        setResizable(false);
        pack();
        usernameTextField.setEnabled(false);
        passwordPasswordField.setEnabled(false);
        loginButton.setEnabled(false);
        errorInfo.setText("<html>\n" +
                "  <head>\n" +
                "    \n" +
                "  </head>\n" +
                "  <body>\n" +
                "    <p align=\"center\" style=\"margin-top: 10\" style=\"font-family: Lato, Helvetica, Arial, sans-serif, BrutalType; font-size: 10px;color: #444444; font-weight: bold;\">\n" +
                "    \tChecking for auto login...\n" +
                "    </p>\n" +
                "  </body>\n" +
                "</html>");
        usernameTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent focusEvent) {
                super.focusGained(focusEvent);
                if (usernameTextField.getText().equalsIgnoreCase("Username:"))
                    usernameTextField.setText("");
            }

            @Override
            public void focusLost(FocusEvent focusEvent) {
                super.focusGained(focusEvent);
                if (usernameTextField.getText().equalsIgnoreCase(""))
                    usernameTextField.setText("Username:");
            }
        });
        passwordPasswordField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent focusEvent) {
                super.focusGained(focusEvent);
                if (new String(passwordPasswordField.getPassword()).equalsIgnoreCase("Password:") || _changePassword) {
                    passwordPasswordField.setText("");
                    _changePassword = false;
                }
            }

            @Override
            public void focusLost(FocusEvent focusEvent) {
                super.focusGained(focusEvent);
                if (new String(passwordPasswordField.getPassword()).equalsIgnoreCase(""))
                    passwordPasswordField.setText("Password:");
            }
        });
        loginButton.addActionListener(actionEvent -> new PerformLogin().start());
        passwordPasswordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent keyEvent) {
                super.keyReleased(keyEvent);
                if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
                    new PerformLogin().start();
                }
            }
        });
    }

    public void increaseProgressBarValue(int value) {
        progressBar1.setValue(progressBar1.getValue() + value);
    }

    public void allowActions() {
        errorInfo.setText("");
        usernameTextField.setEnabled(true);
        passwordPasswordField.setEnabled(true);
        loginButton.setEnabled(true);
    }

    public void badLogin() {
        errorInfo.setText("<html>\n" +
                "  <head>\n" +
                "    \n" +
                "  </head>\n" +
                "  <body>\n" +
                "    <p align=\"center\" style=\"margin-top: 10\" style=\"font-family: Lato, Helvetica, Arial, sans-serif, BrutalType; font-size: 10px;color: #dc5353; font-weight: bold;\">\n" +
                "    \tSorry the credentials you are using are invalid.\n" +
                "    </p>\n" +
                "  </body>\n" +
                "</html>");
        usernameTextField.setEnabled(true);
        passwordPasswordField.setEnabled(true);
        loginButton.setEnabled(true);
        _changePassword = true;
    }

    public void loginError(String error) {
        setErrorText(error);
        usernameTextField.setEnabled(true);
        passwordPasswordField.setEnabled(true);
        loginButton.setEnabled(true);
        _changePassword = true;
    }

    public void clearError()
    {
        errorInfo.setText("");
    }

    public void setErrorText(String error)
    {
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
    }

    public void setLoginData(String username, String password) {
        usernameTextField.setText(username);
        passwordPasswordField.setText(password);
    }


    private class PerformLogin extends Thread {

        @Override
        public void run() {
            errorInfo.setText("");
            usernameTextField.setEnabled(false);
            passwordPasswordField.setEnabled(false);
            loginButton.setEnabled(false);
            progressBar1.setValue(0);
            Main.getInstance().getUser().setUsername(usernameTextField.getText());
            Main.getInstance().getUser().setPassword(new String(passwordPasswordField.getPassword()));
            Main.getInstance().getEpicAPI().doLogin();
        }
    }
}
