package com.glmx.tools.controller;

import com.glmx.tools.common.Result;
import com.glmx.tools.service.ImageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Api(tags = "图片处理工具")
@RestController
@RequestMapping("/api/image")
public class ImageController {

    @Autowired
    private ImageService imageService;

    @ApiOperation("图片压缩")
    @PostMapping("/compress")
    public Result<ImageService.ImageResult> compress(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "quality", required = false, defaultValue = "0.8") Float quality,
            @RequestParam(value = "maxWidth", required = false) Integer maxWidth,
            @RequestParam(value = "maxHeight", required = false) Integer maxHeight) {
        try {
            ImageService.ImageResult result = imageService.compress(file, quality, maxWidth, maxHeight);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("格式转换")
    @PostMapping("/convert")
    public Result<ImageService.ImageResult> convertFormat(
            @RequestParam("file") MultipartFile file,
            @RequestParam("format") String format) {
        try {
            ImageService.ImageResult result = imageService.convertFormat(file, format);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("图片缩放")
    @PostMapping("/resize")
    public Result<ImageService.ImageResult> resize(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "width", required = false) Integer width,
            @RequestParam(value = "height", required = false) Integer height,
            @RequestParam(value = "keepRatio", required = false, defaultValue = "true") Boolean keepRatio) {
        try {
            ImageService.ImageResult result = imageService.resize(file, width, height, keepRatio);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("图片旋转")
    @PostMapping("/rotate")
    public Result<ImageService.ImageResult> rotate(
            @RequestParam("file") MultipartFile file,
            @RequestParam("angle") Integer angle) {
        try {
            ImageService.ImageResult result = imageService.rotate(file, angle);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
