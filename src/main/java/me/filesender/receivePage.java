package me.filesender;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class receivePage implements Initializable {

    @FXML private Button startServer;
    @FXML private TextField serverPort;
    @FXML private Label serverStatus, senderStatus, fileName;
    @FXML private ProgressBar progressBar;

    private ServerSocket serverSocket;
    private boolean isServerRunning = false;
    private Task<Void> serverTask;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        progressBar.setVisible(false);

        startServer.setOnAction(event -> {
            if (!isServerRunning) {
                try {
                    int port = Integer.parseInt(serverPort.getText().trim());
                    startFileReceiver(port);
                    isServerRunning = true;
                    startServer.setText("Stop Server");
                    startServer.setStyle("-fx-background-color: #cc0707;");

                    serverPort.setDisable(true);

                } catch (NumberFormatException e) {
                    serverStatus.setText("Invalid port number");
                }
            } else {
                stopServer();
                isServerRunning = false;
                startServer.setText("Start Server");
                startServer.setStyle("-fx-background-color: #0078d7;");

                serverPort.setDisable(false);
            }
        });
    }



    private void stopServer() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                // First unbind the text property to avoid the binding error
                serverStatus.textProperty().unbind();

                // Cancel the task if it's running
                if (serverTask != null && !serverTask.isDone()) {
                    serverTask.cancel();
                }

                // Close the server socket
                serverSocket.close();

                // Now we can safely set the text
                serverStatus.setText("Server stopped");
                serverStatus.setStyle("-fx-text-fill: #cc0707;");

                // Reset other UI elements
                progressBar.progressProperty().unbind();
                progressBar.setVisible(false);
                progressBar.setProgress(0);

            } catch (IOException e) {
                // We've already unbound the property, so this is safe
                serverStatus.setStyle("-fx-text-fill: #cc0707;");
                serverStatus.setText("Error stopping server: " + e.getMessage());
            }
        }
    }




    private void startFileReceiver(int port) {
        // Create a new task
        serverTask = new Task<Void>() {
            @Override
            protected Void call() {
                updateMessage("Starting server...");

                try {
                    serverSocket = new ServerSocket(port);
                    updateMessage("Server running on port " + port);

                    while (!isCancelled() && !serverSocket.isClosed()) {
                        try {
                            // Reset progress for new connection
                            Platform.runLater(() -> progressBar.setVisible(true));
                            updateProgress(0, 1);

                            // Accept client connection (with timeout to check for cancellation)
                            serverSocket.setSoTimeout(1000); // 1 second timeout
                            Socket socket = null;

                            try {
                                socket = serverSocket.accept();
                                updateMessage("Connected < " + socket.getInetAddress().getHostAddress());
                            } catch (java.net.SocketTimeoutException e) {
                                // Timeout - check if we should continue
                                if (isCancelled() || serverSocket.isClosed()) {
                                    break;
                                }
                                continue; // Try again
                            }

                            try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
                                // Read file name
                                final String receivedFileName = ois.readUTF();
                                Platform.runLater(() -> fileName.setText("Receiving: " + receivedFileName));

                                // Read file size as long
                                final long fileSize = ois.readLong();

                                try (FileOutputStream fos = new FileOutputStream(receivedFileName)) {
                                    byte[] buffer = new byte[8192];
                                    int bytesRead;
                                    long totalBytesRead = 0;

                                    // Read file data in chunks
                                    while (totalBytesRead < fileSize && !isCancelled()) {
                                        bytesRead = ois.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalBytesRead));

                                        if (bytesRead == -1) {
                                            // End of stream reached unexpectedly
                                            break;
                                        }

                                        fos.write(buffer, 0, bytesRead);
                                        totalBytesRead += bytesRead;

                                        // Update progress
                                        double progress = (double) totalBytesRead / fileSize;
                                        updateProgress(progress, 1.0);

                                        // Update status message with percentage
                                        int percentage = (int) (progress * 100);
                                        updateMessage("Receiving file: " + percentage + "%");
                                    }

                                    if (!isCancelled()) {
                                        updateMessage("File received successfully!");
                                    }
                                }
                            } catch (IOException e) {
                                if (!isCancelled()) {
                                    updateMessage("Error receiving file: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            } finally {
                                try {
                                    if (socket != null && !socket.isClosed()) {
                                        socket.close();
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (IOException e) {
                            if (!serverSocket.isClosed() && !isCancelled()) {
                                updateMessage("Connection error: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (IOException e) {
                    if (!isCancelled()) {
                        updateMessage("Server error: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                return null;
            }
        };

        // Bind UI elements to task properties
        progressBar.progressProperty().bind(serverTask.progressProperty());
        serverStatus.textProperty().bind(serverTask.messageProperty());

        // Handle task completion
        serverTask.setOnSucceeded(e -> {
            // Unbind properties when task completes
            serverStatus.textProperty().unbind();
            progressBar.progressProperty().unbind();
            serverStatus.setText("Server stopped");
            isServerRunning = false;
            startServer.setText("Start Server");
        });

        // Handle task cancellation
        serverTask.setOnCancelled(e -> {
            // This will be called when we manually cancel the task
            serverStatus.textProperty().unbind();
            progressBar.progressProperty().unbind();
            isServerRunning = false;
        });

        new Thread(serverTask).start();
    }
}