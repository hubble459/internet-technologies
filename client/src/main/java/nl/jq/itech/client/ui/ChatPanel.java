package nl.jq.itech.client.ui;

import nl.jq.itech.client.helper.model.Command;
import nl.jq.itech.client.helper.model.Message;
import nl.jq.itech.client.model.Channel;
import nl.jq.itech.client.model.DisabledItemSelectionModel;

import javax.swing.*;
import java.awt.*;

/**
 * Chat part of the MainScreen
 * <p>
 * Takes user input and holds a list for server/client output
 */
public class ChatPanel {
    private final DefaultListModel<Message> messages;
    private JPanel chatPanel = new JPanel();
    private JList<Message> messageList = new JList<>();
    private JTextField textField = new JTextField();
    private JButton sendButton = new JButton();
    private JButton uploadButton = new JButton();
    private JButton kickButton = new JButton();
    private Channel channel;
    private CommandListener commandListener;
    private OnUploadListener uploadListener;

    public ChatPanel() {
        messages = new DefaultListModel<>();
        messageList.setSelectionModel(new DisabledItemSelectionModel());
        messageList.setCellRenderer(new CellRenderer(275));
        messageList.setModel(messages);

        textField.addActionListener(e -> sendFromTextField());
        sendButton.addActionListener(e -> sendFromTextField());
        uploadButton.addActionListener(e -> uploadFile());
        kickButton.addActionListener((e -> commandListener.command(new Message(Command.START_KICK, ""))));
    }

    /**
     * Upload a file
     */
    private void uploadFile() {
        uploadListener.upload();
    }

    public Channel currentChannel() {
        return channel;
    }

    /**
     * Send input
     */
    public void sendFromTextField() {
        String text = textField.getText();
        if (text != null && !text.isEmpty()) {
            // Send message
            if (channel == null) {
                addMessage(new Message(Command.SERVER, "You are not in a channel"));
            } else {
                // Send message
                if (channel.isPM()) {
                    commandListener.command(new Message(channel.getCommand(), channel.getName() + " " + text));
                } else {
                    commandListener.command(new Message(channel.getCommand(), text));
                }
            }
        }
        // Clear input
        textField.setText("");
    }

    public void setChannel(Channel channel) {
        // Don't do anything if this channel is already selected
        if (this.channel != channel) {
            this.channel = channel;
            // Upload button should only be visible in a PM
            uploadButton.setVisible(channel.isPM());
            // Kick button should only be visible in a room
            kickButton.setVisible(channel.isRoom());
            // Clear channel notifications
            channel.clearNotifications();
            SwingUtilities.invokeLater(() -> {
                // Clear chat
                messages.clear();
                // Add channel history to chat
                messages.addAll(channel.getMessages());
                // Scroll to last element
                messageList.ensureIndexIsVisible(messages.getSize() - 1);
            });
        }
    }

    public void setCommandListener(CommandListener commandListener) {
        this.commandListener = commandListener;
    }

    public void setOnUpload(OnUploadListener uploadListener) {
        this.uploadListener = uploadListener;
    }

    public void addMessage(Message message) {
        addMessage(message, channel != null);
    }

    public void addMessage(Message message, boolean save) {
        if (save) {
            channel.addMessage(message);
        }
        SwingUtilities.invokeLater(() -> {
            messages.addElement(message);
            messageList.ensureIndexIsVisible(messages.getSize() - 1);
        });
    }

    /**
     * Render messages so they don't go off-screen
     */
    static class CellRenderer extends DefaultListCellRenderer {
        public static final String HTML_1 = "<html><body style='width: ";
        public static final String HTML_2 = "px'>";
        public static final String HTML_3 = "</html>";
        private final int width;

        public CellRenderer(int width) {
            this.width = width;
        }

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Message message = (Message) value;
            String text = HTML_1 + width + HTML_2 + message.toHTML() + HTML_3;
            return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
        }
    }

    public interface CommandListener {
        void command(Message message);
    }

    public interface OnUploadListener {
        void upload();
    }
}
