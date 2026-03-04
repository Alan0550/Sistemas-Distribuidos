package edu.upb.desktop.util;

import edu.upb.desktop.model.UserModel;

public class SessionManager {
    private static UserModel currentUser;

    private SessionManager() {
    }

    public static UserModel getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(UserModel user) {
        currentUser = user;
    }

    public static void clear() {
        currentUser = null;
    }
}
