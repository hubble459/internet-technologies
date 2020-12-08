package ui;

import model.Channel;
import model.Command;
import model.Message;
import util.ServerUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

public class MainScreen extends JFrame {
    private JPanel mainPanel;
    private ChannelPanel channelPanel;
    private ChatPanel chatPanel;
    private boolean askedForRooms;

    public MainScreen(String title) throws HeadlessException {
        super(title);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setContentPane(mainPanel);
        ImageIcon img = new ImageIcon("./Discord 2/uwu.jpg");
        setIconImage(img.getImage());
        setupMenu();
        pack();
        setLocationRelativeTo(null);
        setResizable(false);

        setVisible(true);

        channelPanel.setChatPanel(chatPanel);

        ServerUtil.connect();

        ServerUtil.afterLogin(new ServerUtil.AfterLogin() {
            @Override
            public void loggedIn(String username) {
                ServerUtil.send(Command.ROOMS);
                askedForRooms = true;
                ServerUtil.removeAfterLogin(this);
            }
        });

        ServerUtil.onReceive(new ServerUtil.OnReceive() {
            @Override
            public void received(Message message) {
                if (askedForRooms && message.getCommand() == Command.ROOM_LIST) {
                    String rooms = message.getPayload();
                    if (!rooms.isEmpty()) {
                        String[] roomNames = rooms.split(";");
                        for (String roomName : roomNames) {
                            channelPanel.addRoom(new Channel(roomName, Channel.ChannelType.ROOM));
                        }
                        ServerUtil.send(Command.USERS);
                    }
                } else if (askedForRooms && message.getCommand() == Command.UNKNOWN) {
                    ServerUtil.removeOnReceive(this);
                } else if (askedForRooms && message.getCommand() == Command.USER_LIST) {
                    String users = message.getPayload();
                    if (!users.isEmpty()) {
                        String[] usernames = users.split(";");
                        for (String username : usernames) {
                            if (!username.equals(ServerUtil.getUsername())) {
                                channelPanel.addUser(new Channel(username, Channel.ChannelType.PM));
                            }
                        }
                    }
                } else if (askedForRooms && message.getCommand() == Command.JOINED_SERVER) {
                    channelPanel.addUser(new Channel(message.getPayload().split(" ", 2)[0], Channel.ChannelType.PM));
                } else if (askedForRooms && message.getCommand() == Command.ROOM_CREATED) {
                    channelPanel.addRoom(new Channel(message.getPayload(), Channel.ChannelType.ROOM));
                }
            }
        });
    }

    private void setupMenu() {
        JMenuBar menuBar = new JMenuBar();

        JMenu options = new JMenu("Options");
        options.setMnemonic(KeyEvent.VK_O);

        options.add("Quit").addActionListener(e -> {
            ServerUtil.onReceive(message -> {
                if (message.getCommand() == Command.QUITED) {
                    JOptionPane.showMessageDialog(null, "Quit successfully", "Quited", JOptionPane.PLAIN_MESSAGE);
                    System.exit(0);
                }
            });
            ServerUtil.send(Command.QUIT);
        });
        options.add("Help");

        menuBar.add(options);

        JMenuItem createRoom = new JMenuItem("Add Room");
        createRoom.setMnemonic(KeyEvent.VK_R);

        createRoom.addActionListener(e -> {
            String roomName = JOptionPane.showInputDialog(null, "Room name (3 ~ 14 chars, only characters, numbers and underscore)");
            if (roomName != null) {
                roomName = roomName.strip();
                if (roomName.matches("\\w{3,14}") && !roomName.equals("Main")) {
                    ServerUtil.send(Command.CREATE_ROOM, roomName);
                }
            }
            createRoom.setSelected(false);
        });

        menuBar.add(createRoom);

        setJMenuBar(menuBar);
    }
}
