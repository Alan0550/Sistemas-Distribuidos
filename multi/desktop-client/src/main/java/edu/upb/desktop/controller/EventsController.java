package edu.upb.desktop.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.upb.desktop.app.DesktopApp;
import edu.upb.desktop.model.AdminTicketSaleModel;
import edu.upb.desktop.model.AdminUserModel;
import edu.upb.desktop.model.BackendStatusModel;
import edu.upb.desktop.model.EventModel;
import edu.upb.desktop.model.PurchaseResultModel;
import edu.upb.desktop.model.SalesAggregateModel;
import edu.upb.desktop.model.TicketTypeModel;
import edu.upb.desktop.model.UserModel;
import edu.upb.desktop.model.UserTicketHistoryModel;
import edu.upb.desktop.service.AdminUserService;
import edu.upb.desktop.service.EventService;
import edu.upb.desktop.service.MonitorService;
import edu.upb.desktop.service.TicketGrpcClient;
import edu.upb.desktop.util.AlertUtil;
import edu.upb.desktop.util.SessionManager;
import edu.upb.desktop.util.SpinnerUtil;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class EventsController {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final long GENERAL_SALES_ID = 0L;

    @FXML
    private Label welcomeLabel;
    @FXML
    private Label clientWelcomeLabel;
    @FXML
    private HBox adminShell;
    @FXML
    private VBox clientRoot;
    @FXML
    private Button dashboardNavButton;
    @FXML
    private Button eventsNavButton;
    @FXML
    private Button usersNavButton;
    @FXML
    private Button salesNavButton;
    @FXML
    private VBox dashboardPane;
    @FXML
    private VBox adminEventsPane;
    @FXML
    private VBox usersListPane;
    @FXML
    private VBox userHistoryPane;
    @FXML
    private VBox salesPane;
    @FXML
    private Label monitorStatusLabel;
    @FXML
    private Label monitorBackendsLabel;
    @FXML
    private Label monitorErrorsLabel;
    @FXML
    private Label monitorLatencyLabel;
    @FXML
    private Label monitorLastUpdateLabel;
    @FXML
    private TableView<BackendStatusModel> backendTable;
    @FXML
    private TableColumn<BackendStatusModel, String> backendUrlColumn;
    @FXML
    private TableColumn<BackendStatusModel, String> backendStatusColumn;
    @FXML
    private TableColumn<BackendStatusModel, String> backendBalancingColumn;
    @FXML
    private TableColumn<BackendStatusModel, String> backendVerificationColumn;
    @FXML
    private TableColumn<BackendStatusModel, Number> backendFailCountColumn;
    @FXML
    private TableColumn<BackendStatusModel, String> backendDatabaseColumn;
    @FXML
    private TableColumn<BackendStatusModel, String> backendDiskColumn;
    @FXML
    private TableColumn<BackendStatusModel, String> backendLastCheckColumn;
    @FXML
    private TableColumn<BackendStatusModel, String> backendLastErrorColumn;
    @FXML
    private TextField adminEventFilterField;
    @FXML
    private Button openEventWizardButton;
    @FXML
    private Label adminEventsSummaryLabel;
    @FXML
    private TableView<EventModel> adminEventsTable;
    @FXML
    private TableColumn<EventModel, Number> adminEventIdColumn;
    @FXML
    private TableColumn<EventModel, String> adminEventNameColumn;
    @FXML
    private TableColumn<EventModel, String> adminEventDateColumn;
    @FXML
    private TableColumn<EventModel, Number> adminEventCapacityColumn;
    @FXML
    private TableColumn<EventModel, Number> adminEventAvailableColumn;
    @FXML
    private TableColumn<EventModel, Number> adminEventSoldColumn;
    @FXML
    private TextField userSearchField;
    @FXML
    private Label usersListSummaryLabel;
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
    private Label selectedUserMetaLabel;
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
    @FXML
    private Label salesSummaryLabel;
    @FXML
    private Label salesUsersLabel;
    @FXML
    private Label salesRevenueLabel;
    @FXML
    private TextField salesSearchField;
    @FXML
    private TableView<SalesAggregateModel> eventSalesTable;
    @FXML
    private TableColumn<SalesAggregateModel, String> eventSalesLabelColumn;
    @FXML
    private TableColumn<SalesAggregateModel, Number> eventSalesTicketsColumn;
    @FXML
    private TableColumn<SalesAggregateModel, BigDecimal> eventSalesRevenueColumn;
    @FXML
    private TableView<SalesAggregateModel> seatTypeSalesTable;
    @FXML
    private TableColumn<SalesAggregateModel, String> seatSalesLabelColumn;
    @FXML
    private TableColumn<SalesAggregateModel, Number> seatSalesTicketsColumn;
    @FXML
    private TableColumn<SalesAggregateModel, BigDecimal> seatSalesRevenueColumn;
    @FXML
    private TableView<AdminTicketSaleModel> soldTicketsTable;
    @FXML
    private TableColumn<AdminTicketSaleModel, Number> soldTicketIdColumn;
    @FXML
    private TableColumn<AdminTicketSaleModel, String> soldTicketUserColumn;
    @FXML
    private TableColumn<AdminTicketSaleModel, String> soldTicketNameColumn;
    @FXML
    private TableColumn<AdminTicketSaleModel, String> soldTicketEventColumn;
    @FXML
    private TableColumn<AdminTicketSaleModel, String> soldTicketDateColumn;
    @FXML
    private TableColumn<AdminTicketSaleModel, String> soldTicketSeatTypeColumn;
    @FXML
    private TableColumn<AdminTicketSaleModel, String> soldTicketSeatColumn;
    @FXML
    private TableColumn<AdminTicketSaleModel, BigDecimal> soldTicketPriceColumn;
    @FXML
    private TableView<EventModel> clientEventsTable;
    @FXML
    private TableColumn<EventModel, Number> clientEventIdColumn;
    @FXML
    private TableColumn<EventModel, String> clientEventNameColumn;
    @FXML
    private TableColumn<EventModel, String> clientEventDateColumn;
    @FXML
    private TableColumn<EventModel, Number> clientEventCapacityColumn;
    @FXML
    private ComboBox<TicketTypeModel> ticketTypeCombo;
    @FXML
    private Spinner<Integer> quantitySpinner;

    private final EventService eventService = new EventService();
    private final MonitorService monitorService = new MonitorService();
    private final AdminUserService adminUserService = new AdminUserService();
    private final TicketGrpcClient ticketGrpcClient = new TicketGrpcClient();
    private final AtomicBoolean monitorFetchInFlight = new AtomicBoolean(false);
    private final AtomicBoolean businessDataFetchInFlight = new AtomicBoolean(false);

    private final List<EventModel> cachedEvents = new ArrayList<EventModel>();
    private final List<AdminUserModel> cachedUsers = new ArrayList<AdminUserModel>();
    private final List<AdminTicketSaleModel> cachedSales = new ArrayList<AdminTicketSaleModel>();
    private Timeline monitorTimeline;
    private Timeline businessDataTimeline;
    private AdminUserModel selectedAdminUser;
    private long selectedSalesEventId = GENERAL_SALES_ID;

    @FXML
    private void initialize() {
        configureTables();
        configureAdminInteractions();
        quantitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1, 1));
        SpinnerUtil.makeEditableInteger(quantitySpinner);
        quantitySpinner.setDisable(true);
    }

    public void loadInitialData() {
        UserModel user = SessionManager.getCurrentUser();
        if (user != null) {
            String welcomeText = "Bienvenido, " + user.getNombre() + " (" + user.getRol() + ")";
            welcomeLabel.setText(welcomeText);
            if (clientWelcomeLabel != null) {
                clientWelcomeLabel.setText(welcomeText);
            }
        }

        if (isAdmin()) {
            adminShell.setVisible(true);
            adminShell.setManaged(true);
            clientRoot.setVisible(false);
            clientRoot.setManaged(false);
            showAdminPane(dashboardPane, dashboardNavButton);
            loadEvents();
            loadAdminUsers();
            startMonitorAutoRefresh();
            startBusinessAutoRefresh();
        } else {
            adminShell.setVisible(false);
            adminShell.setManaged(false);
            setPaneVisible(dashboardPane, false);
            setPaneVisible(adminEventsPane, false);
            setPaneVisible(usersListPane, false);
            setPaneVisible(userHistoryPane, false);
            setPaneVisible(salesPane, false);
            updateNavState(dashboardNavButton, false);
            updateNavState(eventsNavButton, false);
            updateNavState(usersNavButton, false);
            updateNavState(salesNavButton, false);
            clientRoot.setVisible(true);
            clientRoot.setManaged(true);
            loadEvents();
            stopMonitorAutoRefresh();
            startBusinessAutoRefresh();
        }
    }

    @FXML
    private void onShowDashboard() {
        showAdminPane(dashboardPane, dashboardNavButton);
    }

    @FXML
    private void onShowAdminEvents() {
        showAdminPane(adminEventsPane, eventsNavButton);
    }

    @FXML
    private void onShowUsers() {
        showAdminPane(usersListPane, usersNavButton);
    }

    @FXML
    private void onShowSales() {
        showAdminPane(salesPane, salesNavButton);
    }

    @FXML
    private void onBackToUsers() {
        showAdminPane(usersListPane, usersNavButton);
    }

    @FXML
    private void onRefreshUsers() {
        loadAdminUsers();
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
                showAdminPane(adminEventsPane, eventsNavButton);
                selectEventById(adminEventsTable, event.getId());
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
    private void onBuy() {
        EventModel event = clientEventsTable.getSelectionModel().getSelectedItem();
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
        if (!isBuyer(user)) {
            AlertUtil.error("Compra", "Solo CLIENTE, FRECUENTE y VIP pueden comprar tickets");
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
                    "Primer ticket ID: " + result.getPrimerTicketId()
                            + "\nTickets creados: " + result.getTicketsCreados()
                            + "\nMensaje: " + result.getMensaje());
            loadEvents();
            refreshBusinessDataAsync();
            selectEventById(clientEventsTable, event.getId());
        } catch (Exception e) {
            AlertUtil.error("Compra", e.getMessage() != null ? e.getMessage() : "No se pudo realizar la compra");
        }
    }

    @FXML
    private void onLogout() {
        try {
            stopMonitorAutoRefresh();
            stopBusinessAutoRefresh();
            SessionManager.clear();
            DesktopApp.getInstance().showLogin();
        } catch (Exception e) {
            AlertUtil.error("Sesion", "No se pudo cerrar sesion");
        }
    }

    private void configureTables() {
        adminEventIdColumn.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().getId()));
        adminEventNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNombre()));
        adminEventDateColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFecha()));
        adminEventCapacityColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getCapacidad()));
        adminEventAvailableColumn
                .setCellValueFactory(data -> new SimpleIntegerProperty(availableTickets(data.getValue())));
        adminEventSoldColumn.setCellValueFactory(data -> new SimpleIntegerProperty(soldTickets(data.getValue())));

        clientEventIdColumn.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().getId()));
        clientEventNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNombre()));
        clientEventDateColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFecha()));
        clientEventCapacityColumn
                .setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getCapacidad()));

        userIdColumn.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().getId()));
        usernameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        userNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNombre()));
        userRoleColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getRol() + (data.getValue().isBaneado() ? " | BANEADO" : "")));

        historyTicketIdColumn.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().getTicketId()));
        historyEventColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEventName()));
        historyDateColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEventDate()));
        historySeatTypeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSeatType()));
        historySeatColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSeatNumber()));
        historyPriceColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPrice()));

        backendUrlColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBackendUrl()));
        backendStatusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        backendBalancingColumn
                .setCellValueFactory(data -> new SimpleStringProperty(boolText(data.getValue().isInBalancing())));
        backendVerificationColumn
                .setCellValueFactory(data -> new SimpleStringProperty(boolText(data.getValue().isInVerification())));
        backendFailCountColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getFailCount()));
        backendDatabaseColumn
                .setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDatabaseStatus()));
        backendDiskColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDiskStatus()));
        backendLastCheckColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLastCheck()));
        backendLastErrorColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLastError()));

        soldTicketIdColumn.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().getTicketId()));
        soldTicketUserColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        soldTicketNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullName()));
        soldTicketEventColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEventName()));
        soldTicketDateColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEventDate()));
        soldTicketSeatTypeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSeatType()));
        soldTicketSeatColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSeatNumber()));
        soldTicketPriceColumn
                .setCellValueFactory(data -> new SimpleObjectProperty<BigDecimal>(data.getValue().getPrice()));

        eventSalesLabelColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLabel()));
        eventSalesTicketsColumn
                .setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getTicketsSold()));
        eventSalesRevenueColumn
                .setCellValueFactory(data -> new SimpleObjectProperty<BigDecimal>(data.getValue().getRevenue()));

        seatSalesLabelColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLabel()));
        seatSalesTicketsColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getTicketsSold()));
        seatSalesRevenueColumn
                .setCellValueFactory(data -> new SimpleObjectProperty<BigDecimal>(data.getValue().getRevenue()));
    }

    private void configureAdminInteractions() {
        adminEventFilterField.textProperty().addListener((obs, old, value) -> applyAdminEventFilter());
        userSearchField.textProperty().addListener((obs, old, value) -> applyUserFilter());
        salesSearchField.textProperty().addListener((obs, old, value) -> refreshSalesView());

        adminEventsTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, selected) -> updateAdminEventSummary(selected));
        clientEventsTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, selected) -> updateClientPurchaseOptions(selected));
        ticketTypeCombo.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, selected) -> updateQuantityLimit(selected));
        eventSalesTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            selectedSalesEventId = selected != null ? selected.getId() : GENERAL_SALES_ID;
            refreshSalesView();
        });

        usersTable.setRowFactory(table -> {
            TableRow<AdminUserModel> row = new TableRow<AdminUserModel>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openUserHistory(row.getItem());
                }
            });
            row.itemProperty().addListener((obs, old, user) -> {
                if (user == null || "ADMIN".equalsIgnoreCase(user.getRol())) {
                    row.setContextMenu(null);
                    return;
                }
                MenuItem toggleBanItem = new MenuItem(user.isBaneado() ? "Quitar ban" : "Banear usuario");
                toggleBanItem.setOnAction(event -> toggleUserBan(user));
                row.setContextMenu(new ContextMenu(toggleBanItem));
            });
            return row;
        });
    }

    private void loadEvents() {
        try {
            applyEventsData(eventService.fetchEvents(), true);
        } catch (Exception e) {
            AlertUtil.error("Eventos", "No se pudieron cargar los eventos");
        }
    }

    private void loadAdminUsers() {
        try {
            applyAdminUsersData(adminUserService.fetchUsersWithHistory(), true);
        } catch (Exception e) {
            AlertUtil.error("Usuarios", "No se pudieron cargar los usuarios");
        }
    }

    private void applyEventsData(List<EventModel> events, boolean preserveSelections) {
        long selectedAdminEventId = preserveSelections ? selectedEventId(adminEventsTable) : -1L;
        long selectedClientEventId = preserveSelections ? selectedEventId(clientEventsTable) : -1L;
        long selectedTicketTypeId = preserveSelections ? selectedTicketTypeId() : -1L;

        cachedEvents.clear();
        cachedEvents.addAll(events);
        applyAdminEventFilter();
        clientEventsTable.setItems(FXCollections.observableArrayList(cachedEvents));

        selectEventByIdOrFirst(adminEventsTable, selectedAdminEventId);
        selectEventByIdOrFirst(clientEventsTable, selectedClientEventId);
        restoreClientTicketTypeSelection(selectedTicketTypeId);

        if (cachedEvents.isEmpty()) {
            adminEventsSummaryLabel.setText("No hay eventos registrados");
            ticketTypeCombo.setItems(FXCollections.emptyObservableList());
            updateQuantityLimit(null);
        }
    }

    private void applyAdminUsersData(List<AdminUserModel> users, boolean preserveSelections) {
        long selectedTableUserId = preserveSelections ? selectedUserId(usersTable.getSelectionModel().getSelectedItem())
                : -1L;
        long selectedHistoryUserId = preserveSelections ? selectedUserId(selectedAdminUser) : -1L;

        cachedUsers.clear();
        cachedUsers.addAll(users);
        applyUserFilter();
        selectUserByIdOrFirst(usersTable, selectedTableUserId);

        if (selectedHistoryUserId > 0) {
            selectedAdminUser = findUserById(selectedHistoryUserId);
        }

        rebuildSalesData();
        refreshSelectedUser();
    }

    private void startBusinessAutoRefresh() {
        stopBusinessAutoRefresh();
        businessDataTimeline = new Timeline(new KeyFrame(Duration.seconds(4), event -> refreshBusinessDataAsync()));
        businessDataTimeline.setCycleCount(Timeline.INDEFINITE);
        businessDataTimeline.play();
    }

    private void stopBusinessAutoRefresh() {
        if (businessDataTimeline != null) {
            businessDataTimeline.stop();
            businessDataTimeline = null;
        }
    }

    private void refreshBusinessDataAsync() {
        if (!businessDataFetchInFlight.compareAndSet(false, true)) {
            return;
        }

        boolean admin = isAdmin();
        Thread thread = new Thread(() -> {
            List<EventModel> refreshedEvents = null;
            List<AdminUserModel> refreshedUsers = null;
            try {
                try {
                    refreshedEvents = eventService.fetchEvents();
                } catch (Exception ignored) {
                }
                if (admin) {
                    try {
                        refreshedUsers = adminUserService.fetchUsersWithHistory();
                    } catch (Exception ignored) {
                    }
                }

                final List<EventModel> finalEvents = refreshedEvents;
                final List<AdminUserModel> finalUsers = refreshedUsers;
                Platform.runLater(() -> {
                    if (finalEvents != null) {
                        applyEventsData(finalEvents, true);
                    }
                    if (admin && finalUsers != null) {
                        applyAdminUsersData(finalUsers, true);
                    }
                });
            } finally {
                businessDataFetchInFlight.set(false);
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private long selectedEventId(TableView<EventModel> table) {
        EventModel selected = table.getSelectionModel().getSelectedItem();
        return selected != null ? selected.getId() : -1L;
    }

    private long selectedTicketTypeId() {
        TicketTypeModel selected = ticketTypeCombo.getSelectionModel().getSelectedItem();
        return selected != null ? selected.getId() : -1L;
    }

    private long selectedUserId(AdminUserModel user) {
        return user != null ? user.getId() : -1L;
    }

    private void selectEventByIdOrFirst(TableView<EventModel> table, long preferredId) {
        if (table.getItems().isEmpty()) {
            table.getSelectionModel().clearSelection();
            return;
        }
        if (preferredId > 0) {
            for (EventModel event : table.getItems()) {
                if (event.getId() == preferredId) {
                    table.getSelectionModel().select(event);
                    table.scrollTo(event);
                    return;
                }
            }
        }
        table.getSelectionModel().selectFirst();
    }

    private void restoreClientTicketTypeSelection(long preferredId) {
        if (ticketTypeCombo.getItems() == null || ticketTypeCombo.getItems().isEmpty()) {
            ticketTypeCombo.setItems(FXCollections.emptyObservableList());
            updateQuantityLimit(null);
            return;
        }
        if (preferredId > 0) {
            for (TicketTypeModel type : ticketTypeCombo.getItems()) {
                if (type.getId() == preferredId) {
                    ticketTypeCombo.getSelectionModel().select(type);
                    return;
                }
            }
        }
        ticketTypeCombo.getSelectionModel().selectFirst();
    }

    private void selectUserByIdOrFirst(TableView<AdminUserModel> table, long preferredId) {
        if (table.getItems().isEmpty()) {
            table.getSelectionModel().clearSelection();
            return;
        }
        if (preferredId > 0) {
            for (AdminUserModel user : table.getItems()) {
                if (user.getId() == preferredId) {
                    table.getSelectionModel().select(user);
                    table.scrollTo(user);
                    return;
                }
            }
        }
        table.getSelectionModel().selectFirst();
    }

    private AdminUserModel findUserById(long userId) {
        if (userId <= 0) {
            return null;
        }
        for (AdminUserModel user : cachedUsers) {
            if (user.getId() == userId) {
                return user;
            }
        }
        return null;
    }

    private void applyAdminEventFilter() {
        String filter = normalizeText(adminEventFilterField.getText());
        List<EventModel> filtered = cachedEvents.stream()
                .filter(event -> filter.isEmpty() || normalizeText(event.getNombre()).contains(filter))
                .collect(Collectors.toList());
        adminEventsTable.setItems(FXCollections.observableArrayList(filtered));
        if (!filtered.isEmpty() && adminEventsTable.getSelectionModel().getSelectedItem() == null) {
            adminEventsTable.getSelectionModel().selectFirst();
        }
    }

    private void updateAdminEventSummary(EventModel event) {
        if (event == null) {
            adminEventsSummaryLabel.setText("Selecciona un evento para ver su disponibilidad");
            return;
        }

        int available = availableTickets(event);
        int sold = soldTickets(event);
        adminEventsSummaryLabel.setText(
                "Evento: " + event.getNombre()
                        + " | Capacidad total: " + event.getCapacidad()
                        + " | Disponibles: " + available
                        + " | Vendidos estimados: " + sold);
    }

    private void updateClientPurchaseOptions(EventModel event) {
        if (event == null) {
            ticketTypeCombo.setItems(FXCollections.emptyObservableList());
            updateQuantityLimit(null);
            return;
        }

        ticketTypeCombo.setItems(FXCollections.observableArrayList(event.getTicketTypes()));
        if (!event.getTicketTypes().isEmpty()) {
            ticketTypeCombo.getSelectionModel().selectFirst();
        } else {
            updateQuantityLimit(null);
        }
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

    private void applyUserFilter() {
        String filter = normalizeText(userSearchField.getText());
        List<AdminUserModel> filtered = cachedUsers.stream()
                .filter(user -> filter.isEmpty()
                        || normalizeText(user.getUsername()).contains(filter)
                        || normalizeText(user.getNombre()).contains(filter))
                .collect(Collectors.toList());

        usersTable.setItems(FXCollections.observableArrayList(filtered));
        usersListSummaryLabel.setText("Usuarios visibles: " + filtered.size() + " / " + cachedUsers.size());
        if (!filtered.isEmpty() && usersTable.getSelectionModel().getSelectedItem() == null) {
            usersTable.getSelectionModel().selectFirst();
        }
    }

    private void openUserHistory(AdminUserModel user) {
        if (user == null) {
            return;
        }
        selectedAdminUser = user;
        selectedUserLabel.setText("Historial de " + user.getNombre() + " (" + user.getUsername() + ")");
        selectedUserMetaLabel.setText("Estado: " + (user.isBaneado() ? "BANEADO" : "ACTIVO")
                + " | Tickets: " + user.getHistorial().size()
                + " | Gasto estimado: " + formatCurrency(sumHistoryAmount(user.getHistorial())));
        historyTable.setItems(FXCollections.observableArrayList(user.getHistorial()));
        showAdminPane(userHistoryPane, usersNavButton);
    }

    private void refreshSelectedUser() {
        if (selectedAdminUser == null) {
            return;
        }

        for (AdminUserModel user : cachedUsers) {
            if (user.getId() == selectedAdminUser.getId()) {
                selectedAdminUser = user;
                openUserHistory(user);
                return;
            }
        }
        selectedAdminUser = null;
        historyTable.setItems(FXCollections.emptyObservableList());
        selectedUserLabel.setText("Historial de compras");
        selectedUserMetaLabel.setText("Selecciona un usuario desde la lista");
    }

    private void toggleUserBan(AdminUserModel user) {
        if (user == null || "ADMIN".equalsIgnoreCase(user.getRol())) {
            return;
        }
        try {
            adminUserService.updateBanStatus(user.getId(), !user.isBaneado());
            loadEvents();
            loadAdminUsers();
            AlertUtil.info("Usuarios", user.isBaneado()
                    ? "Ban retirado correctamente"
                    : "Usuario baneado correctamente");
        } catch (IllegalArgumentException e) {
            AlertUtil.error("Usuarios", e.getMessage());
        } catch (Exception e) {
            AlertUtil.error("Usuarios", "No se pudo actualizar el estado del usuario");
        }
    }

    private void rebuildSalesData() {
        cachedSales.clear();
        for (AdminUserModel user : cachedUsers) {
            for (UserTicketHistoryModel history : user.getHistorial()) {
                cachedSales.add(new AdminTicketSaleModel(
                        history.getTicketId(),
                        history.getEventId(),
                        user.getUsername(),
                        user.getNombre(),
                        history.getEventName(),
                        history.getEventDate(),
                        history.getSeatType(),
                        history.getSeatNumber(),
                        parsePrice(history.getPrice())));
            }
        }

        cachedSales.sort(Comparator.comparingLong(AdminTicketSaleModel::getTicketId).reversed());
        eventSalesTable.setItems(FXCollections.observableArrayList(buildAggregateByEvent(cachedSales)));
        if (!eventSalesTable.getItems().isEmpty()) {
            SalesAggregateModel selected = eventSalesTable.getItems().stream()
                    .filter(item -> item.getId() == selectedSalesEventId)
                    .findFirst()
                    .orElse(eventSalesTable.getItems().get(0));
            eventSalesTable.getSelectionModel().select(selected);
            selectedSalesEventId = selected.getId();
        }
        refreshSalesView();
    }

    private void refreshSalesView() {
        List<AdminTicketSaleModel> selectedSales = selectedSalesEventId == GENERAL_SALES_ID
                ? new ArrayList<AdminTicketSaleModel>(cachedSales)
                : cachedSales.stream()
                        .filter(sale -> sale.getEventId() == selectedSalesEventId)
                        .collect(Collectors.toList());

        String filter = normalizeText(salesSearchField.getText());
        List<AdminTicketSaleModel> filtered = selectedSales.stream()
                .filter(sale -> filter.isEmpty()
                        || String.valueOf(sale.getTicketId()).contains(filter)
                        || normalizeText(sale.getUsername()).contains(filter)
                        || normalizeText(sale.getFullName()).contains(filter)
                        || normalizeText(sale.getEventName()).contains(filter))
                .collect(Collectors.toList());

        soldTicketsTable.setItems(FXCollections.observableArrayList(filtered));
        salesSummaryLabel.setText("Tickets vendidos: " + selectedSales.size());
        salesUsersLabel.setText("Usuarios compradores: " + distinctBuyerCount(selectedSales));
        salesRevenueLabel.setText("Ingresos estimados: " + formatCurrency(totalRevenue(selectedSales)));
        seatTypeSalesTable.setItems(FXCollections.observableArrayList(buildAggregateBySeatType(selectedSales)));
    }

    private List<SalesAggregateModel> buildAggregateByEvent(List<AdminTicketSaleModel> sales) {
        Map<Long, AggregateCounter> counters = new LinkedHashMap<Long, AggregateCounter>();
        Map<Long, String> labels = new LinkedHashMap<Long, String>();
        for (AdminTicketSaleModel sale : sales) {
            counters.computeIfAbsent(sale.getEventId(), key -> new AggregateCounter())
                    .add(sale.getPrice());
            labels.putIfAbsent(sale.getEventId(), sale.getEventName());
        }
        List<SalesAggregateModel> items = counters.entrySet().stream()
                .map(entry -> new SalesAggregateModel(
                        entry.getKey(),
                        labels.get(entry.getKey()),
                        entry.getValue().count,
                        entry.getValue().revenue))
                .sorted(Comparator.comparingInt(SalesAggregateModel::getTicketsSold).reversed())
                .collect(Collectors.toList());
        items.add(0, new SalesAggregateModel(
                GENERAL_SALES_ID,
                "General",
                sales.size(),
                totalRevenue(sales)));
        return items;
    }

    private List<SalesAggregateModel> buildAggregateBySeatType(List<AdminTicketSaleModel> sales) {
        Map<String, AggregateCounter> counters = new LinkedHashMap<String, AggregateCounter>();
        for (AdminTicketSaleModel sale : sales) {
            counters.computeIfAbsent(sale.getSeatType(), key -> new AggregateCounter())
                    .add(sale.getPrice());
        }
        return counters.entrySet().stream()
                .map(entry -> new SalesAggregateModel(-1L, entry.getKey(), entry.getValue().count,
                        entry.getValue().revenue))
                .sorted(Comparator.comparingInt(SalesAggregateModel::getTicketsSold).reversed())
                .collect(Collectors.toList());
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
        String status = stringValue(health, "status", "UNKNOWN");
        int inService = intValue(health, "in_service_backends");
        int total = intValue(health, "registered_backends");
        long errors = longValue(metrics, "errors_total");
        long requests = longValue(metrics, "requests_total");
        long avgLatency = longValue(metrics, "avg_latency_ms");

        monitorStatusLabel.setText("Estado sistema: " + status);
        monitorBackendsLabel.setText("Backends en balanceo: " + inService + " / registrados: " + total);
        monitorErrorsLabel.setText("Errores acumulados: " + errors + " sobre " + requests + " requests");
        monitorLatencyLabel.setText("Latencia promedio: " + avgLatency + " ms");
        monitorLastUpdateLabel.setText("Actualizacion automatica: " + LocalDateTime.now().format(TIME_FORMATTER));

        backendTable.setItems(FXCollections.observableArrayList(parseBackendStatuses(health)));
    }

    private void applyMonitorDown() {
        monitorStatusLabel.setText("Estado sistema: DOWN");
        monitorBackendsLabel.setText("Backends en balanceo: 0 / registrados: 0");
        monitorErrorsLabel.setText("Errores acumulados: no disponible");
        monitorLatencyLabel.setText("Latencia promedio: no disponible");
        monitorLastUpdateLabel.setText("Actualizacion automatica: " + LocalDateTime.now().format(TIME_FORMATTER));
        backendTable.setItems(FXCollections.emptyObservableList());
    }

    private List<BackendStatusModel> parseBackendStatuses(JsonObject health) {
        List<BackendStatusModel> result = new ArrayList<BackendStatusModel>();
        if (!health.has("backend_status") || !health.get("backend_status").isJsonArray()) {
            return result;
        }

        JsonArray backendStatus = health.getAsJsonArray("backend_status");
        for (JsonElement element : backendStatus) {
            JsonObject item = element.getAsJsonObject();
            JsonObject lastMetrics = item.has("last_metrics") && item.get("last_metrics").isJsonObject()
                    ? item.getAsJsonObject("last_metrics")
                    : new JsonObject();
            JsonObject database = lastMetrics.has("database") && lastMetrics.get("database").isJsonObject()
                    ? lastMetrics.getAsJsonObject("database")
                    : new JsonObject();

            String diskStatus = "n/d";
            if (lastMetrics.has("disk_usable_bytes")) {
                diskStatus = formatBytes(lastMetrics.get("disk_usable_bytes").getAsLong()) + " libres";
            }

            result.add(new BackendStatusModel(
                    stringValue(item, "backend", "-"),
                    stringValue(item, "status", "UNKNOWN"),
                    booleanValue(item, "in_balancing"),
                    booleanValue(item, "in_verification"),
                    intValue(item, "fail_count"),
                    emptyToDash(stringValue(item, "last_error", "")),
                    database.has("up") && database.get("up").getAsBoolean() ? "UP" : "DOWN",
                    diskStatus,
                    formatEpochMillis(longValue(item, "last_check_ms"))));
        }
        return result;
    }

    private void showAdminPane(VBox pane, Button activeButton) {
        setPaneVisible(dashboardPane, pane == dashboardPane);
        setPaneVisible(adminEventsPane, pane == adminEventsPane);
        setPaneVisible(usersListPane, pane == usersListPane);
        setPaneVisible(userHistoryPane, pane == userHistoryPane);
        setPaneVisible(salesPane, pane == salesPane);

        updateNavState(dashboardNavButton, activeButton == dashboardNavButton);
        updateNavState(eventsNavButton, activeButton == eventsNavButton);
        updateNavState(usersNavButton, activeButton == usersNavButton || pane == userHistoryPane);
        updateNavState(salesNavButton, activeButton == salesNavButton);
    }

    private void setPaneVisible(VBox pane, boolean visible) {
        pane.setVisible(visible);
        pane.setManaged(visible);
    }

    private void updateNavState(Button button, boolean active) {
        button.getStyleClass().remove("nav-button-active");
        if (active) {
            button.getStyleClass().add("nav-button-active");
        }
    }

    private void selectEventById(TableView<EventModel> table, long eventId) {
        for (EventModel event : table.getItems()) {
            if (event.getId() == eventId) {
                table.getSelectionModel().select(event);
                table.scrollTo(event);
                return;
            }
        }
    }

    private int availableTickets(EventModel event) {
        int total = 0;
        for (TicketTypeModel type : event.getTicketTypes()) {
            total += Math.max(type.getCantidad(), 0);
        }
        return total;
    }

    private int soldTickets(EventModel event) {
        return Math.max(0, event.getCapacidad() - availableTickets(event));
    }

    private BigDecimal sumHistoryAmount(List<UserTicketHistoryModel> history) {
        BigDecimal total = BigDecimal.ZERO;
        for (UserTicketHistoryModel item : history) {
            total = total.add(parsePrice(item.getPrice()));
        }
        return total;
    }

    private BigDecimal totalRevenue(List<AdminTicketSaleModel> sales) {
        BigDecimal total = BigDecimal.ZERO;
        for (AdminTicketSaleModel sale : sales) {
            total = total.add(sale.getPrice());
        }
        return total;
    }

    private int distinctBuyerCount(List<AdminTicketSaleModel> sales) {
        return (int) sales.stream()
                .map(AdminTicketSaleModel::getUsername)
                .distinct()
                .count();
    }

    private BigDecimal parsePrice(String raw) {
        try {
            return new BigDecimal(raw == null ? "0" : raw.trim());
        } catch (Exception ignored) {
            return BigDecimal.ZERO;
        }
    }

    private String formatCurrency(BigDecimal value) {
        return "Bs " + value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatBytes(long bytes) {
        double gb = bytes / 1024d / 1024d / 1024d;
        return String.format(Locale.US, "%.1f GB", gb);
    }

    private String formatEpochMillis(long epochMillis) {
        if (epochMillis <= 0) {
            return "-";
        }
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
        return dateTime.format(DateTimeFormatter.ofPattern("dd/MM HH:mm:ss"));
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String boolText(boolean value) {
        return value ? "SI" : "NO";
    }

    private String emptyToDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private String stringValue(JsonObject object, String key, String fallback) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : fallback;
    }

    private int intValue(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsInt() : 0;
    }

    private long longValue(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsLong() : 0L;
    }

    private boolean booleanValue(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() && object.get(key).getAsBoolean();
    }

    private boolean isAdmin() {
        UserModel user = SessionManager.getCurrentUser();
        return user != null && "ADMIN".equalsIgnoreCase(user.getRol());
    }

    private boolean isBuyer(UserModel user) {
        if (user == null) {
            return false;
        }
        return "CLIENTE".equalsIgnoreCase(user.getRol())
                || "FRECUENTE".equalsIgnoreCase(user.getRol())
                || "VIP".equalsIgnoreCase(user.getRol());
    }

    private static class AggregateCounter {
        private int count;
        private BigDecimal revenue = BigDecimal.ZERO;

        private void add(BigDecimal price) {
            count++;
            revenue = revenue.add(price == null ? BigDecimal.ZERO : price);
        }
    }
}
