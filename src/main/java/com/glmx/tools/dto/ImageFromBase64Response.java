package com.glmx.tools.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "Base64转图片响应")
public class ImageFromBase64Response {

    @ApiModelProperty(value = "图片数据(Base64)")
    private String imageData;

    @ApiModelProperty(value = "图片格式")
    private String format;

    @ApiModelProperty(value = "图片宽度")
    private Integer width;

    @ApiModelProperty(value = "图片高度")
    private Integer height;
}
