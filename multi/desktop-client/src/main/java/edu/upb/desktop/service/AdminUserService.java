package edu.upb.desktop.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.upb.desktop.model.AdminUserModel;
import edu.upb.desktop.model.UserTicketHistoryModel;
import edu.upb.desktop.util.SessionManager;

import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class AdminUserService {
    private final String baseUrl = System.getenv().getOrDefault("LB_UI_BASE_URL", "http://localhost:1915/tm");

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
                        history));
            }
            return result;
        }
    }

    private void applyAuthHeader(HttpURLConnection conn) {
        String token = SessionManager.getAuthToken();
        if (token != null && !token.trim().isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + token.trim());
        }
    }
}
