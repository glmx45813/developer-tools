package com.glmx.tools.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
@ApiModel(description = "MQTT订阅请求")
public class MqttSubscribeRequest {

    @ApiModelProperty(value = "客户端ID", required = true)
    @NotBlank(message = "客户端ID不能为空")
    private String clientId;

    @ApiModelProperty(value = "主题(单主题订阅时使用)")
    private String topic;

    @ApiModelProperty(value = "主题列表(批量订阅时使用，格式: topic或topic:qos)")
    private List<String> topics;

    @ApiModelProperty(value = "QoS级别: 0, 1, 2", example = "0")
    @Min(value = 0, message = "QoS最小为0")
    @Max(value = 2, message = "QoS最大为2")
    private Integer qos = 0;
}
