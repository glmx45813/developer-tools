package com.glmx.tools.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(description = "随机数生成响应")
public class RandomResponse {

    @ApiModelProperty(value = "生成的随机数列表")
    private List<String> values;

    @ApiModelProperty(value = "生成数量")
    private Integer count;

    @ApiModelProperty(value = "随机数长度")
    private Integer length;

    @ApiModelProperty(value = "随机数类型")
    private String type;
}
