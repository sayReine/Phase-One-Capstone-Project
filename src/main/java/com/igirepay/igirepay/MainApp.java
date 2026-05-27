package com.igirepay.igirepay;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lab1.Customer;
import lab3.DashboardController;

import java.io.IOException;

public class MainApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.setTitle("IgirePay — Igire Rwanda Organization");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        showLogin();
        primaryStage.show();
    }

    public static void showLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource("/com/igirepay/igirepay/lab3/Login.fxml"));
            Scene scene = new Scene(loader.load(), 500, 600);
            scene.getStylesheets().add(
                    MainApp.class.getResource("/com/igirepay/igirepay/lab3/styles.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setWidth(500);
            primaryStage.setHeight(600);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Called by LoginController after successful login — passes the customer in
    public static void showDashboard(Customer customer) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource("/com/igirepay/igirepay/lab3/Dashboard.fxml"));
            Scene scene = new Scene(loader.load(), 1000, 680);
            scene.getStylesheets().add(
                    MainApp.class.getResource("/com/igirepay/igirepay/lab3/styles.css").toExternalForm());

            // Inject the logged-in customer into the dashboard controller
            DashboardController controller = loader.getController();
            controller.setCustomer(customer);

            primaryStage.setScene(scene);
            primaryStage.setWidth(1000);
            primaryStage.setHeight(680);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void showRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource("/com/igirepay/igirepay/lab3/Register.fxml"));
            Scene scene = new Scene(loader.load(), 500, 680);
            scene.getStylesheets().add(
                    MainApp.class.getResource("/com/igirepay/igirepay/lab3/styles.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setWidth(500);
            primaryStage.setHeight(680);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}