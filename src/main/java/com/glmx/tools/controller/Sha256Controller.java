package com.glmx.tools.controller;

import com.glmx.tools.common.Result;
import com.glmx.tools.dto.Sha256Response;
import com.glmx.tools.service.Sha256Service;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Api(tags = "SHA256加密工具")
@RestController
@RequestMapping("/api/sha256")
public class Sha256Controller {

    @Autowired
    private Sha256Service sha256Service;

    @ApiOperation("文本SHA256加密")
    @PostMapping("/encrypt")
    public Result<Sha256Response> encrypt(@RequestBody String text) {
        try {
            Sha256Response response = sha256Service.encrypt(text);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @ApiOperation("文件SHA256加密")
    @PostMapping("/encrypt-file")
    public Result<Sha256Response> encryptFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Result.error("文件不能为空");
            }
            Sha256Response response = sha256Service.encryptFile(file.getBytes(), file.getOriginalFilename());
            return Result.success(response);
        } catch (IOException e) {
            return Result.error("文件读取失败: " + e.getMessage());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @ApiOperation("文本完整性校验")
    @PostMapping("/verify")
    public Result<Boolean> verify(@RequestParam String text, @RequestParam String hash) {
        try {
            boolean result = sha256Service.verify(text, hash);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @ApiOperation("文件完整性校验")
    @PostMapping("/verify-file")
    public Result<Boolean> verifyFile(@RequestParam("file") MultipartFile file, @RequestParam String hash) {
        try {
            if (file.isEmpty()) {
                return Result.error("文件不能为空");
            }
            boolean result = sha256Service.verifyFile(file.getBytes(), hash);
            return Result.success(result);
        } catch (IOException e) {
            return Result.error("文件读取失败: " + e.getMessage());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
