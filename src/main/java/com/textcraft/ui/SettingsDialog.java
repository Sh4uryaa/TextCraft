package com.textcraft.ui;

import com.textcraft.model.UserSettings;
import com.textcraft.service.SettingsService;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

/** Theme, Lite mode, default font, Gemini model, voice language and reading speed. */
public class SettingsDialog extends JDialog {
    private final JComboBox<Theme> theme = new JComboBox<>(Theme.values());
    private final JCheckBox lite = new JCheckBox("Lite mode (for old PCs: no animations, smaller undo history)");
    private final JComboBox<String> font = new JComboBox<>(new String[]{"Arial", "Times New Roman", "Courier New", "Verdana", "Georgia"});
    private final JSpinner size = new JSpinner(new SpinnerNumberModel(12, 8, 48, 1));
    private final JTextField model = new JTextField(18);
    private final JComboBox<String> voiceLang = new JComboBox<>(new String[]{"en-US", "en-IN", "hi-IN"});
    private final JSpinner speed = new JSpinner(new SpinnerNumberModel(1.0, 0.5, 2.0, 0.1));
    private boolean saved;

    public SettingsDialog(Window owner) {
        super(owner, "Settings", ModalityType.APPLICATION_MODAL);
        UserSettings s = SettingsService.getInstance().get();
        theme.setSelectedItem(ThemeManager.getInstance().getChoice());
        lite.setSelected(s.isLiteMode());
        font.setSelectedItem(s.getDefaultFont());
        size.setValue(s.getDefaultFontSize());
        model.setText(s.getGeminiModel() == null ? "" : s.getGeminiModel());
        voiceLang.setSelectedItem(s.getVoiceLanguage());
        speed.setValue(s.getVoiceSpeed());

        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 16, 8, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        int y = 0;
        y = row(p, c, y, "Theme", theme);
        c.gridx = 0;
        c.gridy = y++;
        c.gridwidth = 2;
        p.add(lite, c);
        c.gridwidth = 1;
        y = row(p, c, y, "Default font", font);
        y = row(p, c, y, "Default font size", size);
        y = row(p, c, y, "Gemini model", model);
        y = row(p, c, y, "Voice language", voiceLang);
        y = row(p, c, y, "Read aloud speed", speed);

        JButton ok = new JButton("Save");
        JButton cancel = new JButton("Cancel");
        ok.addActionListener(e -> {
            saved = true;
            dispose();
        });
        cancel.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        buttons.add(cancel);
        buttons.add(ok);
        c.gridx = 0;
        c.gridy = y;
        c.gridwidth = 2;
        p.add(buttons, c);
        setContentPane(p);
        getRootPane().setDefaultButton(ok);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private static int row(JPanel p, GridBagConstraints c, int y, String label, java.awt.Component field) {
        c.gridx = 0;
        c.gridy = y;
        p.add(new JLabel(label), c);
        c.gridx = 1;
        p.add(field, c);
        return y + 1;
    }

    /** Shows the dialog; true when the user pressed Save (values are then written to SettingsService). */
    public boolean showAndApply() {
        setVisible(true);
        if (!saved) return false;
        UserSettings s = SettingsService.getInstance().get();
        s.setTheme(((Theme) theme.getSelectedItem()).name());
        s.setLiteMode(lite.isSelected());
        s.setDefaultFont((String) font.getSelectedItem());
        s.setDefaultFontSize((Integer) size.getValue());
        s.setGeminiModel(model.getText().trim());
        s.setVoiceLanguage((String) voiceLang.getSelectedItem());
        s.setVoiceSpeed(((Number) speed.getValue()).doubleValue());
        SettingsService.getInstance().save();
        return true;
    }
}
