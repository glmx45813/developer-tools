package com.glmx.tools.service;

import com.glmx.tools.common.BusinessException;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
public class RsaService {

    private static final String ALGORITHM = "RSA";
    private static final int DEFAULT_KEY_SIZE = 2048;

    public KeyPairResult generateKeyPair(Integer keySize) {
        if (keySize == null) {
            keySize = DEFAULT_KEY_SIZE;
        }
        if (keySize != 1024 && keySize != 2048 && keySize != 4096) {
            throw new BusinessException("密钥长度必须是1024、2048或4096位");
        }
        
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
            keyPairGenerator.initialize(keySize);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            
            KeyPairResult result = new KeyPairResult();
            result.setPublicKey(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
            result.setPrivateKey(Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
            result.setKeySize(keySize);
            return result;
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException("生成密钥对失败: " + e.getMessage());
        }
    }

    public EncryptResult encrypt(String text, String publicKeyBase64, String padding) {
        if (text == null || text.isEmpty()) {
            throw new BusinessException("待加密文本不能为空");
        }
        if (publicKeyBase64 == null || publicKeyBase64.isEmpty()) {
            throw new BusinessException("公钥不能为空");
        }
        
        String transformation = getTransformation(padding);
        
        try {
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            
            Cipher cipher = Cipher.getInstance(transformation);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            
            byte[] encryptedBytes = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
            
            EncryptResult result = new EncryptResult();
            result.setEncryptedData(Base64.getEncoder().encodeToString(encryptedBytes));
            result.setPadding(padding);
            return result;
        } catch (Exception e) {
            throw new BusinessException("加密失败: " + e.getMessage());
        }
    }

    public DecryptResult decrypt(String encryptedData, String privateKeyBase64, String padding) {
        if (encryptedData == null || encryptedData.isEmpty()) {
            throw new BusinessException("加密数据不能为空");
        }
        if (privateKeyBase64 == null || privateKeyBase64.isEmpty()) {
            throw new BusinessException("私钥不能为空");
        }
        
        String transformation = getTransformation(padding);
        
        try {
            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
            
            Cipher cipher = Cipher.getInstance(transformation);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            
            DecryptResult result = new DecryptResult();
            result.setDecryptedText(new String(decryptedBytes, StandardCharsets.UTF_8));
            result.setPadding(padding);
            return result;
        } catch (Exception e) {
            throw new BusinessException("解密失败: " + e.getMessage());
        }
    }

    public SignResult sign(String text, String privateKeyBase64, String algorithm) {
        if (text == null || text.isEmpty()) {
            throw new BusinessException("待签名文本不能为空");
        }
        if (privateKeyBase64 == null || privateKeyBase64.isEmpty()) {
            throw new BusinessException("私钥不能为空");
        }
        
        if (algorithm == null || algorithm.isEmpty()) {
            algorithm = "SHA256withRSA";
        }
        
        try {
            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
            
            Signature signature = Signature.getInstance(algorithm);
            signature.initSign(privateKey);
            signature.update(text.getBytes(StandardCharsets.UTF_8));
            
            byte[] signBytes = signature.sign();
            
            SignResult result = new SignResult();
            result.setSignature(Base64.getEncoder().encodeToString(signBytes));
            result.setAlgorithm(algorithm);
            return result;
        } catch (Exception e) {
            throw new BusinessException("签名失败: " + e.getMessage());
        }
    }

    public VerifyResult verify(String text, String signatureBase64, String publicKeyBase64, String algorithm) {
        if (text == null || text.isEmpty()) {
            throw new BusinessException("待验证文本不能为空");
        }
        if (signatureBase64 == null || signatureBase64.isEmpty()) {
            throw new BusinessException("签名不能为空");
        }
        if (publicKeyBase64 == null || publicKeyBase64.isEmpty()) {
            throw new BusinessException("公钥不能为空");
        }
        
        if (algorithm == null || algorithm.isEmpty()) {
            algorithm = "SHA256withRSA";
        }
        
        try {
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            
            Signature signature = Signature.getInstance(algorithm);
            signature.initVerify(publicKey);
            signature.update(text.getBytes(StandardCharsets.UTF_8));
            
            byte[] signBytes = Base64.getDecoder().decode(signatureBase64);
            boolean verified = signature.verify(signBytes);
            
            VerifyResult result = new VerifyResult();
            result.setVerified(verified);
            result.setAlgorithm(algorithm);
            return result;
        } catch (Exception e) {
            throw new BusinessException("验证失败: " + e.getMessage());
        }
    }

    private String getTransformation(String padding) {
        if (padding == null || padding.isEmpty()) {
            padding = "PKCS1";
        }
        return "RSA/ECB/" + padding + "Padding";
    }

    public static class KeyPairResult {
        private String publicKey;
        private String privateKey;
        private Integer keySize;

        public String getPublicKey() { return publicKey; }
        public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
        public String getPrivateKey() { return privateKey; }
        public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }
        public Integer getKeySize() { return keySize; }
        public void setKeySize(Integer keySize) { this.keySize = keySize; }
    }

    public static class EncryptResult {
        private String encryptedData;
        private String padding;

        public String getEncryptedData() { return encryptedData; }
        public void setEncryptedData(String encryptedData) { this.encryptedData = encryptedData; }
        public String getPadding() { return padding; }
        public void setPadding(String padding) { this.padding = padding; }
    }

    public static class DecryptResult {
        private String decryptedText;
        private String padding;

        public String getDecryptedText() { return decryptedText; }
        public void setDecryptedText(String decryptedText) { this.decryptedText = decryptedText; }
        public String getPadding() { return padding; }
        public void setPadding(String padding) { this.padding = padding; }
    }

    public static class SignResult {
        private String signature;
        private String algorithm;

        public String getSignature() { return signature; }
        public void setSignature(String signature) { this.signature = signature; }
        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    }

    public static class VerifyResult {
        private Boolean verified;
        private String algorithm;

        public Boolean getVerified() { return verified; }
        public void setVerified(Boolean verified) { this.verified = verified; }
        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    }
}
