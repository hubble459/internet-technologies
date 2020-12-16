package model;

import helper.SocketHelper;

public class UserChannel extends Channel {
    public UserChannel(SocketHelper helper, String name) {
        super(helper, name, ChannelType.PM);
    }
}
