package com.glmx.tools.service;

import com.glmx.tools.common.BusinessException;
import com.glmx.tools.dto.Sha256Response;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Service
public class Sha256Service {

    public Sha256Response encrypt(String text) {
        if (text == null || text.isEmpty()) {
            throw new BusinessException("待加密文本不能为空");
        }
        
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            throw new BusinessException("SHA-256算法不可用");
        }
        
        byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
        
        Sha256Response response = new Sha256Response();
        response.setOriginalText(text);
        response.setHashHex(bytesToHex(hash));
        response.setHashBase64(Base64.getEncoder().encodeToString(hash));
        response.setLength(text.length());
        
        return response;
    }
    
    public Sha256Response encryptFile(byte[] fileData, String filename) {
        if (fileData == null || fileData.length == 0) {
            throw new BusinessException("文件数据不能为空");
        }
        
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            throw new BusinessException("SHA-256算法不可用");
        }
        
        byte[] hash = digest.digest(fileData);
        
        Sha256Response response = new Sha256Response();
        response.setFilename(filename);
        response.setHashHex(bytesToHex(hash));
        response.setHashBase64(Base64.getEncoder().encodeToString(hash));
        response.setLength(fileData.length);
        
        return response;
    }
    
    public boolean verify(String text, String expectedHash) {
        Sha256Response response = encrypt(text);
        return response.getHashHex().equalsIgnoreCase(expectedHash);
    }
    
    public boolean verifyFile(byte[] fileData, String expectedHash) {
        Sha256Response response = encryptFile(fileData, "verification");
        return response.getHashHex().equalsIgnoreCase(expectedHash);
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
