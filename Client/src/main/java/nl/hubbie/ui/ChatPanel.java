package nl.hubbie.ui;

import nl.hubbie.helper.model.Command;
import nl.hubbie.helper.model.Message;
import nl.hubbie.model.Channel;
import nl.hubbie.model.DisabledItemSelectionModel;

import javax.swing.*;
import java.awt.*;

public class ChatPanel extends JPanel {
    private final DefaultListModel<Message> messages;
    private final JList<Message> messageList;
    private final JTextField textField;
    private final JButton sendButton;
    private final JButton uploadButton;
    private final JButton kickButton;
    private Channel channel;
    private CommandListener commandListener;
    private OnUploadListener uploadListener;

    public ChatPanel() {
        messages = new DefaultListModel<>();
        messageList = new JList<>(messages);
        messageList.setSelectionModel(new DisabledItemSelectionModel());
        messageList.setCellRenderer(new CellRenderer(275));

        textField = new JTextField();
        sendButton = new JButton("Send");
        uploadButton = new JButton("Upload");
        kickButton = new JButton("Kick");

        JPanel bottomPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.weightx = 1.0;
        bottomPanel.add(textField, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.0;
        bottomPanel.add(kickButton, gbc);

        gbc.gridx = 2;
        bottomPanel.add(sendButton, gbc);

        gbc.gridx = 3;
        bottomPanel.add(uploadButton, gbc);

        JScrollPane messageScrollPane = new JScrollPane(messageList);

        setLayout(new BorderLayout());
        add(messageScrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        setupListeners();
    }

    private void setupListeners() {
        textField.addActionListener(e -> sendFromTextField());
        sendButton.addActionListener(e -> sendFromTextField());
        uploadButton.addActionListener(e -> uploadFile());
        kickButton.addActionListener(e -> {
            if (commandListener != null) {
                commandListener.command(new Message(Command.START_KICK, ""));
            }
        });
    }

    private void uploadFile() {
        if (uploadListener != null) {
            uploadListener.upload();
        }
    }

    private void sendFromTextField() {
        String text = textField.getText();
        if (text != null && !text.isEmpty()) {
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
        textField.setText("");
    }

    public void setChannel(Channel channel) {
        if (this.channel != channel) {
            this.channel = channel;
            uploadButton.setVisible(channel.isPM());
            kickButton.setVisible(channel.isRoom());
            channel.clearNotifications();
            SwingUtilities.invokeLater(() -> {
                messages.clear();
                messages.addAll(channel.getMessages());
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
        if (save && channel != null) {
            channel.addMessage(message);
        }

        SwingUtilities.invokeLater(() -> {
            messages.addElement(message);
            messageList.ensureIndexIsVisible(messages.getSize() - 1);
        });
    }

    public Channel getChannel() {
        return this.channel;
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
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
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