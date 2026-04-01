package com.glmx.tools.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

@Data
@ApiModel(description = "MQTT连接请求")
public class MqttConnectRequest {

    @ApiModelProperty(value = "服务器地址", required = true)
    @NotBlank(message = "服务器地址不能为空")
    private String broker;

    @ApiModelProperty(value = "端口", example = "1883")
    @Min(value = 1, message = "端口最小为1")
    @Max(value = 65535, message = "端口最大为65535")
    private Integer port = 1883;

    @ApiModelProperty(value = "客户端ID")
    private String clientId;

    @ApiModelProperty(value = "用户名")
    private String username;

    @ApiModelProperty(value = "密码")
    private String password;

    @ApiModelProperty(value = "协议版本: 3.1.1 或 5.0", example = "3.1.1")
    private String version = "3.1.1";

    @ApiModelProperty(value = "是否清除会话", example = "true")
    private Boolean cleanSession = true;

    @ApiModelProperty(value = "连接超时(秒)", example = "30")
    @Min(value = 5, message = "超时时间最小为5秒")
    @Max(value = 120, message = "超时时间最大为120秒")
    private Integer connectionTimeout = 30;

    @ApiModelProperty(value = "心跳间隔(秒)", example = "60")
    @Min(value = 10, message = "心跳间隔最小为10秒")
    @Max(value = 300, message = "心跳间隔最大为300秒")
    private Integer keepAliveInterval = 60;

    @ApiModelProperty(value = "自动重连", example = "true")
    private Boolean autoReconnect = true;

    @ApiModelProperty(value = "连接协议: tcp, ssl, ws, wss", example = "tcp")
    private String protocol = "tcp";

    @ApiModelProperty(value = "是否启用SSL/TLS", example = "false")
    private Boolean sslEnabled = false;

    @ApiModelProperty(value = "SSL证书路径(可选，用于双向认证)")
    private String sslCertificatePath;

    @ApiModelProperty(value = "SSL证书密码(可选)")
    private String sslCertificatePassword;

    @ApiModelProperty(value = "遗嘱主题")
    private String willTopic;

    @ApiModelProperty(value = "遗嘱消息内容")
    private String willMessage;

    @ApiModelProperty(value = "遗嘱消息QoS", example = "0")
    @Min(value = 0, message = "QoS最小为0")
    @Max(value = 2, message = "QoS最大为2")
    private Integer willQos = 0;

    @ApiModelProperty(value = "遗嘱消息是否保留", example = "false")
    private Boolean willRetained = false;

    @ApiModelProperty(value = "最大重连延迟(秒)", example = "60")
    @Min(value = 1, message = "最大重连延迟最小为1秒")
    @Max(value = 300, message = "最大重连延迟最大为300秒")
    private Integer maxReconnectDelay = 60;
}
