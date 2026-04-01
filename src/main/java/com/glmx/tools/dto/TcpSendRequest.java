package com.glmx.tools.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel(description = "TCP发送数据请求")
public class TcpSendRequest {

    @ApiModelProperty(value = "连接ID", required = true)
    @NotBlank(message = "连接ID不能为空")
    private String connectionId;

    @ApiModelProperty(value = "数据内容", required = true)
    @NotBlank(message = "数据内容不能为空")
    private String data;

    @ApiModelProperty(value = "数据格式: TEXT(文本), HEX(十六进制)", example = "TEXT")
    private String format = "TEXT";

    @ApiModelProperty(value = "是否添加换行符", example = "true")
    private Boolean addNewLine = true;
}
