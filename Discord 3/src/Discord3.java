import helper.Shared;
import helper.SocketHelper;
import helper.model.Command;
import helper.model.Request;
import ui.MainScreen;

import javax.swing.*;
import java.io.IOException;

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
            String ip = JOptionPane.showInputDialog(null, "IP adres:PORT", DEFAULT_IP + ":" + DEFAULT_PORT);
//            String ip = JOptionPane.showInputDialog(null, "IP adres:PORT", "86.87.206.20:" + DEFAULT_PORT);
            if (ip == null /* Canceled */) {
                System.exit(0);
                return;
            }

            int port = DEFAULT_PORT;
            try {
                port = Integer.parseInt(ip.split(":", 2)[1]);
            } catch (Exception ignored) {
            }
            ip = ip.split(":", 2)[0];

            try {
                helper = new SocketHelper(ip, port);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Connection Error", "Error", JOptionPane.ERROR_MESSAGE);
                continue;
            }

            Shared.username = JOptionPane.showInputDialog(null, "Enter username", "Username", JOptionPane.PLAIN_MESSAGE);
            if (Shared.username == null || Shared.username.isEmpty()) {
                helper = null;
            } else {
                helper.addPriority(Command.LOGIN);
                // Login
                Request.sendAndWaitForResponse(Request.build(helper)
                        .setMessage(Command.LOGIN, Shared.username)
                        .setOnResponse((success, message) -> {
                            if (success) {
                                loggedIn = true;
                            } else {
                                error = message.getPayload();
                            }
                            return false;
                        }));

                if (!loggedIn) {
                    JOptionPane.showMessageDialog(null, error, "Bad username", JOptionPane.ERROR_MESSAGE);
                    helper = null;
                }
            }
        } while (helper == null);
    }

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
