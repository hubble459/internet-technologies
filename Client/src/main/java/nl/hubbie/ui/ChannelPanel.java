package nl.hubbie.ui;

import nl.hubbie.model.Channel;
import nl.hubbie.model.MainChannel;
import nl.hubbie.model.RoomChannel;
import nl.hubbie.model.UserChannel;

import javax.swing.*;
import java.awt.*;

public class ChannelPanel extends JPanel {
    private static final String TITLE_ROOMS = "Rooms";
    private static final String TITLE_USERS = "Users";

    private final DefaultListModel<Channel> rooms;
    private final DefaultListModel<UserChannel> users;
    private final JTabbedPane tabbedPane;
    private final JList<Channel> roomList;
    private final JList<UserChannel> userList;

    private OnChannelSelect onChannelSelect;

    public ChannelPanel() {
        rooms = new DefaultListModel<>();
        users = new DefaultListModel<>();

        tabbedPane = new JTabbedPane();
        roomList = new JList<>(rooms);
        userList = new JList<>(users);

        roomList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane roomScrollPane = new JScrollPane(roomList);
        JScrollPane userScrollPane = new JScrollPane(userList);

        JPanel roomPanel = new JPanel(new BorderLayout());
        roomPanel.add(roomScrollPane, BorderLayout.CENTER);

        JPanel userPanel = new JPanel(new BorderLayout());
        userPanel.add(userScrollPane, BorderLayout.CENTER);

        tabbedPane.addTab(TITLE_ROOMS, roomPanel);
        tabbedPane.addTab(TITLE_USERS, userPanel);

        add(tabbedPane);

        tabbedPane.addChangeListener(e -> refreshTabNotificationCount());

        roomList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Channel channel = roomList.getSelectedValue();
                if (channel != null) {
                    userList.clearSelection();
                    if (onChannelSelect != null) {
                        onChannelSelect.channel(channel);
                    }
                }
            }
        });

        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Channel channel = userList.getSelectedValue();
                if (channel != null) {
                    roomList.clearSelection();
                    if (onChannelSelect != null) {
                        onChannelSelect.channel(channel);
                    }
                }
            }
        });
    }

    public void setOnChannelSelect(OnChannelSelect onChannelSelect) {
        this.onChannelSelect = onChannelSelect;
    }

    public void refreshTabNotificationCount() {
        if (tabbedPane.getSelectedIndex() == 0) {
            int notificationCount = getNotificationSum();
            if (notificationCount != 0) {
                tabbedPane.setTitleAt(1, TITLE_USERS + " (" + notificationCount + ")");
            } else {
                tabbedPane.setTitleAt(1, TITLE_USERS);
            }
        } else {
            if (!rooms.isEmpty() && rooms.get(0).getNotifications() != 0) {
                tabbedPane.setTitleAt(0, TITLE_ROOMS + " (" + rooms.get(0).getNotifications() + ")");
            } else {
                tabbedPane.setTitleAt(0, TITLE_ROOMS);
            }
        }
        roomList.repaint();
        userList.repaint();
    }

    private int getNotificationSum() {
        int sum = 0;
        for (int i = 0; i < users.size(); i++) {
            sum += users.get(i).getNotifications();
        }
        return sum;
    }

    public void addRoom(MainChannel channel) {
        rooms.addElement(channel);
    }

    public void addRoom(RoomChannel channel) {
        rooms.addElement(channel);
    }

    public void addUser(UserChannel channel) {
        if (getChannelFromUsername(channel.getName()) == null) {
            users.addElement(channel);
        }
    }

    public void removeUser(String username) {
        UserChannel channel = getChannelFromUsername(username);
        if (channel != null) {
            users.removeElement(channel);
        }
    }

    public UserChannel getChannelFromUsername(String username) {
        for (int i = 0; i < users.size(); i++) {
            UserChannel channel = users.get(i);
            if (channel.getName().equals(username)) {
                return channel;
            }
        }
        return null;
    }

    public MainChannel getMain() {
        for (int i = 0; i < rooms.size(); i++) {
            Channel channel = rooms.get(i);
            if (channel instanceof MainChannel) {
                return (MainChannel) channel;
            }
        }

        return null;
    }

    public MainChannel gotoMain() {
        for (int i = 0; i < rooms.size(); i++) {
            Channel channel = rooms.get(i);
            if (channel instanceof MainChannel) {
                return (MainChannel) channel;
            }
        }

        return null;
    }

    public interface OnChannelSelect {
        void channel(Channel channel);
    }
}