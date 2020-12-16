package ui;

import helper.Shared;
import helper.model.Command;
import helper.model.Message;
import model.Channel;
import model.DisabledItemSelectionModel;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

public class ChatPanel {
    private final DefaultListModel<Message> messages;
    private JPanel chatPanel;
    private JList<Message> messageList;
    private JTextField textField;
    private JButton sendButton;
    private JButton uploadButton;
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
    }

    private void uploadFile() {
        JFileChooser fileChooser = new JFileChooser(System.getProperty("user.dir"));
        int returnValue = fileChooser.showOpenDialog(null);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            String filename = file.getName().replace(" ", "_");
            try {
                byte[] bytes = Files.readAllBytes(file.toPath());
                String base64 = Base64.getEncoder().encodeToString(bytes);
                // Send file to self
                commandListener.command(new Message(Command.FILE, Shared.username + ' ' + filename + ' ' +base64));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Channel currentChannel() {
        return channel;
    }

    public void sendFromTextField() {
        String text = textField.getText();
        if (text != null && !text.isEmpty()) {
            if (text.startsWith("/")) {
                Message message = Message.fromLine(text.substring(1));
                if (message.getCommand() != null) {
                    commandListener.command(new Message(message.getCommand(), message.getPayload()));
                } else {
                    addMessage(new Message(Command.SERVER, "'" + text.substring(1) + "' is not a valid command"));
                }
            } else {
                if (channel == null) {
                    addMessage(new Message(Command.SERVER, "You are not in a channel"));
                } else {
                    if (channel.isPM()) {
                        commandListener.command(new Message(channel.getCommand(), channel.getName() + " " + text));
                    } else {
                        commandListener.command(new Message(channel.getCommand(), text));
                    }
                }
            }
        }
        textField.setText("");
    }

    public void setChannel(Channel channel) {
        if (this.channel != channel) {
            this.channel = channel;
            channel.clearNotifications();
            SwingUtilities.invokeLater(() -> {
                messages.clear();
                messages.addAll(channel.getMessages());
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
