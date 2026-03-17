package edu.upb.desktop.controller;

import edu.upb.desktop.model.EventModel;
import edu.upb.desktop.model.EventTicketTypeDraftModel;
import edu.upb.desktop.service.EventService;
import edu.upb.desktop.util.AlertUtil;
import edu.upb.desktop.util.SpinnerUtil;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EventWizardController {
    @FXML
    private VBox stepOnePane;

    @FXML
    private VBox stepTwoPane;

    @FXML
    private TextField eventNameField;

    @FXML
    private DatePicker eventDatePicker;

    @FXML
    private Spinner<Integer> eventHourSpinner;

    @FXML
    private Spinner<Integer> eventMinuteSpinner;

    @FXML
    private Spinner<Integer> eventCapacitySpinner;

    @FXML
    private CheckBox frequentDiscountCheckBox;

    @FXML
    private Label wizardSummaryLabel;

    @FXML
    private Label remainingCapacityLabel;

    @FXML
    private TextField seatTypeField;

    @FXML
    private Spinner<Integer> ticketTypeQuantitySpinner;

    @FXML
    private TextField ticketTypePriceField;

    @FXML
    private TableView<EventTicketTypeDraftModel> ticketTypesTable;

    @FXML
    private TableColumn<EventTicketTypeDraftModel, String> seatTypeColumn;

    @FXML
    private TableColumn<EventTicketTypeDraftModel, Number> quantityColumn;

    @FXML
    private TableColumn<EventTicketTypeDraftModel, BigDecimal> priceColumn;

    @FXML
    private Button finishButton;

    private final EventService eventService = new EventService();
    private final List<EventTicketTypeDraftModel> ticketTypes = new ArrayList<EventTicketTypeDraftModel>();
    private Consumer<EventModel> onEventCreated;

    private String eventName;
    private String eventDateText;
    private int eventCapacity;
    private boolean descuentoFrecuente;

    @FXML
    private void initialize() {
        eventDatePicker.setValue(LocalDate.now());
        eventHourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 20));
        eventMinuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        eventCapacitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50000, 100));
        ticketTypeQuantitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1, 1));
        SpinnerUtil.makeEditableInteger(eventHourSpinner);
        SpinnerUtil.makeEditableInteger(eventMinuteSpinner);
        SpinnerUtil.makeEditableInteger(eventCapacitySpinner);
        SpinnerUtil.makeEditableInteger(ticketTypeQuantitySpinner);

        seatTypeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSeatType()));
        quantityColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getQuantity()));
        priceColumn.setCellValueFactory(data -> new SimpleObjectProperty<BigDecimal>(data.getValue().getPrice()));

        stepOnePane.setManaged(true);
        stepOnePane.setVisible(true);
        stepTwoPane.setManaged(false);
        stepTwoPane.setVisible(false);
        finishButton.setDisable(true);

        refreshDraftTable();
    }

    public void setOnEventCreated(Consumer<EventModel> onEventCreated) {
        this.onEventCreated = onEventCreated;
    }

    @FXML
    private void onNextStep() {
        String nombre = eventNameField.getText().trim();
        LocalDate selectedDate = eventDatePicker.getValue();
        if (nombre.isEmpty()) {
            AlertUtil.error("Evento", "Debes ingresar el nombre del evento");
            return;
        }
        if (selectedDate == null) {
            AlertUtil.error("Evento", "Debes seleccionar una fecha");
            return;
        }

        LocalDateTime dateTime = selectedDate.atTime(eventHourSpinner.getValue(), eventMinuteSpinner.getValue(), 0);
        if (!dateTime.isAfter(LocalDateTime.now())) {
            AlertUtil.error("Evento", "La fecha del evento debe ser posterior al momento actual");
            return;
        }
        eventName = nombre;
        eventDateText = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        eventCapacity = eventCapacitySpinner.getValue();
        descuentoFrecuente = frequentDiscountCheckBox.isSelected();

        wizardSummaryLabel.setText("Evento: " + eventName + " | Fecha: " + eventDateText + " | Capacidad: " + eventCapacity);
        stepOnePane.setManaged(false);
        stepOnePane.setVisible(false);
        stepTwoPane.setManaged(true);
        stepTwoPane.setVisible(true);
        updateRemainingCapacity();
    }

    @FXML
    private void onBackStep() {
        stepTwoPane.setManaged(false);
        stepTwoPane.setVisible(false);
        stepOnePane.setManaged(true);
        stepOnePane.setVisible(true);
    }

    @FXML
    private void onAddTicketType() {
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
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            AlertUtil.error("Tipo de ticket", "El precio debe ser mayor a 0");
            return;
        }

        int remaining = getRemainingCapacity();
        if (quantity <= 0 || quantity > remaining) {
            AlertUtil.error("Tipo de ticket", "La cantidad no puede superar la capacidad restante");
            return;
        }

        ticketTypes.add(new EventTicketTypeDraftModel(seatType, quantity, price));
        seatTypeField.clear();
        ticketTypePriceField.clear();
        refreshDraftTable();
        updateRemainingCapacity();
    }

    @FXML
    private void onRemoveTicketType() {
        EventTicketTypeDraftModel selected = ticketTypesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.error("Tipo de ticket", "Selecciona un tipo para quitarlo");
            return;
        }
        ticketTypes.remove(selected);
        refreshDraftTable();
        updateRemainingCapacity();
    }

    @FXML
    private void onFinish() {
        if (getRemainingCapacity() != 0) {
            AlertUtil.error("Evento", "Debes asignar toda la capacidad antes de guardar");
            return;
        }

        try {
            EventModel created = eventService.createFullEvent(
                    eventName,
                    eventDateText,
                    eventCapacity,
                    descuentoFrecuente,
                    ticketTypes);
            if (onEventCreated != null) {
                onEventCreated.accept(created);
            }
            closeWindow();
            AlertUtil.info("Evento", "Evento y asientos creados correctamente");
        } catch (IllegalArgumentException e) {
            AlertUtil.error("Evento", e.getMessage());
        } catch (Exception e) {
            AlertUtil.error("Evento", "No se pudo guardar el evento");
        }
    }

    @FXML
    private void onCancel() {
        closeWindow();
    }

    private void refreshDraftTable() {
        ticketTypesTable.setItems(FXCollections.observableArrayList(ticketTypes));
    }

    private void updateRemainingCapacity() {
        int remaining = getRemainingCapacity();
        remainingCapacityLabel.setText("Capacidad restante por asignar: " + remaining);
        int spinnerMax = Math.max(1, remaining);
        int spinnerValue = Math.min(Math.max(1, ticketTypeQuantitySpinner.getValue()), spinnerMax);
        ticketTypeQuantitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, spinnerMax, spinnerValue));
        ticketTypeQuantitySpinner.setDisable(remaining <= 0);
        finishButton.setDisable(remaining != 0 || ticketTypes.isEmpty());
    }

    private int getRemainingCapacity() {
        int assigned = 0;
        for (EventTicketTypeDraftModel ticketType : ticketTypes) {
            assigned += ticketType.getQuantity();
        }
        return Math.max(0, eventCapacity - assigned);
    }

    private void closeWindow() {
        Stage stage = (Stage) finishButton.getScene().getWindow();
        stage.close();
    }
}
