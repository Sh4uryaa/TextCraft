package com.textcraft.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.textcraft.exception.AIServiceException;
import com.textcraft.util.HttpUtil;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

/** Gemini REST (generateContent). The API key goes in the x-goog-api-key header, never in the URL. */
public class GeminiProvider extends AbstractAIProvider {
    private static final String BASE = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final String apiKey;

    public GeminiProvider(String apiKey, String model) {
        super(model);
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    @Override
    protected String doGenerate(String prompt) throws AIServiceException {
        if (apiKey.isEmpty()) {
            throw new AIServiceException("No Gemini API key. Add gemini.apiKey to config/app.properties (see README).");
        }
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);
        JsonArray parts = new JsonArray();
        parts.add(part);
        JsonObject content = new JsonObject();
        content.add("parts", parts);
        JsonArray contents = new JsonArray();
        contents.add(content);
        JsonObject body = new JsonObject();
        body.add("contents", contents);

        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + modelName() + ":generateContent"))
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        try {
            HttpResponse<String> resp = HttpUtil.client().send(req, HttpResponse.BodyHandlers.ofString());
            return parse(resp.statusCode(), resp.body());
        } catch (HttpTimeoutException e) {
            throw new AIServiceException("The AI took too long to answer. Please try again.", e);
        } catch (IOException e) {
            if (HttpUtil.isOffline(e)) throw new AIServiceException("You are offline. AI needs an internet connection.", e);
            throw new AIServiceException("Could not reach the AI service: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AIServiceException("The AI request was cancelled.", e);
        }
    }

    /** Public + static so it can be unit tested without the network. */
    public static String parse(int status, String body) throws AIServiceException {
        if (status == 429) throw new AIServiceException("AI rate limit reached (HTTP 429). Wait a minute and try again.");
        if (status == 400 || status == 401 || status == 403) {
            String msg = errorMessage(body);
            if (msg.toLowerCase().contains("api key")) {
                throw new AIServiceException("The Gemini API key is not valid. Check gemini.apiKey in config/app.properties.");
            }
            throw new AIServiceException("The AI refused the request (HTTP " + status + "): " + msg);
        }
        if (status >= 500) throw new AIServiceException("The AI service has a problem (HTTP " + status + "). Try again later.");
        if (status != 200) throw new AIServiceException("Unexpected AI response (HTTP " + status + ").");
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonArray candidates = root.getAsJsonArray("candidates");
            if (candidates == null || candidates.size() == 0) {
                throw new AIServiceException("The AI gave no answer (the text may have been blocked by its safety filter).");
            }
            JsonArray parts = candidates.get(0).getAsJsonObject().getAsJsonObject("content").getAsJsonArray("parts");
            StringBuilder sb = new StringBuilder();
            for (JsonElement p : parts) {
                JsonObject o = p.getAsJsonObject();
                if (o.has("text")) sb.append(o.get("text").getAsString());
            }
            return sb.toString();
        } catch (RuntimeException e) {
            throw new AIServiceException("Could not understand the AI answer.", e);
        }
    }

    private static String errorMessage(String body) {
        try {
            return JsonParser.parseString(body).getAsJsonObject().getAsJsonObject("error").get("message").getAsString();
        } catch (RuntimeException e) {
            return "unknown error";
        }
    }
}
