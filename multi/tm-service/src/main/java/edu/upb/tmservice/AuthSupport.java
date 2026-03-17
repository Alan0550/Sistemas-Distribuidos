package edu.upb.tmservice;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

final class AuthSupport {
    private AuthSupport() {
    }

    static AuthSessionStore.SessionData requireSession(HttpExchange he) throws IOException {
        String authHeader = he.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            HttpJsonSupport.sendJson(he, 401, HttpJsonSupport.jsonStatus("NOK", "No autorizado"));
            return null;
        }

        String token = authHeader.substring("Bearer ".length()).trim();
        AuthSessionStore.SessionData session = AuthSessionStore.getInstance().getSession(token);
        if (session == null) {
            HttpJsonSupport.sendJson(he, 401, HttpJsonSupport.jsonStatus("NOK", "Sesion invalida o expirada"));
            return null;
        }
        return session;
    }

    static boolean requireRole(HttpExchange he, AuthSessionStore.SessionData session, String expectedRole)
            throws IOException {
        if (!hasRole(session, expectedRole)) {
            HttpJsonSupport.sendJson(he, 403, HttpJsonSupport.jsonStatus("NOK", "Acceso denegado para este rol"));
            return false;
        }
        return true;
    }

    static boolean requireAnyRole(HttpExchange he, AuthSessionStore.SessionData session, String... roles)
            throws IOException {
        if (!hasAnyRole(session, roles)) {
            HttpJsonSupport.sendJson(he, 403, HttpJsonSupport.jsonStatus("NOK", "Acceso denegado para este rol"));
            return false;
        }
        return true;
    }

    static boolean hasRole(AuthSessionStore.SessionData session, String expectedRole) {
        return session != null && expectedRole != null && expectedRole.equalsIgnoreCase(session.getRol());
    }

    static boolean hasAnyRole(AuthSessionStore.SessionData session, String... roles) {
        if (session == null || roles == null) {
            return false;
        }
        for (String role : roles) {
            if (role != null && role.equalsIgnoreCase(session.getRol())) {
                return true;
            }
        }
        return false;
    }
}
