package com.textcraft.dao;

import com.textcraft.db.ConnectionProvider;
import com.textcraft.exception.DatabaseException;
import com.textcraft.model.Flashcard;
import com.textcraft.model.FlashcardSet;
import com.textcraft.model.Quiz;
import com.textcraft.model.QuizQuestion;
import com.textcraft.model.StudySetInfo;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Flashcard sets, quizzes and quiz attempts. GenericDAO is implemented for flashcard sets;
 * quizzes have their own save/load methods.
 */
public class StudyDAO implements GenericDAO<FlashcardSet, Long> {
    private final ConnectionProvider db;

    public StudyDAO(ConnectionProvider db) {
        this.db = db;
    }

    // ---- flashcards -----------------------------------------------------------------------------

    /** Saves the set and all its cards in one transaction. */
    @Override
    public FlashcardSet save(FlashcardSet set) throws DatabaseException {
        try (Connection c = db.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO flashcard_sets (user_id, document_id, title) VALUES (?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, set.getUserId());
                    if (set.getDocumentId() == null) ps.setNull(2, java.sql.Types.BIGINT);
                    else ps.setLong(2, set.getDocumentId());
                    ps.setString(3, set.getTitle());
                    ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) {
                        if (k.next()) set.setId(k.getLong(1));
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO flashcards (set_id, question, answer) VALUES (?,?,?)")) {
                    for (Flashcard f : set.getCards()) {
                        ps.setLong(1, set.getId());
                        ps.setString(2, f.getQuestion());
                        ps.setString(3, f.getAnswer());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                c.commit();
                return set;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not save the flashcards: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<FlashcardSet> findById(Long id) throws DatabaseException {
        try (Connection c = db.getConnection()) {
            FlashcardSet set = null;
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM flashcard_sets WHERE id=?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        set = new FlashcardSet();
                        set.setId(rs.getLong("id"));
                        set.setUserId(rs.getLong("user_id"));
                        long doc = rs.getLong("document_id");
                        set.setDocumentId(rs.wasNull() ? null : doc);
                        set.setTitle(rs.getString("title"));
                        Timestamp t = rs.getTimestamp("created_at");
                        if (t != null) set.setCreatedAt(t.toLocalDateTime());
                    }
                }
            }
            if (set == null) return Optional.empty();
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM flashcards WHERE set_id=? ORDER BY id")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Flashcard f = new Flashcard(rs.getString("question"), rs.getString("answer"));
                        f.setId(rs.getLong("id"));
                        set.getCards().add(f);
                    }
                }
            }
            return Optional.of(set);
        } catch (SQLException e) {
            throw new DatabaseException("Could not read the flashcards: " + e.getMessage(), e);
        }
    }

    @Override
    public List<FlashcardSet> findAll() throws DatabaseException {
        List<FlashcardSet> out = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id FROM flashcard_sets ORDER BY id DESC");
             ResultSet rs = ps.executeQuery()) {
            List<Long> ids = new ArrayList<>();
            while (rs.next()) ids.add(rs.getLong(1));
            for (Long id : ids) findById(id).ifPresent(out::add);
            return out;
        } catch (SQLException e) {
            throw new DatabaseException("Could not list the flashcards: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(Long id) throws DatabaseException {
        exec("DELETE FROM flashcard_sets WHERE id=?", id);
    }

    // ---- quizzes --------------------------------------------------------------------------------

    public Quiz saveQuiz(Quiz quiz) throws DatabaseException {
        try (Connection c = db.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO quizzes (user_id, document_id, title) VALUES (?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, quiz.getUserId());
                    if (quiz.getDocumentId() == null) ps.setNull(2, java.sql.Types.BIGINT);
                    else ps.setLong(2, quiz.getDocumentId());
                    ps.setString(3, quiz.getTitle());
                    ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) {
                        if (k.next()) quiz.setId(k.getLong(1));
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO quiz_questions (quiz_id, question, option_a, option_b, option_c, option_d, "
                                + "correct_option, explanation) VALUES (?,?,?,?,?,?,?,?)")) {
                    for (QuizQuestion q : quiz.getQuestions()) {
                        ps.setLong(1, quiz.getId());
                        ps.setString(2, q.getQuestion());
                        for (int i = 0; i < 4; i++) ps.setString(3 + i, q.getOptions()[i]);
                        ps.setString(7, String.valueOf(q.getCorrect()));
                        ps.setString(8, q.getExplanation());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                c.commit();
                return quiz;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not save the quiz: " + e.getMessage(), e);
        }
    }

    public Optional<Quiz> findQuiz(long id) throws DatabaseException {
        try (Connection c = db.getConnection()) {
            Quiz quiz = null;
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM quizzes WHERE id=?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        quiz = new Quiz();
                        quiz.setId(rs.getLong("id"));
                        quiz.setUserId(rs.getLong("user_id"));
                        long doc = rs.getLong("document_id");
                        quiz.setDocumentId(rs.wasNull() ? null : doc);
                        quiz.setTitle(rs.getString("title"));
                        Timestamp t = rs.getTimestamp("created_at");
                        if (t != null) quiz.setCreatedAt(t.toLocalDateTime());
                    }
                }
            }
            if (quiz == null) return Optional.empty();
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM quiz_questions WHERE quiz_id=? ORDER BY id")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String[] opts = {rs.getString("option_a"), rs.getString("option_b"),
                                rs.getString("option_c"), rs.getString("option_d")};
                        QuizQuestion q = new QuizQuestion(rs.getString("question"), opts,
                                rs.getString("correct_option").charAt(0), rs.getString("explanation"));
                        q.setId(rs.getLong("id"));
                        quiz.getQuestions().add(q);
                    }
                }
            }
            return Optional.of(quiz);
        } catch (SQLException e) {
            throw new DatabaseException("Could not read the quiz: " + e.getMessage(), e);
        }
    }

    public void deleteQuiz(long id) throws DatabaseException {
        exec("DELETE FROM quizzes WHERE id=?", id);
    }

    public void saveAttempt(long quizId, long userId, int score, int total) throws DatabaseException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO quiz_attempts (quiz_id, user_id, score, total) VALUES (?,?,?,?)")) {
            ps.setLong(1, quizId);
            ps.setLong(2, userId);
            ps.setInt(3, score);
            ps.setInt(4, total);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not save the quiz attempt: " + e.getMessage(), e);
        }
    }

    // ---- "My study sets" list ----------------------------------------------------------------

    public List<StudySetInfo> listSets(long userId) throws DatabaseException {
        List<StudySetInfo> out = new ArrayList<>();
        String fc = "SELECT s.id, s.title, s.created_at, (SELECT COUNT(*) FROM flashcards f WHERE f.set_id=s.id) AS n "
                + "FROM flashcard_sets s WHERE s.user_id=?";
        String qz = "SELECT q.id, q.title, q.created_at, (SELECT COUNT(*) FROM quiz_questions x WHERE x.quiz_id=q.id) AS n "
                + "FROM quizzes q WHERE q.user_id=?";
        try (Connection c = db.getConnection()) {
            collect(c, fc, userId, StudySetInfo.Kind.FLASHCARDS, out);
            collect(c, qz, userId, StudySetInfo.Kind.QUIZ, out);
        } catch (SQLException e) {
            throw new DatabaseException("Could not list study sets: " + e.getMessage(), e);
        }
        out.sort(Comparator.comparing(StudySetInfo::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return out;
    }

    private void collect(Connection c, String sql, long userId, StudySetInfo.Kind kind, List<StudySetInfo> out)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp t = rs.getTimestamp("created_at");
                    out.add(new StudySetInfo(kind, rs.getLong("id"), rs.getString("title"),
                            t == null ? null : t.toLocalDateTime(), rs.getInt("n")));
                }
            }
        }
    }

    private void exec(String sql, long id) throws DatabaseException {
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Database error: " + e.getMessage(), e);
        }
    }
}
