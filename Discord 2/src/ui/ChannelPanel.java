package ui;

import model.Channel;
import model.Command;
import util.SocketUtil;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class ChannelPanel {
    private final DefaultListModel<Channel> rooms;
    private final DefaultListModel<Channel> users;
    private JPanel channelPanel;
    private JTabbedPane tabbedPane;
    private JList<Channel> roomList;
    private JList<Channel> userList;
    private JPanel roomsTab;
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

        SocketUtil.onReceive(message -> {
            if (message.getCommand() == Command.WHISPER) {
                Channel current = chatPanel.getChannel();
                String from = message.getPayload().split(" ", 2)[0];
                if (!current.getName().equals(from)) {
                    Channel channel = getChannelFromUsername(from);
                    if (channel != null) {
                        channel.addMessage(message);
                        channel.addNotification();
                        userList.repaint();
                    }
                }
            } else if (message.getCommand() == Command.BROADCAST) {
                Channel main = rooms.firstElement();
                if (chatPanel.getChannel() != main) {
                    main.addMessage(message);
                    main.addNotification();
                    roomList.repaint();
                }
            }
        });

        /*
         * [tab] [tab]
         * tab : JPanel -> JLabel
         */
    }

    public Channel getChannelFromUsername(String username) {
        for (int i = 0; i < users.getSize(); i++) {
            Channel channel = users.get(i);
            if (channel.getName().equals(username)) {
                return channel;
            }
        }
        return null;
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
