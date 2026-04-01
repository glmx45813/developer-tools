package com.glmx.tools.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
@ApiModel(description = "正则表达式测试请求")
public class RegexRequest {

    @ApiModelProperty(value = "正则表达式", required = true)
    @NotBlank(message = "正则表达式不能为空")
    private String pattern;

    @ApiModelProperty(value = "待匹配文本", required = true)
    @NotBlank(message = "待匹配文本不能为空")
    private String text;

    @ApiModelProperty(value = "匹配模式: FIND(查找全部), MATCHES(完全匹配), MATCH(部分匹配)", example = "FIND")
    private String mode = "FIND";

    @ApiModelProperty(value = "是否忽略大小写", example = "false")
    private Boolean ignoreCase = false;

    @ApiModelProperty(value = "是否多行模式", example = "false")
    private Boolean multiline = false;

    @ApiModelProperty(value = "是否点匹配换行", example = "false")
    private Boolean dotAll = false;
}
