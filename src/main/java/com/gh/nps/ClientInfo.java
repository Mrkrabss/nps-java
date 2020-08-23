package com.gh.nps;

import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;

public class ClientInfo {
    public static AttributeKey<ClientInfo> CLIENT_INFO = AttributeKey.valueOf("ClientInfo");
    public static AttributeKey<Boolean> NEED_CHECK_READ = AttributeKey.valueOf("needCheckRead");

    public HelloMsg helloMsg;
    public ConcurrentHashMap<Long,ChannelHandlerContext> connectId_Channel=new ConcurrentHashMap<>();
    public boolean needCheckRead =false;
    
}