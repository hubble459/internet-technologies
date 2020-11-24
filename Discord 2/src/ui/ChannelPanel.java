package ui;

import model.Channel;
import model.Command;
import util.SocketUtil;

import javax.swing.*;

public class ChannelPanel {
    private final DefaultListModel<Channel> rooms;
    private final DefaultListModel<Channel> users;
    private JPanel channelPanel;
    private JTabbedPane tabbedPane;
    private JList<Channel> roomList;
    private JList<Channel> userList;
    private ChatPanel chatPanel;

    public ChannelPanel() {
        rooms = new DefaultListModel<>();
        users = new DefaultListModel<>();

        roomList.setModel(rooms);
        userList.setModel(users);

        rooms.addElement(new Channel("Main", Channel.ChannelType.MAIN));
        roomList.setSelectedIndex(0);

        roomList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Channel channel = roomList.getSelectedValue();
                if (channel != null && chatPanel.getChannel() != channel) {
                    userList.clearSelection();
                    if (channel.isMain()) {
                        SocketUtil.send(Command.LEAVE_ROOM, "");
                    } else {
                        SocketUtil.send(Command.JOIN_ROOM, channel.toString());
                    }
                    chatPanel.setChannel(channel);
                }
            }
        });

        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Channel channel = userList.getSelectedValue();
                if (channel != null && chatPanel.getChannel() != channel) {
                    roomList.clearSelection();
                    if (chatPanel.getChannel().isRoom()) {
                        SocketUtil.send(Command.LEAVE_ROOM, "");
                    }
                    chatPanel.setChannel(channel);
                }
            }
        });
    }

    public void setChatPanel(ChatPanel chatPanel) {
        this.chatPanel = chatPanel;
        chatPanel.setChannel(rooms.firstElement());
    }

    public void addRoom(Channel channel) {
        rooms.addElement(channel);
    }

    public void addUser(Channel channel) {
        users.addElement(channel);
    }
}
