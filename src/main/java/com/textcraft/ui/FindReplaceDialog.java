package com.textcraft.ui;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** Non-modal Find &amp; replace with match case, whole word and regular expression options. */
public class FindReplaceDialog extends JDialog {
    private final Supplier<TextEditor> editorSupplier;
    private final JTextField find = new JTextField(20);
    private final JTextField replace = new JTextField(20);
    private final JCheckBox matchCase = new JCheckBox("Match case");
    private final JCheckBox wholeWord = new JCheckBox("Whole word");
    private final JCheckBox regex = new JCheckBox("Regex");
    private final JLabel status = new JLabel(" ");

    public FindReplaceDialog(Window owner, Supplier<TextEditor> editorSupplier) {
        super(owner, "Find and replace", ModalityType.MODELESS);
        this.editorSupplier = editorSupplier;
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 3, 3, 3);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        p.add(new JLabel("Find"), c);
        c.gridx = 1;
        p.add(find, c);
        c.gridx = 0;
        c.gridy = 1;
        p.add(new JLabel("Replace with"), c);
        c.gridx = 1;
        p.add(replace, c);
        JPanel opts = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        opts.add(matchCase);
        opts.add(wholeWord);
        opts.add(regex);
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        p.add(opts, c);

        JButton next = new JButton("Find next");
        JButton rep = new JButton("Replace");
        JButton all = new JButton("Replace all");
        next.addActionListener(e -> findNext());
        rep.addActionListener(e -> replaceOne());
        all.addActionListener(e -> replaceAll());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.add(next);
        buttons.add(rep);
        buttons.add(all);
        c.gridy = 3;
        p.add(buttons, c);
        c.gridy = 4;
        p.add(status, c);

        setContentPane(p);
        getRootPane().setDefaultButton(next);
        pack();
        setAlwaysOnTop(true);
        setLocationRelativeTo(owner);
    }

    /** Pre-fill with the current selection and bring to front. */
    public void open(String selected) {
        if (selected != null && !selected.isEmpty() && !selected.contains("\n")) find.setText(selected);
        setVisible(true);
        find.requestFocusInWindow();
        find.selectAll();
    }

    private Pattern pattern() {
        String f = find.getText();
        if (f.isEmpty()) return null;
        try {
            String body = regex.isSelected() ? f : Pattern.quote(f);
            if (wholeWord.isSelected()) body = "\\b(?:" + body + ")\\b";
            int flags = matchCase.isSelected() ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
            return Pattern.compile(body, flags);
        } catch (PatternSyntaxException e) {
            status.setText("Invalid regular expression");
            return null;
        }
    }

    private void findNext() {
        TextEditor ed = editorSupplier.get();
        Pattern p = pattern();
        if (ed == null || p == null) return;
        Matcher m = p.matcher(ed.getAllText());
        int from = ed.getSelectionEnd();
        boolean found = m.find(Math.min(from, ed.getAllText().length()));
        if (!found) found = m.find(0); // wrap around
        if (found && m.end() > m.start()) {
            ed.select(m.start(), m.end());
            status.setText(" ");
        } else {
            status.setText("Not found");
        }
    }

    private void replaceOne() {
        TextEditor ed = editorSupplier.get();
        Pattern p = pattern();
        if (ed == null || p == null) return;
        String sel = ed.getSelectedText();
        Matcher m = p.matcher(sel);
        if (!sel.isEmpty() && m.matches()) {
            ed.replaceSelection(regex.isSelected() ? m.replaceFirst(replace.getText()) : replace.getText());
        }
        findNext();
    }

    private void replaceAll() {
        TextEditor ed = editorSupplier.get();
        Pattern p = pattern();
        if (ed == null || p == null) return;
        String text = ed.getAllText();
        Matcher m = p.matcher(text);
        List<int[]> spans = new ArrayList<>();
        List<String> repl = new ArrayList<>();
        while (m.find()) {
            if (m.end() == m.start()) continue;
            spans.add(new int[]{m.start(), m.end()});
            repl.add(regex.isSelected()
                    ? p.matcher(m.group()).replaceFirst(replace.getText()) : replace.getText());
        }
        for (int i = spans.size() - 1; i >= 0; i--) { // from the end so earlier offsets stay valid
            ed.replaceRange(spans.get(i)[0], spans.get(i)[1], repl.get(i));
        }
        status.setText(spans.size() + " replaced");
    }
}
