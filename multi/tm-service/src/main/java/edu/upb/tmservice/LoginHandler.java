package edu.upb.tmservice;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import edu.upb.tmservice.dao.UsuarioDao;
import edu.upb.tmservice.dao.UsuarioEntity;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class LoginHandler implements HttpHandler {
    private final UsuarioDao usuarioDao = new UsuarioDao();

    @Override
    public void handle(HttpExchange he) throws IOException {
        Headers h = he.getResponseHeaders();
        h.add("Access-Control-Allow-Origin", "*");
        h.add("Content-Type", "application/json");
        h.add("Access-Control-Allow-Methods", "POST, OPTIONS");
        h.add("Access-Control-Allow-Headers", "Content-Type");

        String method = he.getRequestMethod();
        if ("OPTIONS".equals(method)) {
            send(he, 200, "{}");
            return;
        }

        if (!"POST".equals(method)) {
            send(he, 405, "{\"status\":\"NOK\",\"message\":\"Metodo no soportado\"}");
            return;
        }

        try {
            JsonObject body = JsonParser.parseReader(
                    new InputStreamReader(he.getRequestBody(), StandardCharsets.UTF_8)).getAsJsonObject();

            String username = body.has("username") ? body.get("username").getAsString().trim() : "";
            String password = body.has("password") ? body.get("password").getAsString() : "";

            if (username.isEmpty() || password.isEmpty()) {
                send(he, 400, "{\"status\":\"NOK\",\"message\":\"Faltan credenciales\"}");
                return;
            }

            JsonObject user = null;
            UsuarioEntity entity = usuarioDao.findAuthByUsername(username);
            if (entity != null) {
                long userId = entity.getId();
                String storedPassword = entity.getPassword();
                boolean validPassword = verifyPassword(password, storedPassword);

                if (validPassword) {
                    if (!isBcryptHash(storedPassword)) {
                        migratePasswordToHash(userId, password);
                    }
                    String token = AuthSessionStore.getInstance().createSession(
                            userId,
                            entity.getUsername(),
                            entity.getRol());
                    user = new JsonObject();
                    user.addProperty("id", userId);
                    user.addProperty("username", entity.getUsername());
                    user.addProperty("nombre", entity.getNombre());
                    user.addProperty("rol", entity.getRol());
                    user.addProperty("token", token);
                }
            }

            if (user == null) {
                send(he, 401, "{\"status\":\"NOK\",\"message\":\"Credenciales invalidas\"}");
                return;
            }

            JsonObject resp = new JsonObject();
            resp.addProperty("status", "OK");
            resp.add("user", user);
            send(he, 200, resp.toString());
        } catch (Exception e) {
            send(he, 500, "{\"status\":\"NOK\",\"message\":\"Error en login\"}");
        }
    }

    private boolean verifyPassword(String rawPassword, String storedPassword) {
        if (storedPassword == null || storedPassword.isEmpty()) {
            return false;
        }
        if (isBcryptHash(storedPassword)) {
            try {
                return BCrypt.checkpw(rawPassword, storedPassword);
            } catch (Exception ignored) {
                return false;
            }
        }
        return rawPassword.equals(storedPassword);
    }

    private boolean isBcryptHash(String value) {
        if (value == null) {
            return false;
        }
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }

    private void migratePasswordToHash(long userId, String rawPassword) throws Exception {
        String hash = BCrypt.hashpw(rawPassword, BCrypt.gensalt(12));
        usuarioDao.updatePasswordHash(userId, hash);
    }

    private void send(HttpExchange he, int statusCode, String bodyText) throws IOException {
        byte[] body = bodyText.getBytes(StandardCharsets.UTF_8);
        he.sendResponseHeaders(statusCode, body.length);
        try (OutputStream os = he.getResponseBody()) {
            os.write(body);
        }
    }
}
