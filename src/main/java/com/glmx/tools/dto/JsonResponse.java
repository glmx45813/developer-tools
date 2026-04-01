package com.glmx.tools.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "JSON处理响应")
public class JsonResponse {

    @ApiModelProperty(value = "处理后的JSON")
    private String result;

    @ApiModelProperty(value = "是否有效")
    private Boolean valid;

    @ApiModelProperty(value = "错误信息")
    private String error;

    @ApiModelProperty(value = "错误位置(行)")
    private Integer errorLine;

    @ApiModelProperty(value = "错误位置(列)")
    private Integer errorColumn;

    @ApiModelProperty(value = "原始大小(字节)")
    private Integer originalSize;

    @ApiModelProperty(value = "处理后大小(字节)")
    private Integer resultSize;

    @ApiModelProperty(value = "压缩率")
    private String compressionRatio;
}
