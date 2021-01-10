package ui;

import model.Channel;
import model.Command;
import model.DisabledItemSelectionModel;
import model.Message;
import util.ServerUtil;

import javax.swing.*;
import java.awt.*;

public class ChatPanel {
    private final DefaultListModel<Message> messages;
    private JPanel chatPanel;
    private JList<Message> messageList;
    private JTextField textField;
    private JButton kickButton;
    private JButton button;
    private Channel channel;

    public ChatPanel() {
        messages = new DefaultListModel<>();
        messageList.setSelectionModel(new DisabledItemSelectionModel());
        messageList.setModel(messages);
        MyCellRenderer cellRenderer = new MyCellRenderer(275);
        messageList.setCellRenderer(cellRenderer);

        ServerUtil.onReceive(message -> {
            if (channel.getCommand() == message.getCommand()
                    || message.getCommand() == Command.WHISPERED
                    || message.getCommand() == Command.JOINED_ROOM
                    || (channel.getName().equals("Main")
                    && message.getCommand() == Command.JOINED_SERVER)) {
                if (message.getCommand() != Command.JOINED_ROOM && message.getCommand() != Command.JOINED_SERVER) {
                    channel.addMessage(message);
                }
                showMessage(message);
            } else if (!kickPopup &&
                    (message.getCommand() == Command.VOTE_KICK || message.getCommand() == Command.VOTES)) {
                startKick();
            } else if (kickPopup && message.getCommand() == Command.KICK_RESULT) {
                kickPopup = false;
            } else if (message.getCommand().toString().startsWith("4")) {
                message = new Message(Command.BROADCAST, "SERVER " + message.getPayload());
                showMessage(message);
            }
        });

        textField.addActionListener(e -> sendFromTextField());
        button.addActionListener(e -> sendFromTextField());
    }

    private void showMessage(Message message) {
        SwingUtilities.invokeLater(() -> {
            messages.addElement(message);
            messageList.ensureIndexIsVisible(messages.getSize() - 1);
        });
    }

    private boolean kickPopup = false;

    private void startKick() {
        kickPopup = true;
        KickPanel dialog = new KickPanel();
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        SwingUtilities.invokeLater(() -> textField.setText(""));
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

    public void addMessage(Message message) {
        SwingUtilities.invokeLater(() -> {
            messages.addElement(message);
            messageList.ensureIndexIsVisible(messages.getSize() - 1);
        });
    }

    static class MyCellRenderer extends DefaultListCellRenderer {
        public static final String HTML_1 = "<html><body style='width: ";
        public static final String HTML_2 = "px'>";
        public static final String HTML_3 = "</html>";
        private final int width;

        public MyCellRenderer(int width) {
            this.width = width;
        }

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            String text = HTML_1 + width + HTML_2 + value.toString() + HTML_3;
            return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
        }
    }
}
