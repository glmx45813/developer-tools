package com.glmx.tools.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "URL编解码响应")
public class UrlEncodeResponse {

    @ApiModelProperty(value = "原始文本")
    private String original;

    @ApiModelProperty(value = "编码后文本")
    private String encoded;

    @ApiModelProperty(value = "解码后文本")
    private String decoded;

    @ApiModelProperty(value = "使用的编码格式")
    private String charset;
}
