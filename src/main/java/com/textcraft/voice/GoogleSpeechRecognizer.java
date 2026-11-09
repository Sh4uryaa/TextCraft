package com.textcraft.voice;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.textcraft.exception.VoiceException;
import com.textcraft.util.HttpUtil;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

/** Google Speech-to-Text V1 REST (speech:recognize). No gRPC, so it stays light on old PCs. */
public class GoogleSpeechRecognizer implements SpeechRecognizer {
    private static final String URL = "https://speech.googleapis.com/v1/speech:recognize";

    private final String apiKey;

    public GoogleSpeechRecognizer(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    @Override
    public String recognize(byte[] pcm, String langCode) throws VoiceException {
        if (apiKey.isEmpty()) throw new VoiceException("No Speech API key. Add speech.apiKey to config/app.properties (see README).");
        JsonObject config = new JsonObject();
        config.addProperty("encoding", "LINEAR16");
        config.addProperty("sampleRateHertz", 16000);
        config.addProperty("languageCode", langCode);
        config.addProperty("enableAutomaticPunctuation", true);
        JsonObject audio = new JsonObject();
        audio.addProperty("content", Base64.getEncoder().encodeToString(pcm));
        JsonObject body = new JsonObject();
        body.add("config", config);
        body.add("audio", audio);

        HttpRequest req = HttpRequest.newBuilder(URI.create(URL))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("X-Goog-Api-Key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        try {
            HttpResponse<String> resp = HttpUtil.client().send(req, HttpResponse.BodyHandlers.ofString());
            return parse(resp.statusCode(), resp.body());
        } catch (IOException e) {
            if (HttpUtil.isOffline(e)) throw new VoiceException("You are offline. Voice typing needs internet.", e);
            throw new VoiceException("Could not reach the speech service: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VoiceException("Voice typing was cancelled.", e);
        }
    }

    /** Static so it can be tested without the network. Returns "" when nothing was recognised. */
    public static String parse(int status, String body) throws VoiceException {
        if (status == 429) throw new VoiceException("The speech service quota is used up (HTTP 429).");
        if (status == 400 || status == 401 || status == 403) {
            throw new VoiceException("The speech service refused the request (HTTP " + status
                    + "). Check speech.apiKey and that the Speech-to-Text API is enabled.");
        }
        if (status != 200) throw new VoiceException("Unexpected speech service answer (HTTP " + status + ").");
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonArray results = root.getAsJsonArray("results");
            if (results == null) return "";
            StringBuilder sb = new StringBuilder();
            for (JsonElement r : results) {
                JsonArray alts = r.getAsJsonObject().getAsJsonArray("alternatives");
                if (alts != null && alts.size() > 0) {
                    if (sb.length() > 0) sb.append(' ');
                    sb.append(alts.get(0).getAsJsonObject().get("transcript").getAsString().trim());
                }
            }
            return sb.toString();
        } catch (RuntimeException e) {
            throw new VoiceException("Could not understand the speech service answer.", e);
        }
    }
}
