package util;

import model.Command;
import model.Message;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class SocketUtil {
    public static final int DEFAULT_PORT = 1337;
    private static final SocketUtil instance = new SocketUtil();
    private final ArrayList<OnReceive> onReceiveHandlers;
    private final ArrayList<AfterLogin> afterLoginListeners;
    private Socket socket;
    private OutputStream outputStream;
    private PrintWriter writer;
    private InputStream inputStream;
    private final Runnable inputReader = new Runnable() {
        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                while (!socket.isClosed()) {
                    String line = reader.readLine();
                    if (line == null) break;

                    System.out.println(line);

                    for (OnReceive onReceive : new ArrayList<>(onReceiveHandlers)) {
                        onReceive.received(Message.fromLine(line));
                    }

                    // TODO replace
                    if (line.startsWith("DCST")) {
                        socket.close();
                    }
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    };
    private String username;
    private boolean loggedIn;

    private SocketUtil() {
        onReceiveHandlers = new ArrayList<>();
        afterLoginListeners = new ArrayList<>();
    }

    public static SocketUtil getInstance() {
        return instance;
    }

    public static void onReceive(OnReceive handler) {
        instance.onReceiveHandlers.add(handler);
    }

    public static void removeOnReceive(OnReceive handler) {
        instance.onReceiveHandlers.remove(handler);
    }

    public static void afterLogin(AfterLogin listener) {
        instance.afterLoginListeners.add(listener);
    }

    public static void removeAfterLogin(AfterLogin listener) {
        instance.afterLoginListeners.remove(listener);
    }

    public static void connect() {
        do {
            String ip = JOptionPane.showInputDialog(null, "IP adres:PORT", "127.0.0.1:" + DEFAULT_PORT);
            if (ip == null /* Canceled */) {
                cancel();
                return;
            }

            int port = DEFAULT_PORT;
            try {
                port = Integer.parseInt(ip.split(":", 2)[1]);
            } catch (Exception ignored) {
            }
            ip = ip.split(":", 2)[0];

            try {
                instance.socket = new Socket(ip, port);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } while (instance.socket == null);

        try {
            instance.inputStream = instance.socket.getInputStream();
            instance.outputStream = instance.socket.getOutputStream();
            instance.writer = new PrintWriter(instance.outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        OnReceive onReceive = new OnReceive() {
            @Override
            public void received(Message message) {
                if (message.getCommand() == Command.INFO) {
                    login();
                } else if (message.getCommand() == Command.LOGGED_IN) {
                    instance.loggedIn = true;
                    postLogin();
                    instance.onReceiveHandlers.remove(this);
                } else if (!instance.loggedIn) {
                    login();
                }
            }
        };
        instance.onReceiveHandlers.add(onReceive);

        new Thread(instance.inputReader).start();
    }

    private static void cancel() {
        System.out.println("Canceled");
        System.exit(0);
    }

    private static void login() {
        instance.username = JOptionPane.showInputDialog(null, "Username (3 ~ 14 chars, only characters, numbers and underscore)");
        if (instance.username == null) {
            cancel();
        } else {
            send(Command.LOGIN, instance.username);
        }
    }

    private static void postLogin() {
        for (AfterLogin afterLoginListener : new ArrayList<>(instance.afterLoginListeners)) {
            afterLoginListener.loggedIn(instance.username);
        }
    }

    public static void send(Command command, String message) {
        send(new Message(command, message));
    }

    public static void send(Message message) {
        if (instance.socket != null && !instance.socket.isClosed()) {
            instance.writer.println(message.toString());
            instance.writer.flush();
        }
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public static String getUsername() {
        return instance.username;
    }

    public interface OnReceive {
        void received(Message message);
    }

    public interface AfterLogin {
        void loggedIn(String username);
    }
}
