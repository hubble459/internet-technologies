package model;

import helper.SocketHelper;

public class MainChannel extends Channel {
    public MainChannel(SocketHelper helper) {
        super(helper, "Main", ChannelType.MAIN);
    }
}
