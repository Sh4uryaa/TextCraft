package com.textcraft.ui;

import com.textcraft.app.AppContext;
import com.textcraft.model.User;
import com.textcraft.util.AppExecutors;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/** Sign in with Google (main button) or with email + password / create an account (fallback). */
public class LoginDialog extends JDialog {
    private final JTextField email = new JTextField(22);
    private final JPasswordField password = new JPasswordField(22);
    private final JTextField name = new JTextField(22);
    private final JLabel message = new JLabel(" ");
    private final JButton google = new JButton("Sign in with Google");
    private final JButton login = new JButton("Sign in");
    private final JButton register = new JButton("Create account");

    public LoginDialog(Window owner) {
        super(owner, "Sign in to TextCraft", ModalityType.APPLICATION_MODAL);
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 20, 16, 20));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridwidth = 2;
        c.gridy = 0;
        p.add(google, c);
        c.gridy = 1;
        p.add(new JLabel("or use your email:", JLabel.CENTER), c);
        c.gridwidth = 1;
        row(p, c, 2, "Email", email);
        row(p, c, 3, "Password", password);
        row(p, c, 4, "Name (new account)", name);
        c.gridx = 0;
        c.gridy = 5;
        p.add(login, c);
        c.gridx = 1;
        p.add(register, c);
        c.gridx = 0;
        c.gridy = 6;
        c.gridwidth = 2;
        p.add(message, c);
        setContentPane(p);

        google.addActionListener(e -> run(() -> AppContext.get().auth().signInWithGoogle(), "Waiting for Google in your browser..."));
        login.addActionListener(e -> run(() -> AppContext.get().auth().login(email.getText(), new String(password.getPassword())), "Signing in..."));
        register.addActionListener(e -> run(() -> AppContext.get().auth().register(email.getText(),
                new String(password.getPassword()), name.getText()), "Creating account..."));
        getRootPane().setDefaultButton(login);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private static void row(JPanel p, GridBagConstraints c, int y, String label, java.awt.Component field) {
        c.gridy = y;
        c.gridx = 0;
        p.add(new JLabel(label), c);
        c.gridx = 1;
        p.add(field, c);
    }

    private void run(AppExecutors.ThrowingSupplier<User> work, String busy) {
        message.setText(busy);
        message.setForeground(ThemeManager.getInstance().palette().text2);
        setButtons(false);
        AppExecutors.asyncChecked(work, r -> {
            setButtons(true);
            if (r.isOk()) {
                dispose();
            } else {
                message.setText("<html>" + r.error().replace("\n", "<br>") + "</html>");
                message.setForeground(ThemeManager.getInstance().palette().error);
                pack();
            }
        });
    }

    private void setButtons(boolean on) {
        google.setEnabled(on);
        login.setEnabled(on);
        register.setEnabled(on);
    }
}
