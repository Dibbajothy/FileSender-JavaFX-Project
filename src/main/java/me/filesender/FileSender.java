package me.filesender;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class FileSender extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(FileSender.class.getResource("modeSelector.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("LocalSender");
        Image icon = new Image(getClass().getResourceAsStream("send.png"));
        stage.getIcons().add(icon);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}