package nl.jq.itech.client.helper;

import nl.jq.itech.client.helper.model.Command;
import nl.jq.itech.client.helper.model.Message;
import nl.jq.itech.client.helper.model.Request;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;

/**
 * SocketHelper
 * <p>
 * Handles the connection between server and client
 * After send a message it will wait for a response before sending another
 * Upon receiving a response the requester can easily handle and use it
 */
public class SocketHelper implements Runnable {
    private static boolean ENABLE_LOGGING = true; // false if logging should be disabled
    // If logging is enabled you can skip logging commands by adding them to the list below
    private static final List<Command> DONT_LOG = Arrays.asList(Command.PONG, Command.PING, Command.FILE);

    private final ArrayDeque<Command> required;
    private final ArrayList<Interfaces.OnReceivedListener> onReceivedListeners;
    private final String ip;
    private final int port;

    private Socket socket;
    private Scanner reader;
    private PrintWriter writer;
    private Thread thread;

    private Interfaces.OnDisconnectListener onDisconnectListener;
    private Interfaces.OnConnectListener onConnectListener;
    private Interfaces.OnSendListener onSendListener;
    private ActionOnDisconnect actionOnDisconnect = ActionOnDisconnect.NOTHING;
    private Request request;
    private boolean welcomed;

    /**
     * Create the SocketHelper
     *
     * @param ip   ip adres
     * @param port port
     * @throws IOException exception
     */
    public SocketHelper(String ip, int port) throws IOException {
        this.ip = ip;
        this.port = port;
        this.required = new ArrayDeque<>();
        this.onReceivedListeners = new ArrayList<>();
        connect();
    }

    /**
     * Connect with the server
     *
     * @throws IOException throw if connection fails
     */
    public void connect() throws IOException {
        this.socket = new Socket();
        // If a connection couldn't be made after 5 seconds throw IOException
        this.socket.connect(new InetSocketAddress(ip, port), 5000);
        // Create reader and writer
        this.reader = new Scanner(socket.getInputStream());
        this.writer = new PrintWriter(socket.getOutputStream());

        // Start this as a thread
        this.thread = new Thread(this);
        this.thread.start();
    }

    /**
     * Wait for input from the server
     */
    @Override
    public void run() {
        // While the socket isn't closed and the reader has a next line
        while (!socket.isClosed() && reader.hasNextLine()) {
            // Wait for nextLine
            String line = reader.nextLine();
            // If line is null; the connection was closed
            if (line == null) break;

            // Get the message from the line
            Message message = Message.fromLine(line);

            // Relay the message to all listeners
            for (Interfaces.OnReceivedListener onReceivedListener : new ArrayList<>(onReceivedListeners)) {
                // Start a thread for each listeners so the thread doesn't have to wait for these
                new Thread(() -> onReceivedListener.onReceive(message)).start();
            }

            // If this is the first message we get
            if (!welcomed) {
                welcomed = true;
                if (onConnectListener != null) {
                    // Tell the on connect listener we connected
                    onConnectListener.onConnect(message);
                }
            } else if (message.isResponse()) {
                /* If the message we received was a response to a request made earlier
                 * Call the response method*/
                new Thread(() -> response(message)).start();
            } else if (message.getCommand() == Command.DISCONNECTED) {
                // If a DISCONNECTED message was received, we have been disconnected from the server
                disconnected("Ping timeout");
                return;
            }
        }

        // If not in the while loop anymore, we are disconnected
        disconnected("Lost connection");
    }

    /**
     * Add priority to a command
     *
     * @param command command to prioritize
     */
    public void addPriority(Command command) {
        required.offer(command);
    }

    /**
     * True if already sending something and waiting for response
     *
     * @return busy sending
     */
    private boolean busySending() {
        return request != null;
    }

    /**
     * Check if request has no priority
     *
     * @param request request to check
     * @return true if no priority
     */
    private boolean noPriority(Request request) {
        return !required.isEmpty() && required.peek() != request.getMessage().getCommand();
    }

    /**
     * Asynchronously send a message to server
     *
     * @param request request to send
     */
    public synchronized void send(Request request) {
        new Thread(() -> syncSend(request)).start();
    }

