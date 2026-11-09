package com.textcraft.study;

/** Prompts that demand STRICT JSON so the answer can be parsed with Gson. */
public final class StudyPromptBuilder {
    private StudyPromptBuilder() { }

    public static String flashcards(String text, int count) {
        return "Create " + count + " study flashcards from the text below. Reply with STRICT JSON only: no markdown, "
                + "no comments, no text before or after. Format: "
                + "[{\"question\":\"...\",\"answer\":\"...\"}]. Use the language of the text." + body(text);
    }

    public static String quiz(String text, int count) {
        return "Create a multiple choice quiz of " + count + " questions from the text below. Reply with STRICT JSON only: "
                + "no markdown, no comments, no text before or after. Format: "
                + "[{\"question\":\"...\",\"options\":[\"first\",\"second\",\"third\",\"fourth\"],"
                + "\"correct\":\"A\",\"explanation\":\"...\"}]. Exactly 4 options per question (without letters), "
                + "\"correct\" is one letter A, B, C or D. Use the language of the text." + body(text);
    }

    public static String keyPoints(String text) {
        return "List the 5 to 8 most important key points of the text below as short bullet lines starting with \"- \". "
                + "Return only the list." + body(text);
    }

    /** Added on the single retry. */
    public static String retrySuffix() {
        return "\n\nYour previous reply was not valid JSON in the required format. Reply again with ONLY the JSON array.";
    }

    private static String body(String text) {
        return "\n\nText:\n\"\"\"\n" + text + "\n\"\"\"";
    }
}
