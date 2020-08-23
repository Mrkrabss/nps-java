package com.gh.nps;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.StringUtil;

public class MsgProcessorClient extends ChannelInboundHandlerAdapter {
    public static String myId = "123";
    static int clientType = 1;
    ConcurrentHashMap<Integer, ConnectPortRequestMsg> map;
    EventLoopGroup server;
    EventLoopGroup local;
    ConcurrentHashMap<Long, Channel> connectId_channel;

    public MsgProcessorClient(ConcurrentHashMap<Long, Channel> connectId_channel, EventLoopGroup local,
            EventLoopGroup server, ConcurrentHashMap<Integer, ConnectPortRequestMsg> map) {
        this.server = server;
        this.map = map;
        this.local = local;
        this.connectId_channel = connectId_channel;
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        HelloMsg helloMsg = new HelloMsg(myId);
        helloMsg.clientType = clientType;
        ctx.channel().writeAndFlush(helloMsg);
        Client.ctx = ctx;
        System.out.println("服务器连接成功");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        for (Channel channel : connectId_channel.values()) {
            channel.close();
        }
        Client.ctx = null;
        System.out.println("服务器断开连接");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }

    volatile int readCount = 0;

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Message) {
            Message message = (Message) msg;
            if (message.type == 2) {
                return;
            }
            if (message.type == -1) {
                DataMsg dataMsg = (DataMsg) message;
                final int size = dataMsg.data.readableBytes();
                readCount += size;
                if (readCount >= 1024 * 1024 * 2) {
                    ctx.channel().config().setAutoRead(false);
                }
                Channel channel = connectId_channel.get(dataMsg.connectId);
                if (channel != null) {
                    channel.writeAndFlush(dataMsg.data).addListener(new GenericFutureListener<Future<? super Void>>() {
                        @Override
                        public void operationComplete(Future<? super Void> future) throws Exception {
                            readCount -= size;
                            if (readCount < 1024 * 1024) {
                                ctx.channel().config().setAutoRead(true);
                                ctx.channel().read();
                            }
                        }
                    });
                }else{
                    ReferenceCountUtil.release(dataMsg.data);
                    readCount -= size;
                    if (readCount < 1024 * 1024) {
                        ctx.channel().config().setAutoRead(true);
                        ctx.channel().read();
                    }
                }
                return;
            }

            if (message.type == 3) {
                final ConnectPortRequestMsg connectPortRequestMsg = (ConnectPortRequestMsg) message;
                if (connectId_channel.contains(connectPortRequestMsg.connectId)) {
                    ConnectPortResponseMsg connectPortResponseMsg = new ConnectPortResponseMsg(myId);
                    connectPortResponseMsg.connectId = connectPortRequestMsg.connectId;
                    connectPortResponseMsg.result = -3;
                    ctx.channel().writeAndFlush(connectPortResponseMsg);
                    return;
                }
                if (local == null) {
                    ConnectPortResponseMsg connectPortResponseMsg = new ConnectPortResponseMsg(myId);
                    connectPortResponseMsg.connectId = connectPortRequestMsg.connectId;
                    connectPortResponseMsg.result = -6;
                    ctx.channel().writeAndFlush(connectPortResponseMsg);
                    return;
                }
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(local);
                bootstrap.channel(NioSocketChannel.class);
                bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000);
                bootstrap.option(ChannelOption.SO_KEEPALIVE, false);
                bootstrap.option(ChannelOption.TCP_NODELAY, true);
                bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(
                                new MsgLocalProcessor(connectPortRequestMsg.connectId, ctx, myId, connectId_channel));
                    }
                });
                bootstrap.connect(connectPortRequestMsg.ip, connectPortRequestMsg.port)
                        .addListener(new ChannelFutureListener() {
                            @Override
                            public void operationComplete(ChannelFuture future) throws Exception {
                                if (!future.isSuccess()) {
                                    ConnectPortResponseMsg connectPortResponseMsg = new ConnectPortResponseMsg(myId);
                                    connectPortResponseMsg.connectId = connectPortRequestMsg.connectId;
                                    connectPortResponseMsg.result = -4;
                                    ctx.channel().writeAndFlush(connectPortResponseMsg);
                                }

                            }
                        });
            }
            if (message.type == 4) {
                final ConnectPortResponseMsg connectPortResponseMsg = (ConnectPortResponseMsg) message;
                Channel channel = connectId_channel.get(connectPortResponseMsg.connectId);
                if (connectPortResponseMsg.result != 1 && channel != null) {
                    channel.close();
                    connectId_channel.remove(connectPortResponseMsg.connectId);
                    System.out.println("连接失败 "+ connectPortResponseMsg.result +" "+connectPortResponseMsg.userId);
                } else if (connectPortResponseMsg.result == 1 && channel != null) {
                    channel.config().setAutoRead(true);
                    channel.attr(MsgLocalServerProcessor.Confirm).set("YES");
                    System.out.println("连接成功");
                }
            }
            if (message.type == 5) {
                Onlines onlines = (Onlines) message;
                System.out.println(StringUtil.join(",",onlines.list));
            }
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE&&ctx.channel().attr(ClientInfo.NEED_CHECK_READ).get()!=null&&ctx.channel().attr(ClientInfo.NEED_CHECK_READ).get()) {
                ctx.close().sync();
            } else if (e.state() == IdleState.ALL_IDLE) {
                ctx.writeAndFlush(new HeartBeatMsg(myId));
                ctx.channel().attr(ClientInfo.NEED_CHECK_READ).set(true);
            }
        }

    }
}