package com.gh.nps;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;

public class MsgLocalServerProcessor extends ChannelInboundHandlerAdapter {
    ConnectPortRequestMsg connectPortRequestMsg;
    ConcurrentHashMap<Long, Channel> connectId_channel;
    String myId;
    public static AttributeKey<String> Confirm = AttributeKey.valueOf("Confirm");

    public MsgLocalServerProcessor(ConnectPortRequestMsg connectPortRequestMsg,
            ConcurrentHashMap<Long, Channel> connectId_channel, String myId) {
        this.connectPortRequestMsg = new ConnectPortRequestMsg(myId);
        this.connectPortRequestMsg.connectId = new Random().nextLong();
        this.connectPortRequestMsg.ip = connectPortRequestMsg.ip;
        this.connectPortRequestMsg.port = connectPortRequestMsg.port;
        this.connectPortRequestMsg.destUserId = connectPortRequestMsg.destUserId;
        this.connectId_channel = connectId_channel;
        this.myId = myId;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.channelReadComplete(ctx);
        // ctx.flush();
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        ctx.channel().config().setAutoRead(false);
        if (connectPortRequestMsg != null && Client.ctx != null) {
            Client.ctx.writeAndFlush(connectPortRequestMsg).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    connectId_channel.put(connectPortRequestMsg.connectId, ctx.channel());
                }
            });
            ctx.executor().schedule(new Runnable() {

                @Override
                public void run() {
                    if (!ctx.channel().hasAttr(Confirm)) {
                        ctx.channel().close();
                    }
                }
            }, 3, TimeUnit.SECONDS);
        } else {
            ctx.channel().close();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ConnectPortResponseMsg connectPortResponseMsg = new ConnectPortResponseMsg(myId);
        connectPortResponseMsg.connectId = connectPortRequestMsg.connectId;
        connectPortResponseMsg.result = -5;
        connectId_channel.remove(connectPortRequestMsg.connectId);
        Client.ctx.channel().writeAndFlush(connectPortResponseMsg);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf tmp = ByteBufAllocator.DEFAULT.buffer();
        tmp.writeInt(-1);
        tmp.writeLong(connectPortRequestMsg.connectId);
        tmp.ensureWritable(((ByteBuf) msg).readableBytes());            
        System.out.println("包大小："+((ByteBuf) msg).readableBytes());
        tmp.writeBytes(((ByteBuf) msg), ((ByteBuf) msg).readableBytes());
        if (Client.ctx != null) {
            Client.ctx.channel().writeAndFlush(tmp);
        } else {
            ctx.channel().close();
        }
    }

}