package com.gh.nps;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

public class MsgProcessorServer extends ChannelInboundHandlerAdapter {
    static String myId = "server";
    static ConcurrentHashMap<String, ChannelHandlerContext> id_channel = new ConcurrentHashMap<>();

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        System.out.println("channelActive");
        ctx.executor().schedule(new Runnable() {
            @Override
            public void run() {
                if (!ctx.channel().hasAttr(ClientInfo.CLIENT_INFO)) {
                    ctx.channel().close();
                }
            }
        }, 3, TimeUnit.SECONDS);
        // ctx.executor().scheduleAtFixedRate(new Runnable(){
        //     @Override
        //     public void run() {
        //         ctx.flush();
        //     }
        // }, 500, 500, TimeUnit.MILLISECONDS);
    }



    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        System.out.println("channelInactive");
        if (ctx.channel().hasAttr(ClientInfo.CLIENT_INFO)) {
            ChannelHandlerContext channelHandlerContext=id_channel.get(ctx.channel().attr(ClientInfo.CLIENT_INFO).get().helloMsg.userId);
            if(channelHandlerContext==ctx){
                id_channel.remove(ctx.channel().attr(ClientInfo.CLIENT_INFO).get().helloMsg.userId);
            }
            for (final Long connectId : ctx.channel().attr(ClientInfo.CLIENT_INFO).get().connectId_Channel.keySet()) {
                ConnectPortResponseMsg connectPortResponseMsg = new ConnectPortResponseMsg(myId);
                connectPortResponseMsg.connectId = connectId;
                connectPortResponseMsg.result = -5;
                ctx.channel().attr(ClientInfo.CLIENT_INFO).get().connectId_Channel.get(connectId)
                        .writeAndFlush(connectPortResponseMsg).addListener(new ChannelFutureListener() {

                            @Override
                            public void operationComplete(ChannelFuture future) throws Exception {
                                ctx.channel().attr(ClientInfo.CLIENT_INFO).get().connectId_Channel.get(connectId)
                                        .channel().attr(ClientInfo.CLIENT_INFO).get().connectId_Channel
                                                .remove(connectId);
                            }
                        });
            }
        }
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }

    // @Override
    // public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    //     ctx.flush();
    // }

    volatile int readCount = 0;



    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        super.channelRead(ctx, msg);
        if (msg instanceof Message) {
            final Message message = (Message) msg;
            System.out.println(message.type);
            if (message.type == 1) {
                HelloMsg helloMsg = (HelloMsg) msg;
                if (helloMsg.userId.equals(myId)) {
                    ctx.channel().close();
                    return;
                }
                ChannelHandlerContext channel = id_channel.get(helloMsg.userId);
                if (channel != null) {
                    channel.close();
                }
                ClientInfo clientInfo = new ClientInfo();
                clientInfo.helloMsg = helloMsg;
                ctx.channel().attr(ClientInfo.CLIENT_INFO).set(clientInfo);
                id_channel.put(helloMsg.userId, ctx);
                System.out.println(helloMsg.userId + " hello");
            } else if (!ctx.channel().hasAttr(ClientInfo.CLIENT_INFO)) {
                ctx.channel().close();
                return;
            }
            if (message.type == -1) {
                DataMsg dataMsg = (DataMsg) message;
                final int size=dataMsg.data.readableBytes();
                readCount+=size;
                if(readCount>=1024*1024*5){
                    ctx.channel().config().setAutoRead(false);
                }
                ChannelHandlerContext dst = ctx.channel().attr(ClientInfo.CLIENT_INFO).get().connectId_Channel
                        .get(dataMsg.connectId);
                if (dst != null) {
                    dst.writeAndFlush(dataMsg).addListener(new GenericFutureListener<Future<? super Void>>() {
                        @Override
                        public void operationComplete(Future<? super Void> future) throws Exception {
                            readCount-=size;
                            if(readCount<1024*1024){
                                ctx.channel().config().setAutoRead(true);
                                ctx.channel().read();
                            }
                        }
                    });
                } else {
                    ReferenceCountUtil.release(dataMsg.data);
                    readCount-=size;
                            if(readCount<1024*1024){
                                ctx.channel().config().setAutoRead(true);
                                ctx.channel().read();
                            }
                }
                return;
            }
            if (message.type == 2) {
                ctx.writeAndFlush(msg);
                return;
            }
            if (message.type == 3) {
                final ConnectPortRequestMsg connectPortRequestMsg = (ConnectPortRequestMsg) msg;
                if (ctx.channel().attr(ClientInfo.CLIENT_INFO).get().connectId_Channel
                        .containsKey(connectPortRequestMsg.connectId)) {
                    ConnectPortResponseMsg connectPortResponseMsg = new ConnectPortResponseMsg(myId);
                    connectPortResponseMsg.connectId = connectPortRequestMsg.connectId;
                    connectPortResponseMsg.result = -3;
                    ctx.writeAndFlush(connectPortResponseMsg);
                    return;
                }
                final ChannelHandlerContext channel = id_channel.get(connectPortRequestMsg.destUserId);
                if (channel != null) {
                    if (channel.channel().attr(ClientInfo.CLIENT_INFO).get().connectId_Channel
                            .contains(connectPortRequestMsg.connectId)) {
                        ConnectPortResponseMsg connectPortResponseMsg = new ConnectPortResponseMsg(myId);
                        connectPortResponseMsg.connectId = connectPortRequestMsg.connectId;
                        connectPortResponseMsg.result = -3;
                        ctx.writeAndFlush(connectPortResponseMsg);
                        return;
                    }
                    channel.channel().attr(ClientInfo.CLIENT_INFO).get().connectId_Channel
                            .put(connectPortRequestMsg.connectId, ctx);
                    ctx.channel().attr(ClientInfo.CLIENT_INFO).get().connectId_Channel
                            .put(connectPortRequestMsg.connectId, channel);
                    // ctx.executor().schedule(new Runnable() {

                    // @Override
                    // public void run() {
                    // if (!ctx.channel().attr(ClientInfo.CLIENT_INFO).get().connectId_Channel
                    // .containsKey(connectPortRequestMsg.connectId)) {
                    // channel.channel().attr(ClientInfo.CLIENT_INFO).get().connectId_Channel
                    // .remove(connectPortRequestMsg.connectId);
                    // ctx.channel().attr(ClientInfo.CLIENT_INFO).get().connectId_Channel
                    // .remove(connectPortRequestMsg.connectId);
                    // ConnectPortResponseMsg connectPortResponseMsg = new
                    // ConnectPortResponseMsg(myId);
                    // connectPortResponseMsg.connectId = connectPortRequestMsg.connectId;
                    // connectPortResponseMsg.result = -2;
                    // ctx.writeAndFlush(connectPortResponseMsg);
                    // }
                    // }
                    // }, 3, TimeUnit.SECONDS);
                    channel.writeAndFlush(connectPortRequestMsg);
                } else {
                    ConnectPortResponseMsg connectPortResponseMsg = new ConnectPortResponseMsg(myId);
                    connectPortResponseMsg.connectId = connectPortRequestMsg.connectId;
                    connectPortResponseMsg.result = -1;
                    ctx.writeAndFlush(connectPortResponseMsg);
                }
            }
            if (message.type == 4) {
                final ConnectPortResponseMsg connectPortResponseMsg = (ConnectPortResponseMsg) msg;
                final ChannelHandlerContext channelHandlerContext = ctx.channel().attr(ClientInfo.CLIENT_INFO)
                        .get().connectId_Channel.get(connectPortResponseMsg.connectId);
                if (channelHandlerContext != null) {
                    if (connectPortResponseMsg.result != 1) {
                        ctx.channel().attr(ClientInfo.CLIENT_INFO).get().connectId_Channel
                                .remove(connectPortResponseMsg.connectId);
                        channelHandlerContext.channel().attr(ClientInfo.CLIENT_INFO).get().connectId_Channel
                                .remove(connectPortResponseMsg.connectId);
                    }
                    channelHandlerContext.channel().writeAndFlush(connectPortResponseMsg);
                }
            }

        }
    }

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            final IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                ctx.channel().close().sync();
            }
        }

    }

}