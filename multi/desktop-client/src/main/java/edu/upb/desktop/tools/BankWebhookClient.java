package edu.upb.desktop.tools;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class BankWebhookClient {

    public static void main(String[] args) throws Exception {
        String url = args.length > 0
                ? args[0]
                : "https://a084-181-115-147-74.ngrok-free.app/webhook/banco";

        String secret = args.length > 1
                ? args[1]
                : "Noche420";

        String body = args.length > 2
                ? args[2]
                : "{\"transaccionId\":\"abc-123\",\"monto\":150.75,\"moneda\":\"USD\",\"estado\":\"PAGADO\",\"timestamp\":\"2026-03-05T18:25:43Z\"}";

        String signatureHex = hmacSha256Hex(secret, body);
        String signatureHeader = "sha256=" + signatureHex;

        System.out.println("URL: " + url);
        System.out.println("X-Signature: " + signatureHeader);
        System.out.println("Body: " + body);

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("X-Signature", signatureHeader);

        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload);
        }

        int status = conn.getResponseCode();
        String responseBody = readAll(status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream());

        System.out.println("Status: " + status);
        System.out.println("Response: " + responseBody);
    }

    private static String hmacSha256Hex(String secret, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(key);
        byte[] hash = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String readAll(InputStream is) throws Exception {
        if (is == null) {
            return "";
        }
        try (InputStream in = is) {
            byte[] bytes = in.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }
}
