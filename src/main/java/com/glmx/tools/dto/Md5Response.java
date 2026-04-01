package com.glmx.tools.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "MD5加密响应")
public class Md5Response {

    @ApiModelProperty(value = "原始文本")
    private String originalText;

    @ApiModelProperty(value = "标准MD5(32位)")
    private String md5Standard;

    @ApiModelProperty(value = "16位MD5")
    private String md5Bit16;

    @ApiModelProperty(value = "32位MD5")
    private String md5Bit32;

    @ApiModelProperty(value = "选择的加密结果")
    private String result;

    @ApiModelProperty(value = "加密类型")
    private String type;
}
