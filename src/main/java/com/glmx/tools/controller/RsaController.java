package com.glmx.tools.controller;

import com.glmx.tools.common.Result;
import com.glmx.tools.service.RsaService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "RSA加密工具")
@RestController
@RequestMapping("/api/rsa")
public class RsaController {

    @Autowired
    private RsaService rsaService;

    @ApiOperation("生成密钥对")
    @GetMapping("/generate-keypair")
    public Result<RsaService.KeyPairResult> generateKeyPair(@RequestParam(defaultValue = "2048") Integer keySize) {
        try {
            RsaService.KeyPairResult result = rsaService.generateKeyPair(keySize);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("公钥加密")
    @PostMapping("/encrypt")
    public Result<RsaService.EncryptResult> encrypt(@RequestBody RsaEncryptRequest request) {
        try {
            RsaService.EncryptResult result = rsaService.encrypt(
                request.getText(),
                request.getPublicKey(),
                request.getPadding() != null ? request.getPadding() : "PKCS1"
            );
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("私钥解密")
    @PostMapping("/decrypt")
    public Result<RsaService.DecryptResult> decrypt(@RequestBody RsaDecryptRequest request) {
        try {
            RsaService.DecryptResult result = rsaService.decrypt(
                request.getEncryptedData(),
                request.getPrivateKey(),
                request.getPadding() != null ? request.getPadding() : "PKCS1"
            );
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("私钥签名")
    @PostMapping("/sign")
    public Result<RsaService.SignResult> sign(@RequestBody RsaSignRequest request) {
        try {
            RsaService.SignResult result = rsaService.sign(
                request.getText(),
                request.getPrivateKey(),
                request.getAlgorithm()
            );
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("公钥验签")
    @PostMapping("/verify")
    public Result<RsaService.VerifyResult> verify(@RequestBody RsaVerifyRequest request) {
        try {
            RsaService.VerifyResult result = rsaService.verify(
                request.getText(),
                request.getSignature(),
                request.getPublicKey(),
                request.getAlgorithm()
            );
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}

class RsaEncryptRequest {
    private String text;
    private String publicKey;
    private String padding;

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
    public String getPadding() { return padding; }
    public void setPadding(String padding) { this.padding = padding; }
}

class RsaDecryptRequest {
    private String encryptedData;
    private String privateKey;
    private String padding;

    public String getEncryptedData() { return encryptedData; }
    public void setEncryptedData(String encryptedData) { this.encryptedData = encryptedData; }
    public String getPrivateKey() { return privateKey; }
    public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }
    public String getPadding() { return padding; }
    public void setPadding(String padding) { this.padding = padding; }
}

class RsaSignRequest {
    private String text;
    private String privateKey;
    private String algorithm;

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getPrivateKey() { return privateKey; }
    public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }
    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
}

class RsaVerifyRequest {
    private String text;
    private String signature;
    private String publicKey;
    private String algorithm;

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
}
