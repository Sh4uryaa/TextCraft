package com.textcraft.ai;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PromptBuilderTest {
    @Test
    void everyActionBuildsAPromptContainingTheText() {
        for (AIAction a : AIAction.values()) {
            String p = PromptBuilder.build(a, Tone.FORMAL, "MY TEXT", "French");
            assertTrue(p.contains("MY TEXT"), a.name());
        }
    }

    @Test
    void rewritePromptsDemandOnlyTheRewrittenTextInTheSameLanguage() {
        String p = PromptBuilder.build(AIAction.FIX_GRAMMAR, null, "t", null);
        assertTrue(p.contains("Return only the rewritten text"));
        assertTrue(p.contains("Keep the meaning and the language"));
    }

    @Test
    void toneAndTranslationUseTheirParameter() {
        assertTrue(PromptBuilder.build(AIAction.TONE, Tone.CASUAL, "t", null).contains("casual"));
        assertTrue(PromptBuilder.build(AIAction.TRANSLATE, null, "t", "Hindi").contains("Hindi"));
        assertTrue(PromptBuilder.build(AIAction.ASK, null, "t", "Why?").contains("Why?"));
    }

    @Test
    void onlyRewritingActionsCanReplaceText() {
        assertTrue(AIAction.IMPROVE.rewrites());
        assertFalse(AIAction.EXPLAIN.rewrites());
        assertFalse(AIAction.SUMMARIZE.rewrites());
    }
}
