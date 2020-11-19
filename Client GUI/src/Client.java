import ui.listener.InputListener;
import ui.model.Message;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;

public class Client extends JFrame {
    private final int TIMEOUT = 10000;
    private final DefaultListModel<String> dlm;
    private final JList<String> list;
    private JTextField textField;
    private JButton sendButton;
    private JPanel mainPanel;
    private JPanel messagePanel;
    private Socket socket;
    private PrintWriter writer;
    private Thread inputThread;
    private InputListener.OnReplyListener onReplyListener;
    private boolean connected;
    private String username;
    private long last;

    public static void main(String[] args) {
        new Client("Discord 2").setVisible(true);
    }

    public Client(String title) throws HeadlessException {
        super(title);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(mainPanel);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
                super.windowClosing(e);
            }
        });

        dlm = new DefaultListModel<>();
        list = new JList<>(dlm);
        messagePanel.add(new JScrollPane(list));

        pack();
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        init();
    }

    private void init() {
        onReplyListener = new InputListener.OnReplyListener() {
            @Override
            public void reply(Message message) {
                if (connected) {
                    if (message.getType().equals(Message.GOOD)
                            && message.getMessage().isEmpty()) {
                        message.setMessage("Logged in!");
                        last = System.currentTimeMillis();
                    }

                    if (message.getType().equals(Message.PING)) {
                        if (last == 0) last = System.currentTimeMillis();
                        if (System.currentTimeMillis() - last <= TIMEOUT) {
                            Message msg = new Message(username, Message.PONG, "");
                            send(msg);
                        }
                        return;
                    }

                    if (message.getType().equals(Message.DCSN)) {
                        message.setMessage("Disconnected due to inactivity");
                        textField.setEnabled(false);
                        sendButton.setEnabled(false);
                    }

                    received(message);
                }

                if (!connected) {
                    if (message.getType().equals(Message.GOOD)) {
                        received(message);
                        return;
                    }

                    if (message.getType().equals(Message.INFO)) {
                        connected = true;
                    } else {
                        received(message);
                    }

                    login();
                }
            }

            @Override
            public void onClosed() {
                if (connected) {
                    JOptionPane.showMessageDialog(Client.this, "The server isn't responding!", "Server Crashed?", JOptionPane.ERROR_MESSAGE);
                    disconnect();
                    dispose();
                }
            }
        };

        textField.addActionListener(e -> broadcast(textField.getText()));

        sendButton.addActionListener(e -> broadcast(textField.getText()));

        connect();
    }

    private void received(Message message) {
        SwingUtilities.invokeLater(() -> {
            dlm.addElement(message.toString());
            list.ensureIndexIsVisible(dlm.getSize() - 1);
        });
    }

    private void login() {
        do {
            username = JOptionPane.showInputDialog(this, "Enter username", "Login", JOptionPane.PLAIN_MESSAGE);
            if (username == null) {
                disconnect();
                System.exit(0);
                return;
            } else if (username.isEmpty()) {
                username = "Anonymous" + randomNumbers();
            }
        } while (!username.matches("\\w{3,14}"));

        Message message = new Message(Message.CONN, Message.CONN, username);
        send(message);
        received(message);
    }

    private int randomNumbers() {
        Random random = new Random();
        int amount = random.nextInt(4) + 1;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < amount; i++) {
            sb.append(random.nextInt(10));
        }
        return Integer.parseInt(sb.toString());
    }

    private void broadcast(String message) {
        if (!message.isEmpty() && connected) {
            Message msg = new Message(username, Message.BCST, message);
            send(msg);

            SwingUtilities.invokeLater(() -> textField.setText(""));

            last = System.currentTimeMillis();
        }
    }

    private void send(Message message) {
        String command = message.getType() + " " + message.getMessage();
        writer.println(command.trim());
        writer.flush();
    }

    private void connect() {
        try {
            socket = new Socket("127.0.0.1", 1337);
            inputThread = new Thread(new InputListener(socket, onReplyListener));
            writer = new PrintWriter(socket.getOutputStream());

            inputThread.start();
        } catch (IOException e) {
            if (isVisible()) {
                dlm.addElement("[CLIENT] Connection failed...");
                boolean tryAgain = JOptionPane.showConfirmDialog(this, "Connection failed... Try again?", "Error", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
                if (tryAgain) {
                    connect();
                } else {
                    disconnect();
                    dispose();
                }
            }
        }
    }

    private void disconnect() {
        connected = false;
        if (socket != null && !socket.isClosed()) {
            inputThread.interrupt();
            writer.close();

            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
