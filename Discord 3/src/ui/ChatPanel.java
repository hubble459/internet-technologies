package ui;

import helper.model.Command;
import helper.model.Message;
import model.Channel;
import model.DisabledItemSelectionModel;
import util.Checksum;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

/**
 * Chat part of the MainScreen
 * <p>
 * Takes user input and holds a list for server/client output
 */
public class ChatPanel {
    private final DefaultListModel<Message> messages;
    private JPanel chatPanel;
    private JList<Message> messageList;
    private JTextField textField;
    private JButton sendButton;
    private JButton uploadButton;
    private JButton kickButton;
    private Channel channel;
    private MainScreen.CommandListener commandListener;

    public ChatPanel() {
        messages = new DefaultListModel<>();
        messageList.setSelectionModel(new DisabledItemSelectionModel());
        messageList.setCellRenderer(new CellRenderer(275));
        messageList.setModel(messages);

        textField.addActionListener(e -> sendFromTextField());
        sendButton.addActionListener(e -> sendFromTextField());
        uploadButton.addActionListener(e -> uploadFile());
        kickButton.addActionListener((e -> commandListener.command(new Message(Command.VOTE_KICK, ""))));
    }

    /**
     * Upload a file
     */
    private void uploadFile() {
        JFileChooser fileChooser = new JFileChooser(System.getProperty("user.dir"));
        int returnValue = fileChooser.showOpenDialog(null);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            String filename = file.getName().replace(" ", "_");
            try {
                byte[] bytes = Files.readAllBytes(file.toPath());
                String base64 = Base64.getEncoder().encodeToString(bytes);
                commandListener.command(
                        new Message(
                                Command.FILE,
                                channel.getName() + ' ' + filename + ' ' + base64 + ' ' + Checksum.getMD5Checksum(file)
                        ));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
            // If its a command
            if (commandListener != null && text.startsWith("/")) {
                Message message = Message.fromLine(text.substring(1).toUpperCase());
                if (message.getCommand() != null) {
                    // Send command
                    commandListener.command(new Message(message.getCommand(), message.getPayload()));
                } else {
                    addMessage(new Message(Command.SERVER, "'" + text.substring(1) + "' is not a valid command"));
                }
            } else {
                // If its a chat
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

    public void setCommandListener(MainScreen.CommandListener commandListener) {
        this.commandListener = commandListener;
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
}
