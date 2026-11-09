package com.textcraft.study;

import static org.junit.jupiter.api.Assertions.*;

import com.textcraft.ai.MockAIProvider;
import com.textcraft.exception.AIServiceException;
import com.textcraft.model.Flashcard;
import com.textcraft.model.QuizQuestion;
import java.util.List;
import org.junit.jupiter.api.Test;

class StudyServiceTest {
    private static final String TEXT = "Java is a programming language. A class is a blueprint for objects.";

    @Test
    void flashcardsFromMockProvider() throws Exception {
        List<Flashcard> cards = new StudyService(new MockAIProvider()).flashcards(TEXT, 5);
        assertEquals(5, cards.size());
        assertFalse(cards.get(0).getQuestion().isEmpty());
    }

    @Test
    void quizFromMockProvider() throws Exception {
        List<QuizQuestion> q = new StudyService(new MockAIProvider()).quiz(TEXT, 5);
        assertEquals(5, q.size());
        assertEquals(4, q.get(0).getOptions().length);
        assertEquals(1, q.get(0).getCorrectIndex());
    }

    @Test
    void markdownFencesAroundJsonAreAccepted() throws Exception {
        List<Flashcard> c = StudyService.parseFlashcards("```json\n[{\"question\":\"Q\",\"answer\":\"A\"}]\n```");
        assertEquals(1, c.size());
    }

    @Test
    void invalidJsonIsRetriedExactlyOnce() throws Exception {
        MockAIProvider ai = new MockAIProvider().reply("sorry, here you go: not json",
                "[{\"question\":\"Q\",\"answer\":\"A\"}]");
        List<Flashcard> c = new StudyService(ai).flashcards(TEXT, 5);
        assertEquals(1, c.size());
        assertEquals(2, ai.callCount());
    }

    @Test
    void failsAfterTheSecondBadAnswer() {
        MockAIProvider ai = new MockAIProvider().reply("nope", "still nope");
        assertThrows(AIServiceException.class, () -> new StudyService(ai).quiz(TEXT, 5));
        assertEquals(2, ai.callCount());
    }

    @Test
    void validationRejectsBadQuestions() {
        assertThrows(AIServiceException.class, () -> StudyService.parseQuiz(
                "[{\"question\":\"Q\",\"options\":[\"a\",\"b\",\"c\"],\"correct\":\"A\",\"explanation\":\"\"}]")); // 3 options
        assertThrows(AIServiceException.class, () -> StudyService.parseQuiz(
                "[{\"question\":\"Q\",\"options\":[\"a\",\"b\",\"c\",\"d\"],\"correct\":\"E\",\"explanation\":\"\"}]")); // bad letter
        assertThrows(AIServiceException.class, () -> StudyService.parseFlashcards("[]"));
        assertThrows(AIServiceException.class, () -> StudyService.parseFlashcards("[{\"question\":\"\",\"answer\":\"A\"}]"));
    }

    @Test
    void lowerCaseCorrectLetterIsNormalised() throws Exception {
        List<QuizQuestion> q = StudyService.parseQuiz(
                "[{\"question\":\"Q\",\"options\":[\"a\",\"b\",\"c\",\"d\"],\"correct\":\"c\",\"explanation\":\"x\"}]");
        assertEquals('C', q.get(0).getCorrect());
    }
}
