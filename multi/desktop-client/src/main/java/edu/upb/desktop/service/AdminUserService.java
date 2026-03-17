package edu.upb.desktop.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.upb.desktop.model.AdminUserModel;
import edu.upb.desktop.model.UserTicketHistoryModel;
import edu.upb.desktop.util.SessionManager;

import java.net.URI;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class AdminUserService {
    private final String baseUrl = System.getenv().getOrDefault("LB_UI_BASE_URL", "http://localhost:1915/tm");
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public List<AdminUserModel> fetchUsersWithHistory() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + "/usuarios?include_history=true").openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(4000);
        conn.setReadTimeout(4000);
        applyAuthHeader(conn);

        if (conn.getResponseCode() != 200) {
            throw new IllegalStateException("No se pudo cargar historial de usuarios");
        }

        try (Reader reader = new java.io.InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
            JsonObject resp = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray users = resp.getAsJsonArray("users");
            List<AdminUserModel> result = new ArrayList<AdminUserModel>();
            for (JsonElement userEl : users) {
                JsonObject user = userEl.getAsJsonObject();
                List<UserTicketHistoryModel> history = new ArrayList<UserTicketHistoryModel>();
                JsonArray historyArray = user.getAsJsonArray("historial");
                if (historyArray != null) {
                    for (JsonElement historyEl : historyArray) {
                        JsonObject row = historyEl.getAsJsonObject();
                        history.add(new UserTicketHistoryModel(
                                row.get("ticket_id").getAsLong(),
                                row.get("event_id").getAsLong(),
                                row.get("event_name").getAsString(),
                                row.get("event_date").getAsString(),
                                row.get("seat_type").getAsString(),
                                row.get("seat_number").getAsString(),
                                row.get("price").getAsString()));
                    }
                }

                result.add(new AdminUserModel(
                        user.get("id").getAsLong(),
                        user.get("username").getAsString(),
                        user.get("nombre").getAsString(),
                        user.get("rol").getAsString(),
                        user.has("baneado") && user.get("baneado").getAsBoolean(),
                        history));
            }
            return result;
        }
    }

    public void updateBanStatus(long userId, boolean banned) throws Exception {
        String token = SessionManager.getAuthToken();
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalStateException("No hay sesion activa");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/usuarios/" + userId + "/" + (banned ? "ban" : "unban")))
                .header("Authorization", "Bearer " + token.trim())
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            try {
                JsonObject error = JsonParser.parseString(response.body()).getAsJsonObject();
                if (error.has("message")) {
                    throw new IllegalArgumentException(error.get("message").getAsString());
                }
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception ignored) {
            }
            throw new IllegalStateException("No se pudo actualizar el estado del usuario");
        }
    }

    private void applyAuthHeader(HttpURLConnection conn) {
        String token = SessionManager.getAuthToken();
        if (token != null && !token.trim().isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + token.trim());
        }
    }
}
