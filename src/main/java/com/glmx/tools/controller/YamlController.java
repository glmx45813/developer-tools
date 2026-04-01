package com.glmx.tools.controller;

import com.glmx.tools.common.Result;
import com.glmx.tools.service.YamlService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "YAML工具")
@RestController
@RequestMapping("/api/yaml")
public class YamlController {

    @Autowired
    private YamlService yamlService;

    @ApiOperation("YAML格式化")
    @PostMapping("/format")
    public Result<YamlService.YamlResult> format(@RequestBody YamlRequest request) {
        try {
            int indent = request.getIndent() != null ? request.getIndent() : 2;
            YamlService.YamlResult result = yamlService.format(request.getYaml(), indent);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("YAML压缩")
    @PostMapping("/compress")
    public Result<YamlService.YamlResult> compress(@RequestBody YamlRequest request) {
        try {
            YamlService.YamlResult result = yamlService.compress(request.getYaml());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("YAML校验")
    @PostMapping("/validate")
    public Result<YamlService.YamlResult> validate(@RequestBody YamlRequest request) {
        try {
            YamlService.YamlResult result = yamlService.validate(request.getYaml());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("YAML转JSON")
    @PostMapping("/to-json")
    public Result<YamlService.YamlResult> toJson(@RequestBody YamlRequest request) {
        try {
            YamlService.YamlResult result = yamlService.toJson(request.getYaml());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("JSON转YAML")
    @PostMapping("/from-json")
    public Result<YamlService.YamlResult> fromJson(@RequestBody YamlFromJsonRequest request) {
        try {
            YamlService.YamlResult result = yamlService.fromJson(request.getJson());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("YAML转Properties")
    @PostMapping("/to-properties")
    public Result<YamlService.YamlResult> toProperties(@RequestBody YamlRequest request) {
        try {
            YamlService.YamlResult result = yamlService.toProperties(request.getYaml());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}

class YamlRequest {
    private String yaml;
    private Integer indent;

    public String getYaml() { return yaml; }
    public void setYaml(String yaml) { this.yaml = yaml; }
    public Integer getIndent() { return indent; }
    public void setIndent(Integer indent) { this.indent = indent; }
}

class YamlFromJsonRequest {
    private String json;

    public String getJson() { return json; }
    public void setJson(String json) { this.json = json; }
}
