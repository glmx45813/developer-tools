package com.glmx.tools.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
@ApiModel(description = "URL编解码请求")
public class UrlEncodeRequest {

    @ApiModelProperty(value = "待处理文本", required = true)
    @NotBlank(message = "待处理文本不能为空")
    private String text;

    @ApiModelProperty(value = "编码格式: UTF-8, GBK, ISO-8859-1", example = "UTF-8")
    private String charset = "UTF-8";
}
