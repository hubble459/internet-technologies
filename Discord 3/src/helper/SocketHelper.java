package helper;

import helper.model.Command;
import helper.model.Message;
import helper.model.Request;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Scanner;

public class SocketHelper implements Runnable {
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

    public SocketHelper(String ip, int port) throws IOException {
        this.ip = ip;
        this.port = port;
        this.required = new ArrayDeque<>();
        this.onReceivedListeners = new ArrayList<>();
        connect();
    }

    public void connect() throws IOException {
        this.socket = new Socket();
        this.socket.connect(new InetSocketAddress(ip, port), 2000);
//        this.socket.setSoTimeout(2000);
        this.reader = new Scanner(socket.getInputStream());
        this.writer = new PrintWriter(socket.getOutputStream());

        this.thread = new Thread(this);
        this.thread.start();
    }

    @Override
    public void run() {
        while (!socket.isClosed() && reader.hasNextLine()) {
            String line = reader.nextLine();
            if (line == null) break;

            Message message = Message.fromLine(line);

            for (Interfaces.OnReceivedListener onReceivedListener : onReceivedListeners) {
                new Thread(() -> onReceivedListener.onReceive(message)).start();
            }

            if (!welcomed) {
                // First message
                welcomed = true;
                if (onConnectListener != null) {
                    onConnectListener.onConnect(message);
                }
            } else if (message.isResponse()) {
                new Thread(() -> response(message)).start();
            } else if (message.getCommand() == Command.DISCONNECTED) {
                disconnected("Ping timeout");
                return;
            }
        }

        disconnected("Lost connection");
    }

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

    private boolean noPriority(Request request) {
        return !required.isEmpty() && required.peek() != request.getMessage().getCommand();
    }

    public synchronized void send(Request request) {
        new Thread(() -> syncSend(request)).start();
    }

    public synchronized void syncSend(Request request) {
        System.out.println("Sending: " + request);
        if (request == null) return;

        long start = System.currentTimeMillis();
        while (busySending() || noPriority(request)) {
            try {
                System.out.println("Waiting: " + request);
                wait(5000);

                // 30 seconds timeout
                if (System.currentTimeMillis() - start >= 30000) {
                    System.out.println("Timed out: " + this.request);
                    this.request = null;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        required.poll();

        this.request = request;

        write();
    }

    private synchronized void write() {
        if (socket != null && !socket.isClosed() && request != null) {
            writer.println(request.getMessage().toString());
            writer.flush();

            if (onSendListener != null) {
                onSendListener.onSend(request.getMessage());
            }
        }
    }

    private synchronized void response(Message message) {
        System.out.println("Response: " + message);
        if (request != null) {
            boolean success = message.isSuccessful();
            Request.OnResponse listener = request.getOnResponse();
            if (listener != null) {
                boolean retry = listener.response(success, message);
                if (retry) {
                    request.retry();
                }
            }

            request.reply();

            if (request.getWaitForRespond() != null) {
                request.getWaitForRespond().notifyLock();
            }

            request = null;

            notify();
        } else {
            System.err.println("Response but no request?");
        }
    }

    public void disconnected(String message) {
        if (onDisconnectListener != null) {
            onDisconnectListener.onDisconnect(message);
        }

        switch (actionOnDisconnect) {
            case RESTART:
                closeSocket();
                try {
                    System.out.println("Reconnecting: " + message);
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

    private void closeSocket() {
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
