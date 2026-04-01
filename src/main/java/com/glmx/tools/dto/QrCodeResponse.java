package com.glmx.tools.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "二维码响应")
public class QrCodeResponse {

    @ApiModelProperty(value = "二维码Base64数据")
    private String base64Image;

    @ApiModelProperty(value = "Data URL格式")
    private String dataUrl;

    @ApiModelProperty(value = "二维码内容")
    private String content;

    @ApiModelProperty(value = "尺寸")
    private Integer size;
}
