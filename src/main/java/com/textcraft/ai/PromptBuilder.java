package com.textcraft.ai;

/** One prompt per AIAction/Tone. Every rewrite prompt demands "only the rewritten text". */
public final class PromptBuilder {
    private static final String RULES =
            " Return only the rewritten text, with no introduction, quotes or notes. Keep the meaning and the language of the original.";

    private PromptBuilder() { }

    /**
     * @param extra target language for TRANSLATE, the question for ASK; ignored otherwise
     * @param tone  only used for TONE
     */
    public static String build(AIAction action, Tone tone, String text, String extra) {
        String t = "\n\nText:\n\"\"\"\n" + text + "\n\"\"\"";
        switch (action) {
            case FIX_GRAMMAR:
                return "Fix the grammar, spelling and punctuation of the text below. Do not change the style." + RULES + t;
            case IMPROVE:
                return "Improve the clarity and flow of the text below." + RULES + t;
            case TONE:
                Tone tn = tone == null ? Tone.PROFESSIONAL : tone;
                return "Rewrite the text below in a " + tn.word() + " tone." + RULES + t;
            case SHORTER:
                return "Make the text below about half as long while keeping every key point." + RULES + t;
            case LONGER:
                return "Expand the text below with a little more detail, about twice as long." + RULES + t;
            case SUMMARIZE:
                return "Summarize the text below in a few short sentences. Return only the summary." + t;
            case EXPLAIN:
                return "Explain the text below in simple words for a student. Be brief." + t;
            case TRANSLATE:
                String lang = (extra == null || extra.trim().isEmpty()) ? "English" : extra.trim();
                return "Translate the text below into " + lang + ". Return only the translation." + t;
            case ASK:
                String q = (extra == null || extra.trim().isEmpty()) ? "Help me with this text." : extra.trim();
                return "Answer the request about the text below. Be concise.\nRequest: " + q + t;
            default:
                throw new IllegalArgumentException("Unknown action " + action);
        }
    }
}
