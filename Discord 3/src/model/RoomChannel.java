package model;

import helper.SocketHelper;
import helper.model.Command;
import helper.model.Message;
import helper.model.Request;

import java.util.ArrayList;

public class RoomChannel extends Channel {
    public RoomChannel(String name) {
        super(name, ChannelType.ROOM);
    }
}
