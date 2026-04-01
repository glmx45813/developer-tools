package com.glmx.tools.controller;

import com.glmx.tools.common.Result;
import com.glmx.tools.dto.*;
import com.glmx.tools.service.AesService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "AES加密工具")
@RestController
@RequestMapping("/api/aes")
public class AesController {

    @Autowired
    private AesService aesService;

    @ApiOperation("生成随机密钥")
    @GetMapping("/generate-key")
    public Result<String> generateKey(@RequestParam(defaultValue = "128") Integer keySize) {
        try {
            String key = aesService.generateKey(keySize);
            return Result.success(key);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @ApiOperation("生成随机IV")
    @GetMapping("/generate-iv")
    public Result<String> generateIv() {
        try {
            String iv = aesService.generateIv();
            return Result.success(iv);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @ApiOperation("AES加密")
    @PostMapping("/encrypt")
    public Result<AesEncryptResponse> encrypt(@RequestBody AesEncryptRequest request) {
        try {
            AesEncryptResponse response = aesService.encrypt(
                request.getText(),
                request.getKey(),
                request.getIv(),
                request.getMode() != null ? request.getMode() : "CBC",
                request.getPadding() != null ? request.getPadding() : "PKCS5Padding",
                request.getKeySize() != null ? request.getKeySize() : 128
            );
            return Result.success(response);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @ApiOperation("AES解密")
    @PostMapping("/decrypt")
    public Result<AesDecryptResponse> decrypt(@RequestBody AesDecryptRequest request) {
        try {
            AesDecryptResponse response = aesService.decrypt(
                request.getEncryptedData(),
                request.getKey(),
                request.getIv(),
                request.getMode() != null ? request.getMode() : "CBC",
                request.getPadding() != null ? request.getPadding() : "PKCS5Padding",
                request.getKeySize() != null ? request.getKeySize() : 128
            );
            return Result.success(response);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
