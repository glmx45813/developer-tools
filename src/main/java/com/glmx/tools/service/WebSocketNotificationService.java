package com.glmx.tools.service;

import com.glmx.tools.dto.WebSocketMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketNotificationService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketNotificationService.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void sendToUser(String username, String destination, WebSocketMessage message) {
        try {
            messagingTemplate.convertAndSendToUser(username, destination, message);
            log.info("WebSocket消息发送: user={}, destination={}, type={}", username, destination, message.getType());
        } catch (Exception e) {
            log.error("WebSocket消息发送失败: user={}, destination={}", username, destination, e);
        }
    }

    public void sendMqttMessage(String username, String connectionId, Object data) {
        WebSocketMessage message = WebSocketMessage.mqttMessage(connectionId, data);
        sendToUser(username, "/topic/mqtt", message);
    }

    public void sendMqttStatus(String username, String connectionId, String status) {
        WebSocketMessage message = WebSocketMessage.mqttStatus(connectionId, status);
        sendToUser(username, "/topic/mqtt", message);
    }

    public void sendTcpPacket(String username, String connectionId, Object data) {
        WebSocketMessage message = WebSocketMessage.tcpPacket(connectionId, data);
        sendToUser(username, "/topic/tcp", message);
    }

    public void sendTcpStatus(String username, String connectionId, String status) {
        WebSocketMessage message = WebSocketMessage.tcpStatus(connectionId, status);
        sendToUser(username, "/topic/tcp", message);
    }

    public void sendConnectionList(String username, Object data) {
        WebSocketMessage message = WebSocketMessage.connectionList(data);
        sendToUser(username, "/topic/connections", message);
    }
}
