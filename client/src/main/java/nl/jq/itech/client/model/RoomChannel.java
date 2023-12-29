package nl.jq.itech.client.model;

/**
 * Talk in room channel
 */
public class RoomChannel extends Channel {
    public RoomChannel(String name) {
        super(name, ChannelType.ROOM);
    }
}
