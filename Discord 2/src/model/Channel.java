package model;

import util.ServerUtil;

import java.util.ArrayList;

public class Channel {
    private final String name;
    private final ArrayList<Message> messages;
    private final Command command;
    private int notification;

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
            ServerUtil.send(command, name + " " + message);
        } else {
            ServerUtil.send(command, message);
        }
    }

    public Command getCommand() {
        return command;
    }

    public String getName() {
        return name;
    }

    public int getNotifications() {
        return notification;
    }

    public void clearNotifications() {
        notification = 0;
    }

    public void addNotification() {
        notification++;
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
        String notif = "";
        if (notification > 0) {
            notif = " (" + notification + ")";
        }
        return name + notif;
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
