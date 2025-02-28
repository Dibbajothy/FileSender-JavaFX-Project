package me.filesender;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.io.IOException;
import java.net.URL;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.Inet4Address;
import java.util.Enumeration;
import java.util.ResourceBundle;

public class modeSelector implements Initializable {

    @FXML private Button sendButton, receiveButton;
    @FXML private Label wiredIp, wifiIP;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        try {
            getNetworkIPs();
        } catch (Exception e) {
            wiredIp.setText("Error retrieving IP addresses");
            wifiIP.setText("Error retrieving IP addresses");
            e.printStackTrace();
        }

        sendButton.setOnAction(event -> {
            try {
                sceneChange.change(event, "sendPage.fxml");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        receiveButton.setOnAction(event -> {
            try {
                sceneChange.change(event, "receivePage.fxml");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void getNetworkIPs() throws SocketException {
        InetAddress wiredIP = null;
        InetAddress wirelessIP = null;

        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();

            // Skip loop back, virtual, and inactive interfaces
            if (networkInterface.isLoopback() || !networkInterface.isUp() || networkInterface.isVirtual()) {
                continue;
            }

            String name = networkInterface.getName().toLowerCase();

            // Check if this might be a wireless interface
            boolean isWireless = name.contains("wlan") || name.contains("wifi") || name.contains("wireless") || name.startsWith("wl");

            // Check if this might be a wired interface
            boolean isWired = name.contains("eth") || name.contains("ethernet") || ( name.startsWith("en") && !isWireless);

            Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();

            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();

                // Only consider IPv4 addresses that aren't loopback
                if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                    if (isWireless && wirelessIP == null) {
                        wirelessIP = address;
                    } else if (isWired && wiredIP == null) {
                        wiredIP = address;
                    }
                }
            }
        }

        // Update your label
        String wiredIPText = (wiredIP != null) ? wiredIP.getHostAddress() : "Not Found";
        String wirelessIPText = (wirelessIP != null) ? wirelessIP.getHostAddress() : "Not Found";

        wifiIP.setText("Wireless IP - " + wirelessIPText);
        wiredIp.setText("Wired IP - " + wiredIPText);
    }

}