package com.glmx.tools.controller;

import com.glmx.tools.common.Result;
import com.glmx.tools.service.UnicodeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "Unicode编解码工具")
@RestController
@RequestMapping("/api/unicode")
public class UnicodeController {

    @Autowired
    private UnicodeService unicodeService;

    @ApiOperation("Unicode编码")
    @PostMapping("/encode")
    public Result<UnicodeResult> encode(@RequestBody UnicodeRequest request) {
        try {
            String encoded = unicodeService.encode(request.getText(), request.getFormat());
            
            UnicodeResult result = new UnicodeResult();
            result.setOriginal(request.getText());
            result.setEncoded(encoded);
            result.setFormat(request.getFormat());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("Unicode编码（仅中文）")
    @PostMapping("/encode-chinese")
    public Result<UnicodeResult> encodeChinese(@RequestBody UnicodeRequest request) {
        try {
            String encoded = unicodeService.encodeChinese(request.getText(), request.getFormat());
            
            UnicodeResult result = new UnicodeResult();
            result.setOriginal(request.getText());
            result.setEncoded(encoded);
            result.setFormat(request.getFormat());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("Unicode解码")
    @PostMapping("/decode")
    public Result<UnicodeResult> decode(@RequestBody UnicodeRequest request) {
        try {
            String decoded = unicodeService.decode(request.getText());
            
            UnicodeResult result = new UnicodeResult();
            result.setOriginal(request.getText());
            result.setDecoded(decoded);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("获取字符信息")
    @GetMapping("/codepoint")
    public Result<UnicodeService.CodePointInfo> getCodePointInfo(@RequestParam String text) {
        try {
            UnicodeService.CodePointInfo info = unicodeService.getCodePointInfo(text);
            return Result.success(info);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}

class UnicodeRequest {
    private String text;
    private String format;

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
}

class UnicodeResult {
    private String original;
    private String encoded;
    private String decoded;
    private String format;

    public String getOriginal() { return original; }
    public void setOriginal(String original) { this.original = original; }
    public String getEncoded() { return encoded; }
    public void setEncoded(String encoded) { this.encoded = encoded; }
    public String getDecoded() { return decoded; }
    public void setDecoded(String decoded) { this.decoded = decoded; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
}
