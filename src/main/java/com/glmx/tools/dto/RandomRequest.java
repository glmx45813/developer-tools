package com.glmx.tools.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@ApiModel(description = "随机数生成请求")
public class RandomRequest {

    @ApiModelProperty(value = "随机数长度", required = true)
    @NotNull(message = "长度不能为空")
    @Min(value = 1, message = "最小长度为1")
    @Max(value = 100, message = "最大长度为100")
    private Integer length;

    @ApiModelProperty(value = "随机数类型: NUMBER(纯数字), LETTER(纯字母), MIXED(混合), SPECIAL(含特殊字符)", example = "MIXED")
    private String type = "MIXED";

    @ApiModelProperty(value = "是否包含大写字母", example = "true")
    private Boolean includeUppercase = true;

    @ApiModelProperty(value = "是否包含小写字母", example = "true")
    private Boolean includeLowercase = true;

    @ApiModelProperty(value = "是否包含数字", example = "true")
    private Boolean includeNumber = true;

    @ApiModelProperty(value = "是否包含特殊字符", example = "false")
    private Boolean includeSpecial = false;

    @ApiModelProperty(value = "排除的字符")
    private String excludeChars = "";

    @ApiModelProperty(value = "生成数量", example = "1")
    @Min(value = 1, message = "最少生成1个")
    @Max(value = 100, message = "最多生成100个")
    private Integer count = 1;
}
