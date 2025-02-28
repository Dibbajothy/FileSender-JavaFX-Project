module me.filesender {
    requires javafx.controls;
    requires javafx.fxml;


    opens me.filesender to javafx.fxml;
    exports me.filesender;
}