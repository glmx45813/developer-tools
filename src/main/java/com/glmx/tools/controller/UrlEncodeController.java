package com.glmx.tools.controller;

import com.glmx.tools.common.Result;
import com.glmx.tools.dto.UrlEncodeRequest;
import com.glmx.tools.dto.UrlEncodeResponse;
import com.glmx.tools.service.UrlEncodeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "URL编解码工具")
@RestController
@RequestMapping("/api/url")
public class UrlEncodeController {

    @Autowired
    private UrlEncodeService urlEncodeService;

    @ApiOperation("URL编码")
    @PostMapping("/encode")
    public Result<UrlEncodeResponse> encode(@Valid @RequestBody UrlEncodeRequest request) {
        UrlEncodeResponse response = urlEncodeService.encode(request.getText(), request.getCharset());
        return Result.success(response);
    }

    @ApiOperation("URL解码")
    @PostMapping("/decode")
    public Result<UrlEncodeResponse> decode(@Valid @RequestBody UrlEncodeRequest request) {
        UrlEncodeResponse response = urlEncodeService.decode(request.getText(), request.getCharset());
        return Result.success(response);
    }

    @ApiOperation("URL编码解码对比")
    @PostMapping("/encode-decode")
    public Result<UrlEncodeResponse> encodeDecode(@Valid @RequestBody UrlEncodeRequest request) {
        UrlEncodeResponse response = urlEncodeService.encodeDecode(request.getText(), request.getCharset());
        return Result.success(response);
    }

    @ApiOperation("批量URL编码")
    @PostMapping("/batch-encode")
    public Result<List<UrlEncodeResponse>> batchEncode(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> texts = (List<String>) request.get("texts");
        String charset = (String) request.getOrDefault("charset", "UTF-8");
        List<UrlEncodeResponse> responses = urlEncodeService.batchEncode(texts, charset);
        return Result.success(responses);
    }

    @ApiOperation("批量URL解码")
    @PostMapping("/batch-decode")
    public Result<List<UrlEncodeResponse>> batchDecode(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> texts = (List<String>) request.get("texts");
        String charset = (String) request.getOrDefault("charset", "UTF-8");
        List<UrlEncodeResponse> responses = urlEncodeService.batchDecode(texts, charset);
        return Result.success(responses);
    }

    @ApiOperation("获取支持的编码格式")
    @GetMapping("/charsets")
    public Result<Map<String, Object>> getSupportedCharsets() {
        Map<String, Object> result = new HashMap<>();
        result.put("charsets", new String[]{"UTF-8", "GBK", "ISO-8859-1", "GB2312", "BIG5"});
        return Result.success(result);
    }
}
