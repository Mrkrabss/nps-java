package com.gh.nps;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class MsgLocalProcessor extends ChannelInboundHandlerAdapter {
    long connectId;
    ChannelHandlerContext out;
    String myId;
    ConcurrentHashMap<Long, Channel> connectId_channel;

    public MsgLocalProcessor(long connectId, ChannelHandlerContext out, String myId,
            ConcurrentHashMap<Long, Channel> connectId_channel) {
        this.connectId = connectId;
        this.out = out;
        this.myId = myId;
        this.connectId_channel = connectId_channel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        ConnectPortResponseMsg connectPortResponseMsg = new ConnectPortResponseMsg(myId);
        connectPortResponseMsg.connectId = connectId;
        connectPortResponseMsg.result = 1;
        connectId_channel.put(connectId, ctx.channel());
        out.channel().writeAndFlush(connectPortResponseMsg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        ConnectPortResponseMsg connectPortResponseMsg = new ConnectPortResponseMsg(myId);
        connectPortResponseMsg.connectId = connectId;
        connectPortResponseMsg.result = -5;
        connectId_channel.remove(connectId);
        out.channel().writeAndFlush(connectPortResponseMsg);
    }

    AtomicInteger readCount = new AtomicInteger();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        int size = ((ByteBuf) msg).readableBytes();
        int current = readCount.addAndGet(size);
        if (current > 1024 * 2014 * 1024 * 2 && ctx.channel().config().isAutoRead()) {
            ctx.channel().config().setAutoRead(false);
        }
        ByteBuf tmp = ByteBufAllocator.DEFAULT.buffer();
        tmp.writeInt(-1);
        tmp.writeLong(connectId);
        tmp.ensureWritable(((ByteBuf) msg).readableBytes());
        tmp.writeBytes((ByteBuf) msg);
        out.writeAndFlush(tmp);
        current = readCount.addAndGet(-size);
        if (current < 1024 * 2014 * 1024 && !ctx.channel().config().isAutoRead()) {
            ctx.channel().config().setAutoRead(true);
            ctx.channel().read();
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.channelReadComplete(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        ctx.channel().close();
    }

}