package com.glmx.tools.controller;

import com.glmx.tools.common.Result;
import com.glmx.tools.service.DiffService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "文本差异对比工具")
@RestController
@RequestMapping("/api/diff")
public class DiffController {

    @Autowired
    private DiffService diffService;

    @ApiOperation("文本差异对比")
    @PostMapping("/compare")
    public Result<DiffService.DiffResult> compare(@RequestBody DiffRequest request) {
        try {
            DiffService.DiffResult result = diffService.compare(
                request.getText1(),
                request.getText2(),
                request.getMode()
            );
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("生成HTML格式差异")
    @PostMapping("/html")
    public Result<String> generateHtmlDiff(@RequestBody DiffRequest request) {
        try {
            DiffService.DiffResult diffResult = diffService.compare(
                request.getText1(),
                request.getText2(),
                request.getMode()
            );
            String html = diffService.generateHtmlDiff(diffResult);
            return Result.success(html);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("生成Unified Diff格式")
    @PostMapping("/unified")
    public Result<String> generateUnifiedDiff(@RequestBody DiffRequest request) {
        try {
            String unifiedDiff = diffService.generateUnifiedDiff(
                request.getText1(),
                request.getText2(),
                request.getContextLines() != null ? request.getContextLines() : 3
            );
            return Result.success(unifiedDiff);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}

class DiffRequest {
    private String text1;
    private String text2;
    private String mode;
    private Integer contextLines;

    public String getText1() { return text1; }
    public void setText1(String text1) { this.text1 = text1; }
    public String getText2() { return text2; }
    public void setText2(String text2) { this.text2 = text2; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public Integer getContextLines() { return contextLines; }
    public void setContextLines(Integer contextLines) { this.contextLines = contextLines; }
}
