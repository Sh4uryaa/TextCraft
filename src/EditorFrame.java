
import javax.swing.*;
import java.awt.*;

public class EditorFrame extends JFrame {

    private final EditorPanel editorPanel;

    public EditorFrame() {
        setTitle("TextCraft - Java Text Editor");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        editorPanel = new EditorPanel();

        JLabel statusBar = new JLabel(" Ready");

        setJMenuBar(createMenuBar());
        add(editorPanel, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
    }

    private JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();

        String[] menus = {"File", "Edit", "View", "Help"};

        for (String name : menus) {
            bar.add(new JMenu(name));
        }

        return bar;
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
