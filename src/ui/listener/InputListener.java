package ui.listener;

import ui.model.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class InputListener implements Runnable {
    private final Socket socket;
    private final BufferedReader reader;
    private final OnReply onReplyListener;

    public InputListener(Socket socket, OnReply listener) throws IOException {
        this.socket = socket;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.onReplyListener = listener;
    }

    @Override
    public void run() {
        try {
            while (!socket.isClosed()) {
                String line = reader.readLine().trim();
                String[] parts = line.split(" ");
                Message message = new Message(parts);
                onReplyListener.reply(message);
            }
        } catch (IOException e) {
            onReplyListener.onClosed();
        } catch (Exception ignored) {
        }
    }

    public interface OnReply {
        void reply(Message message);

        void onClosed();
    }
}
