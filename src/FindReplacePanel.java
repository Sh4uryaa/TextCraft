
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
    }
}
