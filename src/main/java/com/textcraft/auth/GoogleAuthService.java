package com.textcraft.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import com.textcraft.app.AppConfig;
import com.textcraft.util.AppPaths;
import com.textcraft.util.HttpUtil;
import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

/**
 * Google sign-in with the desktop "loopback" flow: open the browser, receive the code on
 * http://127.0.0.1:PORT/, exchange it for tokens, read the profile.
 * Scopes: openid email profile. The refresh token is stored in ~/.textcraft for auto-login.
 * Simple on purpose: JDK HttpServer + HttpClient + Gson, no extra OAuth libraries (PKCE is used).
 */
public class GoogleAuthService {
    private static final Logger LOG = Logger.getLogger(GoogleAuthService.class.getName());
    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL = "https://openidconnect.googleapis.com/v1/userinfo";
    private static final String TOKEN_FILE = "google_refresh_token";

    /** What Google tells us about the user. */
    public static class Profile {
        public final String sub;
        public final String email;
        public final String name;
        public final String picture;

        public Profile(String sub, String email, String name, String picture) {
            this.sub = sub;
            this.email = email;
            this.name = name;
            this.picture = picture;
        }
    }

    public boolean isConfigured() {
        return !clientId().isEmpty();
    }

    private String clientId() {
        return AppConfig.get().get("google.clientId", "").trim();
    }

    private String clientSecret() {
        return AppConfig.get().get("google.clientSecret", "").trim();
    }

