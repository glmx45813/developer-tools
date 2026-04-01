package com.glmx.tools.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "正则表达式模板")
public class RegexTemplate {

    @ApiModelProperty(value = "模板ID")
    private String id;

    @ApiModelProperty(value = "模板名称")
    private String name;

    @ApiModelProperty(value = "正则表达式")
    private String pattern;

    @ApiModelProperty(value = "描述")
    private String description;

    @ApiModelProperty(value = "示例")
    private String example;

    @ApiModelProperty(value = "分类")
    private String category;
}
