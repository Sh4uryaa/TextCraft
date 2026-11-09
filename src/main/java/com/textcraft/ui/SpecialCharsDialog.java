package com.textcraft.ui;

import java.awt.GridLayout;
import java.awt.Window;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

/** Small grid of common special characters; clicking one inserts it at the caret. */
public class SpecialCharsDialog extends JDialog {
    private static final String CHARS = "©®™°±×÷≤≥≠≈∞πΩμ∑"
            + "←↑→↓↔⇒✓✗★☆•…–—«»"
            + "€£¥₹¢§¶†½¼¾²³éñü";

    public SpecialCharsDialog(Window owner, Consumer<String> insert) {
        super(owner, "Special characters", ModalityType.MODELESS);
        JPanel grid = new JPanel(new GridLayout(0, 8, 4, 4));
        grid.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        CHARS.codePoints().forEach(cp -> {
            String s = new String(Character.toChars(cp));
            JButton b = new JButton(s);
            b.setFocusable(false);
            b.addActionListener(e -> insert.accept(s));
            grid.add(b);
        });
        setContentPane(grid);
        pack();
        setAlwaysOnTop(true);
        setLocationRelativeTo(owner);
    }
}
