package com.glmx.tools.service;

import com.glmx.tools.common.BusinessException;
import com.glmx.tools.dto.JasyptEncryptResponse;
import com.glmx.tools.dto.JasyptDecryptResponse;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class JasyptService {

    private static final String DEFAULT_ALGORITHM = "PBEWithMD5AndDES";
    private static final String PREFIX = "ENC(";
    private static final String SUFFIX = ")";
    
    private static final List<String> SUPPORTED_ALGORITHMS = Arrays.asList(
        "PBEWithMD5AndDES",
        "PBEWithMD5AndTripleDES",
        "PBEWithSHA1AndDESede",
        "PBEWithSHA1AndRC2_40",
        "PBEWITHHMACSHA512ANDAES_256",
        "PBEWITHHMACSHA256ANDAES_128"
    );

    public List<String> getSupportedAlgorithms() {
        return SUPPORTED_ALGORITHMS;
    }

    public JasyptEncryptResponse encrypt(String text, String password, String algorithm, Integer saltIterations) {
        if (text == null || text.isEmpty()) {
            throw new BusinessException("待加密文本不能为空");
        }
        if (password == null || password.isEmpty()) {
            throw new BusinessException("密码不能为空");
        }
        
        if (algorithm == null || algorithm.isEmpty()) {
            algorithm = DEFAULT_ALGORITHM;
        }
        
        if (!SUPPORTED_ALGORITHMS.contains(algorithm)) {
            throw new BusinessException("不支持的算法: " + algorithm);
        }
        
        try {
            StandardPBEStringEncryptor encryptor = createEncryptor(password, algorithm, saltIterations);
            String encrypted = encryptor.encrypt(text);
            
            JasyptEncryptResponse response = new JasyptEncryptResponse();
            response.setEncryptedData(encrypted);
            response.setEncryptedWithPrefix(PREFIX + encrypted + SUFFIX);
            response.setPassword(password);
            response.setAlgorithm(algorithm);
            response.setSaltIterations(saltIterations);
            
            return response;
        } catch (Exception e) {
            throw new BusinessException("加密失败: " + e.getMessage());
        }
    }
    
    public JasyptDecryptResponse decrypt(String encryptedText, String password, String algorithm, Integer saltIterations) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            throw new BusinessException("加密文本不能为空");
        }
        if (password == null || password.isEmpty()) {
            throw new BusinessException("密码不能为空");
        }
        
        if (algorithm == null || algorithm.isEmpty()) {
            algorithm = DEFAULT_ALGORITHM;
        }
        
        if (!SUPPORTED_ALGORITHMS.contains(algorithm)) {
            throw new BusinessException("不支持的算法: " + algorithm);
        }
        
        String cleanEncryptedText = extractEncryptedValue(encryptedText);
        
        try {
            StandardPBEStringEncryptor encryptor = createEncryptor(password, algorithm, saltIterations);
            String decrypted = encryptor.decrypt(cleanEncryptedText);
            
            JasyptDecryptResponse response = new JasyptDecryptResponse();
            response.setDecryptedText(decrypted);
            response.setPassword(password);
            response.setAlgorithm(algorithm);
            response.setSaltIterations(saltIterations);
            
            return response;
        } catch (Exception e) {
            throw new BusinessException("解密失败: " + e.getMessage());
        }
    }
    
    private StandardPBEStringEncryptor createEncryptor(String password, String algorithm, Integer saltIterations) {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(password);
        config.setAlgorithm(algorithm);
        
        if (saltIterations != null && saltIterations > 0) {
            config.setKeyObtentionIterations(saltIterations);
        } else {
            config.setKeyObtentionIterations(1000);
        }
        
        config.setPoolSize("1");
        config.setProviderName("SunJCE");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setStringOutputType("base64");
        
        encryptor.setConfig(config);
        return encryptor;
    }
    
    private String extractEncryptedValue(String encryptedText) {
        if (encryptedText.startsWith(PREFIX) && encryptedText.endsWith(SUFFIX)) {
            return encryptedText.substring(PREFIX.length(), encryptedText.length() - SUFFIX.length());
        }
        return encryptedText;
    }
}
