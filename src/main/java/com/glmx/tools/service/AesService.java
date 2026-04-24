package com.glmx.tools.service;

import com.glmx.tools.common.BusinessException;
import com.glmx.tools.dto.AesEncryptResponse;
import com.glmx.tools.dto.AesDecryptResponse;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class AesService {

    private static final String ALGORITHM = "AES";
    
    public String generateKey(int keySize) {
        if (keySize != 128 && keySize != 192 && keySize != 256) {
            throw new BusinessException("密钥长度必须是128、192或256位");
        }
        byte[] key = new byte[keySize / 8];
        SecureRandom random = new SecureRandom();
        random.nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
    
    public String generateIv() {
        byte[] iv = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        return Base64.getEncoder().encodeToString(iv);
    }
    
    public AesEncryptResponse encrypt(String text, String key, String iv, String mode, String padding, Integer keySize) {
        if (text == null || text.isEmpty()) {
            throw new BusinessException("待加密文本不能为空");
        }
        if (key == null || key.isEmpty()) {
            throw new BusinessException("密钥不能为空");
        }
        
        if (keySize == null) {
            keySize = 128;
        }
        
        if (keySize != 128 && keySize != 192 && keySize != 256) {
            throw new BusinessException("密钥长度必须是128、192或256位");
        }
        
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        
        byte[] ivBytes = null;
        if ("CBC".equals(mode) || "GCM".equals(mode)) {
            if (iv == null || iv.isEmpty()) {
                ivBytes = generateIvBytes();
            } else {
                ivBytes = iv.getBytes(StandardCharsets.UTF_8);
            }
        }
        
        try {
            String transformation = buildTransformation(mode, padding);
            Cipher cipher = Cipher.getInstance(transformation);
            
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM);
            
            if ("CBC".equals(mode) && ivBytes != null) {
                IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            } else if ("GCM".equals(mode) && ivBytes != null) {
                IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            } else {
                cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            }
            
            byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
            byte[] encrypted = cipher.doFinal(textBytes);
            
            String encryptedBase64 = Base64.getEncoder().encodeToString(encrypted);
            
            AesEncryptResponse response = new AesEncryptResponse();
            response.setEncryptedData(encryptedBase64);
            response.setIv(ivBytes != null ? Base64.getEncoder().encodeToString(ivBytes) : null);
            response.setKey(key);
            response.setMode(mode);
            response.setPadding(padding);
            response.setKeySize(keySize);
            
            return response;
        } catch (Exception e) {
            throw new BusinessException("加密失败: " + e.getMessage());
        }
    }
    
    public AesDecryptResponse decrypt(String encryptedData, String key, String iv, String mode, String padding, Integer keySize) {
        if (encryptedData == null || encryptedData.isEmpty()) {
            throw new BusinessException("加密数据不能为空");
        }
        if (key == null || key.isEmpty()) {
            throw new BusinessException("密钥不能为空");
        }
        
        if (keySize == null) {
            keySize = 128;
        }
        
        if (keySize != 128 && keySize != 192 && keySize != 256) {
            throw new BusinessException("密钥长度必须是128、192或256位");
        }
        
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        
        byte[] ivBytes = null;
        if ("CBC".equals(mode) || "GCM".equals(mode)) {
            if (iv == null || iv.isEmpty()) {
                throw new BusinessException("CBC/GCM模式需要IV向量");
            }
            ivBytes = iv.getBytes(StandardCharsets.UTF_8);
        }
        
        try {
            String transformation = buildTransformation(mode, padding);
            Cipher cipher = Cipher.getInstance(transformation);
            
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM);
            
            if ("CBC".equals(mode) && ivBytes != null) {
                IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
                cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            } else if ("GCM".equals(mode) && ivBytes != null) {
                IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
                cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            } else {
                cipher.init(Cipher.DECRYPT_MODE, keySpec);
            }
            
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
            byte[] decrypted = cipher.doFinal(encryptedBytes);
            
            String decryptedText = new String(decrypted, StandardCharsets.UTF_8);
            
            AesDecryptResponse response = new AesDecryptResponse();
            response.setDecryptedText(decryptedText);
            response.setIv(ivBytes != null ? Base64.getEncoder().encodeToString(ivBytes) : null);
            response.setKey(key);
            response.setMode(mode);
            response.setPadding(padding);
            response.setKeySize(keySize);
            
            return response;
        } catch (Exception e) {
            throw new BusinessException("解密失败: " + e.getMessage());
        }
    }
    
    private String buildTransformation(String mode, String padding) {
        String modeStr = mode != null ? mode : "CBC";
        String paddingStr = padding != null ? padding : "PKCS5Padding";
        
        if ("GCM".equals(modeStr)) {
            return "AES/GCM/NoPadding";
        }
        
        return "AES/" + modeStr + "/" + paddingStr;
    }
    
    private byte[] decodeKey(String key, int keySize) {
        byte[] keyBytes = Base64.getDecoder().decode(key);
        if (keyBytes.length != keySize / 8) {
            throw new BusinessException("密钥长度不正确，期望 " + keySize / 8 + " 字节，实际 " + keyBytes.length + " 字节");
        }
        return keyBytes;
    }
    
    private byte[] decodeIv(String iv) {
        byte[] ivBytes = Base64.getDecoder().decode(iv);
        if (ivBytes.length != 16) {
            throw new BusinessException("IV向量长度必须是16字节");
        }
        return ivBytes;
    }
    
    private byte[] generateIvBytes() {
        byte[] iv = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        return iv;
    }
}
