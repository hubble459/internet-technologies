package ui;

import model.Channel;
import model.Command;
import model.Message;
import util.SocketUtil;

import javax.swing.*;
import java.awt.*;

public class MainScreen extends JFrame {
    private JPanel mainPanel;
    private ChannelPanel channelPanel;
    private ChatPanel chatPanel;
    private boolean askedForRooms;

    public MainScreen(String title) throws HeadlessException {
        super(title);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setContentPane(mainPanel);
        setupMenu();
        pack();
        setLocationRelativeTo(null);
        setResizable(false);

        setVisible(true);

        channelPanel.setChatPanel(chatPanel);

        SocketUtil.connect();

        SocketUtil.afterLogin(new SocketUtil.AfterLogin() {
            @Override
            public void loggedIn(String username) {
                SocketUtil.send(Command.ROOMS, "");
                askedForRooms = true;
                SocketUtil.removeAfterLogin(this);
            }
        });

        SocketUtil.onReceive(new SocketUtil.OnReceive() {
            @Override
            public void received(Message message) {
                if (askedForRooms && message.getCommand() == Command.ROOMS) {
                    String rooms = message.getPayload();
                    if (!rooms.isEmpty()) {
                        String[] roomNames = rooms.split(";");
                        for (String roomName : roomNames) {
                            channelPanel.addRoom(new Channel(roomName, Channel.ChannelType.ROOM));
                        }
                        SocketUtil.send(Command.USERS, "");
                    }
                } else if (askedForRooms && message.getCommand() == Command.UNKNOWN) {
                    SocketUtil.removeOnReceive(this);
                } else if (askedForRooms && message.getCommand() == Command.USERS) {
                    String users = message.getPayload();
                    if (!users.isEmpty()) {
                        String[] usernames = users.split(";");
                        for (String username : usernames) {
                            if (!username.equals(SocketUtil.getUsername())) {
                                channelPanel.addUser(new Channel(username, Channel.ChannelType.PM));
                            }
                        }
                    }
                } else if (askedForRooms && message.getCommand() == Command.JOINED) {
                    channelPanel.addUser(new Channel(message.getPayload(), Channel.ChannelType.PM));
                }
            }
        });
    }

    private void setupMenu() {
        JMenuBar menuBar = new JMenuBar();

        JMenu options = new JMenu("Options");

        options.add("Close");
        options.add("Logout");
        options.add("Help");

        menuBar.add(options);

        setJMenuBar(menuBar);
    }
}
