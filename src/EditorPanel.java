
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;

public class EditorPanel extends JPanel {

    private final JTextArea textArea;
    private final JTextArea lineNumbers;

    public EditorPanel() {
        setLayout(new BorderLayout());

        textArea = new JTextArea();
        textArea.setFont(new Font("Consolas", Font.PLAIN, 15));
        textArea.setTabSize(4);

        lineNumbers = new JTextArea("1");
        lineNumbers.setFont(new Font("Consolas", Font.PLAIN, 15));
        lineNumbers.setBackground(new Color(240, 240, 240));
        lineNumbers.setForeground(Color.GRAY);
        lineNumbers.setEditable(false);
        lineNumbers.setFocusable(false);
        lineNumbers.setMargin(new Insets(0, 5, 0, 5));
        lineNumbers.setBorder(null);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setRowHeaderView(lineNumbers);

        textArea.getDocument().addDocumentListener(
            new DocumentListener() {
                public void insertUpdate(DocumentEvent e) {
                    updateLineNumbers();
                }

                public void removeUpdate(DocumentEvent e) {
                    updateLineNumbers();
                }

                public void changedUpdate(DocumentEvent e) {
                    updateLineNumbers();
                }
            }
        );

        add(scrollPane, BorderLayout.CENTER);
    }

    private void updateLineNumbers() {
        int lineCount = textArea.getLineCount();
        StringBuilder numbers = new StringBuilder();

        for (int i = 1; i <= lineCount; i++) {
            numbers.append(i).append("\n");
        }

        lineNumbers.setText(numbers.toString());
    }

    public JTextArea getTextArea() {
        return textArea;
    }
}
