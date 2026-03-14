package edu.upb.desktop.controller;

import com.google.gson.JsonObject;
import edu.upb.desktop.app.DesktopApp;
import edu.upb.desktop.model.AdminUserModel;
import edu.upb.desktop.model.EventModel;
import edu.upb.desktop.model.PurchaseResultModel;
import edu.upb.desktop.model.TicketTypeModel;
import edu.upb.desktop.model.UserModel;
import edu.upb.desktop.model.UserTicketHistoryModel;
import edu.upb.desktop.service.AdminUserService;
import edu.upb.desktop.service.EventService;
import edu.upb.desktop.service.MonitorService;
import edu.upb.desktop.service.TicketGrpcClient;
import edu.upb.desktop.util.AlertUtil;
import edu.upb.desktop.util.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class EventsController {
    @FXML
    private Label welcomeLabel;

    @FXML
    private VBox adminPanel;

    @FXML
    private VBox adminUsersPanel;

    @FXML
    private VBox purchasePanel;

    @FXML
    private Button openEventWizardButton;

    @FXML
    private Label monitorStatusLabel;

    @FXML
    private Label monitorBackendsLabel;

    @FXML
    private Label monitorErrorsLabel;

    @FXML
    private Label monitorLastUpdateLabel;

    @FXML
    private TableView<EventModel> eventsTable;

    @FXML
    private TableColumn<EventModel, Number> idColumn;

    @FXML
    private TableColumn<EventModel, String> nameColumn;

    @FXML
    private TableColumn<EventModel, String> dateColumn;

    @FXML
    private TableColumn<EventModel, Number> capacityColumn;

    @FXML
    private ComboBox<TicketTypeModel> ticketTypeCombo;

    @FXML
    private Spinner<Integer> quantitySpinner;

    @FXML
    private TableView<AdminUserModel> usersTable;

    @FXML
    private TableColumn<AdminUserModel, Number> userIdColumn;

    @FXML
    private TableColumn<AdminUserModel, String> usernameColumn;

    @FXML
    private TableColumn<AdminUserModel, String> userNameColumn;

    @FXML
    private TableColumn<AdminUserModel, String> userRoleColumn;

    @FXML
    private Label selectedUserLabel;

    @FXML
    private TableView<UserTicketHistoryModel> historyTable;

    @FXML
    private TableColumn<UserTicketHistoryModel, Number> historyTicketIdColumn;

    @FXML
    private TableColumn<UserTicketHistoryModel, String> historyEventColumn;

    @FXML
    private TableColumn<UserTicketHistoryModel, String> historyDateColumn;

    @FXML
    private TableColumn<UserTicketHistoryModel, String> historySeatTypeColumn;

    @FXML
    private TableColumn<UserTicketHistoryModel, String> historySeatColumn;

    @FXML
    private TableColumn<UserTicketHistoryModel, String> historyPriceColumn;

    private final EventService eventService = new EventService();
    private final MonitorService monitorService = new MonitorService();
    private final AdminUserService adminUserService = new AdminUserService();
    private final TicketGrpcClient ticketGrpcClient = new TicketGrpcClient();
    private final AtomicBoolean monitorFetchInFlight = new AtomicBoolean(false);

    private Timeline monitorTimeline;

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().getId()));
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNombre()));
        dateColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFecha()));
        capacityColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getCapacidad()));

        quantitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1, 1));
        quantitySpinner.setDisable(true);

        userIdColumn.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().getId()));
        usernameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        userNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNombre()));
        userRoleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRol()));

        historyTicketIdColumn.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().getTicketId()));
        historyEventColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEventName()));
        historyDateColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEventDate()));
        historySeatTypeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSeatType()));
        historySeatColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSeatNumber()));
        historyPriceColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPrice()));

        eventsTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                ticketTypeCombo.setItems(FXCollections.observableArrayList(selected.getTicketTypes()));
                if (!selected.getTicketTypes().isEmpty()) {
                    ticketTypeCombo.getSelectionModel().selectFirst();
                } else {
                    updateQuantityLimit(null);
                }
            } else {
                ticketTypeCombo.setItems(FXCollections.emptyObservableList());
                updateQuantityLimit(null);
            }
        });

        ticketTypeCombo.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) ->
                updateQuantityLimit(selected));

        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) ->
                updateSelectedUserHistory(selected));
    }

    public void loadInitialData() {
        UserModel user = SessionManager.getCurrentUser();
        if (user != null) {
            welcomeLabel.setText("Bienvenido, " + user.getNombre() + " (" + user.getRol() + ")");
        }

        boolean isAdmin = isAdmin();
        adminPanel.setVisible(isAdmin);
        adminPanel.setManaged(isAdmin);
        adminUsersPanel.setVisible(isAdmin);
        adminUsersPanel.setManaged(isAdmin);
        openEventWizardButton.setVisible(isAdmin);
        openEventWizardButton.setManaged(isAdmin);
        purchasePanel.setVisible(!isAdmin);
        purchasePanel.setManaged(!isAdmin);

        loadEvents();
        if (isAdmin) {
            loadAdminUsers();
            startMonitorAutoRefresh();
        } else {
            stopMonitorAutoRefresh();
        }
    }

    @FXML
    private void onOpenEventWizard() {
        if (!isAdmin()) {
            AlertUtil.error("Permisos", "Solo ADMIN puede crear eventos");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/event-wizard.fxml"));
            Parent root = loader.load();
            EventWizardController controller = loader.getController();
            controller.setOnEventCreated(event -> {
                loadEvents();
                selectEventById(event.getId());
            });

            Stage dialog = new Stage();
            dialog.setTitle("Crear evento");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(openEventWizardButton.getScene().getWindow());
            Scene scene = new Scene(root, 820, 620);
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (Exception e) {
            AlertUtil.error("Evento", "No se pudo abrir el asistente de eventos");
        }
    }

    @FXML
    private void onRefreshUsers() {
        loadAdminUsers();
    }

    @FXML
    private void onBuy() {
        EventModel event = eventsTable.getSelectionModel().getSelectedItem();
        TicketTypeModel type = ticketTypeCombo.getSelectionModel().getSelectedItem();
        UserModel user = SessionManager.getCurrentUser();

        if (user == null) {
            AlertUtil.error("Compra", "No hay una sesion activa");
            return;
        }
        if (event == null || type == null) {
            AlertUtil.error("Compra", "Selecciona un evento y un tipo de ticket");
            return;
        }
        if (!isCliente(user)) {
            AlertUtil.error("Compra", "Solo CLIENTE puede comprar tickets");
            return;
        }

        int quantity = quantitySpinner.getValue();
        if (quantity <= 0 || quantity > type.getCantidad()) {
            AlertUtil.error("Compra", "La cantidad debe estar dentro del stock disponible");
            return;
        }

        try {
            PurchaseResultModel result = ticketGrpcClient.buyTickets(
                    user.getId(),
                    event.getId(),
                    type.getId(),
                    quantity);

            AlertUtil.info("Compra exitosa",
                    "Primer ticket ID: " + result.getPrimerTicketId() +
                            "\nTickets creados: " + result.getTicketsCreados() +
                            "\nMensaje: " + result.getMensaje());
            loadEvents();
            selectEventById(event.getId());
        } catch (Exception e) {
            String message = e.getMessage() != null ? e.getMessage() : "No se pudo realizar la compra";
            AlertUtil.error("Compra", message);
        }
    }

    @FXML
    private void onLogout() {
        try {
            stopMonitorAutoRefresh();
            SessionManager.clear();
            DesktopApp.getInstance().showLogin();
        } catch (Exception e) {
            AlertUtil.error("Sesion", "No se pudo cerrar sesion");
        }
    }

    private void loadEvents() {
        try {
            List<EventModel> events = eventService.fetchEvents();
            eventsTable.setItems(FXCollections.observableArrayList(events));
            if (!events.isEmpty()) {
                eventsTable.getSelectionModel().selectFirst();
            } else {
                ticketTypeCombo.setItems(FXCollections.emptyObservableList());
                updateQuantityLimit(null);
            }
        } catch (Exception e) {
            AlertUtil.error("Eventos", "No se pudieron cargar los eventos");
        }
    }

    private void loadAdminUsers() {
        try {
            List<AdminUserModel> users = adminUserService.fetchUsersWithHistory();
            usersTable.setItems(FXCollections.observableArrayList(users));
            if (!users.isEmpty()) {
                usersTable.getSelectionModel().selectFirst();
            } else {
                updateSelectedUserHistory(null);
            }
        } catch (Exception e) {
            AlertUtil.error("Usuarios", "No se pudieron cargar los usuarios");
        }
    }

    private void updateSelectedUserHistory(AdminUserModel selected) {
        if (selected == null) {
            selectedUserLabel.setText("Historial de compras: sin seleccion");
            historyTable.setItems(FXCollections.emptyObservableList());
            return;
        }
        selectedUserLabel.setText("Historial de compras: " + selected.getNombre() + " (" + selected.getUsername() + ")");
        historyTable.setItems(FXCollections.observableArrayList(selected.getHistorial()));
    }

    private void updateQuantityLimit(TicketTypeModel selected) {
        int available = selected != null ? selected.getCantidad() : 0;
        if (available <= 0) {
            quantitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 0, 0));
            quantitySpinner.setDisable(true);
            return;
        }

        int current = quantitySpinner.getValueFactory() != null ? quantitySpinner.getValue() : 1;
        int safeValue = Math.min(Math.max(current, 1), available);
        quantitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, available, safeValue));
        quantitySpinner.setDisable(false);
    }

    private void startMonitorAutoRefresh() {
        stopMonitorAutoRefresh();
        monitorTimeline = new Timeline(new KeyFrame(Duration.seconds(3), event -> refreshMonitorPanelAsync()));
        monitorTimeline.setCycleCount(Timeline.INDEFINITE);
        monitorTimeline.play();
        refreshMonitorPanelAsync();
    }

    private void stopMonitorAutoRefresh() {
        if (monitorTimeline != null) {
            monitorTimeline.stop();
            monitorTimeline = null;
        }
    }

    private void refreshMonitorPanelAsync() {
        if (!monitorFetchInFlight.compareAndSet(false, true)) {
            return;
        }

        Thread thread = new Thread(() -> {
            try {
                JsonObject health = monitorService.fetchHealth();
                JsonObject metrics = monitorService.fetchMetrics();
                Platform.runLater(() -> applyMonitorData(health, metrics));
            } catch (Exception e) {
                Platform.runLater(this::applyMonitorDown);
            } finally {
                monitorFetchInFlight.set(false);
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void applyMonitorData(JsonObject health, JsonObject metrics) {
        String status = health.has("status") ? health.get("status").getAsString() : "UNKNOWN";
        int inService = health.has("in_service_backends") ? health.get("in_service_backends").getAsInt() : 0;
        int total = health.has("registered_backends") ? health.get("registered_backends").getAsInt() : 0;
        long errors = metrics.has("errors_total") ? metrics.get("errors_total").getAsLong() : 0L;
        long requests = metrics.has("requests_total") ? metrics.get("requests_total").getAsLong() : 0L;

        monitorStatusLabel.setText("Estado sistema: " + status);
        monitorBackendsLabel.setText("Backends activos: " + inService + " / " + total);
        monitorErrorsLabel.setText("Errores: " + errors + " de " + requests + " requests");
        monitorLastUpdateLabel.setText("Actualizacion automatica: " + java.time.LocalTime.now().withNano(0));
    }

    private void applyMonitorDown() {
        monitorStatusLabel.setText("Estado sistema: DOWN");
        monitorBackendsLabel.setText("Backends activos: 0 / 0");
        monitorErrorsLabel.setText("Errores: no disponible");
        monitorLastUpdateLabel.setText("Actualizacion automatica: " + java.time.LocalTime.now().withNano(0));
    }

    private void selectEventById(long eventId) {
        for (EventModel event : eventsTable.getItems()) {
            if (event.getId() == eventId) {
                eventsTable.getSelectionModel().select(event);
                eventsTable.scrollTo(event);
                break;
            }
        }
    }

    private boolean isAdmin() {
        UserModel user = SessionManager.getCurrentUser();
        return user != null && "ADMIN".equalsIgnoreCase(user.getRol());
    }

    private boolean isCliente(UserModel user) {
        return user != null && "CLIENTE".equalsIgnoreCase(user.getRol());
    }
}
