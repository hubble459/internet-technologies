package ui;

import helper.AESUtil;
import helper.Shared;
import helper.SocketHelper;
import helper.model.Command;
import helper.model.Message;
import helper.model.Request;
import model.Channel;
import model.MainChannel;
import model.RoomChannel;
import model.UserChannel;
import util.Checksum;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Locale;

/**
 * MainScreen is basically the whole container of our program
 */
public class MainScreen extends JFrame implements ChatPanel.OnUploadListener, ChatPanel.CommandListener {
    private static final String DOWNLOAD_FOLDER = "downloads/";
    private final SocketHelper helper;
    private int pingTimeout = 30000; // Default afk timeout in ms
    private JPanel mainPanel;
    private ChannelPanel channelPanel;
    private ChatPanel chatPanel;
    private boolean kickPopup;
    private long lastActivity;

    public MainScreen(String title, SocketHelper helper) throws HeadlessException {
        super(title);
        this.helper = helper;
        setupJFrame();

        // When a channel is selected, leave the channel you are currently in
        // And then join the channel you clicked
        channelPanel.setOnChannelSelect(selectedChannel -> {
            Channel currentChannel = chatPanel.currentChannel();
            // If I'm in a channel and this is not the selected one
            if (currentChannel != null && currentChannel != selectedChannel) {
                // Check if im currently inside a room and I want to go to Main or a PM
                if (currentChannel.isRoom() && (selectedChannel.isMain() || selectedChannel.isPM())) {
                    // Leave the room
                    Request.build(helper)
                            .setCommand(Command.LEAVE_ROOM)
                            .send();
                } else if (selectedChannel.isRoom()) {
                    // If i want to join another room; send JOIN_ROOM message
                    Request.build(helper)
                            .setMessage(Command.JOIN_ROOM, selectedChannel.getName())
                            .setOnResponse((success, message) -> {
                                if (!success) {
                                    message.setCommand(Command.SERVER);
                                    messageChannel(selectedChannel, message);
                                }
                                return false;
                            })
                            .send();
                }

                // Start HANDSHAKE
                if (selectedChannel.isPM()) {
                    Request.build(helper)
                            .setCommand(Command.HANDSHAKE)
                            .setPayload(selectedChannel.getName())
                            .setOnResponse(this::handshake)
                            .setMaxRetries(5)
                            .send();
                }
            }
            chatPanel.setChannel(selectedChannel);
        });

        // Chat can send commands through MainScreen by using the command listener
        chatPanel.setCommandListener(this);

        // Chat can tell this screen to upload a file
        chatPanel.setOnUpload(this);

        // If SocketHelper receives a message from server
        helper.addOnReceivedListener(message -> {
            String username;
            String from;

            switch (message.getCommand()) {
                case PING:
                    // If PING; PONG back if you had any activity between now and 30 seconds ago.
                    // 30 seconds being pingTimout
                    long difference = System.currentTimeMillis() - lastActivity;
                    if (difference <= pingTimeout || pingTimeout == -1) {
                        Request.build(helper)
                                .setCommand(Command.PONG)
                                .setOnResponse((success, message1) -> !success)
                                .setMaxRetries(5)
                                .send();
                    }
                    break;
                case BROADCAST:
                    // When a BROADCAST is received add it to the main channel
                    Channel main = channelPanel.getMain();
                    messageChannel(main, message);
                    break;
                case BROADCAST_IN_ROOM:
                    // Someone in the room I'm in said something
                    // So show it in the chat
                    messageChannel(chatPanel.currentChannel(), message);
                    break;
                case ROOM_CREATED:
                    // When a room is created add it to the room list
                    channelPanel.addRoom(new RoomChannel(message.getPayload()));
                    break;
                case WHISPER:
                    // Received a PM from someone
                    from = message.getPayload().split(" ", 2)[0];
                    UserChannel userChannel = channelPanel.getChannelFromUsername(from);
                    messageChannel(userChannel, message);
                    break;
                case SESSION_TOKEN:
                    // Received a session token from someone who want to PM
                    // [username] [token encrypted with my pubKey]
                    String[] split = message.getPayload().split(" ", 2);
                    from = split[0];
                    String encryptedToken = split[1];
                    encryptedToken = new String(Base64.getDecoder().decode(encryptedToken));
                    String token = decryptWithPrivateKey(encryptedToken);
                    break;
                case JOINED_ROOM:
                    // Someone joined the room I'm in
                    message.setCommand(Command.SERVER);
                    chatPanel.addMessage(message, false);
                    break;
                case LEFT_ROOM:
                    // Someone left the room I'm in
                    String who = message.getPayload().split(" ", 2)[0];
                    if (!who.equals(Shared.username)) {
                        message.setCommand(Command.SERVER);
                        chatPanel.addMessage(message, false);
                    }
                    break;
                case LEFT:
                    // Someone left the server
                    message.setCommand(Command.SERVER);
                    messageChannel(channelPanel.getMain(), message);
                    break;
                case JOINED_SERVER:
                    // Someone joined the server
                    username = message.getPayload().split(" ", 2)[0];
                    channelPanel.addUser(new UserChannel(username));
                    message.setCommand(Command.SERVER);
                    chatPanel.addMessage(message, false);
                    break;
                case VOTES:
                    // Upon receiving a VOTES message, also start the kick
                    // if it wasn't already started, so no 'break;'
                case VOTE_KICK:
                    // Someone started the kick
                    if (!kickPopup) {
                        startKick();
                    }
                    break;
                case KICK_RESULT:
                    // Kick is over
                    // KRES <type> <username>
                    // If type is '1' someone got kicked
                    if (message.getPayload().startsWith("1")) {
                        // Get the part after the type, the username
                        username = message.getPayload().split(" ", 3)[1];
                        chatPanel.addMessage(new Message(Command.SERVER, username + " was kicked..."), true);
                        if (Shared.username.equals(username)) {
                            // If me
                            JOptionPane.showMessageDialog(null, "You were kicked from the chat!", "KICKED", JOptionPane.ERROR_MESSAGE);
                            channelPanel.gotoMain();
                        }
                    } else {
                        // No one was kicked
                        chatPanel.addMessage(new Message(Command.SERVER, "no-one was kicked"), true);
                    }
                    kickPopup = false;
                    break;
                case FILE:
                    // Received a file
                    String[] parts = message.getPayload().split(" ", 4);
                    from = parts[0];
                    String filename = parts[1];
                    double fileSize = Double.parseDouble(parts[2]);

                    // Show a popup that you received a file
                    SwingUtilities.invokeLater(() -> {
                        int chosen = JOptionPane.showConfirmDialog(null, String.format(Locale.US,
                                "You received a file from %s." +
                                        "It has a size of %fmb." +
                                        "Download file?", from, fileSize), "Download", JOptionPane.YES_NO_OPTION);
                        // If you want the file
                        if (chosen == JOptionPane.YES_OPTION) {
                            downloadConcurrently(filename);
                        }
                    });
            }
        });

        // Make the main room
        // Used to sent and receive broadcasts
        channelPanel.addRoom(new MainChannel());
        channelPanel.gotoMain();

        // Request a list of rooms
        Request.build(helper)
                .setCommand(Command.ROOMS)
                .setOnResponse((success, message) -> {
                    if (success) {
                        // Add rooms to channelPanel
                        String[] rooms = message.getPayload().split(";");
                        for (String room : rooms) {
                            channelPanel.addRoom(new RoomChannel(room));
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
                                channelPanel.addUser(new UserChannel(user));
                            }
                        }
                    } else {
                        Shared.stupidJSServer = true;
                    }
                    return false;
                })
                .send();

