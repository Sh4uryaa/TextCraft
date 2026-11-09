package com.textcraft.ui;

import com.textcraft.app.AppContext;
import com.textcraft.io.HtmlCodec;
import com.textcraft.model.DocType;
import com.textcraft.model.DocumentVersion;
import com.textcraft.util.AppExecutors;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.text.DefaultStyledDocument;

/** Lists the saved versions of a cloud document with a text preview; the user can restore one. */
public class VersionHistoryDialog extends JDialog {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");

    private final DocType type;
    private final DefaultListModel<DocumentVersion> model = new DefaultListModel<>();
    private final JList<DocumentVersion> list = new JList<>(model);
    private final JTextArea preview = new JTextArea();
    private DocumentVersion restore;

    public VersionHistoryDialog(Window owner, long userId, long documentId, DocType type) {
        super(owner, "Version history", ModalityType.APPLICATION_MODAL);
        this.type = type;
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer((l, v, i, sel, f) -> {
            javax.swing.JLabel lab = new javax.swing.JLabel("  " + (v.getSavedAt() == null ? "?" : v.getSavedAt().format(FMT)));
            lab.setOpaque(true);
            lab.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 4, 5, 4));
            lab.setBackground(sel ? l.getSelectionBackground() : l.getBackground());
            lab.setForeground(sel ? l.getSelectionForeground() : l.getForeground());
            return lab;
        });
        list.addListSelectionListener(e -> showPreview(list.getSelectedValue()));
        preview.setEditable(false);
        preview.setLineWrap(true);
        preview.setWrapStyleWord(true);

        JButton restoreBtn = new JButton("Restore this version");
        JButton close = new JButton("Close");
        restoreBtn.addActionListener(e -> {
            restore = list.getSelectedValue();
            dispose();
        });
        close.addActionListener(e -> dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        bottom.add(close);
        bottom.add(restoreBtn);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(list), new JScrollPane(preview));
        split.setResizeWeight(0.3);
        setLayout(new BorderLayout());
        add(split, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        setSize(720, 440);
        setLocationRelativeTo(owner);

        AppExecutors.asyncChecked(() -> AppContext.get().documents().versionsOf(userId, documentId), r -> {
            if (!r.isOk()) {
                ErrorHandler.error(r.error());
                return;
            }
            r.get().forEach(model::addElement);
            if (!model.isEmpty()) list.setSelectedIndex(0);
        });
    }

    public Optional<DocumentVersion> showAndGet() {
        setVisible(true);
        return Optional.ofNullable(restore);
    }

    private void showPreview(DocumentVersion v) {
        if (v == null) {
            preview.setText("");
            return;
        }
        String text = v.getContent() == null ? "" : v.getContent();
        if (type == DocType.RICH) { // show readable text, not HTML tags
            try {
                DefaultStyledDocument d = new DefaultStyledDocument();
                HtmlCodec.fromHtml(text, d);
                text = d.getText(0, d.getLength());
            } catch (Exception e) {
                // fall back to the raw content
            }
        }
        preview.setText(text);
        preview.setCaretPosition(0);
    }
}
