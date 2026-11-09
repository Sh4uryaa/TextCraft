package com.textcraft.ai;

import static org.junit.jupiter.api.Assertions.*;

import com.textcraft.exception.AIServiceException;
import org.junit.jupiter.api.Test;

class GeminiProviderTest {
    @Test
    void parsesTheTextOfTheFirstCandidate() throws Exception {
        String body = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Hello \"},{\"text\":\"there\"}]}}]}";
        assertEquals("Hello there", GeminiProvider.parse(200, body));
    }

    @Test
    void rateLimitBadKeyAndServerErrorsGiveFriendlyMessages() {
        AIServiceException e429 = assertThrows(AIServiceException.class, () -> GeminiProvider.parse(429, "{}"));
        assertTrue(e429.getMessage().contains("429"));
        AIServiceException bad = assertThrows(AIServiceException.class,
                () -> GeminiProvider.parse(400, "{\"error\":{\"message\":\"API key not valid\"}}"));
        assertTrue(bad.getMessage().toLowerCase().contains("api key"));
        assertThrows(AIServiceException.class, () -> GeminiProvider.parse(503, "{}"));
    }

    @Test
    void blockedAnswerWithoutCandidatesIsReported() {
        assertThrows(AIServiceException.class, () -> GeminiProvider.parse(200, "{\"candidates\":[]}"));
    }

    @Test
    void missingKeyIsReportedBeforeAnyNetworkCall() {
        assertThrows(AIServiceException.class, () -> new GeminiProvider("", "m").generate("hi"));
    }

    @Test
    void mockProviderEchoesText() throws Exception {
        String out = new MockAIProvider().generate(PromptBuilder.build(AIAction.IMPROVE, null, "abc", null));
        assertTrue(out.contains("abc"));
    }
}