        // When a message is send through the helper
        helper.setOnSendListener(message -> {
            if (message.getCommand() == Command.FILE) {
                // Print this instead of all the Base64 stuff,
                // cus that do be lagging the console tho
                System.out.println("File SEND!");
            }
            // If its anything but PONG, update last activity
            if (message.getCommand() != Command.PONG) {
                lastActivity = System.currentTimeMillis();
            }
        });
    }

    private String decryptWithPrivateKey(String token) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, Shared.keyPair.getPrivate());
            byte[] decrypted = cipher.doFinal(token.getBytes());
            return new String(decrypted);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean handshake(boolean success, Message message) {
        if (success) {
            String publicKey = message.getPayload();

            try {
                Cipher cipher = Cipher.getInstance("RSA");

                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey.getBytes(StandardCharsets.UTF_8)));
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                PublicKey pubKey = keyFactory.generatePublic(keySpec);
                cipher.init(Cipher.ENCRYPT_MODE, pubKey);

                SecretKey secret = AESUtil.generateKey();
                String secretToken = Base64.getEncoder().encodeToString(secret.getEncoded());
                byte[] data = secretToken.getBytes();
                byte[] bytesToSend = cipher.doFinal(data);
                String base64ToSend = Base64.getEncoder().encodeToString(bytesToSend);
                String username = chatPanel.currentChannel().getName();
                Request.build(helper)
                        .setMessage(Command.SESSION_TOKEN, username + " " + base64ToSend)
                        .send();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return !success;
    }

    /**
     * Download file on another socket, so we can still send and receive messages.
     */
    private void downloadConcurrently(String filename) {
        try {
            // Create a new socket connection
            SocketHelper downloadHelper = new SocketHelper(helper.getIp(), helper.getPort());
            // Tell server to download
            Request.build(downloadHelper)
                    .setCommand(Command.DOWNLOAD)
                    .setPayload(filename)
                    .setOnResponse((success, message2) -> {
                        if (success) {
                            // Save the file
                            String[] parts2 = message2.getPayload().split(" ", 2);
                            String base64 = parts2[0];
                            String checksum = parts2[1];
                            downloadFile(filename, Base64.getDecoder().decode(base64), checksum);
                        }
                        downloadHelper.closeSocket();
                        return false;
                    })
                    .send();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Failed to download file\n" + e.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Save a message in a channel
     *
     * @param channel Channel
     * @param message Message
     * @param save    true if the message should be saved in the channel history
     */
    private void messageChannel(Channel channel, Message message, boolean save) {
        if (channel != null) {
            if (save && chatPanel.currentChannel() != channel) {
                channel.addMessage(message);
                channel.addNotification();
                channelPanel.refreshTabNotificationCount();
            } else {
                chatPanel.addMessage(message, save);
            }
        }
    }

    /**
     * {@link MainScreen#messageChannel(Channel, Message, boolean)}
     *
     * @param channel Channel
     * @param message Message
     */
    private void messageChannel(Channel channel, Message message) {
        messageChannel(channel, message, true);
    }

    /**
     * Setup the JFrame
     */
    private void setupJFrame() {
        // When JFrame gets closed, stop the program
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        // Main content pane is mainPanel
        setContentPane(mainPanel);
        // Set the window icon
        setIconImage(new ImageIcon("./Discord 3/uwu.jpg").getImage());
        // Setup the toolbar menu
        setupMenu();
        // Pack all items nicely into the frame
        pack();
        // Set location of the frame to the center of the screen
        setLocationRelativeTo(null);
        // Disable resizing because layout gets fuckie when it's resized
        setResizable(false);
        // Show the JFrame
        setVisible(true);
    }

    /**
     * Setup the options menu
     */
    private void setupMenu() {
        JMenuBar menuBar = new JMenuBar();

        // Make an options tab
        JMenu options = new JMenu("Options");
        // Shortcut for the tab is CTRL-O
        options.setMnemonic(KeyEvent.VK_O);

        // Add a quit button to the options
        options.add("Quit")
                // When clicked, send a quit request
                .addActionListener(e -> {
                    Shared.quit = true;
                    Request.build(helper)
                            .setCommand(Command.QUIT)
                            .setOnResponse((success, message) -> {
                                if (success) {
                                    // If successful stop the Java Program
                                    JOptionPane.showMessageDialog(null, "Quit successfully", "Quited", JOptionPane.PLAIN_MESSAGE);
                                    System.exit(0);
                                } else {
                                    Shared.quit = false;
                                }
                                return false;
                            })
                            // Send the message
                            .send();
                });

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
                60000 * 60,
        };
        options.add("Timeout")
                .addActionListener(e -> {
                    Integer millis = (Integer) JOptionPane.showInputDialog(null, "If you have not had any activity in the amount of milliseconds below, you will be timed out", "Timout Milliseconds", JOptionPane.PLAIN_MESSAGE, null, milliOptions, milliOptions[5]);
                    if (millis != null) {
                        pingTimeout = millis;
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

    /**
     * Show the kick dialog
     */
    private void startKick() {
        kickPopup = true;
        KickPanel dialog = new KickPanel(helper);
        dialog.setSocketHelper(helper);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    /**
     * Download a file
     *
     * @param filename filename to save as
     * @param bytes    file bytes
     * @param checksum file checksum
     */
    private void downloadFile(String filename, byte[] bytes, String checksum) {
        File downloads = new File(DOWNLOAD_FOLDER);
        // If downloads folder doesn't exist, create it
        if (!downloads.exists() && !downloads.mkdir()) {
            // If folder not created, show error
            messageChannel(chatPanel.currentChannel(), new Message(Command.SERVER, String.format("Unable to create '%s' folder", DOWNLOAD_FOLDER)), false);
            return;
        }

        try {
            // Get checksum of bytes received
            String fileChecksum = Checksum.getMD5Checksum(bytes);
            // Check if checksums match
            if (!checksum.equals(fileChecksum)) {
                throw new Exception("Uploaded file does not match given checksum");
            }

            // Create path
            File file = new File(DOWNLOAD_FOLDER + filename);
            int count = 0;
            // Check if file already exists
            while (file.exists()) {
                // Change name until filename is unique
                file = new File(DOWNLOAD_FOLDER + count++ + '_' + filename);
            }

            // Write file
            Files.write(file.toPath(), bytes);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            messageChannel(chatPanel.currentChannel(), new Message(Command.SERVER, "Could not save file<br/>" + e.getMessage()), false);
        }
    }

    @Override
    public void command(Message message) {
        Request.build(helper)
                .setMessage(message)
                .setOnResponse((success, msg) -> {
                    // Show response in chat
                    if (!success) {
                        msg.setCommand(Command.SERVER);
                        chatPanel.addMessage(msg);
                    } else if (message.getCommand() == Command.WHISPER) {
                        String to = message.getPayload().split(" ", 2)[0];
                        String payload = Shared.username + message.getPayload().substring(to.length());
                        message.setPayload(payload);
                        MainScreen.this.messageChannel(chatPanel.currentChannel(), message);
                    } else if (Shared.stupidJSServer && message.getCommand() == Command.BROADCAST) {
                        message.setPayload(Shared.username + " " + message.getPayload());
                        MainScreen.this.messageChannel(chatPanel.currentChannel(), message);
                    }
                    return false;
                })
                .send();
    }

    @Override
    public void upload() {
        new Thread(() -> {
            // Open file chooser
            JFileChooser fileChooser = new JFileChooser(System.getProperty("user.dir"));
            fileChooser.setApproveButtonText("Send");
            int returnValue = fileChooser.showOpenDialog(null);

            // When a file is selected
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                messageChannel(chatPanel.currentChannel(), new Message(Command.SERVER, "Sending file..."));
                // Get file
                File file = fileChooser.getSelectedFile();
                // Remove spaces from file
                String filename = file.getName().replace(" ", "_");
                try {
                    // Get bytes
                    byte[] bytes = Files.readAllBytes(file.toPath());
                    // Bytes to Base64
                    String base64 = Base64.getEncoder().encodeToString(bytes);
                    // Send file
                    final int lastPingTimeout = pingTimeout; // 360000
                    pingTimeout = -1;
                    Request.build(helper)
                            // FILE command
                            .setCommand(Command.FILE)
                            // [username] [filename] [base64] [checksum]
                            .setPayload(String.format("%s %s %s %s", chatPanel.currentChannel().getName(), filename, base64, Checksum.getMD5Checksum(file)))
                            .setOnResponse((success, message) -> {
                                boolean result = false;
                                if (success) {
                                    JOptionPane.showMessageDialog(null, "File send successfully!", "Send", JOptionPane.PLAIN_MESSAGE);
                                } else {
                                    result = JOptionPane.showConfirmDialog(null, "File failed to send...\nTry again?", "Failed", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
                                }
                                lastActivity = System.currentTimeMillis();
                                pingTimeout = lastPingTimeout;

                                return result;
                            })
                            .setMaxRetries(3)
                            .send();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
