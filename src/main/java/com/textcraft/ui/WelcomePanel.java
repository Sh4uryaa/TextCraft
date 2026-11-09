package com.textcraft.ui;

import java.awt.Dimension;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Start screen: New, Open, Open from cloud, Recent files, Sign in with Google. */
public class WelcomePanel extends JPanel implements ThemeChangeListener {
    private final JPanel recentBox = new JPanel();
    private final Consumer<File> openRecent;
    private final JLabel hello = new JLabel("Welcome to TextCraft");
    private final JLabel recentTitle = new JLabel("Recent files");

    public WelcomePanel(Runnable onNew, Runnable onOpen, Runnable onCloud, Runnable onSignIn, Consumer<File> openRecent) {
        this.openRecent = openRecent;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(80, 0, 0, 0));
        hello.setFont(hello.getFont().deriveFont(28f));
        hello.setAlignmentX(0.5f);
        add(hello);
        add(Box.createVerticalStrut(24));
        add(button("New document", onNew));
        add(button("Open file...", onOpen));
        add(button("Open from cloud", onCloud));
        add(button("Sign in with Google", onSignIn));
        add(Box.createVerticalStrut(20));
        recentTitle.setAlignmentX(0.5f);
        add(recentTitle);
        recentBox.setOpaque(false);
        recentBox.setLayout(new BoxLayout(recentBox, BoxLayout.Y_AXIS));
        recentBox.setAlignmentX(0.5f);
        add(recentBox);
        ThemeManager.getInstance().addListener(this);
        onThemeChanged(ThemeManager.getInstance().getResolved(), ThemeManager.getInstance().palette());
    }

    private JButton button(String text, Runnable action) {
        JButton b = new JButton(text);
        b.setAlignmentX(0.5f);
        b.setMaximumSize(new Dimension(240, 38));
        b.setPreferredSize(new Dimension(240, 38));
        b.addActionListener(e -> action.run());
        add(Box.createVerticalStrut(10));
        return b;
    }

    public void setRecent(List<File> files) {
        recentBox.removeAll();
        recentTitle.setVisible(!files.isEmpty());
        for (File f : files) {
            JButton b = new JButton(f.getName());
            b.setToolTipText(f.getAbsolutePath());
            b.putClientProperty("JButton.buttonType", "borderless");
            b.setAlignmentX(0.5f);
            b.addActionListener(e -> openRecent.accept(f));
            recentBox.add(b);
        }
        revalidate();
        repaint();
    }

    @Override
    public void onThemeChanged(Theme resolved, Palette p) {
        setBackground(p.workspace);
        hello.setForeground(p.text);
        recentTitle.setForeground(p.text2);
    }
}
