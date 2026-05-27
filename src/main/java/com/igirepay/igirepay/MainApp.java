// MainApp.java
package com.igirepay.igirepay;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        showLogin();
    }

    public static void showLogin() {
        try {
            Parent root = FXMLLoader.load(MainApp.class.getResource("/lab3/Login.fxml"));
            Scene scene = new Scene(root, 400, 600);
            scene.getStylesheets().add(MainApp.class.getResource("/lab3/styles.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setTitle("Igire Pay - Login");
            primaryStage.setResizable(false);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showRegister() {
        try {
            Parent root = FXMLLoader.load(MainApp.class.getResource("/lab3/Register.fxml"));
            Scene scene = new Scene(root, 400, 650);
            scene.getStylesheets().add(MainApp.class.getResource("/lab3/styles.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setTitle("Igire Pay - Register");
            primaryStage.setResizable(false);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showDashboard() {
        try {
            Parent root = FXMLLoader.load(MainApp.class.getResource("/lab3/Dashboard.fxml"));
            Scene scene = new Scene(root, 900, 700);
            scene.getStylesheets().add(MainApp.class.getResource("/lab3/styles.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setTitle("Igire Pay - Dashboard");
            primaryStage.setResizable(true);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}