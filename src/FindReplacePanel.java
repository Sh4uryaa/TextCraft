
import javax.swing.*;
import java.awt.*;

public class FindReplacePanel extends JPanel {

    public FindReplacePanel() {
        setLayout(new FlowLayout(FlowLayout.LEFT));

        add(new JLabel("Find:"));
        add(new JTextField(12));

        add(new JLabel("Replace:"));
        add(new JTextField(12));

        add(new JButton("Find Next"));
        add(new JButton("Replace"));
        add(new JButton("Replace All"));

        add(new JCheckBox("Match case"));
        JButton closeButton = new JButton("X");
        add(closeButton);

    closeButton.addActionListener(e -> setVisible(false));
    }
}
