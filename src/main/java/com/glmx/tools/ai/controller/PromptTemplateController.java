package com.glmx.tools.ai.controller;

import com.glmx.tools.ai.model.PromptTemplate;
import com.glmx.tools.ai.service.PromptTemplateService;
import com.glmx.tools.common.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "Prompt模板库")
@RestController
@RequestMapping("/api/prompt-template")
public class PromptTemplateController {

    @Autowired
    private PromptTemplateService promptTemplateService;

    @ApiOperation("获取所有系统模板")
    @GetMapping("/list")
    public Result<List<PromptTemplate>> getAllTemplates() {
        List<PromptTemplate> templates = promptTemplateService.getAllSystemTemplates();
        return Result.success(templates);
    }

    @ApiOperation("获取所有分类")
    @GetMapping("/categories")
    public Result<List<String>> getAllCategories() {
        List<String> categories = promptTemplateService.getAllCategories();
        return Result.success(categories);
    }

    @ApiOperation("按分类获取模板")
    @GetMapping("/category/{category}")
    public Result<List<PromptTemplate>> getTemplatesByCategory(@PathVariable String category) {
        List<PromptTemplate> templates = promptTemplateService.getTemplatesByCategory(category);
        return Result.success(templates);
    }

    @ApiOperation("获取模板详情")
    @GetMapping("/{id}")
    public Result<PromptTemplate> getTemplateById(@PathVariable String id) {
        PromptTemplate template = promptTemplateService.getTemplateById(id);
        if (template == null) {
            return Result.error(404, "模板不存在");
        }
        return Result.success(template);
    }

    @ApiOperation("搜索模板")
    @GetMapping("/search")
    public Result<List<PromptTemplate>> searchTemplates(@RequestParam(required = false) String keyword) {
        List<PromptTemplate> templates = promptTemplateService.searchTemplates(keyword);
        return Result.success(templates);
    }

    @ApiOperation("填充模板变量")
    @PostMapping("/{id}/fill")
    public Result<String> fillTemplate(@PathVariable String id, @RequestBody Map<String, String> variables) {
        String content = promptTemplateService.fillTemplate(id, variables);
        if (content == null) {
            return Result.error(404, "模板不存在");
        }
        return Result.success(content);
    }

    @ApiOperation("获取热门模板")
    @GetMapping("/hot")
    public Result<List<PromptTemplate>> getHotTemplates(@RequestParam(defaultValue = "10") int limit) {
        List<PromptTemplate> templates = promptTemplateService.getHotTemplates(limit);
        return Result.success(templates);
    }
}
