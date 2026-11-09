package com.textcraft.ui;

import com.textcraft.model.Flashcard;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

/** Flip card, next/previous, "Got it" / "Review", shuffle and progress. */
public class FlashcardView extends JPanel {
    private final List<Flashcard> cards;
    private final Set<Integer> known = new HashSet<>();
    private final JTextArea face = new JTextArea();
    private final JLabel side = new JLabel("Question", SwingConstants.CENTER);
    private final JLabel counter = new JLabel("", SwingConstants.CENTER);
    private final JProgressBar progress = new JProgressBar();
    private int index;
    private boolean showingAnswer;

    public FlashcardView(List<Flashcard> source) {
        super(new BorderLayout(0, 8));
        this.cards = new ArrayList<>(source);
        setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        side.setFont(side.getFont().deriveFont(Font.BOLD, 12f));
        face.setLineWrap(true);
        face.setWrapStyleWord(true);
        face.setEditable(false);
        face.setFont(face.getFont().deriveFont(16f));
        face.setMargin(new java.awt.Insets(16, 16, 16, 16));
        face.setToolTipText("Click to flip");
        face.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { flip(); }
        });

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(counter, BorderLayout.NORTH);
        progress.setStringPainted(true);
        top.add(progress, BorderLayout.CENTER);
        top.add(side, BorderLayout.SOUTH);

        JButton flip = new JButton("Flip");
        JButton prev = new JButton("◀");
        JButton next = new JButton("▶");
        JButton got = new JButton("Got it");
        JButton review = new JButton("Review");
        JButton shuffle = new JButton("Shuffle");
        flip.addActionListener(e -> flip());
        prev.addActionListener(e -> go(-1));
        next.addActionListener(e -> go(1));
        got.addActionListener(e -> { known.add(index); go(1); });
        review.addActionListener(e -> { known.remove(index); go(1); });
        shuffle.addActionListener(e -> {
            Collections.shuffle(cards);
            known.clear();
            index = 0;
            refresh();
        });
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        nav.setOpaque(false);
        for (JButton b : new JButton[]{prev, flip, next}) nav.add(b);
        JPanel mark = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        mark.setOpaque(false);
        for (JButton b : new JButton[]{got, review, shuffle}) mark.add(b);
        JPanel bottom = new JPanel(new BorderLayout(0, 4));
        bottom.setOpaque(false);
        bottom.add(nav, BorderLayout.NORTH);
        bottom.add(mark, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(face), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        refresh();
    }

    private void flip() {
        showingAnswer = !showingAnswer;
        refresh();
    }

    private void go(int delta) {
        index = Math.floorMod(index + delta, cards.size());
        showingAnswer = false;
        refresh();
    }

    private void refresh() {
        Flashcard c = cards.get(index);
        face.setText(showingAnswer ? c.getAnswer() : c.getQuestion());
        face.setCaretPosition(0);
        side.setText(showingAnswer ? "Answer (click to flip)" : "Question (click to flip)");
        counter.setText("Card " + (index + 1) + " of " + cards.size() + (known.contains(index) ? "  ✓ known" : ""));
        progress.setMaximum(cards.size());
        progress.setValue(known.size());
        progress.setString(known.size() + " / " + cards.size() + " known");
    }
}
