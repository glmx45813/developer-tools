package com.glmx.tools.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "时间戳转换请求")
public class TimestampRequest {

    @ApiModelProperty(value = "时间戳(毫秒或秒)")
    private Long timestamp;

    @ApiModelProperty(value = "日期时间字符串")
    private String datetime;

    @ApiModelProperty(value = "日期格式", example = "yyyy-MM-dd HH:mm:ss")
    private String pattern = "yyyy-MM-dd HH:mm:ss";

    @ApiModelProperty(value = "时区", example = "GMT+8")
    private String timezone = "GMT+8";
}
