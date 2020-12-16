package model;

import helper.SocketHelper;
import helper.model.Command;
import helper.model.Message;
import helper.model.Request;

import java.util.ArrayList;

public class RoomChannel extends Channel {
    public RoomChannel(SocketHelper helper, String name) {
        super(helper, name, ChannelType.ROOM);
    }
}
