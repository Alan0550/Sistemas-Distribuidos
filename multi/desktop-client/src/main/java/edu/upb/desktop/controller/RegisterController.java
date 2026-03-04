package edu.upb.desktop.controller;

import edu.upb.desktop.app.DesktopApp;
import edu.upb.desktop.service.AuthService;
import edu.upb.desktop.util.AlertUtil;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {
    @FXML
    private TextField usernameField;

    @FXML
    private TextField nameField;

    @FXML
    private PasswordField passwordField;

    private final AuthService authService = new AuthService();

    @FXML
    private void onRegister() {
        try {
            authService.register(
                    usernameField.getText().trim(),
                    nameField.getText().trim(),
                    passwordField.getText());
            AlertUtil.info("Registro", "Usuario creado correctamente");
            DesktopApp.getInstance().showLogin();
        } catch (IllegalArgumentException e) {
            AlertUtil.error("Registro", e.getMessage());
        } catch (Exception e) {
            AlertUtil.error("Registro", "No se pudo registrar el usuario");
        }
    }

    @FXML
    private void onBack() {
        try {
            DesktopApp.getInstance().showLogin();
        } catch (Exception e) {
            AlertUtil.error("Login", "No se pudo volver al login");
        }
    }
}
