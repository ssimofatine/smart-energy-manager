package com.smartenergy.controller;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import com.smartenergy.model.Dispositivo;
import com.smartenergy.service.DataInitializer;
import com.smartenergy.service.DispositivoService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.util.ResourceBundle;

public class DispositivoController implements Initializable {

    private final DispositivoService dispositivoService = new DispositivoService();

    @FXML
    private TableView<Dispositivo> dispositivosTable;

    @FXML
    private TableColumn<Dispositivo, String> nombreColumn;

    @FXML
    private TableColumn<Dispositivo, String> tipoColumn;

    @FXML
    private TableColumn<Dispositivo, String> potenciaColumn;

    @FXML
    private TableColumn<Dispositivo, String> estadoColumn;

    @FXML
    private Label detalleNombreLabel;

    @FXML
    private Label detalleTipoLabel;

    @FXML
    private Label detallePotenciaLabel;

    @FXML
    private Label detalleEstadoLabel;

    @FXML
    private Button editarButton;

    @FXML
    private Button eliminarButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeColumns();
        initializeSelection();
        runAsync(() -> {
            new DataInitializer().initializeIfEmpty();
            return dispositivoService.findAll();
        }, dispositivos -> dispositivosTable.setItems(FXCollections.observableArrayList(dispositivos)));
        clearDetails();
    }

    @FXML
    private void onNuevo() {
        Optional<Dispositivo> nuevoDispositivo = showDispositivoDialog(null);
        nuevoDispositivo.ifPresent(dispositivo -> runAsync(() -> {
            dispositivoService.save(dispositivo);
            return dispositivoService.findAll();
        }, dispositivos -> dispositivosTable.setItems(FXCollections.observableArrayList(dispositivos))));
    }

    @FXML
    private void onEditar() {
        Dispositivo selected = dispositivosTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        Optional<Dispositivo> editado = showDispositivoDialog(selected);
        editado.ifPresent(dispositivoActualizado -> {
            dispositivoActualizado.setId(selected.getId());
            runAsync(() -> {
                dispositivoService.update(dispositivoActualizado);
                return dispositivoService.findAll();
            }, dispositivos -> {
                dispositivosTable.setItems(FXCollections.observableArrayList(dispositivos));
                reselectById(selected.getId());
            });
        });
    }

    @FXML
    private void onEliminar() {
        Dispositivo selected = dispositivosTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar eliminacion");
        confirm.setHeaderText("Eliminar dispositivo");
        confirm.setContentText("Se eliminara '" + selected.getNombre() + "'. Esta accion no se puede deshacer.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            runAsync(() -> {
                dispositivoService.delete(selected.getId());
                return dispositivoService.findAll();
            }, dispositivos -> {
                dispositivosTable.setItems(FXCollections.observableArrayList(dispositivos));
                clearDetails();
            });
        }
    }

    private void initializeColumns() {
        nombreColumn.setCellValueFactory(cell -> new SimpleStringProperty(valueOrDash(cell.getValue().getNombre())));
        tipoColumn.setCellValueFactory(cell -> new SimpleStringProperty(valueOrDash(cell.getValue().getTipo())));
        potenciaColumn.setCellValueFactory(
                cell -> new SimpleStringProperty(cell.getValue().getPotenciaNominalW() == null
                        ? "-"
                        : String.format("%.0f W", cell.getValue().getPotenciaNominalW())));
        estadoColumn.setCellValueFactory(
                cell -> new SimpleStringProperty(Boolean.TRUE.equals(cell.getValue().getActivo()) ? "Activo" : "Inactivo"));
    }

    private void initializeSelection() {
        dispositivosTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected == null) {
                clearDetails();
                return;
            }
            showDetails(selected);
        });
    }

    private void showDetails(Dispositivo dispositivo) {
        detalleNombreLabel.setText(valueOrDash(dispositivo.getNombre()));
        detalleTipoLabel.setText(valueOrDash(dispositivo.getTipo()));
        detallePotenciaLabel.setText(dispositivo.getPotenciaNominalW() == null
                ? "-"
                : String.format("%.0f W", dispositivo.getPotenciaNominalW()));
        detalleEstadoLabel.setText(Boolean.TRUE.equals(dispositivo.getActivo()) ? "Activo" : "Inactivo");
        editarButton.setDisable(false);
        eliminarButton.setDisable(false);
    }

    private void clearDetails() {
        detalleNombreLabel.setText("-");
        detalleTipoLabel.setText("-");
        detallePotenciaLabel.setText("-");
        detalleEstadoLabel.setText("-");
        editarButton.setDisable(true);
        eliminarButton.setDisable(true);
    }

    private Optional<Dispositivo> showDispositivoDialog(Dispositivo base) {
        boolean isEdit = base != null;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(isEdit ? "Editar dispositivo" : "Nuevo dispositivo");
        alert.setHeaderText(isEdit ? "Actualizar datos del dispositivo" : "Crear nuevo dispositivo");

        ButtonType guardarButton = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(guardarButton, ButtonType.CANCEL);

        TextField nombreField = new TextField(isEdit ? valueOrEmpty(base.getNombre()) : "");
        TextField tipoField = new TextField(isEdit ? valueOrEmpty(base.getTipo()) : "");
        TextField potenciaField = new TextField(
                isEdit && base.getPotenciaNominalW() != null ? String.valueOf(base.getPotenciaNominalW()) : "");
        TextField estadoField = new TextField(isEdit ? (Boolean.TRUE.equals(base.getActivo()) ? "activo" : "inactivo") : "activo");

        nombreField.setPromptText("Nombre");
        tipoField.setPromptText("Tipo");
        potenciaField.setPromptText("Potencia nominal en W");
        estadoField.setPromptText("activo o inactivo");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Nombre:"), 0, 0);
        grid.add(nombreField, 1, 0);
        grid.add(new Label("Tipo:"), 0, 1);
        grid.add(tipoField, 1, 1);
        grid.add(new Label("Potencia (W):"), 0, 2);
        grid.add(potenciaField, 1, 2);
        grid.add(new Label("Estado:"), 0, 3);
        grid.add(estadoField, 1, 3);
        alert.getDialogPane().setContent(grid);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() != guardarButton) {
            return Optional.empty();
        }

        String nombre = nombreField.getText().trim();
        String tipo = tipoField.getText().trim();
        String potenciaRaw = potenciaField.getText().trim();
        String estadoRaw = estadoField.getText().trim();

        if (nombre.isBlank() || tipo.isBlank() || potenciaRaw.isBlank() || estadoRaw.isBlank()) {
            showValidationError("Ningun campo puede quedar vacio.");
            return Optional.empty();
        }

        Double potencia = parseDoubleOrDefault(potenciaRaw, -1.0);
        if (potencia <= 0) {
            showValidationError("La potencia nominal debe ser un numero positivo.");
            return Optional.empty();
        }

        if (!"activo".equalsIgnoreCase(estadoRaw) && !"inactivo".equalsIgnoreCase(estadoRaw)) {
            showValidationError("El estado debe ser 'activo' o 'inactivo'.");
            return Optional.empty();
        }
        boolean activo = "activo".equalsIgnoreCase(estadoRaw);

        Dispositivo dispositivo = new Dispositivo();
        dispositivo.setNombre(nombre);
        dispositivo.setTipo(tipo);
        dispositivo.setPotenciaNominalW(potencia);
        dispositivo.setActivo(activo);
        return Optional.of(dispositivo);
    }

    private void reselectById(Long id) {
        for (Dispositivo dispositivo : dispositivosTable.getItems()) {
            if (dispositivo.getId().equals(id)) {
                dispositivosTable.getSelectionModel().select(dispositivo);
                return;
            }
        }
    }

    private Double parseDoubleOrDefault(String value, Double fallback) {
        try {
            return value.isBlank() ? fallback : Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
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
}