    /**
     * Synchronously send a message to server
     *
     * @param request request to send
     */
    public synchronized void syncSend(Request request) {
        log("Sending: ", request);

        // Do not send null
        if (request == null) return;

        long start = System.currentTimeMillis();
        // While busyWaiting or something with priority is waiting
        while (busySending() || noPriority(request)) {
            try {
                // Wait with a timeout of 5 seconds
                wait(5000);
                log("Waiting: ", request);

                // 30 seconds timeout
                if (System.currentTimeMillis() - start >= 30000) {
                    log("Timed out: ", this.request);
                    this.request = null;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // Remove the required if there was one
        required.poll();

        // Set the request to a global value
        this.request = request;

        // Actually send the request to server
        write();
    }

    /**
     * Log a request
     *
     * @param prefix  the words in front of the request
     * @param request the request
     */
    private void log(String prefix, Request request) {
        if (ENABLE_LOGGING && !DONT_LOG.contains(request.getMessage().getCommand())) {
            System.out.println(prefix + request);
        }
    }

    /**
     * Send the {@link SocketHelper#request} to the server
     */
    private synchronized void write() {
        if (socket != null && !socket.isClosed() && request != null) {
            writer.println(request.getMessage().toString());
            writer.flush();

            if (onSendListener != null) {
                onSendListener.onSend(request.getMessage());
            }
        }
    }

    /**
     * Gets called when a response is received
     *
     * @param message response
     */
    private synchronized void response(Message message) {
        if (ENABLE_LOGGING && !DONT_LOG.contains(request.getMessage().getCommand())) {
            // Log if logging is enabled and the request was not something that should be logged
            System.out.println("Response: " + message);
        }

        // Check if the request is not null
        if (request != null) {
            boolean success = message.isSuccessful();
            Request.OnResponse listener = request.getOnResponse();
            if (listener != null) {
                // If there is a response listener, relay the message
                boolean retry = listener.response(success, message);
                if (retry) {
                    // if listener.response() returned true, then retry the request
                    request.retry();
                }
            }

            // Send a reply if there is one
            request.reply();

            if (request.getWaitForRespond() != null) {
                // Notify the requester if they were waiting for a response
                request.getWaitForRespond().notifyLock();
            }

            // Remove the request, since it is now reached the end of its lifeline
            request = null;

            // Notify other request that they can be made
            notify();
        } else if (ENABLE_LOGGING) {
            // If the request was null, then we got a response without requesting something
            System.err.println("Response but no request?");
        }
    }

    /**
     * Handles disconnection with server
     *
     * @param message reason
     */
    private void disconnected(String message) {
        // Tell the disconnect listener that there's been a disconnection
        if (onDisconnectListener != null) {
            onDisconnectListener.onDisconnect(message);
        }

        // Do the action on disconnect
        switch (actionOnDisconnect) {
            case RESTART:
                closeSocket();
                try {
                    connect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case STOP_PROGRAM:
                closeSocket();
                System.exit(0);
                break;
        }
    }

    /**
     * Close the socket if it wasn't already closed
     * And stop the running input thread
     */
    public void closeSocket() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!thread.isAlive()) {
            thread.interrupt();
        }
    }

    public void setActionOnDisconnect(ActionOnDisconnect actionOnDisconnect) {
        this.actionOnDisconnect = actionOnDisconnect;
    }

    public void setOnDisconnectListener(Interfaces.OnDisconnectListener onDisconnectListener) {
        this.onDisconnectListener = onDisconnectListener;
    }

    public void setOnConnectListener(Interfaces.OnConnectListener onConnectListener) {
        this.onConnectListener = onConnectListener;
    }

    public void setOnSendListener(Interfaces.OnSendListener onSendListener) {
        this.onSendListener = onSendListener;
    }

    public void addOnReceivedListener(Interfaces.OnReceivedListener onReceivedListener) {
        this.onReceivedListeners.add(onReceivedListener);
    }

    public void removeOnReceivedListener(Interfaces.OnReceivedListener onReceivedListener) {
        this.onReceivedListeners.remove(onReceivedListener);
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public void setDebug(boolean logging) {
        ENABLE_LOGGING = logging;
    }

    public enum ActionOnDisconnect {
        NOTHING,
        RESTART,
        STOP_PROGRAM
    }

    public static class Interfaces {
        public interface OnConnectListener {
            void onConnect(Message message);
        }

        public interface OnDisconnectListener {
            void onDisconnect(String e);
        }

        public interface OnReceivedListener {
            void onReceive(Message message);
        }

        public interface OnSendListener {
            void onSend(Message message);
        }
    }
}
