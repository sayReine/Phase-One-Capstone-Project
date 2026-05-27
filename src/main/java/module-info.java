module com.igirepay.igirepay {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;
    requires java.sql;

    opens com.igirepay.igirepay to javafx.fxml;
    exports com.igirepay.igirepay;
    exports lab3;
    opens lab3 to javafx.fxml;
    exports lab3.service;
    opens lab3.service to javafx.fxml;
}