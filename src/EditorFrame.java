
import javax.swing.*;
import java.awt.*;

public class EditorFrame extends JFrame {

    private final EditorPanel editorPanel;
    private final JLabel statusBar;
    public EditorFrame() {
        setTitle("TextCraft - Java Text Editor");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        editorPanel = new EditorPanel();
        statusBar = new JLabel(" Line 1, Column 1");    
        setJMenuBar(createMenuBar());
        add(editorPanel, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
        editorPanel.getTextArea().addCaretListener(e -> updateStatusBar());
    }
    
private void updateStatusBar() {
    JTextArea textArea = editorPanel.getTextArea();

    int caretPosition = textArea.getCaretPosition();

    try {
        int line = textArea.getLineOfOffset(caretPosition);
        int column = caretPosition
                - textArea.getLineStartOffset(line);

        statusBar.setText(
                " Line " + (line + 1)
                + ", Column " + (column + 1)
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
    addMenuItem(editMenu, "Find", "control F");
    addMenuItem(editMenu, "Replace", "control H");
    editMenu.addSeparator();
    addMenuItem(editMenu, "Select All", "control A");

    // View menu
    JMenu viewMenu = new JMenu("View");
    addMenuItem(viewMenu, "Light Theme", null);
    addMenuItem(viewMenu, "Dark Theme", null);

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
