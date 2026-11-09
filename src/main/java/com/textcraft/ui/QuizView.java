package com.textcraft.ui;

import com.textcraft.model.QuizQuestion;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/** One question at a time, instant feedback, final score, and a review of the wrong answers. */
public class QuizView extends JPanel {
    private final List<QuizQuestion> questions;
    private final IntConsumer onFinished; // receives the score
    private final List<QuizQuestion> wrong = new ArrayList<>();
    private final List<Integer> wrongPicks = new ArrayList<>();
    private int index;
    private int score;

    public QuizView(List<QuizQuestion> questions, IntConsumer onFinished) {
        super(new BorderLayout());
        this.questions = questions;
        this.onFinished = onFinished;
        setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        showQuestion();
    }

    private void showQuestion() {
        removeAll();
        QuizQuestion q = questions.get(index);
        JLabel head = new JLabel("Question " + (index + 1) + " of " + questions.size() + "    Score: " + score);
        head.setFont(head.getFont().deriveFont(Font.BOLD, 12f));

        JTextArea text = wrapped(q.getQuestion(), 15f);
        JPanel options = new JPanel();
        options.setOpaque(false);
        options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
        JTextArea feedback = wrapped("", 13f);
        JButton next = new JButton(index == questions.size() - 1 ? "Finish" : "Next");
        next.setEnabled(false);
        next.addActionListener(e -> {
            if (index == questions.size() - 1) showResult();
            else { index++; showQuestion(); }
        });

        List<JButton> buttons = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            final int pick = i;
            JButton b = new JButton((char) ('A' + i) + ".  " + q.getOptions()[i]);
            b.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
            b.setAlignmentX(0f);
            b.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 36));
            b.addActionListener(e -> {
                boolean right = pick == q.getCorrectIndex();
                if (right) score++;
                else { wrong.add(q); wrongPicks.add(pick); }
                for (JButton x : buttons) x.setEnabled(false);
                Palette p = ThemeManager.getInstance().palette();
                buttons.get(q.getCorrectIndex()).setForeground(p.ok);
                if (!right) b.setForeground(p.error);
                feedback.setText((right ? "Correct! " : "Not quite. The answer is " + q.getCorrect() + ". ") + q.getExplanation());
                head.setText("Question " + (index + 1) + " of " + questions.size() + "    Score: " + score);
                next.setEnabled(true);
            });
            buttons.add(b);
            options.add(b);
            options.add(Box.createVerticalStrut(6));
        }

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(text, BorderLayout.NORTH);
        center.add(options, BorderLayout.CENTER);
        center.add(feedback, BorderLayout.SOUTH);
        add(head, BorderLayout.NORTH);
        add(new JScrollPane(center), BorderLayout.CENTER);
        add(next, BorderLayout.SOUTH);
        revalidate();
        repaint();
    }

    private void showResult() {
        onFinished.accept(score);
        removeAll();
        StringBuilder sb = new StringBuilder();
        sb.append("Your score: ").append(score).append(" / ").append(questions.size()).append("\n\n");
        if (wrong.isEmpty()) {
            sb.append("Perfect! Nothing to review.");
        } else {
            sb.append("Review of wrong answers:\n\n");
            for (int i = 0; i < wrong.size(); i++) {
                QuizQuestion q = wrong.get(i);
                sb.append(i + 1).append(". ").append(q.getQuestion()).append('\n')
                  .append("   You chose: ").append((char) ('A' + wrongPicks.get(i))).append(". ")
                  .append(q.getOptions()[wrongPicks.get(i)]).append('\n')
                  .append("   Correct:   ").append(q.getCorrect()).append(". ").append(q.getOptions()[q.getCorrectIndex()]).append('\n');
                if (!q.getExplanation().isEmpty()) sb.append("   ").append(q.getExplanation()).append('\n');
                sb.append('\n');
            }
        }
        JTextArea area = wrapped(sb.toString(), 13f);
        JButton again = new JButton("Retake quiz");
        again.addActionListener(e -> {
            index = 0;
            score = 0;
            wrong.clear();
            wrongPicks.clear();
            showQuestion();
        });
        add(new JScrollPane(area), BorderLayout.CENTER);
        add(again, BorderLayout.SOUTH);
        revalidate();
        repaint();
    }

    private static JTextArea wrapped(String s, float size) {
        JTextArea a = new JTextArea(s);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setEditable(false);
        a.setOpaque(false);
        a.setFont(a.getFont().deriveFont(size));
        return a;
    }
}
