package com.gh.nps;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.gson.Gson;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.ReferenceCountUtil;

public class MsgDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(final ChannelHandlerContext ctx, final ByteBuf msg, final List<Object> out) throws Exception {
        Message message = null;
        final int type = msg.readInt();
        final Gson gson = new Gson();
        if (type > 0) {
            byte[] data = new byte[msg.readableBytes()];
            msg.readBytes(data);
            switch (type) {
                case 1:
                    message = gson.fromJson(new String(data, StandardCharsets.UTF_8), HelloMsg.class);
                    break;
                case 2:
                    message = gson.fromJson(new String(data, StandardCharsets.UTF_8), HeartBeatMsg.class);
                    break;
                case 3:
                    message = gson.fromJson(new String(data, StandardCharsets.UTF_8), ConnectPortRequestMsg.class);
                    break;
                case 4:
                    message = gson.fromJson(new String(data, StandardCharsets.UTF_8), ConnectPortResponseMsg.class);
                    break;
                default:
                    break;
            }
        } else if (type == -1) {
            long connectId = msg.readLong();
            message = new DataMsg("");
            ((DataMsg)message).connectId = connectId;
            ((DataMsg)message).data = msg;
            ReferenceCountUtil.retain(msg);
        }
        out.add(message);
    }

}