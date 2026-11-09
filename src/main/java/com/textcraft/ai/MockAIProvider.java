package com.textcraft.ai;

import com.textcraft.exception.AIServiceException;

/**
 * Offline / test provider. Returns fixed, predictable answers. For study prompts it returns valid JSON
 * so StudyService can be tested without a network. Set ai.provider=mock in config to try the UI offline.
 */
public class MockAIProvider extends AbstractAIProvider {
    private String nextReply;
    private int calls;
    private String[] queue;

    public MockAIProvider() {
        super("mock");
    }

    /** Make the next call return exactly this text (used by tests). */
    public MockAIProvider reply(String... replies) {
        this.queue = replies;
        this.calls = 0;
        return this;
    }

    public int callCount() {
        return calls;
    }

    @Override
    protected String doGenerate(String prompt) throws AIServiceException {
        if (queue != null && queue.length > 0) {
            String r = queue[Math.min(calls, queue.length - 1)];
            calls++;
            return r;
        }
        calls++;
        if (prompt.contains("STRICT JSON") && prompt.contains("flashcards")) {
            return "[{\"question\":\"What is Java?\",\"answer\":\"A programming language.\"},"
                    + "{\"question\":\"What is a class?\",\"answer\":\"A blueprint for objects.\"},"
                    + "{\"question\":\"What is an interface?\",\"answer\":\"A contract of methods.\"},"
                    + "{\"question\":\"What is inheritance?\",\"answer\":\"A class reusing another class.\"},"
                    + "{\"question\":\"What is polymorphism?\",\"answer\":\"One interface, many forms.\"}]";
        }
        if (prompt.contains("STRICT JSON") && prompt.contains("quiz")) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 1; i <= 5; i++) {
                if (i > 1) sb.append(',');
                sb.append("{\"question\":\"Question ").append(i).append("?\",\"options\":[\"A1\",\"B1\",\"C1\",\"D1\"],")
                  .append("\"correct\":\"B\",\"explanation\":\"B is right.\"}");
            }
            return sb.append(']').toString();
        }
        int i = prompt.indexOf("\"\"\"\n");
        String text = i >= 0 ? prompt.substring(i + 4, Math.max(i + 4, prompt.lastIndexOf("\n\"\"\""))) : prompt;
        return "[mock] " + text;
    }
}
