package com.gh.nps;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import io.netty.handler.traffic.TrafficCounter;

public class test {
    public static void main(String[] args) {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        final GlobalTrafficShapingHandler trafficHandler = new GlobalTrafficShapingHandler(workerGroup, 1000) {
            @Override
            protected void doAccounting(TrafficCounter counter) {
                super.doAccounting(counter);
                System.out.println("read: " + (counter.lastReadBytes()));
                System.out.println("write: " + (counter.lastWrittenBytes()));
            }
        };
        Bootstrap b = new Bootstrap();
        b.group(workerGroup);
        b.channel(NioSocketChannel.class);
        b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000);
        b.option(ChannelOption.SO_KEEPALIVE, false);
        b.option(ChannelOption.TCP_NODELAY, true);
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast("trafficHandler", trafficHandler);
            }
        });
        try {
             ChannelFuture channelFuture= b.connect("127.0.0.1", 2000).sync();
             while(true)
             {
                ByteBuf buf=ByteBufAllocator.DEFAULT.buffer(4020);
                buf.writeBytes(new byte[4020]);
                channelFuture.channel().writeAndFlush(buf);
             }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
}