package com.glmx.tools.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
@ApiModel(description = "JSON处理请求")
public class JsonRequest {

    @ApiModelProperty(value = "JSON字符串", required = true)
    @NotBlank(message = "JSON内容不能为空")
    private String json;

    @ApiModelProperty(value = "缩进空格数", example = "2")
    private Integer indent = 2;

    @ApiModelProperty(value = "是否保留注释", example = "false")
    private Boolean keepComments = false;
}
