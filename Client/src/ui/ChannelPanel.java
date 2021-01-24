package ui;

import model.Channel;
import model.MainChannel;
import model.RoomChannel;
import model.UserChannel;

import javax.swing.*;

/**
 * Channel part of the MainScreen
 * <p>
 * Shows a list of channels
 * - Main
 * - Rooms
 * - Users
 */
public class ChannelPanel {
    public static final String TITLE_ROOMS = "Rooms";
    public static final String TITLE_USERS = "Users";
    private final DefaultListModel<Channel> rooms;
    private final DefaultListModel<UserChannel> users;
    private JPanel channelPanel;
    private JTabbedPane tabbedPane;
    private JList<Channel> roomList;
    private JList<UserChannel> userList;

    private OnChannelSelect onChannelSelect;

    public ChannelPanel() {
        rooms = new DefaultListModel<>();
        users = new DefaultListModel<>();

        roomList.setModel(rooms);
        userList.setModel(users);

        tabbedPane.addChangeListener(e -> refreshTabNotificationCount());

        roomList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Channel channel = roomList.getSelectedValue();
                if (channel != null) {
                    userList.clearSelection();
                    onChannelSelect.channel(channel);
                }
            }
        });

        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Channel channel = userList.getSelectedValue();
                if (channel != null) {
                    roomList.clearSelection();
                    onChannelSelect.channel(channel);
                }
            }
        });
    }

    public void setOnChannelSelect(OnChannelSelect onChannelSelect) {
        this.onChannelSelect = onChannelSelect;
    }

    /**
     * Refresh the (n) notification count in the tab label
     */
    public void refreshTabNotificationCount() {
        // Set the room notification
        if (tabbedPane.getSelectedIndex() == 1) {
            tabbedPane.setTitleAt(1, TITLE_USERS);

            Channel main = rooms.firstElement();
            if (main.getNotifications() != 0) {
                tabbedPane.setTitleAt(0, TITLE_ROOMS + " (" + main.getNotifications() + ")");
            } else {
                tabbedPane.setTitleAt(0, TITLE_ROOMS);
            }
        } else {
            // Set the user notification
            tabbedPane.setTitleAt(0, TITLE_ROOMS);

            int notificationCount = getNotificationSum();
            if (notificationCount != 0) {
                tabbedPane.setTitleAt(1, TITLE_USERS + " (" + notificationCount + ")");
            } else {
                tabbedPane.setTitleAt(1, TITLE_USERS);
            }
        }
        // Refresh
        roomList.repaint();
        userList.repaint();
    }

    /**
     * Sum of notifications in user tab
     */
    private int getNotificationSum() {
        int sum = 0;
        for (int i = 0; i < users.size(); i++) {
            sum += users.get(i).getNotifications();
        }
        return sum;
    }

    /**
     * Get a user channel from their username
     *
     * @param username username
     * @return UserChannel
     */
    public UserChannel getChannelFromUsername(String username) {
        for (int i = 0; i < users.getSize(); i++) {
            UserChannel channel = users.get(i);
            if (channel.getName().equals(username)) {
                return channel;
            }
        }
        return null;
    }

    public void gotoMain() {
        roomList.setSelectedIndex(0);
    }

    public Channel getMain() {
        return rooms.get(0);
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

    public void reload() {
        roomList.repaint();
        userList.repaint();
    }

    public interface OnChannelSelect {
        void channel(Channel channel);
    }
}
