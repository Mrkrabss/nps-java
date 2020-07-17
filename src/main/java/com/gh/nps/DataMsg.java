package com.gh.nps;

import io.netty.buffer.ByteBuf;

public class DataMsg extends Message{
    transient ByteBuf data;
    long connectId;
    public DataMsg(String userId) {
        super(-1, userId);
    }
    
    
}