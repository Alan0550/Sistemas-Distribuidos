package edu.upb.tmservice;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import edu.upb.tmservice.dao.UsuarioDao;
import edu.upb.tmservice.dao.UsuarioEntity;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;

public class LoginHandler implements HttpHandler {
    private final UsuarioDao usuarioDao = new UsuarioDao();

    @Override
    public void handle(HttpExchange he) throws IOException {
        Headers h = he.getResponseHeaders();
        HttpJsonSupport.addJsonHeaders(h, "POST, OPTIONS");

        String method = he.getRequestMethod();
        if ("OPTIONS".equals(method)) {
            HttpJsonSupport.sendJsonText(he, 200, "{}");
            return;
        }

        if (!"POST".equals(method)) {
            HttpJsonSupport.sendJson(he, 405, HttpJsonSupport.jsonStatus("NOK", "Metodo no soportado"));
            return;
        }

        try {
            JsonObject body = HttpJsonSupport.readJsonBody(he);

            String username = body.has("username") ? body.get("username").getAsString().trim() : "";
            String password = body.has("password") ? body.get("password").getAsString() : "";

            if (username.isEmpty() || password.isEmpty()) {
                HttpJsonSupport.sendJson(he, 400, HttpJsonSupport.jsonStatus("NOK", "Faltan credenciales"));
                return;
            }

            JsonObject user = null;
            UsuarioEntity entity = usuarioDao.findAuthByUsername(username);
            if (entity != null) {
                if (entity.isBaneado()) {
                    HttpJsonSupport.sendJson(he, 403, HttpJsonSupport.jsonStatus("NOK", "Tu cuenta fue baneada"));
                    return;
                }
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
                HttpJsonSupport.sendJson(he, 401, HttpJsonSupport.jsonStatus("NOK", "Credenciales invalidas"));
                return;
            }

            JsonObject resp = new JsonObject();
            resp.addProperty("status", "OK");
            resp.add("user", user);
            HttpJsonSupport.sendJson(he, 200, resp);
        } catch (Exception e) {
            HttpJsonSupport.sendJson(he, 500, HttpJsonSupport.jsonStatus("NOK", "Error en login"));
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
}
