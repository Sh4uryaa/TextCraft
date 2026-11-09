package com.textcraft.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.Timer;

/** Small message that appears bottom-right of the window and hides itself (no animation = Lite friendly). */
public final class Toast {
    private Toast() { }

    public static void show(JFrame owner, String message) {
        Palette p = ThemeManager.getInstance().palette();
        JWindow w = new JWindow(owner);
        JLabel label = new JLabel(message);
        label.setOpaque(true);
        label.setBackground(p.text);          // inverted colours so it stands out in both themes
        label.setForeground(p.surface);
        label.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        w.add(label);
        w.pack();
        Dimension d = w.getSize();
        Point loc = owner.getLocationOnScreen();
        w.setLocation(loc.x + owner.getWidth() - d.width - 24, loc.y + owner.getHeight() - d.height - 56);
        w.setBackground(new Color(0, 0, 0, 0));
        w.setVisible(true);
        Timer t = new Timer(2500, e -> w.dispose());
        t.setRepeats(false);
        t.start();
    }
}
