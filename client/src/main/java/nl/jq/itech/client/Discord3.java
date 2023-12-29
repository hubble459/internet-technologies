package nl.jq.itech.client;

import nl.jq.itech.client.helper.Shared;
import nl.jq.itech.client.helper.SocketHelper;
import nl.jq.itech.client.helper.model.Command;
import nl.jq.itech.client.helper.model.Request;
import nl.jq.itech.client.ui.MainScreen;

import javax.swing.*;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

/**
 * It's better than Discord
 */
public class Discord3 {
    public static final int DEFAULT_PORT = 1337;
    public static final String DEFAULT_IP = "127.0.0.1";
    private SocketHelper helper;
    private MainScreen mainScreen;
    private String error;
    private boolean loggedIn;

    public static void main(String[] args) {
        new Discord3().run();
    }

    public void run() {
        // Public and private keys
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair kp = Shared.keyPair = kpg.genKeyPair();
            Shared.publicKeyBase64 = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());

            // START Quentin
            // Quentin: token encoden met PUK
            // Quentin: stuurt token naar joost
            // Joost: decode token
            // Joost: PM nsdghwsjhr
            // Quentin: nsfhswkhgfwes -> kutkind
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Connect and Login
        connectAndLogin();

        // Start UI
        mainScreen = new MainScreen("Discord 3", helper);

        // Restart when disconnected
        helper.setOnDisconnectListener(this::onDisconnect);
    }

    public void connectAndLogin() {
        helper = null;
        loggedIn = false;
        error = "";

        do {
            String ip = JOptionPane.showInputDialog(null, "IP address:PORT", DEFAULT_IP + ":" + DEFAULT_PORT);
            if (ip == null /* Canceled */) {
                System.exit(0);
                return;
            }

            // Get port
            int port = DEFAULT_PORT;
            try {
                // Try to get port from string
                port = Integer.parseInt(ip.split(":", 2)[1]);
            } catch (Exception ignored) {
            }
            // Get ip
            ip = ip.split(":", 2)[0];

            try {
                // Create the helper
                helper = new SocketHelper(ip, port);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Connection Error", "Error", JOptionPane.ERROR_MESSAGE);
                continue;
            }

            // Set the username
            Shared.username = JOptionPane.showInputDialog(null, "Enter username", "Username", JOptionPane.PLAIN_MESSAGE);
            if (Shared.username == null || Shared.username.isEmpty()) {
                // If canceled
                helper = null;
            } else {
                // Add priority to LOGIN
                helper.addPriority(Command.LOGIN);
                // Login
                Request.sendAndWaitForResponse(Request.build(helper)
                        .setMessage(Command.LOGIN, Shared.username + ' ' + Shared.publicKeyBase64)
                        .setOnResponse((success, message) -> {
                            if (success) {
                                loggedIn = true;
                            } else {
                                error = message.getPayload();
                            }
                            return false;
                        }));

                // If not logged in; there has been an error
                if (!loggedIn) {
                    JOptionPane.showMessageDialog(null, error, "Bad username", JOptionPane.ERROR_MESSAGE);
                    helper = null;
                }
            }
        } while (helper == null);
    }

    /**
     * When disconnected try to reconnect
     *
     * @param error reason
     */
    private void onDisconnect(String error) {
        if (!Shared.quit) {
            SwingUtilities.invokeLater(() -> {
                System.err.println(error);
                mainScreen.dispose();
                boolean reconnect = JOptionPane.showConfirmDialog(null, error + "\nReconnect?", "Disconnected", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE) == JOptionPane.YES_OPTION;
                if (reconnect) {
                    connectAndLogin();

                    helper.setOnDisconnectListener(this::onDisconnect);

                    mainScreen = new MainScreen("Discord 3", helper);
                }
            });
        }
    }
}
