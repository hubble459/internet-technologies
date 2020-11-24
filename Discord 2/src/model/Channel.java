package model;

import util.SocketUtil;

import java.util.ArrayList;

public class Channel {
    private final String name;
    private final ArrayList<Message> messages;
    private final Command command;

    public Channel(String name, ChannelType type) {
        this.name = name;
        this.messages = new ArrayList<>();
        this.command = type.getCommand();
    }

    public ArrayList<Message> getMessages() {
        return messages;
    }

    public void addMessage(Message message) {
        this.messages.add(message);
    }

    public void send(String message) {
        if (isPM()) {
            SocketUtil.send(command, name + " " + message);
        } else {
            SocketUtil.send(command, message);
        }
    }

    public boolean isPM() {
        return command == Command.WHISPER;
    }

    public boolean isMain() {
        return command == Command.BROADCAST;
    }

    public boolean isRoom() {
        return command == Command.BROADCAST_IN_ROOM;
    }

    @Override
    public String toString() {
        return name;
    }

    public enum ChannelType {
        MAIN(Command.BROADCAST),
        ROOM(Command.BROADCAST_IN_ROOM),
        PM(Command.WHISPER);

        private final Command command;

        ChannelType(Command command) {
            this.command = command;
        }

        public Command getCommand() {
            return command;
        }
    }
}
