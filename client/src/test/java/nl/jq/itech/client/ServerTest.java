package nl.jq.itech.client;

import nl.jq.itech.client.helper.AESUtil;
import nl.jq.itech.client.helper.SocketHelper;
import nl.jq.itech.client.helper.model.Command;
import nl.jq.itech.client.helper.model.Message;
import nl.jq.itech.client.helper.model.Request;
import org.junit.jupiter.api.*;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Make sure to restart the server before running these tests
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServerTest {
    private static SocketHelper quentinHelper;
    private static SocketHelper joostHelper;
    private static KeyPair quentinKeyPair;
    private static boolean loggedIn;

    @BeforeAll
    static void connect() throws IOException, NoSuchAlgorithmException {
        quentinHelper = new SocketHelper("localhost", 1337);
        quentinHelper.setDebug(false);
        quentinKeyPair = generateKeyPair();

        joostHelper = new SocketHelper("localhost", 1337);
        joostHelper.setDebug(false);
        KeyPair joostKeyPair = generateKeyPair();
        login("Joost", joostKeyPair, joostHelper);
    }

    @Test
    @Order(0)
    void loginRequired() {
        if (!loggedIn) {
            send(Command.USERS, test(false, "Please log in first"));
            send(Command.ROOMS, test(false, "Please log in first"));
            send(Command.ROOM, test(false, "Please log in first"));
            send(Command.BROADCAST, test(false, "Please log in first"));
        }
    }

    @Test
    void loginTest() {
        send(Command.LOGIN, test(false));
        send(Command.LOGIN, "no_key", test(false));
        send(Command.LOGIN, "bad_key a", test(false));
        send(Command.LOGIN, "bad~username a", test(false));
        if (!loggedIn) {
            loggedIn = true;
            send(Command.LOGIN, "Quentin " + publicKeyBase64(quentinKeyPair), test(true));
        }
        // Already logged in
        send(Command.LOGIN, "Quentin " + publicKeyBase64(quentinKeyPair), test(false));
    }

    @Test
    void testUsersAndRooms() {
        login();
        send(Command.ROOMS, test(true, "Random;Waifus;Java;Room"));
        send(Command.CREATE_ROOM, test(false));
        send(Command.CREATE_ROOM, "my_first_room", test(true));
        send(Command.ROOMS, test(true, "Random;Waifus;Java;Room;my_first_room"));

        send(Command.USERS, test(true, "Joost;Quentin"));
    }

    @Test
    void testBroadcast() {
        login();
        send(Command.BROADCAST, test(false));
        String msg = "owo";
        received(joostHelper, Command.BROADCAST, "Quentin " + msg, () -> send(quentinHelper, Command.BROADCAST, msg, test(true)));

        received(quentinHelper, Command.BROADCAST, "Joost " + msg, () -> send(joostHelper, Command.BROADCAST, msg, test(true)));
    }

    @Test
    void testRoom() {
        login();
        // Can't join room with no name
        send(Command.JOIN_ROOM, test(false));
        // Can't join unknown room
        send(Command.JOIN_ROOM, "unknown", test(false));
        // Can't talk in room if not in a room
        send(Command.BROADCAST_IN_ROOM, test(false));
        send(Command.BROADCAST_IN_ROOM, "owo", test(false));
        // Can't leave room if not in a room
        send(Command.LEAVE_ROOM, test(false));

        // Quentin joins Room
        send(Command.JOIN_ROOM, "Room", test(true));

        // Joost joins Room
        send(joostHelper, Command.JOIN_ROOM, "Room", test(true));

        String msg = "Hewo òwó";
        // Joost waits for a msg
        received(joostHelper, Command.BROADCAST_IN_ROOM, "Quentin " + msg, () -> {
            // Quentin sends message
            send(Command.BROADCAST_IN_ROOM, msg, test(true));
        });

        // Quentin waits for message
        received(quentinHelper, Command.BROADCAST_IN_ROOM, "Joost " + msg, () -> {
            // Joost sends message
            send(joostHelper, Command.BROADCAST_IN_ROOM, msg, test(true));
        });

        send(quentinHelper, Command.LEAVE_ROOM, "", test(true));
        send(joostHelper, Command.LEAVE_ROOM, "", test(true));
    }

    @Test
    void testPM() {
        login();
        send(Command.WHISPER, "unknown owo", test(false));

        String msg = "Hewo òwó";
        // Joost waits for a msg
        received(joostHelper, Command.WHISPER, "Quentin " + msg, () -> {
            // Quentin sends message
            send(Command.WHISPER, "Joost " + msg, test(true));
        });

        // Quentin waits for message
        received(quentinHelper, Command.WHISPER, "Joost " + msg, () -> {
            // Joost sends message
            send(joostHelper, Command.WHISPER, "Quentin " + msg, test(true));
        });
    }

    @Test
    void testEmergencyMeeting() {
        login();
        // Not in a room
        send(Command.START_KICK, test(false));
        // Join room
        send(quentinHelper, Command.JOIN_ROOM, "Room", test(true));
        send(joostHelper, Command.JOIN_ROOM, "Room", test(true));
        // Start meeting
        send(Command.START_KICK, test(true));
        // Already started
        send(Command.START_KICK, test(false));

        // Quentin votes but does not give a name
        send(Command.VOTE_KICK_USER, test(false));
        // Quentin votes for Joost
        send(Command.VOTE_KICK_USER, "Joost", test(true));
        // Quentin votes a second time
        send(Command.VOTE_KICK_USER, "Joost", test(false));
        send(Command.VOTE_SKIP, test(false));

        // Joost skips
        send(joostHelper, Command.VOTE_SKIP, "", test(true));
        // Votes should now be Joost 1;Quentin 0
        // So Joost should het kicked

        // Wait for server to kick Joost
        sleep();

        // Now Joost got kicked, we check this by trying to get all users in the Room
        // This should say that Joost is not in a room
        send(joostHelper, Command.ROOM, null, test(false, "Join a room first"));
    }

    @Test
    void testEncryption() throws Exception {
        login();

        // In this scenario Joost want to start an encrypted conversation
        // with Quentin so he needs his Public Key to send the session key

        // User with username not found
        send(Command.HANDSHAKE, "Quenny", test(false));
        // Get public key
        send(Command.HANDSHAKE, "Quentin", test(true)); // returns 200 abc==
        // Because it's really annoying to turn the base64 public key into an actual PublicKey
        // We will just use quentinKeyPair.getPublic() instead of converting it
        // If you want to see how we convert it, check MainScreen.java line 323

        // Now Joost had to encrypt a session key with Quentins PublicKey
        SecretKey sessionKey = AESUtil.generateKey();

        // Create a Cipher instance
        Cipher cipher = Cipher.getInstance("RSA");
        // Set ENCRYPT_MODE with Quentin's Public Key
        cipher.init(Cipher.ENCRYPT_MODE, quentinKeyPair.getPublic());
        // Encrypt sessionKey bytes with Quentin's  Public Key
        byte[] bytesToSend = cipher.doFinal(sessionKey.getEncoded());
        // Encode bytes into Base64
        String stringToSend = Base64.getEncoder().encodeToString(bytesToSend);

        // Send without username or bad username
        send(joostHelper, Command.SESSION_TOKEN, stringToSend, test(false));
        send(joostHelper, Command.SESSION_TOKEN, "bad " + stringToSend, test(false));
        // Server can't check if key is valid because server is unable to decrypt it

        // Quentin waits for session token
        received(quentinHelper, Command.SESSION_TOKEN, "Joost " + stringToSend, () -> {
            // Joost sends Quentin a session token
            send(joostHelper, Command.SESSION_TOKEN, "Quentin " + stringToSend, test(true));
        });

        // Now Quentin has the session key which is encrypted with his PublicKey and encoded in Base64
        // It's basically like this: [Base64[RSA_PublicKey[SessionToken]]]
        // Only Quentin can decrypt de session token because the private key is needed for that
        // And again, its a whole process to decrypt the key and turn the bytes into an actual SecretKey object
        // For this check line 155 in MainScreen.java, for this example we will use the sessionKey joost has generated

        String plainText = "owo";
        String message = AESUtil.encrypt(plainText, sessionKey);

        // Quentin waits for WHISPER
        received(quentinHelper, Command.WHISPER, "Joost " + message, () -> {
            // Joost sends Quentin an encrypted message
            send(joostHelper, Command.WHISPER, "Quentin " + message, test(true));
        });

        // Quentin can then decrypt the message with the sessionKey
        String received = AESUtil.decrypt(message, sessionKey);
        // The send and received message should be the same
        assertEquals(plainText, received);
    }

    /*                                                          *
     * Below this are methods to make the tests easier to write *
     *                                                          */

    static Request.OnResponse test(boolean success, String payload) {
        return (s, message) -> {
            assertEquals(success, s, message.toString());
            if (payload != null) {
                testPayload(message, payload);
            }
            return false;
        };
    }

    synchronized void received(SocketHelper helper, Command command, String payload, BeforeWait beforeWait) {
        final Object lock = new Object();
        Message[] message = new Message[1];
        helper.addOnReceivedListener(new SocketHelper.Interfaces.OnReceivedListener() {
            @Override
            public synchronized void onReceive(Message msg) {
                message[0] = msg;
                helper.removeOnReceivedListener(this);

                synchronized (lock) {
                    lock.notify();
                }
            }
        });
        beforeWait.action();
        synchronized (lock) {
            try {
                lock.wait(1000);
                assertEquals(command, message[0].getCommand());
                if (payload != null) {
                    testPayload(message[0], payload);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    interface BeforeWait {
        void action();
    }

    static Request.OnResponse test(boolean success) {
        return test(success, null);
    }

    static void testPayload(Message message, String msg) {
        assertEquals(msg, message.getPayload());
    }

    void send(Command command, Request.OnResponse onResponse) {
        send(quentinHelper, command, "", onResponse);
    }

    void send(Command command, String payload, Request.OnResponse onResponse) {
        send(quentinHelper, command, payload, onResponse);
    }

    void send(SocketHelper helper, Command command, String payload, Request.OnResponse onResponse) {
        final boolean[] suc = new boolean[1];
        final Message[] msg = new Message[1];
        Request.sendAndWaitForResponse(
                Request.build(helper)
                        .setMessage(command, payload)
                        .setOnResponse((success, message) -> {
                            suc[0] = success;
                            msg[0] = message;
                            return false;
                        })
        );

        onResponse.response(suc[0], msg[0]);
    }

    static String publicKeyBase64(KeyPair keyPair) {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    static void login() {
        if (!loggedIn) {
            loggedIn = true;
            login("Quentin", quentinKeyPair, quentinHelper);
        }
    }

    static void login(String name, KeyPair keyPair, SocketHelper helper) {
        Request.sendAndWaitForResponse(Request.build(helper)
                .setCommand(Command.LOGIN)
                .setPayload(name + ' ' + publicKeyBase64(keyPair))
                .setOnResponse(test(true)));
    }

    static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        return kpg.generateKeyPair();
    }

    /**
     * Sleep for one second
     */
    void sleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
