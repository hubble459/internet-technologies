package ui;

import model.Channel;
import model.Message;
import util.SocketUtil;

import javax.swing.*;

public class ChatPanel {
    private final DefaultListModel<Message> messages;
    private JPanel chatPanel;
    private JList<Message> messageList;
    private JTextField textField;
    private JButton button;
    private Channel channel;

    public ChatPanel() {
        messages = new DefaultListModel<>();
        messageList.setModel(messages);

        SocketUtil.onReceive(message -> {
            if (channel.getCommand() == message.getCommand()) {
                channel.addMessage(message);
                SwingUtilities.invokeLater(() -> {
                    messages.addElement(message);
                    messageList.ensureIndexIsVisible(messages.getSize() - 1);
                });
            }
        });

        textField.addActionListener(e -> sendFromTextField());
        button.addActionListener(e -> sendFromTextField());
    }

    public void sendFromTextField() {
        String text = textField.getText();
        if (text != null && !text.isEmpty()) {
            channel.send(text);
            textField.setText("");
        }
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
        channel.clearNotifications();
        SwingUtilities.invokeLater(() -> {
            messages.clear();
            messages.addAll(channel.getMessages());
        });
    }
}
