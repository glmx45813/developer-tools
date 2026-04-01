package com.glmx.tools.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
@ApiModel(description = "MD5加密请求")
public class Md5Request {

    @ApiModelProperty(value = "待加密文本", required = true)
    @NotBlank(message = "待加密文本不能为空")
    private String text;

    @ApiModelProperty(value = "加密类型: STANDARD(标准32位), BIT16(16位), BIT32(32位)", example = "STANDARD")
    private String type = "STANDARD";

    @ApiModelProperty(value = "是否大写", example = "false")
    private Boolean uppercase = false;
}
