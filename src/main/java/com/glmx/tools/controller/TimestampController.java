package com.glmx.tools.controller;

import com.glmx.tools.common.Result;
import com.glmx.tools.dto.TimestampRequest;
import com.glmx.tools.dto.TimestampResponse;
import com.glmx.tools.service.TimestampService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api(tags = "时间戳转换工具")
@RestController
@RequestMapping("/api/timestamp")
public class TimestampController {

    @Autowired
    private TimestampService timestampService;

    @ApiOperation("时间戳转日期时间")
    @GetMapping("/from-timestamp")
    public Result<TimestampResponse> fromTimestamp(
            @RequestParam Long timestamp,
            @RequestParam(defaultValue = "yyyy-MM-dd HH:mm:ss") String pattern,
            @RequestParam(defaultValue = "GMT+8") String timezone) {
        TimestampResponse response = timestampService.fromTimestamp(timestamp, pattern, timezone);
        return Result.success(response);
    }

    @ApiOperation("日期时间转时间戳")
    @PostMapping("/from-datetime")
    public Result<TimestampResponse> fromDatetime(@RequestBody TimestampRequest request) {
        TimestampResponse response = timestampService.fromDatetime(
                request.getDatetime(),
                request.getPattern(),
                request.getTimezone()
        );
        return Result.success(response);
    }

    @ApiOperation("获取当前时间戳")
    @GetMapping("/now")
    public Result<TimestampResponse> getCurrentTime(
            @RequestParam(defaultValue = "yyyy-MM-dd HH:mm:ss") String pattern,
            @RequestParam(defaultValue = "GMT+8") String timezone) {
        TimestampResponse response = timestampService.getCurrentTime(pattern, timezone);
        return Result.success(response);
    }

    @ApiOperation("智能转换")
    @PostMapping("/convert")
    public Result<TimestampResponse> convert(@RequestBody TimestampRequest request) {
        TimestampResponse response = timestampService.convert(request);
        return Result.success(response);
    }

    @ApiOperation("获取常用日期格式")
    @GetMapping("/patterns")
    public Result<Map<String, Object>> getCommonPatterns() {
        Map<String, Object> result = timestampService.getCommonPatterns();
        return Result.success(result);
    }
}
