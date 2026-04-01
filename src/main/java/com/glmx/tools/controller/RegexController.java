package com.glmx.tools.controller;

import com.glmx.tools.common.Result;
import com.glmx.tools.dto.RegexRequest;
import com.glmx.tools.dto.RegexResponse;
import com.glmx.tools.dto.RegexTemplate;
import com.glmx.tools.service.RegexService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Api(tags = "正则表达式工具")
@RestController
@RequestMapping("/api/regex")
public class RegexController {

    @Autowired
    private RegexService regexService;

    @ApiOperation("测试正则表达式")
    @PostMapping("/test")
    public Result<RegexResponse> test(@Valid @RequestBody RegexRequest request) {
        RegexResponse response = regexService.test(request);
        return Result.success(response);
    }

    @ApiOperation("获取所有正则模板")
    @GetMapping("/templates")
    public Result<List<RegexTemplate>> getTemplates() {
        List<RegexTemplate> templates = regexService.getTemplates();
        return Result.success(templates);
    }

    @ApiOperation("按分类获取正则模板")
    @GetMapping("/templates/category/{category}")
    public Result<List<RegexTemplate>> getTemplatesByCategory(@PathVariable String category) {
        List<RegexTemplate> templates = regexService.getTemplatesByCategory(category);
        return Result.success(templates);
    }

    @ApiOperation("获取指定正则模板")
    @GetMapping("/templates/{id}")
    public Result<RegexTemplate> getTemplateById(@PathVariable String id) {
        RegexTemplate template = regexService.getTemplateById(id);
        if (template == null) {
            return Result.error("模板不存在");
        }
        return Result.success(template);
    }

    @ApiOperation("获取所有分类")
    @GetMapping("/categories")
    public Result<List<String>> getCategories() {
        List<String> categories = regexService.getCategories();
        return Result.success(categories);
    }
}
