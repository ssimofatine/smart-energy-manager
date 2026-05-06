package com.smartenergy.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.layout.StackPane;

public class MainController implements Initializable {

    @FXML
    private StackPane contentPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadView("dashboard.fxml");
    }

    @FXML
    private void showPanel() {
        loadView("dashboard.fxml");
    }

    @FXML
    private void showDispositivos() {
        loadView("dispositivos.fxml");
    }

    @FXML
    private void showLecturas() {
        loadView("lecturas.fxml");
    }

    private void loadView(String fxmlName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/smartenergy/fxml/" + fxmlName));
            contentPane.getChildren().clear();
            contentPane.getChildren().add(loader.load());
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cargar la vista: " + fxmlName, e);
        }
    }
}
