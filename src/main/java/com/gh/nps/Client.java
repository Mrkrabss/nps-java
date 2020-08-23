package com.gh.nps;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import io.netty.handler.traffic.TrafficCounter;

public class Client {

    Bootstrap b;
    EventLoopGroup local;
    EventLoopGroup localServer;
    ConcurrentHashMap<Integer, ConnectPortRequestMsg> map = new ConcurrentHashMap<>();
    ConcurrentHashMap<Long, Channel> connectId_channel = new ConcurrentHashMap<>();
    public static ChannelHandlerContext ctx;

    public static void main(String[] args) throws Exception {
        // Client name 被控端启动命令
        // Client name ip:port:port:name
        args = new String[] { "", "127.0.0.1:8081:8081:LNGNW1K289","127.0.0.1:5555:5555:LNGNW1K289","127.0.0.1:22:8022:LNGNW1K289","127.0.0.1:8686:8686:LNGNW1K289" };
        
        // args=new String[]{};
        final Client client = new Client();
        if (args.length > 1) {
            MsgProcessorClient.myId = "456";
            client.initLocalServer();

            
            client.init();
            for (int i = 1; i < args.length; i++) {
                final String a[] = args[i].split(":");
                final ConnectPortRequestMsg connectPortRequestMsg = new ConnectPortRequestMsg(args[0]);
                connectPortRequestMsg.ip = a[0];
                connectPortRequestMsg.port = Integer.parseInt(a[1]);
                connectPortRequestMsg.destUserId = a[3];
                client.map.put(Integer.parseInt(a[2]), connectPortRequestMsg);
                try {
                    ServerBootstrap serverBootstrap = new ServerBootstrap();
                    serverBootstrap.group(client.localServer, client.localServer).channel(NioServerSocketChannel.class)
                            .childOption(ChannelOption.TCP_NODELAY, true).option(ChannelOption.SO_BACKLOG, 128)
                            .childHandler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                public void initChannel(SocketChannel ch) throws Exception {
                                    ch.pipeline().addLast(new MsgLocalServerProcessor(connectPortRequestMsg,
                                            client.connectId_channel, MsgProcessorClient.myId));
                                }
                            }).childOption(ChannelOption.SO_KEEPALIVE, false);
                    serverBootstrap.bind(Integer.parseInt(a[2])).sync();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            MsgProcessorClient.myId = "123";
            client.initLocal();
            client.init();
        }
        client.connect("120.92.133.186", 8081);
    }

    public void initLocal() {
        local = new NioEventLoopGroup();
    }

    public void initLocalServer() throws Exception {
        localServer = new NioEventLoopGroup();
    }

    public void init() {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        final GlobalTrafficShapingHandler trafficHandler = new GlobalTrafficShapingHandler(workerGroup, 1000){
            @Override
            protected void doAccounting(TrafficCounter counter) {
                super.doAccounting(counter);
                System.out.println("read: "+(counter.lastReadBytes()));
                System.out.println("write: "+(counter.lastWrittenBytes()));
            }
        };
        b = new Bootstrap();
        b.group(workerGroup);
        b.channel(NioSocketChannel.class);
        b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000);
        b.option(ChannelOption.SO_KEEPALIVE, false);
        b.option(ChannelOption.TCP_NODELAY, true);
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast("trafficHandler", trafficHandler);
                ch.pipeline().addLast(new IdleStateHandler(65, 60, 0, TimeUnit.SECONDS));
                ch.pipeline().addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1024 * 1024 * 100, 0, 4, 0, 4));
                ch.pipeline().addLast("frameEncoder", new LengthFieldPrepender(4));
                ch.pipeline().addLast("MsgDecoder", new MsgDecoder());
                ch.pipeline().addLast("MsgEncoder", new MsgEecoder());
                ch.pipeline().addLast("MsgProcessor",
                        new MsgProcessorClient(connectId_channel, local, localServer, map));
            }
        });

    }

    Channel channel;

    public void connect(final String host, final int port) {

        b.config().group().scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                try {
                    if (channel == null || !channel.isActive())
                        b.connect(host, port).sync().addListener(new ChannelFutureListener() {
                            @Override
                            public void operationComplete(ChannelFuture future) throws Exception {
                                channel = future.channel();
                            }
                        });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, 30, TimeUnit.SECONDS);

    }

}