package com.gh.nps;

import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;

public class ClientInfo {
    public static AttributeKey<ClientInfo> CLIENT_INFO = AttributeKey.valueOf("ClientInfo");
    public HelloMsg helloMsg;
    public ConcurrentHashMap<Long,ChannelHandlerContext> connectId_Channel=new ConcurrentHashMap<>();

    
}