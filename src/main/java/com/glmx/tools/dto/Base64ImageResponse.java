package com.glmx.tools.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "Base64图片响应")
public class Base64ImageResponse {

    @ApiModelProperty(value = "Base64编码")
    private String base64;

    @ApiModelProperty(value = "Data URL格式(带MIME类型前缀)")
    private String dataUrl;

    @ApiModelProperty(value = "图片格式")
    private String format;

    @ApiModelProperty(value = "文件大小(字节)")
    private Long size;

    @ApiModelProperty(value = "原始文件名")
    private String filename;
}
