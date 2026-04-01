package com.glmx.tools.service;

import com.glmx.tools.common.BusinessException;
import com.glmx.tools.common.ResultCode;
import com.glmx.tools.dto.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@Service
public class TcpService {

    private static final Logger log = LoggerFactory.getLogger(TcpService.class);

    private final Map<String, Channel> connections = new ConcurrentHashMap<>();
    private final Map<String, TcpConnectionStatus> connectionStatuses = new ConcurrentHashMap<>();
    private final Map<String, List<TcpPacket>> packetLogs = new ConcurrentHashMap<>();
    private final Map<String, String> connectionOwner = new ConcurrentHashMap<>();

    private static final int MAX_LOG_SIZE = 100;

    private EventLoopGroup workerGroup;

    @Autowired
    private WebSocketNotificationService notificationService;

    public TcpService() {
        workerGroup = new NioEventLoopGroup(4, new DefaultThreadFactory("tcp-client", true));
    }

    @PreDestroy
    public void destroy() {
        for (String connectionId : new ArrayList<>(connections.keySet())) {
            try {
                disconnect(connectionId);
            } catch (Exception e) {
                log.warn("关闭连接失败: {}", connectionId, e);
            }
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        }
    }

    public TcpConnectionStatus connect(String username, TcpConnectRequest request) {
        String connectionId = generateConnectionId(username);

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, request.getTimeout())
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_RCVBUF, 65536)
                    .option(ChannelOption.SO_SNDBUF, 65536)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast("logger", new LoggingHandler(LogLevel.INFO));
                            pipeline.addLast("decoder", new TcpMessageDecoder());
                            pipeline.addLast("handler", new TcpClientHandler(
                                    connectionId,
                                    request.getCharset(),
                                    (connId, data) -> handleReceivedData(username, connId, data),
                                    () -> handleDisconnect(username, connectionId)
                            ));
                        }
                    });

            ChannelFuture future = bootstrap.connect(request.getHost(), request.getPort()).sync();

            if (!future.isSuccess()) {
                throw new BusinessException(ResultCode.TCP_CONNECT_ERROR.getCode(), "连接失败");
            }

            Channel channel = future.channel();
            connections.put(connectionId, channel);
            connectionOwner.put(connectionId, username);

            TcpConnectionStatus status = new TcpConnectionStatus();
            status.setConnectionId(connectionId);
            status.setHost(request.getHost());
            status.setPort(request.getPort());
            status.setStatus("CONNECTED");
            status.setConnectedAt(LocalDateTime.now());
            status.setCharset(request.getCharset());

            TcpConnectionStatus.TransferStats stats = new TcpConnectionStatus.TransferStats();
            stats.setBytesSent(0L);
            stats.setBytesReceived(0L);
            stats.setPacketsSent(0L);
            stats.setPacketsReceived(0L);
            status.setTransferStats(stats);

            packetLogs.put(connectionId, new CopyOnWriteArrayList<>());
            status.setPacketLogs(new ArrayList<>());

            connectionStatuses.put(connectionId, status);

            log.info("TCP连接成功: connectionId={}, user={}, host={}, port={}", connectionId, username, request.getHost(), request.getPort());
            notificationService.sendTcpStatus(username, connectionId, "CONNECTED");

            return status;

        } catch (BusinessException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ResultCode.TCP_CONNECT_ERROR.getCode(), "连接被中断");
        } catch (Exception e) {
            log.error("TCP连接失败", e);
            throw new BusinessException(ResultCode.TCP_CONNECT_ERROR.getCode(), "连接失败: " + e.getMessage());
        }
    }

    private String generateConnectionId(String username) {
        return username + "_tcp_" + UUID.randomUUID().toString().substring(0, 8);
    }

    public void disconnect(String username, String connectionId) {
        validateOwnership(username, connectionId);
        disconnect(connectionId);
        notificationService.sendTcpStatus(username, connectionId, "DISCONNECTED");
    }

    private void disconnect(String connectionId) {
        Channel channel = connections.remove(connectionId);
        if (channel != null) {
            try {
                channel.close().await(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        connectionStatuses.remove(connectionId);
        packetLogs.remove(connectionId);
        connectionOwner.remove(connectionId);
        log.info("TCP连接已断开: {}", connectionId);
    }

    public void send(String username, TcpSendRequest request) {
        validateOwnership(username, request.getConnectionId());
        
        Channel channel = connections.get(request.getConnectionId());
        if (channel == null || !channel.isActive()) {
            throw new BusinessException("TCP连接不存在或已关闭");
        }

        TcpConnectionStatus status = connectionStatuses.get(request.getConnectionId());
        String charset = status != null ? status.getCharset() : "UTF-8";

        try {
            byte[] data;
            if ("HEX".equalsIgnoreCase(request.getFormat())) {
                data = hexStringToBytes(request.getData());
                log.info("TCP发送HEX数据: connectionId={}, hexLength={}", request.getConnectionId(), data.length);
            } else {
                String text = request.getData();
                if (Boolean.TRUE.equals(request.getAddNewLine())) {
                    if (!text.endsWith("\n") && !text.endsWith("\r\n")) {
                        text += "\n";
                        log.info("TCP添加换行符: connectionId={}", request.getConnectionId());
                    }
                }
                data = text.getBytes(Charset.forName(charset));
                log.info("TCP发送TEXT数据: connectionId={}, textLength={}, charset={}", 
                        request.getConnectionId(), data.length, charset);
            }

            ByteBuf buffer = Unpooled.wrappedBuffer(data);
            ChannelFuture sendFuture = channel.writeAndFlush(buffer);
            sendFuture.addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    log.info("TCP数据写入成功: connectionId={}, length={}", request.getConnectionId(), data.length);
                } else {
                    log.error("TCP数据写入失败: connectionId={}, error={}", 
                            request.getConnectionId(), future.cause().getMessage(), future.cause());
                }
            });
            sendFuture.await(5, TimeUnit.SECONDS);

            TcpPacket packet = addPacketLog(request.getConnectionId(), data, "SEND", charset);

            if (status != null && status.getTransferStats() != null) {
                TcpConnectionStatus.TransferStats stats = status.getTransferStats();
                stats.setBytesSent(stats.getBytesSent() + data.length);
                stats.setPacketsSent(stats.getPacketsSent() + 1);
            }

            notificationService.sendTcpPacket(username, request.getConnectionId(), packet);

            log.info("TCP数据发送成功: connectionId={}, length={}, hex={}", 
                    request.getConnectionId(), data.length, bytesToHexString(data));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ResultCode.TCP_SEND_ERROR.getCode(), "发送被中断");
        } catch (Exception e) {
            log.error("TCP数据发送失败", e);
            throw new BusinessException(ResultCode.TCP_SEND_ERROR.getCode(), "数据发送失败: " + e.getMessage());
        }
    }

    public TcpConnectionStatus getStatus(String username, String connectionId) {
        validateOwnership(username, connectionId);
        return getStatus(connectionId);
    }

    private TcpConnectionStatus getStatus(String connectionId) {
        TcpConnectionStatus status = connectionStatuses.get(connectionId);
        if (status != null) {
            Channel channel = connections.get(connectionId);
            if (channel != null) {
                status.setStatus(channel.isActive() ? "CONNECTED" : "DISCONNECTED");
            } else {
                status.setStatus("DISCONNECTED");
            }
            status.setPacketLogs(new ArrayList<>(packetLogs.getOrDefault(connectionId, new ArrayList<>())));
        }
        return status;
    }

    public List<TcpConnectionStatus> getUserConnections(String username) {
        List<TcpConnectionStatus> result = new ArrayList<>();
        for (String connectionId : connectionStatuses.keySet()) {
            String owner = connectionOwner.get(connectionId);
            if (username.equals(owner)) {
                result.add(getStatus(connectionId));
            }
        }
        return result;
    }

    public void clearLogs(String username, String connectionId) {
        validateOwnership(username, connectionId);
        List<TcpPacket> logs = packetLogs.get(connectionId);
        if (logs != null) {
            logs.clear();
        }
    }

    private void validateOwnership(String username, String connectionId) {
        String owner = connectionOwner.get(connectionId);
        if (owner == null) {
            throw new BusinessException("连接不存在");
        }
        if (!username.equals(owner)) {
            throw new BusinessException("无权访问此连接");
        }
    }

    private void handleReceivedData(String username, String connectionId, byte[] data) {
        TcpConnectionStatus status = connectionStatuses.get(connectionId);
        String charset = status != null ? status.getCharset() : "UTF-8";
        
        TcpPacket packet = addPacketLog(connectionId, data, "RECEIVE", charset);

        if (status != null && status.getTransferStats() != null) {
            TcpConnectionStatus.TransferStats stats = status.getTransferStats();
            stats.setBytesReceived(stats.getBytesReceived() + data.length);
            stats.setPacketsReceived(stats.getPacketsReceived() + 1);
        }

        notificationService.sendTcpPacket(username, connectionId, packet);
    }

    private void handleDisconnect(String username, String connectionId) {
        TcpConnectionStatus status = connectionStatuses.get(connectionId);
        if (status != null) {
            status.setStatus("DISCONNECTED");
        }
        connections.remove(connectionId);
        log.info("TCP连接断开回调: {}", connectionId);
        notificationService.sendTcpStatus(username, connectionId, "DISCONNECTED");
    }

    private TcpPacket addPacketLog(String connectionId, byte[] data, String direction, String charset) {
        List<TcpPacket> logs = packetLogs.get(connectionId);
        TcpPacket packet = new TcpPacket();
        
        String textData = new String(data, Charset.forName(charset));
        
        boolean isPrintable = isPrintableText(data, charset);
        
        if (isPrintable) {
            packet.setData(textData);
        } else {
            packet.setData("[二进制数据] " + bytesToHexString(data));
        }
        
        packet.setHexData(bytesToHexString(data));
        packet.setDirection(direction);
        packet.setTimestamp(LocalDateTime.now());
        packet.setLength(data.length);

        if (logs != null) {
            logs.add(packet);

            while (logs.size() > MAX_LOG_SIZE) {
                logs.remove(0);
            }
        }
        return packet;
    }
    
    private boolean isPrintableText(byte[] data, String charset) {
        try {
            String text = new String(data, Charset.forName(charset));
            for (char c : text.toCharArray()) {
                if (c < 32 && c != '\n' && c != '\r' && c != '\t') {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] hexStringToBytes(String hexString) {
        String cleanHex = hexString.replaceAll("\\s+", "");
        
        if (cleanHex.isEmpty()) {
            throw new BusinessException("HEX数据不能为空");
        }
        
        if (cleanHex.length() % 2 != 0) {
            throw new BusinessException("HEX格式错误：字节数必须为偶数，当前长度为 " + cleanHex.length());
        }
        
        if (!cleanHex.matches("^[0-9A-Fa-f]+$")) {
            throw new BusinessException("HEX格式错误：只能包含0-9和A-F字符");
        }
        
        int len = cleanHex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(cleanHex.charAt(i), 16) << 4)
                    + Character.digit(cleanHex.charAt(i + 1), 16));
        }
        return data;
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}
