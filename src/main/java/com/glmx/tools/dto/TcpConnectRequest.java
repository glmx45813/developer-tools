package com.glmx.tools.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

@Data
@ApiModel(description = "TCP连接请求")
public class TcpConnectRequest {

    @ApiModelProperty(value = "服务器地址", required = true)
    @NotBlank(message = "服务器地址不能为空")
    private String host;

    @ApiModelProperty(value = "端口", required = true)
    @Min(value = 1, message = "端口最小为1")
    @Max(value = 65535, message = "端口最大为65535")
    private Integer port;

    @ApiModelProperty(value = "连接超时(毫秒)", example = "5000")
    @Min(value = 1000, message = "超时时间最小为1000毫秒")
    @Max(value = 30000, message = "超时时间最大为30000毫秒")
    private Integer timeout = 5000;

    @ApiModelProperty(value = "编码格式", example = "UTF-8")
    private String charset = "UTF-8";
}
