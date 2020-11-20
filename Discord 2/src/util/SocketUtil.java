package util;

import model.Message;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class SocketUtil {
    public static final int DEFAULT_PORT = 1337;
    private static final SocketUtil instance = new SocketUtil();
    private final ArrayList<MessageHandler> messageHandlers;
    private Socket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private String username;

    private SocketUtil() {
        messageHandlers = new ArrayList<>();
    }

    public static SocketUtil getInstance() {
        return instance;
    }

    public static void onReceive(MessageHandler handler) {
        instance.messageHandlers.add(handler);
    }

    public static void connect() {
        String ip = JOptionPane.showInputDialog(null, "ip adres", "127.0.0.1");
        int port = DEFAULT_PORT;
        try {
            port = Integer.parseInt(JOptionPane.showInputDialog(null, "port", String.valueOf(DEFAULT_PORT)));
        } catch (NumberFormatException ignored) {
        }

        do {
            try {
                instance.socket = new Socket(ip, port);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, e.getMessage());
            }
        } while (instance.socket == null);

        try {
            instance.inputStream = instance.socket.getInputStream();
            instance.outputStream = instance.socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        new Thread(instance.inputReader).start();
    }

    private final Runnable inputReader = new Runnable() {
        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                while (!socket.isClosed()) {
                    String line = reader.readLine();
                    if (line == null) break;

                    System.out.println(line);

                    for (MessageHandler messageHandler : messageHandlers) {
                        messageHandler.received(Message.fromLine(line));
                    }

                    // TODO replace
                    if (line.startsWith("DCST")) {
                        socket.close();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    public interface MessageHandler {
        void received(Message message);
    }
}
