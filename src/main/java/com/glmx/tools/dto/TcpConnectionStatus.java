package com.glmx.tools.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@ApiModel(description = "TCP连接状态")
public class TcpConnectionStatus {

    @ApiModelProperty(value = "连接ID")
    private String connectionId;

    @ApiModelProperty(value = "服务器地址")
    private String host;

    @ApiModelProperty(value = "端口")
    private Integer port;

    @ApiModelProperty(value = "连接状态: CONNECTED, DISCONNECTED, CONNECTING")
    private String status;

    @ApiModelProperty(value = "连接时间")
    private LocalDateTime connectedAt;

    @ApiModelProperty(value = "编码格式")
    private String charset;

    @ApiModelProperty(value = "传输统计")
    private TransferStats transferStats;

    @ApiModelProperty(value = "数据包日志")
    private List<TcpPacket> packetLogs;

    @Data
    public static class TransferStats {
        @ApiModelProperty(value = "发送字节数")
        private Long bytesSent;

        @ApiModelProperty(value = "接收字节数")
        private Long bytesReceived;

        @ApiModelProperty(value = "发送包数")
        private Long packetsSent;

        @ApiModelProperty(value = "接收包数")
        private Long packetsReceived;
    }
}
