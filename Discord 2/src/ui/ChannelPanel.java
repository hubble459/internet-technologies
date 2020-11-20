package ui;

import javax.swing.*;

public class ChannelPanel {
    private final DefaultListModel<String> rooms;
    private final DefaultListModel<String> users;
    private JPanel channelPanel;
    private JTabbedPane tabbedPane;
    private JList<String> roomList;
    private JList<String> userList;
    private ChatPanel chatPanel;

    public ChannelPanel() {
        rooms = new DefaultListModel<>();
        users = new DefaultListModel<>();

        roomList.setModel(rooms);
        userList.setModel(users);

        rooms.addElement("Main");
        roomList.setSelectedIndex(0);
    }

    public void setChatPanel(ChatPanel chatPanel) {
        this.chatPanel = chatPanel;
    }
}
