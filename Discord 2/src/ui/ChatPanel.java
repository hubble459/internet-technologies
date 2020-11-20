package ui;

import util.SocketUtil;

import javax.swing.*;

public class ChatPanel {
    private final DefaultListModel<String> messages;
    private JPanel chatPanel;
    private JList<String> messageList;
    private JTextField textField1;
    private JButton button1;

    public ChatPanel() {
        messages = new DefaultListModel<>();
        messageList.setModel(messages);
        SocketUtil.onReceive(message -> {
            System.out.println(message.getPayload());
            SwingUtilities.invokeLater(() -> {
                messages.addElement(message.getCommand() + " " + message.getPayload());
            });
        });
    }
}
