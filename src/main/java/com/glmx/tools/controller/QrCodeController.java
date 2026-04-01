package com.glmx.tools.controller;

import com.glmx.tools.common.Result;
import com.glmx.tools.dto.QrCodeGenerateRequest;
import com.glmx.tools.dto.QrCodeParseResponse;
import com.glmx.tools.dto.QrCodeResponse;
import com.glmx.tools.service.QrCodeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Api(tags = "二维码工具")
@RestController
@RequestMapping("/api/qrcode")
public class QrCodeController {

    @Autowired
    private QrCodeService qrCodeService;

    @ApiOperation("生成二维码")
    @PostMapping("/generate")
    public Result<QrCodeResponse> generateQrCode(@Valid @RequestBody QrCodeGenerateRequest request) {
        QrCodeResponse response = qrCodeService.generateQrCode(request);
        return Result.success(response);
    }

    @ApiOperation("生成二维码并下载")
    @PostMapping("/generate-download")
    public ResponseEntity<byte[]> generateQrCodeDownload(@Valid @RequestBody QrCodeGenerateRequest request) {
        byte[] imageBytes = qrCodeService.generateQrCodeBytes(request);
        String filename = "qrcode_" + System.currentTimeMillis() + ".png";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(imageBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(imageBytes);
    }

    @ApiOperation("识别二维码(上传图片)")
    @PostMapping("/parse")
    public Result<QrCodeParseResponse> parseQrCode(@RequestParam("file") MultipartFile file) throws IOException {
        QrCodeParseResponse response = qrCodeService.parseQrCode(file);
        return Result.success(response);
    }

    @ApiOperation("识别二维码(Base64)")
    @PostMapping("/parse-base64")
    public Result<QrCodeParseResponse> parseQrCodeBase64(@RequestBody Map<String, String> request) throws IOException {
        String base64 = request.get("base64");
        QrCodeParseResponse response = qrCodeService.parseQrCodeFromBase64(base64);
        return Result.success(response);
    }

    @ApiOperation("获取容错级别说明")
    @GetMapping("/error-correction-levels")
    public Result<Map<String, Object>> getErrorCorrectionLevels() {
        Map<String, Object> result = new HashMap<>();
        Map<String, String> levels = new HashMap<>();
        levels.put("L", "低(L) - 约7%的数据可被恢复");
        levels.put("M", "中(M) - 约15%的数据可被恢复");
        levels.put("Q", "较高(Q) - 约25%的数据可被恢复");
        levels.put("H", "高(H) - 约30%的数据可被恢复");
        result.put("levels", levels);
        result.put("default", "M");
        return Result.success(result);
    }
}
