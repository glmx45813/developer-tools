package com.glmx.tools.controller;

import com.glmx.tools.common.Result;
import com.glmx.tools.dto.*;
import com.glmx.tools.service.PromptOptimizeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "AI提示词优化")
@RestController
@RequestMapping("/api/prompt")
public class PromptOptimizeController {

    @Autowired
    private PromptOptimizeService promptOptimizeService;

    @ApiOperation("优化提示词")
    @PostMapping("/optimize")
    public Result<PromptOptimizeResponse> optimize(@RequestBody PromptOptimizeRequest request) {
        try {
            PromptOptimizeResponse response = promptOptimizeService.optimize(request);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @ApiOperation("分析提示词")
    @PostMapping("/analyze")
    public Result<PromptAnalysis> analyze(@RequestBody String prompt) {
        try {
            PromptOptimizeRequest request = new PromptOptimizeRequest();
            request.setPrompt(prompt);
            PromptOptimizeResponse response = promptOptimizeService.optimize(request);
            return Result.success(response.getAnalysis());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @ApiOperation("提取关键词")
    @PostMapping("/keywords")
    public Result<List<String>> extractKeywords(@RequestBody String prompt) {
        try {
            PromptOptimizeRequest request = new PromptOptimizeRequest();
            request.setPrompt(prompt);
            request.setEnhanceKeywords(false);
            PromptOptimizeResponse response = promptOptimizeService.optimize(request);
            return Result.success(response.getExtractedKeywords());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
