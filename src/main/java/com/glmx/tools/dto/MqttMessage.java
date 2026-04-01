package com.glmx.tools.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(description = "MQTT消息")
public class MqttMessage {

    @ApiModelProperty(value = "消息ID")
    private String id;

    @ApiModelProperty(value = "主题")
    private String topic;

    @ApiModelProperty(value = "消息内容")
    private String payload;

    @ApiModelProperty(value = "QoS级别")
    private Integer qos;

    @ApiModelProperty(value = "是否保留")
    private Boolean retained;

    @ApiModelProperty(value = "时间")
    private LocalDateTime timestamp;

    @ApiModelProperty(value = "方向: IN(接收), OUT(发送)")
    private String direction;
}
