package edu.upb.desktop.model;

public class LoginResultModel {
    private final UserModel user;
    private final String token;

    public LoginResultModel(UserModel user, String token) {
        this.user = user;
        this.token = token;
    }

    public UserModel getUser() {
        return user;
    }

    public String getToken() {
        return token;
    }
}
