package com.glmx.tools.controller;

import com.glmx.tools.dto.Md5Request;
import com.glmx.tools.dto.Md5Response;
import com.glmx.tools.common.Result;
import com.glmx.tools.service.Md5Service;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.validation.Valid;
import java.io.IOException;
import java.io.InputStream;

@Api(tags = "MD5加密工具")
@RestController
@RequestMapping("/api/md5")
public class Md5Controller {

    @Autowired
    private Md5Service md5Service;

    @ApiOperation("文本MD5加密")
    @PostMapping("/encrypt")
    public Result<Md5Response> encryptText(@Valid @RequestBody Md5Request request) {
        Md5Response response = new Md5Response();
        response.setOriginalText(request.getText());
        response.setType(request.getType());
        
        String standard = md5Service.encryptStandard(request.getText(), false);
        String upperStandard = md5Service.encryptStandard(request.getText(), true);
        
        response.setMd5Standard(standard);
        response.setMd5Bit16(md5Service.encryptBit16(request.getText(), false));
        response.setMd5Bit32(standard);
        
        String result = md5Service.encrypt(request.getText(), request.getType(), request.getUppercase());
        response.setResult(result);
        
        return Result.success(response);
    }

    @ApiOperation("文件MD5加密")
    @PostMapping("/encrypt-file")
    public Result<Md5Response> encryptFile(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return Result.error("请选择要加密的文件");
        }
        
        String standard;
        try (InputStream inputStream = file.getInputStream()) {
            standard = org.springframework.util.DigestUtils.md5DigestAsHex(inputStream);
        }
        
        Md5Response response = new Md5Response();
        response.setOriginalText("文件: " + file.getOriginalFilename());
        response.setMd5Standard(standard);
        response.setMd5Bit16(standard.substring(8, 24));
        response.setMd5Bit32(standard);
        response.setResult(standard);
        response.setType("FILE");
        
        return Result.success(response);
    }
}
