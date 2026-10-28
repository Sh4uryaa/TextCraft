
import javax.swing.*;
import java.awt.*;

public class EditorPanel extends JPanel {

    private final JTextArea textArea;

    public EditorPanel() {
        setLayout(new BorderLayout());

        textArea = new JTextArea();
        textArea.setFont(new Font("Consolas", Font.PLAIN, 15));

        JScrollPane scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);
    }

    public JTextArea getTextArea() {
        return textArea;
    }
}
