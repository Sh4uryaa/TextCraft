package com.textcraft.ui;

import com.textcraft.app.AppContext;
import com.textcraft.model.Flashcard;
import com.textcraft.model.FlashcardSet;
import com.textcraft.model.Quiz;
import com.textcraft.model.QuizQuestion;
import com.textcraft.model.StudySetInfo;
import com.textcraft.util.AppExecutors;
import com.textcraft.util.TextUtils;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

/** "My study sets": open, delete or export to CSV the saved flashcard sets and quizzes. */
public class StudySetsDialog extends JDialog {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private final long userId;
    private final Consumer<FlashcardSet> openCards;
    private final Consumer<Quiz> openQuiz;
    private final DefaultListModel<StudySetInfo> model = new DefaultListModel<>();
    private final JList<StudySetInfo> list = new JList<>(model);
    private final JLabel status = new JLabel(" ");

    public StudySetsDialog(Window owner, long userId, Consumer<FlashcardSet> openCards, Consumer<Quiz> openQuiz) {
        super(owner, "My study sets", ModalityType.APPLICATION_MODAL);
        this.userId = userId;
        this.openCards = openCards;
        this.openQuiz = openQuiz;
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer((l, s, i, sel, f) -> {
            String kind = s.getKind() == StudySetInfo.Kind.QUIZ ? "quiz" : "flashcards";
            JLabel lab = new JLabel("  " + s.getTitle() + "  — " + kind + ", " + s.getItems() + " items, "
                    + (s.getCreatedAt() == null ? "" : s.getCreatedAt().format(FMT)));
            lab.setOpaque(true);
            lab.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 4, 6, 4));
            lab.setBackground(sel ? l.getSelectionBackground() : l.getBackground());
            lab.setForeground(sel ? l.getSelectionForeground() : l.getForeground());
            return lab;
        });

        JButton open = new JButton("Open");
        JButton export = new JButton("Export CSV");
        JButton delete = new JButton("Delete");
        JButton close = new JButton("Close");
        open.addActionListener(e -> open());
        export.addActionListener(e -> export());
        delete.addActionListener(e -> delete());
        close.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        buttons.add(delete);
        buttons.add(export);
        buttons.add(close);
        buttons.add(open);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(status, BorderLayout.WEST);
        bottom.add(buttons, BorderLayout.EAST);

        setLayout(new BorderLayout());
        add(new JScrollPane(list), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        setSize(600, 400);
        setLocationRelativeTo(owner);
        reload();
    }

    private void reload() {
        status.setText("  Loading...");
        AppExecutors.asyncChecked(() -> AppContext.get().studyDao().listSets(userId), r -> {
            model.clear();
            if (r.isOk()) {
                r.get().forEach(model::addElement);
                status.setText("  " + r.get().size() + " set(s)");
            } else {
                status.setText("  " + r.error());
            }
        });
    }

    private void open() {
        StudySetInfo s = list.getSelectedValue();
        if (s == null) return;
        AppExecutors.asyncChecked(() -> {
            if (s.getKind() == StudySetInfo.Kind.QUIZ) return AppContext.get().studyDao().findQuiz(s.getId()).orElse(null);
            return AppContext.get().studyDao().findById(s.getId()).orElse(null);
        }, r -> {
            if (!r.isOk() || r.get() == null) {
                ErrorHandler.error(r.isOk() ? "That set no longer exists." : r.error());
                return;
            }
            dispose();
            if (r.get() instanceof Quiz) openQuiz.accept((Quiz) r.get());
            else openCards.accept((FlashcardSet) r.get());
        });
    }

    private void delete() {
        StudySetInfo s = list.getSelectedValue();
        if (s == null) return;
        int ok = JOptionPane.showConfirmDialog(this, "Delete \"" + s.getTitle() + "\"?", "Delete",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        AppExecutors.asyncChecked(() -> {
            if (s.getKind() == StudySetInfo.Kind.QUIZ) AppContext.get().studyDao().deleteQuiz(s.getId());
            else AppContext.get().studyDao().delete(s.getId());
            return Boolean.TRUE;
        }, r -> {
            if (r.isOk()) reload();
            else ErrorHandler.error(r.error());
        });
    }

    private void export() {
        StudySetInfo s = list.getSelectedValue();
        if (s == null) return;
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(s.getTitle().replaceAll("[^A-Za-z0-9 _-]", "_") + ".csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File out = fc.getSelectedFile();
        AppExecutors.asyncChecked(() -> {
            StringBuilder sb = new StringBuilder();
            if (s.getKind() == StudySetInfo.Kind.QUIZ) {
                Quiz q = AppContext.get().studyDao().findQuiz(s.getId()).orElseThrow(() -> new IOException("Quiz not found"));
                sb.append("question,A,B,C,D,correct,explanation\n");
                for (QuizQuestion x : q.getQuestions()) {
                    sb.append(TextUtils.csv(x.getQuestion()));
                    for (String o : x.getOptions()) sb.append(',').append(TextUtils.csv(o));
                    sb.append(',').append(x.getCorrect()).append(',').append(TextUtils.csv(x.getExplanation())).append('\n');
                }
            } else {
                FlashcardSet fs = AppContext.get().studyDao().findById(s.getId()).orElseThrow(() -> new IOException("Set not found"));
                sb.append("question,answer\n");
                for (Flashcard f : fs.getCards()) {
                    sb.append(TextUtils.csv(f.getQuestion())).append(',').append(TextUtils.csv(f.getAnswer())).append('\n');
                }
            }
            Files.write(out.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
            return out;
        }, r -> {
            if (r.isOk()) ErrorHandler.toast("Exported to " + r.get().getName());
            else ErrorHandler.error(r.error());
        });
    }
}
