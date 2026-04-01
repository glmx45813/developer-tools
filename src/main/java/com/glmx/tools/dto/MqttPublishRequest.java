package com.glmx.tools.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

@Data
@ApiModel(description = "MQTT消息发布请求")
public class MqttPublishRequest {

    @ApiModelProperty(value = "客户端ID", required = true)
    @NotBlank(message = "客户端ID不能为空")
    private String clientId;

    @ApiModelProperty(value = "主题", required = true)
    @NotBlank(message = "主题不能为空")
    private String topic;

    @ApiModelProperty(value = "消息内容", required = true)
    @NotBlank(message = "消息内容不能为空")
    private String message;

    @ApiModelProperty(value = "QoS级别: 0, 1, 2", example = "0")
    @Min(value = 0, message = "QoS最小为0")
    @Max(value = 2, message = "QoS最大为2")
    private Integer qos = 0;

    @ApiModelProperty(value = "是否保留消息", example = "false")
    private Boolean retained = false;
}
