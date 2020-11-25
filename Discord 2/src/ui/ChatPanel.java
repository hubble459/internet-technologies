package ui;

import model.Channel;
import model.Command;
import model.DisabledItemSelectionModel;
import model.Message;
import util.ServerUtil;

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
        messageList.setSelectionModel(new DisabledItemSelectionModel());
        messageList.setModel(messages);

        ServerUtil.onReceive(message -> {
            if (channel.getCommand() == message.getCommand()) {
                channel.addMessage(message);
                SwingUtilities.invokeLater(() -> {
                    messages.addElement(message);
                    messageList.ensureIndexIsVisible(messages.getSize() - 1);
                });
            } else if (message.getCommand() == Command.VOTE_KICK) {
                // KICK STARTED
                KickPanel dialog = new KickPanel();
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
            }
        });

        textField.addActionListener(e -> sendFromTextField());
        button.addActionListener(e -> sendFromTextField());
    }

    public void sendFromTextField() {
        String text = textField.getText();
        if (text != null && !text.isEmpty()) {
            if (text.toLowerCase().startsWith("/kick")) {
                ServerUtil.send(Command.VOTE_KICK);
            } else {
                channel.send(text);
                textField.setText("");
            }
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
