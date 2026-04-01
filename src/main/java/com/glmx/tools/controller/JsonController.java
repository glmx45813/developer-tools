package com.glmx.tools.controller;

import com.glmx.tools.common.Result;
import com.glmx.tools.dto.JsonRequest;
import com.glmx.tools.dto.JsonResponse;
import com.glmx.tools.service.JsonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Api(tags = "JSON工具")
@RestController
@RequestMapping("/api/json")
public class JsonController {

    @Autowired
    private JsonService jsonService;

    @ApiOperation("JSON格式化")
    @PostMapping("/format")
    public Result<JsonResponse> format(@Valid @RequestBody JsonRequest request) {
        JsonResponse response = jsonService.format(request);
        return Result.success(response);
    }

    @ApiOperation("JSON压缩")
    @PostMapping("/compress")
    public Result<JsonResponse> compress(@Valid @RequestBody JsonRequest request) {
        JsonResponse response = jsonService.compress(request);
        return Result.success(response);
    }

    @ApiOperation("JSON校验")
    @PostMapping("/validate")
    public Result<JsonResponse> validate(@Valid @RequestBody JsonRequest request) {
        JsonResponse response = jsonService.validate(request);
        return Result.success(response);
    }

    @ApiOperation("JSON转义")
    @PostMapping("/escape")
    public Result<JsonResponse> escape(@Valid @RequestBody JsonRequest request) {
        JsonResponse response = jsonService.escape(request);
        return Result.success(response);
    }

    @ApiOperation("JSON反转义")
    @PostMapping("/unescape")
    public Result<JsonResponse> unescape(@Valid @RequestBody JsonRequest request) {
        JsonResponse response = jsonService.unescape(request);
        return Result.success(response);
    }
}
