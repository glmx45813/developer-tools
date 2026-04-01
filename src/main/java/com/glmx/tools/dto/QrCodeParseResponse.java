package com.glmx.tools.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(description = "二维码识别响应")
public class QrCodeParseResponse {

    @ApiModelProperty(value = "识别到的内容列表")
    private List<String> contents;

    @ApiModelProperty(value = "识别成功数量")
    private Integer count;

    @ApiModelProperty(value = "图片格式")
    private String format;
}
