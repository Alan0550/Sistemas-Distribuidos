package edu.upb.desktop.controller;

import edu.upb.desktop.app.DesktopApp;
import edu.upb.desktop.model.LoginResultModel;
import edu.upb.desktop.model.UserModel;
import edu.upb.desktop.service.AuthService;
import edu.upb.desktop.util.AlertUtil;
import edu.upb.desktop.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {
    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    private final AuthService authService = new AuthService();

    @FXML
    private void onLogin() {
        LoginResultModel result;
        try {
            result = authService.login(
                    usernameField.getText().trim(),
                    passwordField.getText());
        } catch (IllegalArgumentException e) {
            AlertUtil.error("Login", e.getMessage());
            return;
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.error("Login", "No se pudo iniciar sesion: " + safeMessage(e));
            return;
        }

        try {
            UserModel user = result.getUser();
            SessionManager.setCurrentUser(user);
            SessionManager.setAuthToken(result.getToken());
            DesktopApp.getInstance().showEvents();
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.error("UI", "Login OK, pero no se pudo abrir la pantalla: " + safeMessage(e));
        }
    }

    @FXML
    private void onGoRegister() {
        try {
            DesktopApp.getInstance().showRegister();
        } catch (Exception e) {
            AlertUtil.error("Registro", "No se pudo abrir la pantalla de registro");
        }
    }

    private String safeMessage(Exception e) {
        return e.getMessage() == null ? "sin detalle" : e.getMessage();
    }
}
