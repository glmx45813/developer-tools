package com.glmx.tools.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(description = "TCP数据包")
public class TcpPacket {

    @ApiModelProperty(value = "数据内容")
    private String data;

    @ApiModelProperty(value = "十六进制格式")
    private String hexData;

    @ApiModelProperty(value = "方向: SEND(发送), RECEIVE(接收)")
    private String direction;

    @ApiModelProperty(value = "时间")
    private LocalDateTime timestamp;

    @ApiModelProperty(value = "数据长度(字节)")
    private Integer length;
}
