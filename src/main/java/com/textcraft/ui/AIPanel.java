package com.textcraft.ui;

import com.textcraft.ai.AIAction;
import com.textcraft.ai.Tone;
import com.textcraft.app.AppContext;
import com.textcraft.util.AppExecutors;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Side panel for AI results. The AI never changes the document by itself: the result is shown here
 * and the user chooses Replace / Insert below / Copy / Discard.
 */
public class AIPanel extends JPanel {
    private final Supplier<TextEditor> editorSupplier;
    private final JLabel title = new JLabel("AI");
    private final JLabel status = new JLabel("Select text, then choose an action in the AI menu.");
    private final JTextArea result = new JTextArea();
    private final JButton replace = new JButton("Replace");
    private final JButton insert = new JButton("Insert below");
    private final JButton copy = new JButton("Copy");
    private final JButton discard = new JButton("Discard");

    private int srcStart;
    private int srcEnd;
    private String srcText = "";
    private boolean wholeDocument;
    private boolean canReplace;
    private int requestId; // ignores answers of older requests

    public AIPanel(Supplier<TextEditor> editorSupplier) {
        super(new BorderLayout(0, 8));
        this.editorSupplier = editorSupplier;
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        JPanel top = new JPanel(new BorderLayout(0, 4));
        top.setOpaque(false);
        top.add(title, BorderLayout.NORTH);
        status.setForeground(ThemeManager.getInstance().palette().text2);
        top.add(status, BorderLayout.SOUTH);

        result.setLineWrap(true);
        result.setWrapStyleWord(true);
        result.setEditable(true); // the user may tweak the text before using it
        result.setMargin(new java.awt.Insets(8, 8, 8, 8));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.setOpaque(false);
        for (JButton b : new JButton[]{replace, insert, copy, discard}) {
            b.setEnabled(false);
            buttons.add(b);
        }
        replace.addActionListener(e -> doReplace());
        insert.addActionListener(e -> doInsert());
        copy.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(result.getText()), null);
            ErrorHandler.toast("Copied");
        });
        discard.addActionListener(e -> reset("Select text, then choose an action in the AI menu."));

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(result), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
    }

    /** Runs one AI action on the current selection (or the whole document when nothing is selected). */
    public void run(AIAction action, Tone tone, String extra) {
        TextEditor ed = editorSupplier.get();
        if (ed == null) {
            ErrorHandler.toast("Open or create a document first.");
            return;
        }
        String sel = ed.getSelectedText();
        wholeDocument = sel.isEmpty();
        srcText = wholeDocument ? ed.getAllText() : sel;
        srcStart = wholeDocument ? 0 : ed.getSelectionStart();
        srcEnd = wholeDocument ? ed.getAllText().length() : ed.getSelectionEnd();
        if (srcText.trim().isEmpty()) {
            ErrorHandler.toast("There is no text yet. Type something or select a passage.");
            return;
        }
        canReplace = action.rewrites();
        start(action, tone, extra, wholeDocument ? "whole document" : "selection");
    }

    /** Runs an action on given text that is not part of the document (e.g. a program error). Result cannot replace anything. */
    public void runOnText(AIAction action, String text) {
        srcText = text;
        srcStart = 0;
        srcEnd = 0;
        wholeDocument = false;
        canReplace = false;
        start(action, null, null, "error message");
    }

    private void start(AIAction action, Tone tone, String extra, String what) {
        title.setText("AI: " + action.label() + (action == AIAction.TONE && tone != null ? " (" + tone.word() + ")" : ""));
        status.setText("Working... (" + what + ")");
        result.setText("");
        setButtons(false);
        final int id = ++requestId;
        final String text = srcText;

        AppExecutors.asyncChecked(() -> AppContext.get().ai().run(action, tone, text, extra), r -> {
            if (id != requestId) return; // a newer request replaced this one
            if (r.isOk()) {
                result.setText(r.get());
                result.setCaretPosition(0);
                status.setText("Done. Review the text, then choose what to do with it.");
                setButtons(true);
            } else {
                status.setText("The AI could not finish.");
                ErrorHandler.error(r.error());
            }
        });
    }

    private void setButtons(boolean ready) {
        replace.setEnabled(ready && canReplace);
        insert.setEnabled(ready);
        copy.setEnabled(ready);
        discard.setEnabled(ready);
    }

    private void reset(String message) {
        requestId++;
        result.setText("");
        status.setText(message);
        title.setText("AI");
        setButtons(false);
    }

    private boolean sourceStillThere(TextEditor ed) {
        String all = ed.getAllText();
        return srcEnd <= all.length() && all.substring(srcStart, srcEnd).equals(srcText);
    }

    private void doReplace() {
        TextEditor ed = editorSupplier.get();
        if (ed == null) return;
        if (!sourceStillThere(ed)) {
            ErrorHandler.error("The text changed since the AI was asked. Select it again and re-run the action.");
            return;
        }
        if (wholeDocument) {
            int ok = JOptionPane.showConfirmDialog(this, "Nothing was selected. Replace the WHOLE document with this text?",
                    "Replace document", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok != JOptionPane.OK_OPTION) return;
        }
        ed.replaceRange(srcStart, srcEnd, result.getText()); // one undoable edit
        ErrorHandler.toast("Replaced (Ctrl+Z to undo)");
        reset("Done. Choose another action when you are ready.");
    }

    private void doInsert() {
        TextEditor ed = editorSupplier.get();
        if (ed == null) return;
        ed.select(Math.min(srcEnd, ed.getAllText().length()), Math.min(srcEnd, ed.getAllText().length()));
        ed.insertBelowSelection(result.getText());
        ErrorHandler.toast("Inserted below (Ctrl+Z to undo)");
    }
}
