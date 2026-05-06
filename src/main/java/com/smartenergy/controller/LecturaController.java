package com.smartenergy.controller;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import com.smartenergy.model.Dispositivo;
import com.smartenergy.model.Lectura;
import com.smartenergy.service.DispositivoService;
import com.smartenergy.service.LecturaService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public class LecturaController implements Initializable {

    private static final double PRICE_PER_KWH = 0.18;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final LecturaService lecturaService = new LecturaService();
    private final DispositivoService dispositivoService = new DispositivoService();

    @FXML
    private ComboBox<FilterOption> dispositivoFilterCombo;

    @FXML
    private DatePicker desdeDatePicker;

    @FXML
    private DatePicker hastaDatePicker;

    @FXML
    private TableView<Lectura> lecturasTable;

    @FXML
    private TableColumn<Lectura, String> fechaHoraColumn;

    @FXML
    private TableColumn<Lectura, String> dispositivoColumn;

    @FXML
    private TableColumn<Lectura, String> consumoColumn;

    @FXML
    private TableColumn<Lectura, String> costeColumn;

    @FXML
    private Label totalKwhLabel;

    @FXML
    private Label totalCosteLabel;

    private List<Lectura> allLecturas = new ArrayList<>();
    private List<Dispositivo> dispositivosCache = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeColumns();
        initializeFilters();
        refreshData();
    }

    @FXML
    private void onFiltrar() {
        applyFilters();
    }

    @FXML
    private void onLimpiarFiltros() {
        dispositivoFilterCombo.getSelectionModel().selectFirst();
        desdeDatePicker.setValue(null);
        hastaDatePicker.setValue(null);
        applyFilters();
    }

    @FXML
    private void onNuevaLectura() {
        Optional<Lectura> lectura = showNuevaLecturaDialog();
        lectura.ifPresent(item -> {
            runAsync(() -> {
                lecturaService.save(item);
                return fetchSnapshot();
            }, this::applySnapshot);
        });
    }

    private void initializeColumns() {
        fechaHoraColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getFechaHora() == null ? "-" : cell.getValue().getFechaHora().format(DATE_TIME_FORMATTER)));
        dispositivoColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getDispositivo() == null ? "-" : valueOrDash(cell.getValue().getDispositivo().getNombre())));
        consumoColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                String.format("%.3f", safeValue(cell.getValue().getConsumoKwh()))));
        costeColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                String.format("%.2f", safeValue(cell.getValue().getCosteEuros()))));
    }

    private void initializeFilters() {
        dispositivoFilterCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        desdeDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        hastaDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
    }

    private void refreshData() {
        runAsync(this::fetchSnapshot, this::applySnapshot);
    }

    private void loadFilterOptions() {
        FilterOption current = dispositivoFilterCombo.getValue();
        List<FilterOption> options = new ArrayList<>();
        options.add(new FilterOption(null, "Todos"));
        for (Dispositivo dispositivo : dispositivosCache) {
            options.add(new FilterOption(dispositivo.getId(), dispositivo.getNombre()));
        }
        dispositivoFilterCombo.setItems(FXCollections.observableArrayList(options));
        if (current != null) {
            for (FilterOption option : options) {
                if ((option.dispositivoId() == null && current.dispositivoId() == null)
                        || (option.dispositivoId() != null && option.dispositivoId().equals(current.dispositivoId()))) {
                    dispositivoFilterCombo.setValue(option);
                    return;
                }
            }
        }
        dispositivoFilterCombo.getSelectionModel().selectFirst();
    }

    private void applyFilters() {
        Long dispositivoId = dispositivoFilterCombo.getValue() == null ? null : dispositivoFilterCombo.getValue().dispositivoId();
        LocalDate desde = desdeDatePicker.getValue();
        LocalDate hasta = hastaDatePicker.getValue();

        List<Lectura> filtered = allLecturas.stream()
                .filter(lectura -> dispositivoId == null
                        || (lectura.getDispositivo() != null && dispositivoId.equals(lectura.getDispositivo().getId())))
                .filter(lectura -> isInDateRange(lectura.getFechaHora(), desde, hasta))
                .toList();

        lecturasTable.setItems(FXCollections.observableArrayList(filtered));
        updateTotals(filtered);
    }

    private Optional<Lectura> showNuevaLecturaDialog() {
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle("Nueva lectura");
        dialog.setHeaderText("Registrar lectura de consumo");

        ButtonType guardarButton = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getButtonTypes().setAll(guardarButton, ButtonType.CANCEL);

        ComboBox<Dispositivo> dispositivoCombo = new ComboBox<>(FXCollections.observableArrayList(dispositivosCache));
        dispositivoCombo.setCellFactory(listView -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Dispositivo item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getNombre());
            }
        });
        dispositivoCombo.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Dispositivo item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getNombre());
            }
        });
        if (!dispositivoCombo.getItems().isEmpty()) {
            dispositivoCombo.getSelectionModel().selectFirst();
        }

        TextField consumoField = new TextField();
        consumoField.setPromptText("kWh");
        DatePicker fechaPicker = new DatePicker(LocalDate.now());
        TextField horaField = new TextField(LocalTime.now().withSecond(0).withNano(0).toString());
        horaField.setPromptText("HH:mm");
        Label costeCalculadoLabel = new Label("Coste estimado: 0.00 EUR");

        consumoField.textProperty().addListener((obs, oldValue, newValue) -> {
            double consumo = parseDoubleOrDefault(newValue, 0.0);
            costeCalculadoLabel.setText(String.format("Coste estimado: %.2f EUR", consumo * PRICE_PER_KWH));
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Dispositivo:"), 0, 0);
        grid.add(dispositivoCombo, 1, 0);
        grid.add(new Label("Consumo (kWh):"), 0, 1);
        grid.add(consumoField, 1, 1);
        grid.add(new Label("Fecha:"), 0, 2);
        grid.add(fechaPicker, 1, 2);
        grid.add(new Label("Hora (HH:mm):"), 0, 3);
        grid.add(horaField, 1, 3);
        grid.add(costeCalculadoLabel, 1, 4);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != guardarButton) {
            return Optional.empty();
        }

        Dispositivo dispositivo = dispositivoCombo.getValue();
        if (dispositivo == null) {
            showValidationError("Debes seleccionar un dispositivo.");
            return Optional.empty();
        }

        String consumoRaw = consumoField.getText() == null ? "" : consumoField.getText().trim();
        String horaRaw = horaField.getText() == null ? "" : horaField.getText().trim();
        LocalDate fecha = fechaPicker.getValue();
        if (consumoRaw.isBlank() || horaRaw.isBlank() || fecha == null) {
            showValidationError("Ningun campo puede quedar vacio.");
            return Optional.empty();
        }

        double consumo = parseDoubleOrDefault(consumoRaw, -1);
        if (consumo <= 0) {
            showValidationError("El consumo kWh debe ser un numero positivo.");
            return Optional.empty();
        }

        LocalTime hora;
        try {
            hora = LocalTime.parse(horaRaw);
        } catch (Exception ex) {
            showValidationError("La hora debe tener formato HH:mm.");
            return Optional.empty();
        }

        LocalDateTime fechaHora = LocalDateTime.of(fecha, hora);
        if (fechaHora.isAfter(LocalDateTime.now())) {
            showValidationError("La fecha de la lectura no puede ser futura.");
            return Optional.empty();
        }

        Lectura lectura = new Lectura();
        lectura.setDispositivo(dispositivo);
        lectura.setConsumoKwh(consumo);
        lectura.setFechaHora(fechaHora);
        lectura.setCosteEuros(round2(consumo * PRICE_PER_KWH));
        return Optional.of(lectura);
    }

    private void updateTotals(List<Lectura> lecturas) {
        double totalKwh = lecturas.stream().mapToDouble(l -> safeValue(l.getConsumoKwh())).sum();
        double totalCoste = lecturas.stream().mapToDouble(l -> safeValue(l.getCosteEuros())).sum();
        totalKwhLabel.setText(String.format("Total kWh: %.3f", totalKwh));
        totalCosteLabel.setText(String.format("Total coste: %.2f EUR", totalCoste));
    }

    private boolean isInDateRange(LocalDateTime fechaHora, LocalDate desde, LocalDate hasta) {
        if (fechaHora == null) {
            return false;
        }
        LocalDate date = fechaHora.toLocalDate();
        boolean afterDesde = desde == null || !date.isBefore(desde);
        boolean beforeHasta = hasta == null || !date.isAfter(hasta);
        return afterDesde && beforeHasta;
    }

    private void showValidationError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Validacion de formulario");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showPersistenceError() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Error de persistencia");
        alert.setContentText("Ha ocurrido un error al acceder a la base de datos. Intentalo de nuevo.");
        alert.showAndWait();
    }

    private double parseDoubleOrDefault(String value, double fallback) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception ex) {
            return fallback;
        }
    }

    private double safeValue(Double value) {
        return value == null ? 0.0 : value;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private Snapshot fetchSnapshot() {
        return new Snapshot(dispositivoService.findAll(), lecturaService.findAll());
    }

    private void applySnapshot(Snapshot snapshot) {
        dispositivosCache = snapshot.dispositivos();
        allLecturas = snapshot.lecturas();
        loadFilterOptions();
        applyFilters();
    }

    private <T> void runAsync(Callable<T> task, java.util.function.Consumer<T> onSuccess) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }).thenAccept(result -> Platform.runLater(() -> onSuccess.accept(result)))
                .exceptionally(ex -> {
                    Platform.runLater(this::showPersistenceError);
                    return null;
                });
    }

    private record FilterOption(Long dispositivoId, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private record Snapshot(List<Dispositivo> dispositivos, List<Lectura> lecturas) {
    }
}
