package com.glmx.tools.service;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.List;

public class TcpMessageDecoder extends ByteToMessageDecoder {

    private static final Logger log = LoggerFactory.getLogger(TcpMessageDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int readableBytes = in.readableBytes();
        if (readableBytes > 0) {
            SocketAddress remoteAddress = ctx.channel().remoteAddress();
            byte[] bytes = new byte[readableBytes];
            in.readBytes(bytes);
            out.add(bytes);
            log.info("TCP解码消息: remoteAddress={}, length={}, hex={}", 
                    remoteAddress, readableBytes, bytesToHex(bytes));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("TCP消息解码异常: remoteAddress={}, errorType={}, message={}", 
                ctx.channel().remoteAddress(), cause.getClass().getSimpleName(), cause.getMessage(), cause);
        ctx.close();
    }

    private String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int len = Math.min(bytes.length, 100);
        for (int i = 0; i < len; i++) {
            sb.append(String.format("%02X ", bytes[i]));
        }
        if (bytes.length > 100) {
            sb.append("... (truncated)");
        }
        return sb.toString().trim();
    }
}
