package me.filesender;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.util.ResourceBundle;

public class sendPage implements Initializable {

    @FXML private Button sendButton, selectFile;
    @FXML private TextField tf_ip, tf_port;
    @FXML private Label fileName, clientStatus;
    @FXML private ProgressBar progressBar;

    private File sendingFile;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initially hide progress bar
        progressBar.setVisible(false);

        selectFile.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select File");
            File file = fileChooser.showOpenDialog(null);
            if (file != null) {
                sendingFile = file;
                fileName.setText(file.getName());
            }
        });

        sendButton.setOnAction(e -> {
            if (sendingFile != null) {
                try {

                    String ip = tf_ip.getText().trim();
                    int port = Integer.parseInt(tf_port.getText().trim());

                    // Reset progress bar
                    progressBar.setProgress(0);
                    progressBar.setVisible(true);

                    // Disable buttons during transfer
                    sendButton.setDisable(true);
                    selectFile.setDisable(true);
                    tf_port.setDisable(true);
                    tf_ip.setDisable(true);

                    //setting the color of the status
                    clientStatus.setStyle("-fx-text-fill: #ff0000;");


                    sendFile(ip, port, sendingFile);
                } catch (NumberFormatException ex) {
                    clientStatus.setText("Invalid port number");
                }
            } else {
                clientStatus.setText("Please select a file first");
            }
        });
    }

    private void sendFile(String ip, int port, File file) {
        Task<Void> sendTask = new Task<>() {
            @Override
            protected Void call() {
                try (Socket socket = new Socket(ip, port);
                     ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                     BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(file.toPath()))) {

                    updateMessage("Connected to server. Sending file...");

                    // Get file details
                    long fileSize = file.length();

                    // Send file name
                    oos.writeUTF(file.getName());
                    oos.flush();

                    // Send file size as long to handle large files
                    oos.writeLong(fileSize);
                    oos.flush();

                    // Send file in chunks
                    byte[] buffer = new byte[8192];
                    long totalBytesSent = 0;
                    int bytesRead;

                    while ((bytesRead = bis.read(buffer)) != -1) {
                        oos.write(buffer, 0, bytesRead);

                        // It's important to flush after each chunk to prevent buffer issues
                        oos.flush();

                        totalBytesSent += bytesRead;

                        // Update progress
                        double progress = (double) totalBytesSent / fileSize;
                        updateProgress(progress, 1.0);

                        // Update status message with percentage
                        int percentage = (int) (progress * 100);
                        updateMessage("Sending: " + percentage + "%");
                    }

                    updateMessage("File sent successfully!");

                } catch (IOException e) {
                    updateMessage("Error: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    // Re-enable buttons
                    Platform.runLater(() -> {
                        sendButton.setDisable(false);
                        selectFile.setDisable(false);
                        tf_port.setDisable(false);
                        tf_ip.setDisable(false);
                        progressBar.setVisible(false);
                    });
                }
                return null;
            }
        };

        // Bind progress bar to task progress
        progressBar.progressProperty().bind(sendTask.progressProperty());

        // Bind status label to task message
        clientStatus.textProperty().bind(sendTask.messageProperty());

        // Set up completion handler
        sendTask.setOnSucceeded(e -> {
            clientStatus.textProperty().unbind();
            progressBar.progressProperty().unbind();
            clientStatus.setStyle("-fx-text-fill: #029302;");
        });

        new Thread(sendTask).start();
    }
}