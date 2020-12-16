package ui;

import helper.Shared;
import helper.SocketHelper;
import helper.model.Command;
import helper.model.Message;
import helper.model.Request;
import model.Channel;
import model.MainChannel;
import model.RoomChannel;
import model.UserChannel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class MainScreen extends JFrame {
    private final SocketHelper helper;
    private int pingTimout = 30000; // Default afk timeout in ms
    private JPanel mainPanel;
    private ChannelPanel channelPanel;
    private ChatPanel chatPanel;
    private boolean kickPopup;
    private long lastActivity;

    public MainScreen(String title, SocketHelper helper) throws HeadlessException {
        super(title);
        this.helper = helper;
        setupJFrame();

        channelPanel.setOnChannelSelect(channel -> {
            if (chatPanel.currentChannel() != null
                    && chatPanel.currentChannel() != channel) {
                if (channel.isMain()) {
                    Request.build(helper)
                            .setCommand(Command.LEAVE_ROOM)
                            .send();
                } else if (channel.isRoom()) {
                    Request.build(helper)
                            .setMessage(Command.JOIN_ROOM, channel.getName())
                            .setOnResponse((success, message) -> {
                                if (!success) {
                                    message.setCommand(Command.SERVER);
                                    messageChannel(channel, message);
                                }
                                return false;
                            })
                            .send();
                }
            }
            chatPanel.setChannel(channel);
        });

        chatPanel.setCommandListener(message -> Request.build(helper)
                .setMessage(message)
                .setOnResponse((success, message1) -> {
                    if (!success) {
                        message1.setCommand(Command.SERVER);
                        chatPanel.addMessage(message1);
                    } else if (message.getCommand() == Command.WHISPER) {
                        String to = message.getPayload().split(" ", 2)[0];
                        String payload = Shared.username + message.getPayload().substring(to.length());
                        message.setPayload(payload);
                        messageChannel(chatPanel.currentChannel(), message);
                    }
                    return false;
                })
                .send());

        helper.addOnReceivedListener(message -> {
            switch (message.getCommand()) {
                case PING:
                    if (System.currentTimeMillis() - lastActivity <= pingTimout) {
                        Request.build(helper)
                                .setCommand(Command.PONG)
                                .send();
                    }
                    break;
                case BROADCAST:
                    Channel main = channelPanel.getMain();
                    main.addMessage(message);
                    if (chatPanel.currentChannel() == main) {
                        chatPanel.addMessage(message);
                    } else {
                        main.addNotification();
                        channelPanel.refreshTabNotificationCount();
                    }
                    break;
                case BROADCAST_IN_ROOM:
                    messageChannel(chatPanel.currentChannel(), message);
                    break;
                case ROOM_CREATED:
                    channelPanel.addRoom(new RoomChannel(helper, message.getPayload()));
                    break;
                case WHISPER:
                    String from = message.getPayload().split(" ", 2)[0];
                    UserChannel userChannel = channelPanel.getChannelFromUsername(from);
                    messageChannel(userChannel, message);
                    break;
                case JOINED_ROOM:
                    message.setCommand(Command.SERVER);
                    chatPanel.addMessage(message, false);
                    break;
                case LEFT_ROOM:
                    String who = message.getPayload().split(" ", 2)[0];
                    if (!who.equals(Shared.username)) {
                        message.setCommand(Command.SERVER);
                        chatPanel.addMessage(message, false);
                    }
                    break;
                case LEFT:
                    message.setCommand(Command.SERVER);
                    messageChannel(channelPanel.getMain(), message);
                    break;
                case JOINED_SERVER:
                    String username = message.getPayload().split(" ", 2)[0];
                    channelPanel.addUser(new UserChannel(helper, username));
                    message.setCommand(Command.SERVER);
                    chatPanel.addMessage(message, false);
                    break;
                case VOTES:
                    // Upon receiving a VOTES message, also start the kick
                    // if it wasn't already started, so no 'break;'
                case VOTE_KICK:
                    if (!kickPopup) {
                        startKick();
                    }
                    break;
                case KICK_RESULT:
                    // KRES <type> <username>
                    // If type is '1' someone got kicked
                    if (message.getPayload().startsWith("1")) {
                        // Get the part after the type, aka the username
                        String username1 = message.getPayload().split(" ", 3)[1];
                        //
                        chatPanel.addMessage(new Message(Command.SERVER, username1 + " was kicked..."), true);
                        if (Shared.username.equals(username1)) {
                            JOptionPane.showMessageDialog(null, "You were kicked from the chat!", "KICKED", JOptionPane.ERROR_MESSAGE);
                            channelPanel.gotoMain();
                        }
                    } else {
                        chatPanel.addMessage(new Message(Command.SERVER, "no-one was kicked"), true);
                    }
                    kickPopup = false;
                    break;
            }
        });

        // Make the main room
        // Used to sent and receive broadcasts
        channelPanel.addRoom(new MainChannel(helper));
        channelPanel.gotoMain();

        // Request a list of rooms
        Request.build(helper)
                .setCommand(Command.ROOMS)
                .setOnResponse((success, message) -> {
                    if (success) {
                        // Add rooms to channelPanel
                        String[] rooms = message.getPayload().split(";");
                        for (String room : rooms) {
                            channelPanel.addRoom(new RoomChannel(helper, room));
                        }
                    }
                    return false;
                })
                .send();

        // Request a list of users
        Request.build(helper)
                .setCommand(Command.USERS)
                .setOnResponse((success, message) -> {
                    if (success) {
                        // Add all users to the channelPanel
                        String[] users = message.getPayload().split(";");
                        for (String user : users) {
                            if (!user.equals(Shared.username)) {
                                channelPanel.addUser(new UserChannel(helper, user));
                            }
                        }
                    }
                    return false;
                })
                .send();

        helper.setOnSendListener(message -> lastActivity = System.currentTimeMillis());
    }

    private void messageChannel(Channel channel, Message message) {
        if (channel != null) {
            if (chatPanel.currentChannel() != channel) {
                channel.addNotification();
                channel.addMessage(message);
                channelPanel.refreshTabNotificationCount();
            } else {
                chatPanel.addMessage(message);
            }
        }
    }

    private void setupJFrame() {
        // When JFrame gets closed, stop the program
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        // Main content pane is mainPanel
        setContentPane(mainPanel);
        // Set the window icon
        ImageIcon img = new ImageIcon("./Discord 3/uwu.jpg");
        setIconImage(img.getImage());
        // Setup the toolbar menu
        setupMenu();
        // Pack all items nicely into the frame
        pack();
        // Set location of the frame to the center of the screen
        setLocationRelativeTo(null);
        // Disable resizing because layout get's fuckie when it's resized
        setResizable(false);
        // Show the JFrame
        setVisible(true);
    }

    private void setupMenu() {
        JMenuBar menuBar = new JMenuBar();

        // Make an options tab
        JMenu options = new JMenu("Options");
        // Shortcut for the tab is CTRL-O
        options.setMnemonic(KeyEvent.VK_O);

        // Add a quit button to the options
        options.add("Quit")
                // When clicked, send a quit request
                .addActionListener(e -> Request.build(helper)
                        .setCommand(Command.QUIT)
                        .setOnResponse((success, message) -> {
                            if (success) {
                                // If successful stop the Java Program
                                JOptionPane.showMessageDialog(null, "Quit successfully", "Quited", JOptionPane.PLAIN_MESSAGE);
                                System.exit(0);
                            }
                            return false;
                        })
                        // Send the message
                        .send());

        // Add a help button
        options.add("Help");

        // Add a help button
        Integer[] milliOptions = new Integer[]{
                500,
                1000,
                1000 * 2,
                1000 * 5,
                1000 * 10,
                1000 * 30,
                60000,
                60000 * 5,
                60000 * 10,
                60000 * 30,
        };
        options.add("Timeout")
                .addActionListener(e -> {
                    Integer millis = (Integer) JOptionPane.showInputDialog(null, "If you have not had any activity in the amount of milliseconds below, you will be timed out", "Timout Milliseconds", JOptionPane.PLAIN_MESSAGE, null, milliOptions, milliOptions[5]);
                    if (millis != null) {
                        pingTimout = millis;
                    }
                });

        // Add a Add Room button
        options.add("Add Room")
                // On click
                .addActionListener(e -> {
                    // Show input dialog to get the room name
                    String roomName = JOptionPane.showInputDialog(null, "Room name (3 ~ 14 chars, only characters, numbers and underscore)");
                    if (roomName != null) {
                        roomName = roomName.strip();
                        // Send a request
                        Request.build(helper)
                                // Message is create room with the room name
                                .setMessage(Command.CREATE_ROOM, roomName)
                                // On response show a dialog telling if room was successfully created or not
                                .setOnResponse((success, message) -> {
                                    if (success) {
                                        JOptionPane.showMessageDialog(this, "Room created!", "Success!", JOptionPane.PLAIN_MESSAGE);
                                    } else {
                                        JOptionPane.showMessageDialog(this, message.getPayload(), "Failed", JOptionPane.ERROR_MESSAGE);
                                    }
                                    return false;
                                })
                                // Send request
                                .send();
                    }
                });


        // Add options to the menu bar
        menuBar.add(options);

        // Set the menu bar to this JFrame
        setJMenuBar(menuBar);
    }

    private void startKick() {
        kickPopup = true;
        KickPanel dialog = new KickPanel(helper);
        dialog.setSocketHelper(helper);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    public interface CommandListener {
        void command(Message message);
    }
}