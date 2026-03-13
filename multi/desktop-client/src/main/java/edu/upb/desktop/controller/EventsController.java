package edu.upb.desktop.controller;

import com.google.gson.JsonObject;
import edu.upb.desktop.app.DesktopApp;
import edu.upb.desktop.model.EventModel;
import edu.upb.desktop.model.PurchaseResultModel;
import edu.upb.desktop.model.TicketTypeModel;
import edu.upb.desktop.model.UserModel;
import edu.upb.desktop.service.EventService;
import edu.upb.desktop.service.MonitorService;
import edu.upb.desktop.service.TicketGrpcClient;
import edu.upb.desktop.util.AlertUtil;
import edu.upb.desktop.util.SessionManager;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class EventsController {
    @FXML
    private Label welcomeLabel;

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
    private TextField eventNameField;

    @FXML
    private DatePicker eventDatePicker;

    @FXML
    private Spinner<Integer> eventCapacitySpinner;

    @FXML
    private Spinner<Integer> eventHourSpinner;

    @FXML
    private Spinner<Integer> eventMinuteSpinner;

    @FXML
    private Label selectedEventLabel;

    @FXML
    private Label remainingCapacityLabel;

    @FXML
    private TextField seatTypeField;

    @FXML
    private Spinner<Integer> ticketTypeQuantitySpinner;

    @FXML
    private TextField ticketTypePriceField;

    @FXML
    private HBox adminPanel;

    @FXML
    private VBox purchasePanel;

    @FXML
    private Label monitorStatusLabel;

    @FXML
    private Label monitorBackendsLabel;

    @FXML
    private Label monitorErrorsLabel;

    @FXML
    private Label monitorLastUpdateLabel;

    private final EventService eventService = new EventService();
    private final MonitorService monitorService = new MonitorService();
    private final TicketGrpcClient ticketGrpcClient = new TicketGrpcClient();

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().getId()));
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNombre()));
        dateColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFecha()));
        capacityColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getCapacidad()));
        quantitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1, 1));
        quantitySpinner.setDisable(true);
        eventCapacitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50000, 100));
        eventHourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 20));
        eventMinuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        ticketTypeQuantitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 0, 0));
        ticketTypeQuantitySpinner.setDisable(true);
        eventDatePicker.setValue(LocalDate.now());

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
            updateEventSelection(selected);
        });

        ticketTypeCombo.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) ->
                updateQuantityLimit(selected));
    }

    public void loadInitialData() {
        UserModel user = SessionManager.getCurrentUser();
        if (user != null) {
            welcomeLabel.setText("Bienvenido, " + user.getNombre() + " (" + user.getRol() + ")");
        }

        boolean isAdmin = user != null && "ADMIN".equalsIgnoreCase(user.getRol());
        adminPanel.setVisible(isAdmin);
        adminPanel.setManaged(isAdmin);
        purchasePanel.setVisible(!isAdmin);
        purchasePanel.setManaged(!isAdmin);

        if (isAdmin) {
            refreshMonitorPanel();
        }

        try {
            List<EventModel> events = eventService.fetchEvents();
            eventsTable.setItems(FXCollections.observableArrayList(events));
            if (!events.isEmpty()) {
                eventsTable.getSelectionModel().selectFirst();
            } else {
                ticketTypeCombo.setItems(FXCollections.emptyObservableList());
                updateQuantityLimit(null);
                updateEventSelection(null);
            }
        } catch (Exception e) {
            AlertUtil.error("Eventos", "No se pudieron cargar los eventos");
        }
    }

    @FXML
    private void onCreateEvent() {
        if (!isAdmin()) {
            AlertUtil.error("Permisos", "Solo ADMIN puede crear eventos");
            return;
        }
        try {
            String nombre = eventNameField.getText().trim();
            int capacidad = eventCapacitySpinner.getValue();
            LocalDate selectedDate = eventDatePicker.getValue();
            if (selectedDate == null) {
                AlertUtil.error("Evento", "Debes seleccionar una fecha");
                return;
            }
            LocalDateTime dateTime = selectedDate.atTime(eventHourSpinner.getValue(), eventMinuteSpinner.getValue(), 0);
            String fecha = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            EventModel created = eventService.createEvent(nombre, fecha, capacidad);
            AlertUtil.info("Evento", "Evento creado. Ahora agrega sus tipos de ticket.");
            eventNameField.clear();
            eventDatePicker.setValue(LocalDate.now());

            loadInitialData();
            selectEventById(created.getId());
        } catch (IllegalArgumentException e) {
            AlertUtil.error("Evento", e.getMessage());
        } catch (Exception e) {
            AlertUtil.error("Evento", "No se pudo crear el evento");
        }
    }

    @FXML
    private void onAddTicketType() {
        if (!isAdmin()) {
            AlertUtil.error("Permisos", "Solo ADMIN puede agregar tipos de ticket");
            return;
        }
        EventModel event = eventsTable.getSelectionModel().getSelectedItem();
        if (event == null) {
            AlertUtil.error("Tipo de ticket", "Primero crea o selecciona un evento");
            return;
        }

        int remaining = event.getRemainingCapacity();
        if (remaining <= 0) {
            AlertUtil.error("Tipo de ticket", "El evento ya no tiene capacidad disponible");
            return;
        }

        String seatType = seatTypeField.getText().trim();
        int quantity = ticketTypeQuantitySpinner.getValue();
        BigDecimal price;
        try {
            price = new BigDecimal(ticketTypePriceField.getText().trim());
        } catch (Exception e) {
            AlertUtil.error("Tipo de ticket", "El precio no es valido");
            return;
        }

        if (seatType.isEmpty()) {
            AlertUtil.error("Tipo de ticket", "Debes ingresar el tipo de asiento");
            return;
        }
        if (quantity <= 0 || quantity > remaining) {
            AlertUtil.error("Tipo de ticket", "La cantidad no puede superar la capacidad restante");
            return;
        }

        try {
            eventService.addTicketType(event.getId(), seatType, quantity, price);
            AlertUtil.info("Tipo de ticket", "Tipo de ticket agregado correctamente");
            seatTypeField.clear();
            ticketTypePriceField.clear();

            loadInitialData();
            selectEventById(event.getId());
        } catch (IllegalArgumentException e) {
            AlertUtil.error("Tipo de ticket", e.getMessage());
        } catch (Exception e) {
            AlertUtil.error("Tipo de ticket", "No se pudo agregar el tipo de ticket");
        }
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
            loadInitialData();
            selectEventById(event.getId());
        } catch (Exception e) {
            String message = e.getMessage() != null ? e.getMessage() : "No se pudo realizar la compra";
            AlertUtil.error("Compra", message);
        }
    }

    @FXML
    private void onLogout() {
        try {
            SessionManager.clear();
            DesktopApp.getInstance().showLogin();
        } catch (Exception e) {
            AlertUtil.error("Sesion", "No se pudo cerrar sesion");
        }
    }

    @FXML
    private void onRefreshMonitor() {
        refreshMonitorPanel();
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

    private void updateEventSelection(EventModel selected) {
        if (selected == null) {
            selectedEventLabel.setText("Evento seleccionado: ninguno");
            remainingCapacityLabel.setText("Capacidad restante: 0");
            ticketTypeQuantitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 0, 0));
            ticketTypeQuantitySpinner.setDisable(true);
            return;
        }

        int remaining = selected.getRemainingCapacity();
        selectedEventLabel.setText("Evento seleccionado: " + selected.getNombre() + " (ID " + selected.getId() + ")");
        remainingCapacityLabel.setText("Capacidad restante: " + remaining);

        if (remaining <= 0) {
            ticketTypeQuantitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 0, 0));
            ticketTypeQuantitySpinner.setDisable(true);
            return;
        }

        int current = ticketTypeQuantitySpinner.getValueFactory() != null ? ticketTypeQuantitySpinner.getValue() : 1;
        int safeValue = Math.min(Math.max(current, 1), remaining);
        ticketTypeQuantitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, remaining, safeValue));
        ticketTypeQuantitySpinner.setDisable(false);
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

    private void refreshMonitorPanel() {
        try {
            JsonObject health = monitorService.fetchHealth();
            JsonObject metrics = monitorService.fetchMetrics();

            String status = health.has("status") ? health.get("status").getAsString() : "UNKNOWN";
            int inService = health.has("in_service_backends") ? health.get("in_service_backends").getAsInt() : 0;
            int total = health.has("registered_backends") ? health.get("registered_backends").getAsInt() : 0;
            long errors = metrics.has("errors_total") ? metrics.get("errors_total").getAsLong() : 0L;
            long requests = metrics.has("requests_total") ? metrics.get("requests_total").getAsLong() : 0L;

            monitorStatusLabel.setText("Estado sistema: " + status);
            monitorBackendsLabel.setText("Backends activos: " + inService + " / " + total);
            monitorErrorsLabel.setText("Errores: " + errors + " de " + requests + " requests");
            monitorLastUpdateLabel.setText("Ultima actualizacion: " + java.time.LocalTime.now().withNano(0));
        } catch (Exception e) {
            monitorStatusLabel.setText("Estado sistema: DOWN");
            monitorBackendsLabel.setText("Backends activos: 0 / 0");
            monitorErrorsLabel.setText("Errores: no disponible");
            monitorLastUpdateLabel.setText("Ultima actualizacion: " + java.time.LocalTime.now().withNano(0));
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
