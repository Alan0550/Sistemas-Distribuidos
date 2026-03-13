package edu.upb.tmservice;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class AuthSessionStore {
    private static final AuthSessionStore INSTANCE = new AuthSessionStore();
    private static final long SESSION_TTL_MS = 12L * 60L * 60L * 1000L;
    private final String signingSecret;
    private final Base64.Encoder urlEncoder = Base64.getUrlEncoder().withoutPadding();
    private final Base64.Decoder urlDecoder = Base64.getUrlDecoder();

    private AuthSessionStore() {
        this.signingSecret = System.getenv().getOrDefault("TM_SESSION_SECRET", "tm-session-dev-secret");
    }

    public static AuthSessionStore getInstance() {
        return INSTANCE;
    }

    public String createSession(long userId, String username, String rol) {
        long expiresAt = System.currentTimeMillis() + SESSION_TTL_MS;
        JsonObject payload = new JsonObject();
        payload.addProperty("uid", userId);
        payload.addProperty("usr", username);
        payload.addProperty("rol", rol);
        payload.addProperty("exp", expiresAt);

        String payload64 = urlEncoder.encodeToString(payload.toString().getBytes(StandardCharsets.UTF_8));
        String signature64 = signToBase64(payload64);
        return payload64 + "." + signature64;
    }

    public SessionData getSession(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        try {
            String[] parts = token.split("\\.", 2);
            if (parts.length != 2) {
                return null;
            }

            String payload64 = parts[0];
            String signature64 = parts[1];
            String expectedSignature64 = signToBase64(payload64);
            if (!safeEquals(signature64, expectedSignature64)) {
                return null;
            }

            String json = new String(urlDecoder.decode(payload64), StandardCharsets.UTF_8);
            JsonObject payload = JsonParser.parseString(json).getAsJsonObject();
            long exp = payload.get("exp").getAsLong();
            if (System.currentTimeMillis() > exp) {
                return null;
            }

            long userId = payload.get("uid").getAsLong();
            String username = payload.get("usr").getAsString();
            String rol = payload.get("rol").getAsString();
            return new SessionData(userId, username, rol, exp);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String signToBase64(String payload64) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(key);
            byte[] sig = mac.doFinal(payload64.getBytes(StandardCharsets.UTF_8));
            return urlEncoder.encodeToString(sig);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar sesion", e);
        }
    }

    private boolean safeEquals(String a, String b) {
        byte[] left = a == null ? new byte[0] : a.getBytes(StandardCharsets.UTF_8);
        byte[] right = b == null ? new byte[0] : b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(left, right);
    }

    public static class SessionData {
        private final long userId;
        private final String username;
        private final String rol;
        private final long expiresAtMs;

        private SessionData(long userId, String username, String rol, long expiresAtMs) {
            this.userId = userId;
            this.username = username;
            this.rol = rol;
            this.expiresAtMs = expiresAtMs;
        }

        public long getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public String getRol() {
            return rol;
        }

        public long getExpiresAtMs() {
            return expiresAtMs;
        }
    }
}
