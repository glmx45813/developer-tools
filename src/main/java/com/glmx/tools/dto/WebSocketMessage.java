package com.glmx.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {
    
    private String type;
    private String connectionId;
    private String connectionType;
    private Object data;
    private LocalDateTime timestamp;

    public static WebSocketMessage mqttMessage(String connectionId, Object data) {
        return WebSocketMessage.builder()
                .type("MQTT_MESSAGE")
                .connectionId(connectionId)
                .connectionType("MQTT")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static WebSocketMessage mqttStatus(String connectionId, String status) {
        return WebSocketMessage.builder()
                .type("MQTT_STATUS")
                .connectionId(connectionId)
                .connectionType("MQTT")
                .data(status)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static WebSocketMessage tcpPacket(String connectionId, Object data) {
        return WebSocketMessage.builder()
                .type("TCP_PACKET")
                .connectionId(connectionId)
                .connectionType("TCP")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static WebSocketMessage tcpStatus(String connectionId, String status) {
        return WebSocketMessage.builder()
                .type("TCP_STATUS")
                .connectionId(connectionId)
                .connectionType("TCP")
                .data(status)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static WebSocketMessage connectionList(Object data) {
        return WebSocketMessage.builder()
                .type("CONNECTION_LIST")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
