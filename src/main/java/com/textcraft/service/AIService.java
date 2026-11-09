package com.textcraft.service;

import com.textcraft.ai.AIAction;
import com.textcraft.ai.AIProvider;
import com.textcraft.ai.GeminiProvider;
import com.textcraft.ai.MockAIProvider;
import com.textcraft.ai.PromptBuilder;
import com.textcraft.ai.Tone;
import com.textcraft.app.AppConfig;
import com.textcraft.auth.SessionManager;
import com.textcraft.dao.AIHistoryDAO;
import com.textcraft.db.DatabaseConnection;
import com.textcraft.exception.AIServiceException;
import com.textcraft.exception.DatabaseException;
import com.textcraft.model.User;
import java.util.logging.Logger;

/** OOP: Facade - builds the prompt, calls the provider, logs to ai_history. Run it on a worker thread. */
public class AIService {
    private static final Logger LOG = Logger.getLogger(AIService.class.getName());

    private final AIProvider provider;

    public AIService(AIProvider provider) {
        this.provider = provider;
    }

    /** Provider chosen from config: ai.provider=mock gives the offline mock, otherwise Gemini. */
    public static AIProvider providerFromConfig() {
        AppConfig cfg = AppConfig.get();
        if ("mock".equalsIgnoreCase(cfg.get("ai.provider", "gemini"))) return new MockAIProvider();
        String model = SettingsService.getInstance().get().getGeminiModel();
        if (model == null || model.trim().isEmpty()) model = cfg.get("gemini.model", "gemini-2.0-flash");
        return new GeminiProvider(cfg.get("gemini.apiKey", ""), cfg.get("gemini.model", model));
    }

    public AIProvider provider() {
        return provider;
    }

    public String run(AIAction action, Tone tone, String text, String extra) throws AIServiceException {
        String prompt = PromptBuilder.build(action, tone, text, extra);
        String out = provider.generate(prompt);
        log(action, text, out);
        return out;
    }

    private void log(AIAction action, String in, String out) {
        User u = SessionManager.getInstance().getUser();
        if (u == null || !SessionManager.getInstance().isDatabaseAvailable()) return;
        try {
            new AIHistoryDAO(DatabaseConnection.getInstance())
                    .save(new AIHistoryDAO.Entry(u.getId(), action.name(), in, out, provider.modelName()));
        } catch (DatabaseException e) {
            LOG.warning("Could not log the AI action: " + e.getMessage()); // never fail the AI call because of logging
        }
    }
}
