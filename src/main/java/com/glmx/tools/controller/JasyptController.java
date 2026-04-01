package com.glmx.tools.controller;

import com.glmx.tools.common.Result;
import com.glmx.tools.dto.*;
import com.glmx.tools.service.JasyptService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "Jasypt加密工具")
@RestController
@RequestMapping("/api/jasypt")
public class JasyptController {

    @Autowired
    private JasyptService jasyptService;

    @ApiOperation("获取支持的算法列表")
    @GetMapping("/algorithms")
    public Result<List<String>> getSupportedAlgorithms() {
        return Result.success(jasyptService.getSupportedAlgorithms());
    }
    
    @ApiOperation("Jasypt加密")
    @PostMapping("/encrypt")
    public Result<JasyptEncryptResponse> encrypt(@RequestBody JasyptEncryptRequest request) {
        try {
            JasyptEncryptResponse response = jasyptService.encrypt(
                request.getText(),
                request.getPassword(),
                request.getAlgorithm(),
                request.getSaltIterations()
            );
            return Result.success(response);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @ApiOperation("Jasypt解密")
    @PostMapping("/decrypt")
    public Result<JasyptDecryptResponse> decrypt(@RequestBody JasyptDecryptRequest request) {
        try {
            JasyptDecryptResponse response = jasyptService.decrypt(
                request.getEncryptedText(),
                request.getPassword(),
                request.getAlgorithm(),
                request.getSaltIterations()
            );
            return Result.success(response);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