    /** Interactive sign-in. Blocks (up to 3 minutes) - call it from a worker thread. */
    public Profile signIn() throws AuthException {
        if (!isConfigured()) {
            throw new AuthException("Google sign-in is not set up. Add google.clientId and google.clientSecret to config/app.properties (see README).");
        }
        String verifier = randomString(48);
        String state = randomString(16);
        CompletableFuture<Map<String, String>> result = new CompletableFuture<>();
        HttpServer server = null;
        try {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            server.createContext("/", exchange -> {
                Map<String, String> q = parseQuery(exchange.getRequestURI().getRawQuery());
                String body = "<html><body style='font-family:sans-serif;text-align:center;margin-top:80px'>"
                        + "<h2>TextCraft</h2><p>Sign-in finished. You can close this window and go back to the app.</p></body></html>";
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
                if (q.containsKey("code") || q.containsKey("error")) result.complete(q);
            });
            server.start();
            String redirect = "http://127.0.0.1:" + server.getAddress().getPort() + "/";

            String url = AUTH_URL + "?response_type=code"
                    + "&client_id=" + enc(clientId())
                    + "&redirect_uri=" + enc(redirect)
                    + "&scope=" + enc("openid email profile")
                    + "&state=" + enc(state)
                    + "&code_challenge=" + enc(challenge(verifier))
                    + "&code_challenge_method=S256"
                    + "&access_type=offline&prompt=consent";
            openBrowser(url);

            Map<String, String> q = result.get(3, TimeUnit.MINUTES);
            if (q.containsKey("error")) throw new AuthException("Google sign-in was cancelled (" + q.get("error") + ").");
            if (!state.equals(q.get("state"))) throw new AuthException("Sign-in response did not match the request. Try again.");

            JsonObject tokens = postForm(TOKEN_URL, form(
                    "code", q.get("code"), "client_id", clientId(), "client_secret", clientSecret(),
                    "redirect_uri", redirect, "grant_type", "authorization_code", "code_verifier", verifier));
            if (tokens.has("refresh_token")) saveRefreshToken(tokens.get("refresh_token").getAsString());
            return fetchProfile(tokens.get("access_token").getAsString());
        } catch (TimeoutException e) {
            throw new AuthException("Sign-in timed out. Please try again.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AuthException("Sign-in was interrupted.", e);
        } catch (java.util.concurrent.ExecutionException | IOException e) {
            throw new AuthException("Google sign-in failed: " + rootMessage(e), e);
        } finally {
            if (server != null) server.stop(0);
        }
    }

    /** Auto-login with the stored refresh token. Empty when there is none or it no longer works. */
    public Optional<Profile> silentSignIn() {
        Optional<String> refresh = readRefreshToken();
        if (!refresh.isPresent() || !isConfigured()) return Optional.empty();
        try {
            JsonObject tokens = postForm(TOKEN_URL, form("client_id", clientId(), "client_secret", clientSecret(),
                    "refresh_token", refresh.get(), "grant_type", "refresh_token"));
            return Optional.of(fetchProfile(tokens.get("access_token").getAsString()));
        } catch (AuthException | IOException e) {
            LOG.info("Silent Google sign-in not possible: " + e.getMessage());
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    public void signOut() {
        try {
            Files.deleteIfExists(AppPaths.file(TOKEN_FILE));
        } catch (IOException e) {
            LOG.warning("Could not delete the stored token: " + e.getMessage());
        }
    }

    // ---- helpers -----------------------------------------------------------------------------

    private Profile fetchProfile(String accessToken) throws AuthException, IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(USERINFO_URL))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + accessToken)
                .GET().build();
        HttpResponse<String> resp = HttpUtil.client().send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) throw new AuthException("Could not read your Google profile (HTTP " + resp.statusCode() + ").");
        JsonObject o = JsonParser.parseString(resp.body()).getAsJsonObject();
        String email = str(o, "email");
        if (email.isEmpty()) throw new AuthException("Google did not return an email address.");
        return new Profile(str(o, "sub"), email, str(o, "name"), str(o, "picture"));
    }

    private JsonObject postForm(String url, String body) throws AuthException, IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> resp = HttpUtil.client().send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new AuthException("Google refused the sign-in (HTTP " + resp.statusCode() + "). Check the client id/secret and test users.");
        }
        return JsonParser.parseString(resp.body()).getAsJsonObject();
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }

    private static String form(String... kv) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < kv.length; i += 2) {
            if (sb.length() > 0) sb.append('&');
            sb.append(enc(kv[i])).append('=').append(enc(kv[i + 1]));
        }
        return sb.toString();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> m = new HashMap<>();
        if (raw == null) return m;
        for (String pair : raw.split("&")) {
            int i = pair.indexOf('=');
            if (i > 0) m.put(URLDecoder.decode(pair.substring(0, i), StandardCharsets.UTF_8),
                    URLDecoder.decode(pair.substring(i + 1), StandardCharsets.UTF_8));
        }
        return m;
    }

    private static String randomString(int bytes) {
        byte[] b = new byte[bytes];
        new SecureRandom().nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static String challenge(String verifier) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void openBrowser(String url) throws AuthException {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                return;
            }
        } catch (IOException | UnsupportedOperationException e) {
            LOG.warning("Cannot open browser: " + e.getMessage());
        }
        throw new AuthException("Could not open the browser. Open this link yourself:\n" + url);
    }

    private void saveRefreshToken(String token) {
        try {
            Path f = AppPaths.file(TOKEN_FILE);
            Files.write(f, token.getBytes(StandardCharsets.UTF_8));
            try {
                Files.setPosixFilePermissions(f, PosixFilePermissions.fromString("rw-------"));
            } catch (UnsupportedOperationException ignored) {
                // Windows: file permissions follow the user profile folder
            }
        } catch (IOException e) {
            LOG.warning("Could not store the Google token: " + e.getMessage());
        }
    }

    private Optional<String> readRefreshToken() {
        try {
            Path f = AppPaths.file(TOKEN_FILE);
            if (!Files.exists(f)) return Optional.empty();
            String t = new String(Files.readAllBytes(f), StandardCharsets.UTF_8).trim();
            return t.isEmpty() ? Optional.empty() : Optional.of(t);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static String rootMessage(Throwable t) {
        while (t.getCause() != null) t = t.getCause();
        return t.getMessage() == null ? t.toString() : t.getMessage();
    }
}
