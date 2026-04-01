package com.glmx.tools.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "时间戳转换响应")
public class TimestampResponse {

    @ApiModelProperty(value = "时间戳(毫秒)")
    private Long timestampMs;

    @ApiModelProperty(value = "时间戳(秒)")
    private Long timestampSec;

    @ApiModelProperty(value = "格式化日期时间")
    private String formatted;

    @ApiModelProperty(value = "年")
    private Integer year;

    @ApiModelProperty(value = "月")
    private Integer month;

    @ApiModelProperty(value = "日")
    private Integer day;

    @ApiModelProperty(value = "时")
    private Integer hour;

    @ApiModelProperty(value = "分")
    private Integer minute;

    @ApiModelProperty(value = "秒")
    private Integer second;

    @ApiModelProperty(value = "星期几")
    private String weekday;

    @ApiModelProperty(value = "ISO格式")
    private String iso8601;
}
