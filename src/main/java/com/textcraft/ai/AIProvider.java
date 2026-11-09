package com.textcraft.ai;

import com.textcraft.exception.AIServiceException;

/** OOP: Interface (Strategy) - any text generator: Gemini, the mock, or something else later. */
public interface AIProvider {
    String generate(String prompt) throws AIServiceException;

    /** Model name for the history log. */
    default String modelName() {
        return "unknown";
    }
}
