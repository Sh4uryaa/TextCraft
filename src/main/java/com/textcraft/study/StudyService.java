package com.textcraft.study;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.textcraft.ai.AIProvider;
import com.textcraft.exception.AIServiceException;
import com.textcraft.model.Flashcard;
import com.textcraft.model.QuizQuestion;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates flashcards and quizzes through any AIProvider (Strategy).
 * The AI must answer with strict JSON; we parse and validate it and retry ONCE when it does not.
 */
public class StudyService {
    private static final int MAX_TEXT = 12_000; // keep prompts small for speed and cost

    private final AIProvider ai;

    public StudyService(AIProvider ai) {
        this.ai = ai;
    }

    public List<Flashcard> flashcards(String text, int count) throws AIServiceException {
        String prompt = StudyPromptBuilder.flashcards(clip(text), count);
        return askTwice(prompt, StudyService::parseFlashcards);
    }

    public List<QuizQuestion> quiz(String text, int count) throws AIServiceException {
        String prompt = StudyPromptBuilder.quiz(clip(text), count);
        return askTwice(prompt, StudyService::parseQuiz);
    }

    public String keyPoints(String text) throws AIServiceException {
        return ai.generate(StudyPromptBuilder.keyPoints(clip(text)));
    }

    private interface Parser<T> {
        T parse(String reply) throws AIServiceException;
    }

    private <T> T askTwice(String prompt, Parser<T> parser) throws AIServiceException {
        String reply = ai.generate(prompt);
        try {
            return parser.parse(reply);
        } catch (AIServiceException first) {
            String again = ai.generate(prompt + StudyPromptBuilder.retrySuffix()); // retry once
            try {
                return parser.parse(again);
            } catch (AIServiceException second) {
                throw new AIServiceException("The AI did not return usable study material. Please try again.", second);
            }
        }
    }

    private static String clip(String text) {
        return text.length() > MAX_TEXT ? text.substring(0, MAX_TEXT) : text;
    }

    // ---- parsing (public + static so tests can call them directly) --------------------------------

    /** Removes ```json fences or text around the array. */
    static String extractJsonArray(String reply) throws AIServiceException {
        int s = reply.indexOf('[');
        int e = reply.lastIndexOf(']');
        if (s < 0 || e <= s) throw new AIServiceException("No JSON array in the reply");
        return reply.substring(s, e + 1);
    }

    public static List<Flashcard> parseFlashcards(String reply) throws AIServiceException {
        try {
            JsonArray arr = JsonParser.parseString(extractJsonArray(reply)).getAsJsonArray();
            List<Flashcard> out = new ArrayList<>();
            for (JsonElement el : arr) {
                JsonObject o = el.getAsJsonObject();
                String q = o.get("question").getAsString().trim();
                String a = o.get("answer").getAsString().trim();
                if (q.isEmpty() || a.isEmpty()) throw new AIServiceException("Empty flashcard");
                out.add(new Flashcard(q, a));
            }
            if (out.isEmpty()) throw new AIServiceException("No flashcards");
            return out;
        } catch (RuntimeException e) { // bad JSON, missing field, wrong type
            throw new AIServiceException("Invalid flashcard JSON", e);
        }
    }

    public static List<QuizQuestion> parseQuiz(String reply) throws AIServiceException {
        try {
            JsonArray arr = JsonParser.parseString(extractJsonArray(reply)).getAsJsonArray();
            List<QuizQuestion> out = new ArrayList<>();
            for (JsonElement el : arr) {
                JsonObject o = el.getAsJsonObject();
                JsonArray opts = o.getAsJsonArray("options");
                if (opts.size() != 4) throw new AIServiceException("A question needs exactly 4 options");
                String[] options = new String[4];
                for (int i = 0; i < 4; i++) options[i] = opts.get(i).getAsString().trim();
                String letter = o.get("correct").getAsString().trim().toUpperCase();
                if (letter.length() != 1 || "ABCD".indexOf(letter.charAt(0)) < 0) {
                    throw new AIServiceException("Correct answer must be A, B, C or D");
                }
                String q = o.get("question").getAsString().trim();
                if (q.isEmpty()) throw new AIServiceException("Empty question");
                String expl = o.has("explanation") && !o.get("explanation").isJsonNull() ? o.get("explanation").getAsString() : "";
                out.add(new QuizQuestion(q, options, letter.charAt(0), expl));
            }
            if (out.isEmpty()) throw new AIServiceException("No questions");
            return out;
        } catch (RuntimeException e) {
            throw new AIServiceException("Invalid quiz JSON", e);
        }
    }
}
