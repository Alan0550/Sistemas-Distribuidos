package edu.upb.desktop.util;

import edu.upb.desktop.model.UserModel;

public class SessionManager {
    private static UserModel currentUser;
    private static String authToken;

    private SessionManager() {
    }

    public static UserModel getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(UserModel user) {
        currentUser = user;
    }

    public static String getAuthToken() {
        return authToken;
    }

    public static void setAuthToken(String token) {
        authToken = token;
    }

    public static void clear() {
        currentUser = null;
        authToken = null;
    }
}
