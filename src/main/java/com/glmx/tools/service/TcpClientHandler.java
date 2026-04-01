package com.glmx.tools.service;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.function.BiConsumer;

public class TcpClientHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(TcpClientHandler.class);
    private static final AttributeKey<String> CONNECTION_ID_KEY = AttributeKey.valueOf("connectionId");
    private static final AttributeKey<String> CHARSET_KEY = AttributeKey.valueOf("charset");
    private static final AttributeKey<BiConsumer<String, byte[]>> DATA_CALLBACK_KEY = AttributeKey.valueOf("dataCallback");

    private final String connectionId;
    private final String charset;
    private final BiConsumer<String, byte[]> dataCallback;
    private final Runnable disconnectCallback;

    public TcpClientHandler(String connectionId, String charset, 
                           BiConsumer<String, byte[]> dataCallback,
                           Runnable disconnectCallback) {
        this.connectionId = connectionId;
        this.charset = charset;
        this.dataCallback = dataCallback;
        this.disconnectCallback = disconnectCallback;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        SocketAddress remoteAddress = ctx.channel().remoteAddress();
        log.info("TCP连接建立成功: connectionId={}, remoteAddress={}", connectionId, remoteAddress);
        ctx.channel().attr(CONNECTION_ID_KEY).set(connectionId);
        ctx.channel().attr(CHARSET_KEY).set(charset);
        ctx.channel().attr(DATA_CALLBACK_KEY).set(dataCallback);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof byte[]) {
            byte[] data = (byte[]) msg;
            log.info("TCP收到数据: connectionId={}, length={}, hex={}", connectionId, data.length, bytesToHex(data));
            
            BiConsumer<String, byte[]> callback = ctx.channel().attr(DATA_CALLBACK_KEY).get();
            if (callback != null) {
                callback.accept(connectionId, data);
            }
        } else {
            log.warn("TCP收到未知类型消息: connectionId={}, msgType={}", connectionId, msg.getClass().getName());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        SocketAddress remoteAddress = ctx.channel().remoteAddress();
        log.info("TCP连接断开: connectionId={}, remoteAddress={}, isActive={}", 
                connectionId, remoteAddress, ctx.channel().isActive());
        
        if (disconnectCallback != null) {
            disconnectCallback.run();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("TCP连接异常: connectionId={}, errorType={}, message={}", 
                connectionId, cause.getClass().getSimpleName(), cause.getMessage(), cause);
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        log.info("TCP用户事件: connectionId={}, event={}", connectionId, evt);
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        log.info("TCP可写状态变化: connectionId={}, writable={}", connectionId, ctx.channel().isWritable());
        super.channelWritabilityChanged(ctx);
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
