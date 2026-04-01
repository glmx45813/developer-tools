package com.glmx.tools.controller;

import com.glmx.tools.common.Result;
import com.glmx.tools.service.StressTestService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Api(tags = "HTTP接口压测工具")
@RestController
@RequestMapping("/api/stress-test")
public class StressTestController {

    @Autowired
    private StressTestService stressTestService;

    @ApiOperation("执行压测")
    @PostMapping("/run")
    public Result<StressTestService.StressTestResult> runTest(@RequestBody StressTestService.StressTestRequest request) {
        try {
            StressTestService.StressTestResult result = stressTestService.runTest(request);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
