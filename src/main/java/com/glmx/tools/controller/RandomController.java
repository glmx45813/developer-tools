package com.glmx.tools.controller;

import com.glmx.tools.common.Result;
import com.glmx.tools.dto.RandomRequest;
import com.glmx.tools.dto.RandomResponse;
import com.glmx.tools.service.RandomService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Api(tags = "随机数生成工具")
@RestController
@RequestMapping("/api/random")
public class RandomController {

    @Autowired
    private RandomService randomService;

    @ApiOperation("生成随机数")
    @PostMapping("/generate")
    public Result<RandomResponse> generate(@Valid @RequestBody RandomRequest request) {
        RandomResponse response = randomService.generate(request);
        return Result.success(response);
    }

    @ApiOperation("生成UUID")
    @GetMapping("/uuid")
    public Result<Map<String, String>> generateUuid() {
        Map<String, String> result = new HashMap<>();
        result.put("uuid", randomService.generateUuid());
        result.put("uuidNoDash", randomService.generateUuidWithoutDash());
        return Result.success(result);
    }

    @ApiOperation("快速生成随机数")
    @GetMapping("/quick")
    public Result<Map<String, String>> quickGenerate(
            @RequestParam(defaultValue = "16") Integer length,
            @RequestParam(defaultValue = "MIXED") String type) {
        RandomRequest request = new RandomRequest();
        request.setLength(length);
        request.setType(type);
        request.setCount(1);

        RandomResponse response = randomService.generate(request);

        Map<String, String> result = new HashMap<>();
        result.put("value", response.getValues().get(0));
        return Result.success(result);
    }

    @ApiOperation("获取随机数类型说明")
    @GetMapping("/types")
    public Result<Map<String, Object>> getTypes() {
        Map<String, Object> result = new HashMap<>();
        Map<String, String> types = new HashMap<>();
        types.put("NUMBER", "纯数字 (0-9)");
        types.put("LETTER", "纯字母 (a-z, A-Z)");
        types.put("MIXED", "混合 (字母+数字)");
        types.put("SPECIAL", "含特殊字符 (字母+数字+特殊字符)");
        result.put("types", types);
        result.put("specialChars", "!@#$%^&*()_+-=[]{}|;:,.<>?");
        return Result.success(result);
    }
}
