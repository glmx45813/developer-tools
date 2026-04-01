package com.glmx.tools.controller;

import com.glmx.tools.common.Result;
import com.glmx.tools.service.CronService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "Cron表达式生成器")
@RestController
@RequestMapping("/api/cron")
public class CronController {

    @Autowired
    private CronService cronService;

    @ApiOperation("生成Cron表达式")
    @PostMapping("/generate")
    public Result<CronService.CronResult> generate(@RequestBody CronService.CronGenerateRequest request) {
        try {
            CronService.CronResult result = cronService.generate(request);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("解析Cron表达式")
    @PostMapping("/parse")
    public Result<CronService.CronResult> parse(@RequestBody CronParseRequest request) {
        try {
            CronService.CronResult result = cronService.parse(request.getExpression());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("验证Cron表达式")
    @PostMapping("/validate")
    public Result<CronService.CronResult> validate(@RequestBody CronParseRequest request) {
        try {
            CronService.CronResult result = cronService.validate(request.getExpression());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("获取下次执行时间")
    @PostMapping("/next-executions")
    public Result<List<String>> getNextExecutions(@RequestBody CronParseRequest request) {
        try {
            List<String> executions = cronService.getNextExecutions(request.getExpression(), request.getCount() != null ? request.getCount() : 5);
            return Result.success(executions);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("获取预设模板")
    @GetMapping("/presets")
    public Result<List<CronService.CronPreset>> getPresets() {
        try {
            List<CronService.CronPreset> presets = cronService.getPresets();
            return Result.success(presets);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}

class CronParseRequest {
    private String expression;
    private Integer count;

    public String getExpression() { return expression; }
    public void setExpression(String expression) { this.expression = expression; }
    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
}
