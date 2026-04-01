package com.glmx.tools.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@ApiModel(description = "二维码生成请求")
public class QrCodeGenerateRequest {

    @ApiModelProperty(value = "二维码内容", required = true)
    @NotBlank(message = "二维码内容不能为空")
    @Size(max = 2000, message = "内容长度不能超过2000字符")
    private String content;

    @ApiModelProperty(value = "二维码尺寸(像素)", example = "200")
    @Min(value = 100, message = "尺寸最小为100px")
    @Max(value = 500, message = "尺寸最大为500px")
    private Integer size = 200;

    @ApiModelProperty(value = "容错级别: L(7%), M(15%), Q(25%), H(30%)", example = "M")
    private String errorCorrectionLevel = "M";

    @ApiModelProperty(value = "前景色(十六进制)", example = "000000")
    private String foregroundColor = "000000";

    @ApiModelProperty(value = "背景色(十六进制)", example = "FFFFFF")
    private String backgroundColor = "FFFFFF";
}
