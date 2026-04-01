package com.glmx.tools.controller;

import com.glmx.tools.common.Result;
import com.glmx.tools.service.PropertiesService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "Properties转换工具")
@RestController
@RequestMapping("/api/properties")
public class PropertiesController {

    @Autowired
    private PropertiesService propertiesService;

    @ApiOperation("Properties转YAML")
    @PostMapping("/to-yaml")
    public Result<PropertiesService.PropertiesResult> propertiesToYaml(@RequestBody PropertiesRequest request) {
        try {
            PropertiesService.PropertiesResult result = propertiesService.propertiesToYaml(request.getContent());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("Properties转JSON")
    @PostMapping("/to-json")
    public Result<PropertiesService.PropertiesResult> propertiesToJson(@RequestBody PropertiesRequest request) {
        try {
            PropertiesService.PropertiesResult result = propertiesService.propertiesToJson(request.getContent());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("YAML转Properties")
    @PostMapping("/yaml-to-properties")
    public Result<PropertiesService.PropertiesResult> yamlToProperties(@RequestBody PropertiesRequest request) {
        try {
            PropertiesService.PropertiesResult result = propertiesService.yamlToProperties(request.getContent());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("JSON转Properties")
    @PostMapping("/json-to-properties")
    public Result<PropertiesService.PropertiesResult> jsonToProperties(@RequestBody PropertiesRequest request) {
        try {
            PropertiesService.PropertiesResult result = propertiesService.jsonToProperties(request.getContent());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("Properties格式化")
    @PostMapping("/format")
    public Result<PropertiesService.PropertiesResult> format(@RequestBody PropertiesRequest request) {
        try {
            PropertiesService.PropertiesResult result = propertiesService.format(request.getContent());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("Properties校验")
    @PostMapping("/validate")
    public Result<PropertiesService.PropertiesResult> validate(@RequestBody PropertiesRequest request) {
        try {
            PropertiesService.PropertiesResult result = propertiesService.validate(request.getContent());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}

class PropertiesRequest {
    private String content;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
