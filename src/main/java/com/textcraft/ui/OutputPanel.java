package com.textcraft.ui;

import com.textcraft.model.CompileResult;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/** Bottom panel in Code mode: stdin box, output (stdout / stderr / compile errors), status line. */
public class OutputPanel extends JPanel implements ThemeChangeListener {
    private final JTextArea stdin = new JTextArea();
    private final JTextPane output = new JTextPane();
    private final JLabel status = new JLabel("Ready");
    private final JButton explain = new JButton("Explain error with AI");
    private String lastError = "";
    private Palette palette = ThemeManager.getInstance().palette();

    /** @param onExplain gets the error text (called when the user clicks "Explain error with AI") */
    public OutputPanel(Runnable onClear, Runnable onStop, java.util.function.Consumer<String> onExplain) {
        super(new BorderLayout());
        setPreferredSize(new Dimension(100, 220));
        output.setEditable(false);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        stdin.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        stdin.setToolTipText("Input for the program (stdin)");

        JPanel left = new JPanel(new BorderLayout());
        left.add(new JLabel("  Input (stdin)"), BorderLayout.NORTH);
        left.add(new JScrollPane(stdin), BorderLayout.CENTER);
        JPanel right = new JPanel(new BorderLayout());
        right.add(new JLabel("  Output"), BorderLayout.NORTH);
        right.add(new JScrollPane(output), BorderLayout.CENTER);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.25);
        split.setBorder(BorderFactory.createEmptyBorder());

        JButton clear = new JButton("Clear");
        clear.addActionListener(e -> onClear.run());
        JButton stop = new JButton("Stop");
        stop.addActionListener(e -> onStop.run());
        explain.setEnabled(false);
        explain.addActionListener(e -> onExplain.accept(lastError));
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setOpaque(false);
        bar.add(status);
        bar.add(stop);
        bar.add(clear);
        bar.add(explain);

        add(bar, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        ThemeManager.getInstance().addListener(this);
        onThemeChanged(ThemeManager.getInstance().getResolved(), palette);
    }

    public String getStdin() {
        return stdin.getText();
    }

    public void showRunning() {
        clear();
        status.setText("Running...");
        status.setForeground(palette.text2);
    }

    public void showResult(CompileResult r) {
        clearText();
        append(r.getCompileOutput(), palette.error);
        append(r.getStdout(), palette.text);
        append(r.getStderr(), palette.error);
        if (r.combinedOutput().trim().isEmpty()) append("(no output)", palette.text2);
        boolean ok = r.isSuccess();
        status.setText("Status: " + r.getStatus() + "   |   Time: " + r.getTimeMs() + " ms");
        status.setForeground(ok ? palette.ok : palette.error);
        lastError = ok ? "" : (r.getCompileOutput() + "\n" + r.getStderr() + "\n" + r.getStatus()).trim();
        explain.setEnabled(!lastError.isEmpty());
    }

    public void showError(String message) {
        clearText();
        append(message, palette.error);
        status.setText("Could not run");
        status.setForeground(palette.error);
        lastError = message;
        explain.setEnabled(false);
    }

    public void clear() {
        clearText();
        status.setText("Ready");
        status.setForeground(palette.text2);
        lastError = "";
        explain.setEnabled(false);
    }

    private void clearText() {
        output.setText("");
    }

    private void append(String text, Color color) {
        if (text == null || text.isEmpty()) return;
        StyledDocument d = output.getStyledDocument();
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setForeground(a, color);
        try {
            d.insertString(d.getLength(), text.endsWith("\n") ? text : text + "\n", a);
        } catch (BadLocationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void onThemeChanged(Theme resolved, Palette p) {
        this.palette = p;
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, p.border));
        status.setForeground(p.text2);
        String keep = output.getText();
        if (!keep.isEmpty()) { // colours were fixed when the text was added: show plain text again in the new theme
            output.setText("");
            append(keep, p.text);
        }
    }
}
