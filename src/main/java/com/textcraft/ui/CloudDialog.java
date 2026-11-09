package com.textcraft.ui;

import com.textcraft.app.AppContext;
import com.textcraft.model.Document;
import com.textcraft.util.AppExecutors;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** "Open from cloud": search by title, sort by date, open or delete. */
public class CloudDialog extends JDialog {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private final long userId;
    private final DefaultListModel<Document> model = new DefaultListModel<>();
    private final JList<Document> list = new JList<>(model);
    private final JTextField search = new JTextField(18);
    private final JCheckBox newest = new JCheckBox("Newest first", true);
    private final JLabel status = new JLabel(" ");
    private Document chosen;

    public CloudDialog(Window owner, long userId) {
        super(owner, "Open from cloud", ModalityType.APPLICATION_MODAL);
        this.userId = userId;
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer((l, d, i, sel, focus) -> {
            JLabel lab = new JLabel("  " + d.getTitle() + "   — " + (d.getDocType().name().equals("CODE") ? "code" : "document")
                    + ", " + (d.getUpdatedAt() == null ? "" : d.getUpdatedAt().format(FMT)));
            lab.setOpaque(true);
            lab.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 4, 6, 4));
            lab.setBackground(sel ? l.getSelectionBackground() : l.getBackground());
            lab.setForeground(sel ? l.getSelectionForeground() : l.getForeground());
            return lab;
        });
        list.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { if (e.getClickCount() == 2) open(); }
        });

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        top.add(new JLabel("Search:"));
        top.add(search);
        top.add(newest);
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { reload(); }
            @Override public void removeUpdate(DocumentEvent e) { reload(); }
            @Override public void changedUpdate(DocumentEvent e) { reload(); }
        });
        newest.addActionListener(e -> reload());

        JButton open = new JButton("Open");
        JButton delete = new JButton("Delete");
        JButton close = new JButton("Close");
        open.addActionListener(e -> open());
        delete.addActionListener(e -> delete());
        close.addActionListener(e -> dispose());
        JPanel bottom = new JPanel(new BorderLayout());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        buttons.add(delete);
        buttons.add(close);
        buttons.add(open);
        bottom.add(status, BorderLayout.WEST);
        bottom.add(buttons, BorderLayout.EAST);

        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(list), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        setSize(560, 400);
        setLocationRelativeTo(owner);
        reload();
    }

    /** The document the user opened, or empty. */
    public Optional<Document> showAndGet() {
        setVisible(true);
        return Optional.ofNullable(chosen);
    }

    private void reload() {
        status.setText("  Loading...");
        String q = search.getText();
        boolean nf = newest.isSelected();
        AppExecutors.asyncChecked(() -> AppContext.get().documents().list(userId, q, nf), r -> {
            model.clear();
            if (r.isOk()) {
                r.get().forEach(model::addElement);
                status.setText("  " + r.get().size() + " document(s)");
            } else {
                status.setText("  " + r.error());
            }
        });
    }

    private void open() {
        Document d = list.getSelectedValue();
        if (d == null) return;
        chosen = d;
        dispose();
    }

    private void delete() {
        Document d = list.getSelectedValue();
        if (d == null) return;
        int ok = JOptionPane.showConfirmDialog(this, "Delete \"" + d.getTitle() + "\" from the cloud? This cannot be undone.",
                "Delete", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        AppExecutors.asyncChecked(() -> {
            AppContext.get().documents().delete(userId, d.getId());
            return Boolean.TRUE;
        }, r -> {
            if (r.isOk()) reload();
            else ErrorHandler.error(r.error());
        });
    }
}
