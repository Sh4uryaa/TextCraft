package com.textcraft.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Bottom bar: words, chars, autosave time, voice minutes, mode. Values are updated by later steps. */
public class StatusBar extends JPanel implements ThemeChangeListener {
    private final JLabel words = new JLabel("Words: 0");
    private final JLabel chars = new JLabel("Chars: 0");
    private final JLabel autosave = new JLabel("Autosave: --");
    private final JLabel voice = new JLabel("Voice minutes 0/60");
    private final JLabel mode = new JLabel("Mode: Rich text");

    public StatusBar() {
        super(new BorderLayout());
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        left.setOpaque(false);
        left.add(words);
        left.add(chars);
        left.add(autosave);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 4));
        right.setOpaque(false);
        right.add(voice);
        right.add(mode);
        add(left, BorderLayout.WEST);
        add(right, BorderLayout.EAST);
        ThemeManager.getInstance().addListener(this);
        onThemeChanged(ThemeManager.getInstance().getResolved(), ThemeManager.getInstance().palette());
    }

    public void setCounts(int wordCount, int charCount) {
        words.setText(wordCount < 0 ? "Words: --" : "Words: " + wordCount); // -1 = skipped (Lite mode, huge file)
        chars.setText("Chars: " + charCount);
    }

    public void setAutosave(String text) {
        autosave.setText("Autosave: " + text);
    }

    public void setVoiceMinutes(double used, int cap) {
        voice.setText(String.format("Voice minutes %.1f/%d", used, cap));
    }

    public void setMode(String text) {
        mode.setText("Mode: " + text);
    }

    @Override
    public void onThemeChanged(Theme t, Palette p) {
        setBackground(p.surface);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, p.border));
        for (JLabel l : new JLabel[]{words, chars, autosave, voice, mode}) {
            l.setForeground(p.text2);
        }
    }
}
