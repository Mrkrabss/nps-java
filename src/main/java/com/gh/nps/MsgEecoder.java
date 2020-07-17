package com.gh.nps;

import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.ReferenceCountUtil;

public class MsgEecoder extends MessageToByteEncoder<Message> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
        if (msg.getType() > 0) {
            Gson gson = new Gson();
            byte[] json = gson.toJson(msg).getBytes(StandardCharsets.UTF_8);
            out.writeInt(msg.type);
            out.writeBytes(json);
        } else if (msg.getType() == -1) {
            DataMsg dataMsg=(DataMsg)msg;
            out.writeInt(msg.type);
            out.writeLong(dataMsg.connectId);
            out.ensureWritable(((DataMsg) msg).data.readableBytes());
            out.writeBytes(((DataMsg) msg).data);
            ReferenceCountUtil.release(((DataMsg) msg).data);
        }
    }

}