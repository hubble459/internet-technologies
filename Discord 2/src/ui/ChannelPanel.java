package ui;

import model.Channel;
import model.Command;
import model.Message;
import util.ServerUtil;

import javax.swing.*;

public class ChannelPanel {
    public static final String TITLE_ROOMS = "Rooms";
    public static final String TITLE_USERS = "Users";
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

        tabbedPane.addChangeListener(e -> refreshTabNotificationCount());

        roomList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Channel channel = roomList.getSelectedValue();
                if (channel != null && chatPanel.getChannel() != channel) {
                    userList.clearSelection();
                    if (channel.isMain()) {
                        ServerUtil.send(Command.LEAVE_ROOM);
                    } else {
                        ServerUtil.send(Command.JOIN_ROOM, channel.toString());
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
                        ServerUtil.send(Command.LEAVE_ROOM);
                    }
                    chatPanel.setChannel(channel);
                }
            }
        });

        ServerUtil.onReceive(message -> {
            if (message.getCommand() == Command.WHISPER
                    || message.getCommand() == Command.WHISPERED) {
                Channel current = chatPanel.getChannel();

                String from = ServerUtil.getUsername();

                if (message.getCommand() == Command.WHISPER) {
                    from = message.getPayload().split(" ", 2)[0];
                }

                if (!current.getName().equals(from)) {
                    Channel channel = getChannelFromUsername(from);
                    if (channel != null) {
                        channel.addMessage(message);
                        channel.addNotification();
                        userList.repaint();
                        refreshTabNotificationCount();
                    }
                }
            } else if (message.getCommand() == Command.BROADCAST
                    || message.getCommand() == Command.JOINED_SERVER) {
                Channel main = rooms.firstElement();
                if (chatPanel.getChannel() != main) {
                    main.addMessage(message);
                    main.addNotification();
                    roomList.repaint();
                    refreshTabNotificationCount();
                }
            } else if (message.getCommand() == Command.KICK_RESULT) {
                if (message.getPayload().startsWith("1")) {
                    String username = message.getPayload().split(" ", 3)[1];
                    chatPanel.addMessage(new Message(Command.BROADCAST_IN_ROOM, "SERVER " + username + " was kicked..."));
                    if (ServerUtil.getUsername().equals(username)) {
                        JOptionPane.showMessageDialog(null, "You were kicked from the chat!", "KICKED", JOptionPane.ERROR_MESSAGE);
                        gotoMain();
                    }
                } else {
                    chatPanel.addMessage(new Message(Command.BROADCAST_IN_ROOM, "SERVER no-one was kicked"));
                }
            }
        });
    }

    private void refreshTabNotificationCount() {
        if (tabbedPane.getSelectedIndex() == 1) {
            tabbedPane.setTitleAt(1, TITLE_USERS);

            Channel main = rooms.firstElement();
            if (main.getNotifications() != 0) {
                tabbedPane.setTitleAt(0, TITLE_ROOMS + " (" + main.getNotifications() + ")");
            } else {
                tabbedPane.setTitleAt(0, TITLE_ROOMS);
            }
        } else {
            tabbedPane.setTitleAt(0, TITLE_ROOMS);

            int notificationCount = getNotificationSum();
            if (notificationCount != 0) {
                tabbedPane.setTitleAt(1, TITLE_USERS + " (" + notificationCount + ")");
            } else {
                tabbedPane.setTitleAt(1, TITLE_USERS);
            }
        }
    }

    private int getNotificationSum() {
        int sum = 0;
        for (int i = 0; i < users.size(); i++) {
            sum += users.get(i).getNotifications();
        }
        return sum;
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

    public void gotoMain() {
        roomList.setSelectedIndex(0);
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
