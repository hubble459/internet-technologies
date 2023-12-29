package nl.jq.itech.client.model;

/**
 * PM Channel (SEND)
 */
public class UserChannel extends Channel {
    public UserChannel(String name) {
        super(name, ChannelType.PM);
    }
}
