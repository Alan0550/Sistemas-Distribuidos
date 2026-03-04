package edu.upb.desktop.controller;

import edu.upb.desktop.app.DesktopApp;
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
        try {
            UserModel user = authService.login(
                    usernameField.getText().trim(),
                    passwordField.getText());
            SessionManager.setCurrentUser(user);
            DesktopApp.getInstance().showEvents();
        } catch (IllegalArgumentException e) {
            AlertUtil.error("Login", e.getMessage());
        } catch (Exception e) {
            AlertUtil.error("Login", "No se pudo iniciar sesion");
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
}
