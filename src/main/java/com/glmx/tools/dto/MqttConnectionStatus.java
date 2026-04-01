package com.glmx.tools.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@ApiModel(description = "MQTT连接状态")
public class MqttConnectionStatus {

    @ApiModelProperty(value = "客户端ID")
    private String clientId;

    @ApiModelProperty(value = "服务器地址")
    private String broker;

    @ApiModelProperty(value = "端口")
    private Integer port;

    @ApiModelProperty(value = "连接协议: tcp, ssl, ws, wss")
    private String protocol;

    @ApiModelProperty(value = "连接状态: CONNECTED, DISCONNECTED, CONNECTING, RECONNECTING")
    private String status;

    @ApiModelProperty(value = "连接时间")
    private LocalDateTime connectedAt;

    @ApiModelProperty(value = "断开时间")
    private LocalDateTime disconnectedAt;

    @ApiModelProperty(value = "订阅的主题列表")
    private List<String> subscribedTopics;

    @ApiModelProperty(value = "消息统计")
    private MessageStats messageStats;

    @ApiModelProperty(value = "是否启用SSL")
    private Boolean sslEnabled;

    @ApiModelProperty(value = "是否自动重连")
    private Boolean autoReconnect;

    @ApiModelProperty(value = "重连次数")
    private Integer reconnectCount;

    @ApiModelProperty(value = "最后错误信息")
    private String lastError;

    @Data
    public static class MessageStats {
        @ApiModelProperty(value = "发送消息数")
        private Long sentCount;

        @ApiModelProperty(value = "接收消息数")
        private Long receivedCount;
    }
}
