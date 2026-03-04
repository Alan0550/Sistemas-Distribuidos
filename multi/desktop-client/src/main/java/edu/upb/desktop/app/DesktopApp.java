package edu.upb.desktop.app;

import edu.upb.desktop.controller.EventsController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class DesktopApp extends Application {
    private static DesktopApp instance;
    private Stage stage;

    public static DesktopApp getInstance() {
        return instance;
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        instance = this;
        this.stage = primaryStage;
        this.stage.setTitle("TickMaster Desktop");
        showLogin();
        this.stage.show();
    }

    public void showLogin() throws IOException {
        setScene("/fxml/login.fxml");
    }

    public void showRegister() throws IOException {
        setScene("/fxml/register.fxml");
    }

    public void showEvents() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/events.fxml"));
        Parent root = loader.load();
        EventsController controller = loader.getController();
        controller.loadInitialData();
        applyScene(root);
    }

    private void setScene(String resource) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(resource));
        Parent root = loader.load();
        applyScene(root);
    }

    private void applyScene(Parent root) {
        Scene scene = new Scene(root, 980, 640);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        stage.setScene(scene);
    }
}
