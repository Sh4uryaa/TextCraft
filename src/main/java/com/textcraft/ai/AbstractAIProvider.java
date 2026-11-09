package com.textcraft.ai;

import com.textcraft.exception.AIServiceException;

/** OOP: Inheritance - shared checks; subclasses only implement doGenerate. */
public abstract class AbstractAIProvider implements AIProvider {
    private final String model;

    protected AbstractAIProvider(String model) {
        this.model = model;
    }

    @Override
    public String generate(String prompt) throws AIServiceException {
        if (prompt == null || prompt.trim().isEmpty()) throw new AIServiceException("There is no text to send to the AI.");
        String out = doGenerate(prompt);
        if (out == null || out.trim().isEmpty()) throw new AIServiceException("The AI returned an empty answer. Try again.");
        return out.trim();
    }

    protected abstract String doGenerate(String prompt) throws AIServiceException;

    @Override
    public String modelName() {
        return model;
    }
}
