package model;

import helper.SocketHelper;

public class UserChannel extends Channel {
    public UserChannel(String name) {
        super(name, ChannelType.PM);
    }
}
