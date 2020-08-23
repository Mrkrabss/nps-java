package com.gh.nps;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
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

            ServerBootstrap websocket = new ServerBootstrap();
            websocket.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new HttpServerCodec());
                            ch.pipeline().addLast(new ChunkedWriteHandler());
                            ch.pipeline().addLast(new HttpObjectAggregator(64 * 1024));
                            ch.pipeline().addLast(new WebSocketServerProtocolHandler("/ws"));
                            ch.pipeline().addLast(new MessageToMessageDecoder<BinaryWebSocketFrame>() {

                                @Override
                                protected void decode(ChannelHandlerContext ctx, BinaryWebSocketFrame msg,
                                        List<Object> out) throws Exception {
                                    msg.content().retain();
                                    out.add(msg.content());
                                }

                            });
                            ch.pipeline().addLast(new MessageToMessageEncoder<ByteBuf>() {

                                @Override
                                protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out)
                                        throws Exception {
                                    msg.retain();
                                    out.add(new BinaryWebSocketFrame(msg));
                                }

                            });
                            ch.pipeline().addLast("MsgDecoder", new MsgDecoder());
                            ch.pipeline().addLast("MsgEncoder", new MsgEecoder());
                            ch.pipeline().addLast("MsgProcessorServer", new MsgProcessorServer());
                        }
                    }).childOption(ChannelOption.TCP_NODELAY, true).option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, false);

            ChannelFuture web_f = websocket.bind(8082).sync();
            web_f.channel().closeFuture().sync();
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
