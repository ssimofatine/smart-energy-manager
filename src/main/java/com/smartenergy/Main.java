package com.smartenergy;

import java.io.IOException;

import com.smartenergy.dao.*;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        Parent root;
        try {
            root = FXMLLoader.load(getClass().getResource("/com/smartenergy/fxml/main.fxml"));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo iniciar la aplicacion JavaFX", e);
        }
        Scene scene = new Scene(root, 1000, 640);
        scene.getStylesheets().add(getClass().getResource("/com/smartenergy/css/styles.css").toExternalForm());

        stage.setTitle("Smart Energy Manager");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (JpaManager.getEntityManagerFactory().isOpen()) {
            JpaManager.getEntityManagerFactory().close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
