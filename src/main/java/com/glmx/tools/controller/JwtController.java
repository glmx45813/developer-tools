package com.glmx.tools.controller;

import com.glmx.tools.common.Result;
import com.glmx.tools.service.JwtService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api(tags = "JWT工具")
@RestController
@RequestMapping("/api/jwt")
public class JwtController {

    @Autowired
    private JwtService jwtService;

    @ApiOperation("生成JWT Token")
    @PostMapping("/generate")
    public Result<JwtService.GenerateResult> generate(@RequestBody JwtService.JwtGenerateRequest request) {
        try {
            JwtService.GenerateResult result = jwtService.generate(request);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("解析JWT Token")
    @PostMapping("/parse")
    public Result<JwtService.ParseResult> parse(@RequestBody JwtParseRequest request) {
        try {
            JwtService.ParseResult result = jwtService.parse(request.getToken(), request.getSecret());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("验证JWT Token")
    @PostMapping("/verify")
    public Result<JwtService.VerifyResult> verify(@RequestBody JwtParseRequest request) {
        try {
            JwtService.VerifyResult result = jwtService.verify(request.getToken(), request.getSecret());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("解码JWT Token（无需密钥）")
    @PostMapping("/decode")
    public Result<JwtService.DecodeResult> decode(@RequestBody JwtDecodeRequest request) {
        try {
            JwtService.DecodeResult result = jwtService.decode(request.getToken());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}

class JwtParseRequest {
    private String token;
    private String secret;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
}

class JwtDecodeRequest {
    private String token;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
