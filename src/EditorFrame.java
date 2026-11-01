
import javax.swing.*;
import java.awt.*;

public class EditorFrame extends JFrame {

    private final EditorPanel editorPanel;
    private final JLabel statusBar;
    private FindReplacePanel findReplacePanel;
    public EditorFrame() {
        setTitle("TextCraft - Java Text Editor");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        editorPanel = new EditorPanel();
        statusBar = new JLabel(" Line 1, Column 1");   
        findReplacePanel = new FindReplacePanel();
        add(findReplacePanel, BorderLayout.NORTH);
        findReplacePanel.setVisible(false);  
        setJMenuBar(createMenuBar());
        add(editorPanel, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
        editorPanel.getTextArea().addCaretListener(e -> updateStatusBar());
    }
    
private void showFindReplacePanel() {
    findReplacePanel.setVisible(true);
    findReplacePanel.revalidate();
    findReplacePanel.repaint();
    findReplacePanel.requestFocusInWindow();
}

private void updateStatusBar() {
    JTextArea textArea = editorPanel.getTextArea();

    int caretPosition = textArea.getCaretPosition();

    try {
        int line = textArea.getLineOfOffset(caretPosition);
        int column = caretPosition
                - textArea.getLineStartOffset(line);

        String text = textArea.getText().trim();
        int wordCount = text.isEmpty() ? 0 : text.split("\\s+").length;

        statusBar.setText(
                " Line " + (line + 1)
                + ", Column " + (column + 1)
                + " | Words: " + wordCount
        );
    } catch (javax.swing.text.BadLocationException ex) {
        statusBar.setText(" Ready");
    }
}


private JMenuBar createMenuBar() {
    JMenuBar bar = new JMenuBar();

    // File menu
    JMenu fileMenu = new JMenu("File");
    addMenuItem(fileMenu, "New", "control N");
    addMenuItem(fileMenu, "Open", "control O");
    addMenuItem(fileMenu, "Save", "control S");
    addMenuItem(fileMenu, "Save As", "control shift S");
    // Edit menu
    JMenu editMenu = new JMenu("Edit");
    addMenuItem(editMenu, "Undo", "control Z");
    addMenuItem(editMenu, "Redo", "control Y");
    editMenu.addSeparator();

    

    JMenuItem findItem = new JMenuItem("Find");
    findItem.setAccelerator(KeyStroke.getKeyStroke("control F"));
    findItem.addActionListener(e -> showFindReplacePanel());

    JMenuItem replaceItem = new JMenuItem("Replace");
    replaceItem.setAccelerator(KeyStroke.getKeyStroke("control R"));
    replaceItem.addActionListener(e -> showFindReplacePanel());

    editMenu.add(findItem);
    editMenu.add(replaceItem);

    editMenu.add(findItem);
    editMenu.add(replaceItem);
    editMenu.addSeparator();
    addMenuItem(editMenu, "Select All", "control A");

    // View menu
    JMenu viewMenu = new JMenu("View");
    JCheckBoxMenuItem wordWrapItem =
        new JCheckBoxMenuItem("Word Wrap");

    wordWrapItem.addActionListener(e -> {
        boolean enabled = wordWrapItem.isSelected();

        editorPanel.getTextArea().setLineWrap(enabled);
        editorPanel.getTextArea().setWrapStyleWord(enabled);

});
    addMenuItem(viewMenu, "Light Theme", null);
    addMenuItem(viewMenu, "Dark Theme", null);
viewMenu.add(wordWrapItem);
    // Help menu
    JMenu helpMenu = new JMenu("Help");
    addMenuItem(helpMenu, "About TextCraft", null);

    bar.add(fileMenu);
    bar.add(editMenu);
    bar.add(viewMenu);
    bar.add(helpMenu);

    return bar;
}

private void addMenuItem(JMenu menu, String title, String shortcut) {
    JMenuItem item = new JMenuItem(title);

    if (shortcut != null) {
        item.setAccelerator(KeyStroke.getKeyStroke(shortcut));
    }

    menu.add(item);
}


    public EditorPanel getEditorPanel() {
        return editorPanel;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            EditorFrame frame = new EditorFrame();
            frame.setVisible(true);
        });
    }
}
