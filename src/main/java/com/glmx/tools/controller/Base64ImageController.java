package com.glmx.tools.controller;

import com.glmx.tools.common.Result;
import com.glmx.tools.dto.Base64ImageResponse;
import com.glmx.tools.dto.ImageFromBase64Response;
import com.glmx.tools.service.Base64ImageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Api(tags = "Base64图片互转工具")
@RestController
@RequestMapping("/api/base64")
public class Base64ImageController {

    @Autowired
    private Base64ImageService base64ImageService;

    @ApiOperation("图片转Base64")
    @PostMapping("/image-to-base64")
    public Result<Base64ImageResponse> imageToBase64(@RequestParam("file") MultipartFile file) throws IOException {
        Base64ImageResponse response = base64ImageService.imageToBase64(file);
        return Result.success(response);
    }

    @ApiOperation("Base64转图片信息")
    @PostMapping("/base64-to-image-info")
    public Result<ImageFromBase64Response> base64ToImageInfo(@RequestBody Map<String, String> request) throws IOException {
        String base64 = request.get("base64");
        ImageFromBase64Response response = base64ImageService.base64ToImage(base64);
        return Result.success(response);
    }

    @ApiOperation("Base64转图片下载")
    @PostMapping("/base64-to-image")
    public ResponseEntity<byte[]> base64ToImage(@RequestBody Map<String, String> request) throws IOException {
        String base64 = request.get("base64");
        String filename = request.getOrDefault("filename", "image");
        
        String format = base64ImageService.detectImageFormat(base64);
        byte[] imageBytes = base64ImageService.base64ToImageBytes(base64);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("image/" + format));
        headers.setContentDispositionFormData("attachment", filename + "." + format);
        headers.setContentLength(imageBytes.length);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(imageBytes);
    }

    @ApiOperation("获取支持的图片格式")
    @GetMapping("/supported-formats")
    public Result<Map<String, Object>> getSupportedFormats() {
        Map<String, Object> result = new HashMap<>();
        result.put("formats", new String[]{"jpg", "jpeg", "png", "gif", "bmp", "webp"});
        result.put("maxSize", "10MB");
        return Result.success(result);
    }
}
