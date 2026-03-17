/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.upb.tickmaster.httpserver;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import com.google.gson.JsonParser;

/**
 * @author rlaredo
 */
@Deprecated
public class UsuariosHandler implements HttpHandler {


    public UsuariosHandler() {

    }

    @Override
    public void handle(HttpExchange he) throws IOException {

        try {
            InputStreamReader isr = new InputStreamReader(he.getRequestBody(), StandardCharsets.UTF_8);
            String response;
            BufferedReader br = new BufferedReader(isr);
            Headers responseHeaders = he.getResponseHeaders();
            responseHeaders.add("Access-Control-Allow-Origin", "*");
            responseHeaders.add("Content-type", ContentType.JSON.toString());


            
            if (he.getRequestMethod().equals("POST")) {
                try {
                    JsonObject body = JsonParser.parseReader(br).getAsJsonObject();
                    String nombre = body.has("nombre") ? body.get("nombre").getAsString() : "";
                    String apellido = body.has("apellido") ? body.get("apellido").getAsString() : "";
                    String email = body.has("email") ? body.get("email").getAsString() : "";

                    if (nombre.isEmpty() || apellido.isEmpty() || email.isEmpty()) {
                        response = "{\"status\": \"NOK\",\"message\": \"Faltan datos\"}";
                        byte[] byteResponse = response.getBytes(StandardCharsets.UTF_8);
                        he.sendResponseHeaders(Integer.parseInt(Status._400.name().substring(1, 4)), byteResponse.length);
                        OutputStream os = he.getResponseBody();
                        os.write(byteResponse);
                        os.close();
                        return;
                    }

                    try (Connection conn = DatabaseConnection.getInstance().getConnection();
                         PreparedStatement ps = conn.prepareStatement(
                                 "INSERT INTO usuarios (nombre, apellido, email) VALUES (?,?,?)",
                                 Statement.RETURN_GENERATED_KEYS)) {
                        ps.setString(1, nombre);
                        ps.setString(2, apellido);
                        ps.setString(3, email);
                        int rows = ps.executeUpdate();

                        if (rows > 0) {
                            ResultSet keys = ps.getGeneratedKeys();
                            long id = keys.next() ? keys.getLong(1) : 0;
                            JsonObject resp = new JsonObject();
                            resp.addProperty("id", id);
                            resp.addProperty("nombre", nombre);
                            resp.addProperty("apellido", apellido);
                            resp.addProperty("email", email);
                            response = resp.toString();
                            byte[] byteResponse = response.getBytes(StandardCharsets.UTF_8);
                            he.sendResponseHeaders(Integer.parseInt(Status._201.name().substring(1, 4)), byteResponse.length);
                            OutputStream os = he.getResponseBody();
                            os.write(byteResponse);
                            os.close();
                            return;
                        } else {
                            response = "{\"status\": \"NOK\",\"message\": \"No se guardo\"}";
                            byte[] byteResponse = response.getBytes(StandardCharsets.UTF_8);
                            he.sendResponseHeaders(Integer.parseInt(Status._500.name().substring(1, 4)), byteResponse.length);
                            OutputStream os = he.getResponseBody();
                            os.write(byteResponse);
                            os.close();
                            return;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    response = "{\"status\": \"NOK\",\"message\": \"Error al procesar\"}";
                    byte[] byteResponse = response.getBytes(StandardCharsets.UTF_8);
                    he.sendResponseHeaders(Integer.parseInt(Status._500.name().substring(1, 4)), byteResponse.length);
                    OutputStream os = he.getResponseBody();
                    os.write(byteResponse);
                    os.close();
                    return;

                }
            }
            if (he.getRequestMethod().equals("GET")) {
                try (Connection conn = DatabaseConnection.getInstance().getConnection();
                     PreparedStatement ps = conn.prepareStatement("SELECT id, nombre, apellido, email FROM usuarios");
                     ResultSet rs = ps.executeQuery()) {

                    StringBuilder sb = new StringBuilder();
                    sb.append("[");
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) {
                            sb.append(",");
                        }
                        sb.append("{")
                                .append("\"id\":").append(rs.getLong("id")).append(",")
                                .append("\"nombre\":\"").append(rs.getString("nombre")).append("\",")
                                .append("\"apellido\":\"").append(rs.getString("apellido")).append("\",")
                                .append("\"email\":\"").append(rs.getString("email")).append("\"")
                                .append("}");
                        first = false;
                    }
                    sb.append("]");

                    response = sb.toString();
                    byte[] byteResponse = response.getBytes(StandardCharsets.UTF_8);
                    he.sendResponseHeaders(Integer.parseInt(Status._200.name().substring(1, 4)), byteResponse.length);
                    OutputStream os = he.getResponseBody();
                    os.write(byteResponse);
                    os.close();
                    return;
                } catch (SQLException e) {
                    e.printStackTrace();
                    response = "{\"status\": \"NOK\",\"message\": \"Error de conexion\"}";
                    byte[] byteResponse = response.getBytes(StandardCharsets.UTF_8);
                    he.sendResponseHeaders(Integer.parseInt(Status._500.name().substring(1, 4)), byteResponse.length);
                    OutputStream os = he.getResponseBody();
                    os.write(byteResponse);
                    os.close();
                    return;
                }

            }

            if (he.getRequestMethod().equals("OPTIONS")) {
                response = "{\"status\": \"OK\",\"message\": \"Factura impreso correctamente\"}";
                he.sendResponseHeaders(Integer.parseInt(Status._200.name().substring(1, 4)), response.length());
            } else {
                response = "{\"status\": \"NOK\",\"message\": \"Methodo nokkk soportado\"}";
                he.sendResponseHeaders(Integer.parseInt(Status._404.name().substring(1, 4)), response.length());
            }
            OutputStream os = he.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } catch (NumberFormatException | IOException e) {
            e.printStackTrace();
        }
    }
}
