package com.gh.nps;

import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * Hello world!
 *
 */
public class App {

    public static void run() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new IdleStateHandler(65, 0, 0, TimeUnit.SECONDS));
                            ch.pipeline().addLast("frameDecoder",
                                    new LengthFieldBasedFrameDecoder(1024 * 1024 * 100, 0, 4, 0, 4));
                            ch.pipeline().addLast("frameEncoder", new LengthFieldPrepender(4));
                            ch.pipeline().addLast("MsgDecoder", new MsgDecoder());
                            ch.pipeline().addLast("MsgEncoder", new MsgEecoder());
                            ch.pipeline().addLast("MsgProcessorServer", new MsgProcessorServer());

                        }
                    }).childOption(ChannelOption.TCP_NODELAY, true).option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, false);

            ChannelFuture f = b.bind(8081).sync();

            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        // System.out.println("Hello World!");
        run();
    }
}
