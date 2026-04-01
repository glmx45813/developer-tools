package com.glmx.tools.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(description = "正则表达式测试响应")
public class RegexResponse {

    @ApiModelProperty(value = "是否匹配成功")
    private Boolean matched;

    @ApiModelProperty(value = "匹配结果列表")
    private List<MatchResult> matches;

    @ApiModelProperty(value = "匹配数量")
    private Integer matchCount;

    @ApiModelProperty(value = "错误信息")
    private String error;

    @ApiModelProperty(value = "正则表达式是否有效")
    private Boolean validPattern;

    @Data
    public static class MatchResult {
        @ApiModelProperty(value = "匹配内容")
        private String value;

        @ApiModelProperty(value = "起始位置")
        private Integer start;

        @ApiModelProperty(value = "结束位置")
        private Integer end;

        @ApiModelProperty(value = "分组信息")
        private List<GroupInfo> groups;
    }

    @Data
    public static class GroupInfo {
        @ApiModelProperty(value = "分组序号")
        private Integer index;

        @ApiModelProperty(value = "分组名称")
        private String name;

        @ApiModelProperty(value = "分组内容")
        private String value;
    }
}
