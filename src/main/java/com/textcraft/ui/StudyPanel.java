package com.textcraft.ui;

import com.textcraft.app.AppContext;
import com.textcraft.auth.SessionManager;
import com.textcraft.model.Flashcard;
import com.textcraft.model.FlashcardSet;
import com.textcraft.model.Quiz;
import com.textcraft.model.QuizQuestion;
import com.textcraft.util.AppExecutors;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Study side panel: choose 5/10/15 items, generate flashcards / a quiz / key points from the
 * selection (or the whole document). Generated sets are saved to the cloud when the user is signed in.
 */
public class StudyPanel extends JPanel {
    private static final String EMPTY = "empty";
    private static final String CONTENT = "content";

    private final Supplier<TextEditor> editorSupplier;
    private final Supplier<Long> documentIdSupplier;
    private final Supplier<String> titleSupplier;
    private final JComboBox<Integer> count = new JComboBox<>(new Integer[]{5, 10, 15});
    private final JLabel status = new JLabel("Choose what to create.");
    private final CardLayout cards = new CardLayout();
    private final JPanel body = new JPanel(cards);
    private final JPanel contentHolder = new JPanel(new BorderLayout());
    private int requestId;

    public StudyPanel(Supplier<TextEditor> editorSupplier, Supplier<Long> documentIdSupplier, Supplier<String> titleSupplier) {
        super(new BorderLayout(0, 8));
        this.editorSupplier = editorSupplier;
        this.documentIdSupplier = documentIdSupplier;
        this.titleSupplier = titleSupplier;
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JButton fc = new JButton("Flashcards");
        JButton qz = new JButton("Quiz");
        JButton kp = new JButton("Key points");
        fc.addActionListener(e -> flashcards());
        qz.addActionListener(e -> quiz());
        kp.addActionListener(e -> keyPoints());
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        controls.setOpaque(false);
        controls.add(new JLabel("Items:"));
        controls.add(count);
        controls.add(fc);
        controls.add(qz);
        controls.add(kp);
        JPanel top = new JPanel(new BorderLayout(0, 4));
        top.setOpaque(false);
        top.add(controls, BorderLayout.NORTH);
        top.add(status, BorderLayout.SOUTH);

        JLabel hint = new JLabel("<html>Select a passage first, or leave nothing selected to use the whole document.</html>");
        JPanel empty = new JPanel(new BorderLayout());
        empty.add(hint, BorderLayout.NORTH);
        body.add(empty, EMPTY);
        body.add(contentHolder, CONTENT);
        cards.show(body, EMPTY);

        add(top, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
    }

    // ---- entry points used by the Study menu ---------------------------------------------------

    public void flashcards() {
        String text = sourceText();
        if (text == null) return;
        int n = (Integer) count.getSelectedItem();
        begin("Creating " + n + " flashcards...");
        final int id = requestId;
        AppExecutors.asyncChecked(() -> {
            List<Flashcard> list = AppContext.get().study().flashcards(text, n);
            saveFlashcards(list); // best effort, only when signed in
            return list;
        }, r -> {
            if (id != requestId) return;
            if (!r.isOk()) { fail(r.error()); return; }
            status.setText(r.get().size() + " flashcards ready." + savedNote());
            showContent(new FlashcardView(r.get()));
        });
    }

    public void quiz() {
        String text = sourceText();
        if (text == null) return;
        int n = (Integer) count.getSelectedItem();
        begin("Creating a " + n + " question quiz...");
        final int id = requestId;
        AppExecutors.asyncChecked(() -> {
            List<QuizQuestion> list = AppContext.get().study().quiz(text, n);
            return saveQuiz(list);
        }, r -> {
            if (id != requestId) return;
            if (!r.isOk()) { fail(r.error()); return; }
            Quiz quiz = r.get();
            status.setText(quiz.getQuestions().size() + " questions ready." + savedNote());
            showContent(new QuizView(quiz.getQuestions(), score -> recordAttempt(quiz, score)));
        });
    }

    public void keyPoints() {
        String text = sourceText();
        if (text == null) return;
        begin("Finding key points...");
        final int id = requestId;
        AppExecutors.asyncChecked(() -> AppContext.get().study().keyPoints(text), r -> {
            if (id != requestId) return;
            if (!r.isOk()) { fail(r.error()); return; }
            JTextArea area = new JTextArea(r.get());
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setEditable(false);
            area.setMargin(new java.awt.Insets(8, 8, 8, 8));
            status.setText("Key points ready.");
            showContent(new JScrollPane(area));
        });
    }

    /** Open a saved flashcard set / quiz from "My study sets". */
    public void showFlashcards(FlashcardSet set) {
        status.setText(set.getTitle());
        showContent(new FlashcardView(set.getCards()));
    }

    public void showQuiz(Quiz quiz) {
        status.setText(quiz.getTitle());
        showContent(new QuizView(quiz.getQuestions(), score -> recordAttempt(quiz, score)));
    }

    // ---- helpers ------------------------------------------------------------------------------

    private String sourceText() {
        TextEditor ed = editorSupplier.get();
        if (ed == null) {
            ErrorHandler.toast("Open or create a document first.");
            return null;
        }
        String t = ed.getSelectedText().isEmpty() ? ed.getAllText() : ed.getSelectedText();
        if (t.trim().length() < 30) {
            ErrorHandler.toast("Please write or select a longer text first.");
            return null;
        }
        return t;
    }

    private void begin(String message) {
        requestId++;
        status.setText(message);
        cards.show(body, EMPTY);
    }

    private void fail(String message) {
        status.setText("Could not create study material.");
        ErrorHandler.error(message);
    }

    private void showContent(JComponent c) {
        contentHolder.removeAll();
        contentHolder.add(c, BorderLayout.CENTER);
        contentHolder.revalidate();
        contentHolder.repaint();
        cards.show(body, CONTENT);
    }

    private static boolean cloudReady() {
        return SessionManager.getInstance().canUseCloud();
    }

    private String savedNote() {
        return cloudReady() ? " Saved to My study sets." : " (Sign in to save study sets.)";
    }

    private String setTitle(String kind) {
        String t = titleSupplier.get();
        return kind + ": " + (t == null || t.isEmpty() ? "Untitled" : t);
    }

    /** Runs on the worker thread. */
    private void saveFlashcards(List<Flashcard> cardsToSave) {
        if (!cloudReady()) return;
        try {
            FlashcardSet set = new FlashcardSet();
            set.setUserId(SessionManager.getInstance().getUser().getId());
            set.setDocumentId(documentIdSupplier.get());
            set.setTitle(setTitle("Flashcards"));
            set.setCards(cardsToSave);
            AppContext.get().studyDao().save(set);
        } catch (Exception e) {
            ErrorHandler.toast("Flashcards were created but could not be saved: " + e.getMessage());
        }
    }

    private Quiz saveQuiz(List<QuizQuestion> list) {
        Quiz quiz = new Quiz();
        quiz.setQuestions(list);
        quiz.setTitle(setTitle("Quiz"));
        if (cloudReady()) {
            try {
                quiz.setUserId(SessionManager.getInstance().getUser().getId());
                quiz.setDocumentId(documentIdSupplier.get());
                AppContext.get().studyDao().saveQuiz(quiz);
            } catch (Exception e) {
                quiz.setId(null);
                ErrorHandler.toast("The quiz was created but could not be saved: " + e.getMessage());
            }
        }
        return quiz;
    }

    private void recordAttempt(Quiz quiz, int score) {
        if (quiz.getId() == null || !cloudReady()) return;
        long userId = SessionManager.getInstance().getUser().getId();
        AppExecutors.asyncChecked(() -> {
            AppContext.get().studyDao().saveAttempt(quiz.getId(), userId, score, quiz.getQuestions().size());
            return Boolean.TRUE;
        }, r -> { /* attempts are a bonus; a failure must not interrupt studying */ });
    }
}
