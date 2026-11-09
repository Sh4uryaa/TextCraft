package com.textcraft.compiler;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.textcraft.exception.CompilerException;
import com.textcraft.model.CompileResult;
import com.textcraft.util.HttpUtil;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

/**
 * Judge0 CE over HTTP. Language ids are read once from /languages (they differ between servers).
 * Submits, then polls until the run finishes or 10 seconds pass.
 */
public class Judge0Runner implements CodeRunner {
    private static final long TIMEOUT_MS = 10_000;

    private final String baseUrl;
    private final String authToken; // optional, for servers that need X-Auth-Token
    private final Map<Language, Integer> ids = new EnumMap<>(Language.class);
    private volatile boolean cancelled;

    public Judge0Runner(String baseUrl, String authToken) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.authToken = authToken == null ? "" : authToken.trim();
    }

    @Override
    public void cancel() {
        cancelled = true;
    }

    @Override
    public CompileResult run(Language lang, String code, String stdin) throws CompilerException {
        cancelled = false;
        long start = System.currentTimeMillis();
        try {
            int id = languageId(lang);
            JsonObject body = new JsonObject();
            body.addProperty("source_code", code);
            body.addProperty("language_id", id);
            body.addProperty("stdin", stdin == null ? "" : stdin);
            body.addProperty("cpu_time_limit", 5);
            String sub = send("POST", "/submissions?base64_encoded=false&wait=false", body.toString());
            String token = JsonParser.parseString(sub).getAsJsonObject().get("token").getAsString();

            while (true) {
                if (cancelled) throw new CompilerException("Run stopped.");
                if (System.currentTimeMillis() - start > TIMEOUT_MS) throw new CompilerException("Run timed out after 10 seconds.");
                Thread.sleep(400);
                String res = send("GET", "/submissions/" + token + "?base64_encoded=false", null);
                JsonObject o = JsonParser.parseString(res).getAsJsonObject();
                int status = o.getAsJsonObject("status").get("id").getAsInt();
                if (status > 2) return toResult(o, status, System.currentTimeMillis() - start);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CompilerException("Run was interrupted.", e);
        } catch (IOException e) {
            if (HttpUtil.isOffline(e)) throw new CompilerException("You are offline. The online code runner needs internet.", e);
            throw new CompilerException("Could not reach the code runner: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new CompilerException("Unexpected answer from the code runner.", e);
        }
    }

    private static CompileResult toResult(JsonObject o, int statusId, long elapsedMs) {
        CompileResult r = new CompileResult();
        r.setStdout(text(o, "stdout"));
        r.setStderr(text(o, "stderr"));
        r.setCompileOutput(text(o, "compile_output"));
        r.setStatus(o.getAsJsonObject("status").get("description").getAsString());
        r.setSuccess(statusId == 3); // 3 = Accepted
        String time = text(o, "time");
        try {
            r.setTimeMs(time.isEmpty() ? elapsedMs : Math.round(Double.parseDouble(time) * 1000));
        } catch (NumberFormatException e) {
            r.setTimeMs(elapsedMs);
        }
        return r;
    }

    private static String text(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e == null || e.isJsonNull() ? "" : e.getAsString();
    }

    private int languageId(Language lang) throws IOException, InterruptedException, CompilerException {
        if (ids.isEmpty()) {
            JsonArray arr = JsonParser.parseString(send("GET", "/languages", null)).getAsJsonArray();
            for (Language l : Language.values()) {
                int best = -1;
                for (JsonElement e : arr) {
                    JsonObject o = e.getAsJsonObject();
                    if (o.get("name").getAsString().startsWith(l.judge0Prefix())) {
                        best = Math.max(best, o.get("id").getAsInt()); // highest id = newest version
                    }
                }
                if (best > 0) ids.put(l, best);
            }
        }
        Integer id = ids.get(lang);
        if (id == null) throw new CompilerException(lang.label() + " is not offered by this code runner server.");
        return id;
    }

    private String send(String method, String path, String json) throws IOException, InterruptedException, CompilerException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(15));
        if (!authToken.isEmpty()) b.header("X-Auth-Token", authToken);
        if (json != null) {
            b.header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(json));
        } else {
            b.method(method, HttpRequest.BodyPublishers.noBody());
        }
        HttpResponse<String> resp = HttpUtil.client().send(b.build(), HttpResponse.BodyHandlers.ofString());
        int s = resp.statusCode();
        if (s == 429) throw new CompilerException("The online code runner is busy (rate limit). Try again soon.");
        if (s == 401 || s == 403) throw new CompilerException("The code runner rejected the request (HTTP " + s + "). Check judge0.authToken.");
        if (s >= 400) throw new CompilerException("The code runner returned HTTP " + s + ".");
        return resp.body();
    }
}
