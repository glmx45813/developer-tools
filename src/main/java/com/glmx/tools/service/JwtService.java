package com.glmx.tools.service;

import com.glmx.tools.common.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public GenerateResult generate(JwtGenerateRequest request) {
        if (request.getSecret() == null || request.getSecret().isEmpty()) {
            throw new BusinessException("密钥不能为空");
        }
        if (request.getSecret().length() < 32) {
            throw new BusinessException("密钥长度至少32个字符");
        }

        try {
            SecretKey key = Keys.hmacShaKeyFor(request.getSecret().getBytes(StandardCharsets.UTF_8));
            
            JwtBuilder builder = Jwts.builder()
                    .setSubject(request.getSubject() != null ? request.getSubject() : "")
                    .setIssuer(request.getIssuer())
                    .setAudience(request.getAudience())
                    .setIssuedAt(new Date());
            
            if (request.getExpiration() != null && request.getExpiration() > 0) {
                long expTime = System.currentTimeMillis() + request.getExpiration() * 1000;
                builder.setExpiration(new Date(expTime));
            }
            
            if (request.getNotBefore() != null && request.getNotBefore() > 0) {
                long nbfTime = System.currentTimeMillis() + request.getNotBefore() * 1000;
                builder.setNotBefore(new Date(nbfTime));
            }
            
            if (request.getClaims() != null && !request.getClaims().isEmpty()) {
                builder.addClaims(request.getClaims());
            }
            
            if (request.getAlgorithm() != null) {
                builder.signWith(key, getAlgorithm(request.getAlgorithm()));
            } else {
                builder.signWith(key, SignatureAlgorithm.HS256);
            }
            
            String token = builder.compact();
            
            GenerateResult result = new GenerateResult();
            result.setToken(token);
            result.setAlgorithm(request.getAlgorithm() != null ? request.getAlgorithm() : "HS256");
            return result;
        } catch (Exception e) {
            throw new BusinessException("生成JWT失败: " + e.getMessage());
        }
    }

    public ParseResult parse(String token, String secret) {
        if (token == null || token.isEmpty()) {
            throw new BusinessException("Token不能为空");
        }
        if (secret == null || secret.isEmpty()) {
            throw new BusinessException("密钥不能为空");
        }

        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            
            Jwt<Header, Claims> jwt = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJwt(token.replaceFirst("\\.", ".eyJ"));
            
            Claims claims = jwt.getBody();
            
            ParseResult result = new ParseResult();
            result.setHeader(parseHeader(token));
            result.setPayload(objectMapper.writeValueAsString(claims));
            result.setSubject(claims.getSubject());
            result.setIssuer(claims.getIssuer());
            result.setAudience(claims.getAudience());
            result.setIssuedAt(claims.getIssuedAt());
            result.setExpiration(claims.getExpiration());
            result.setNotBefore(claims.getNotBefore());
            result.setValid(true);
            
            return result;
        } catch (Exception e) {
            ParseResult result = new ParseResult();
            result.setValid(false);
            result.setError(e.getMessage());
            return result;
        }
    }

    public VerifyResult verify(String token, String secret) {
        if (token == null || token.isEmpty()) {
            throw new BusinessException("Token不能为空");
        }
        if (secret == null || secret.isEmpty()) {
            throw new BusinessException("密钥不能为空");
        }

        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            VerifyResult result = new VerifyResult();
            result.setValid(true);
            result.setMessage("Token验证通过");
            result.setSubject(claims.getSubject());
            result.setIssuer(claims.getIssuer());
            result.setExpiration(claims.getExpiration());
            
            if (claims.getExpiration() != null && claims.getExpiration().before(new Date())) {
                result.setValid(false);
                result.setMessage("Token已过期");
            }
            
            return result;
        } catch (ExpiredJwtException e) {
            VerifyResult result = new VerifyResult();
            result.setValid(false);
            result.setMessage("Token已过期");
            return result;
        } catch (UnsupportedJwtException e) {
            VerifyResult result = new VerifyResult();
            result.setValid(false);
            result.setMessage("不支持的Token格式");
            return result;
        } catch (MalformedJwtException e) {
            VerifyResult result = new VerifyResult();
            result.setValid(false);
            result.setMessage("Token格式错误");
            return result;
        } catch (SignatureException e) {
            VerifyResult result = new VerifyResult();
            result.setValid(false);
            result.setMessage("签名验证失败");
            return result;
        } catch (Exception e) {
            VerifyResult result = new VerifyResult();
            result.setValid(false);
            result.setMessage("验证失败: " + e.getMessage());
            return result;
        }
    }

    public DecodeResult decode(String token) {
        if (token == null || token.isEmpty()) {
            throw new BusinessException("Token不能为空");
        }

        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                throw new BusinessException("无效的JWT格式");
            }
            
            DecodeResult result = new DecodeResult();
            result.setHeader(new String(java.util.Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8));
            result.setPayload(new String(java.util.Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8));
            result.setSignature(parts.length > 2 ? parts[2] : "");
            result.setPartsCount(parts.length);
            
            return result;
        } catch (Exception e) {
            throw new BusinessException("解码失败: " + e.getMessage());
        }
    }

    private SignatureAlgorithm getAlgorithm(String algorithm) {
        switch (algorithm.toUpperCase()) {
            case "HS256": return SignatureAlgorithm.HS256;
            case "HS384": return SignatureAlgorithm.HS384;
            case "HS512": return SignatureAlgorithm.HS512;
            default: return SignatureAlgorithm.HS256;
        }
    }

    private String parseHeader(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length > 0) {
                return new String(java.util.Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {}
        return "{}";
    }

    public static class JwtGenerateRequest {
        private String secret;
        private String subject;
        private String issuer;
        private String audience;
        private Long expiration;
        private Long notBefore;
        private String algorithm;
        private Map<String, Object> claims;

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
        public String getAudience() { return audience; }
        public void setAudience(String audience) { this.audience = audience; }
        public Long getExpiration() { return expiration; }
        public void setExpiration(Long expiration) { this.expiration = expiration; }
        public Long getNotBefore() { return notBefore; }
        public void setNotBefore(Long notBefore) { this.notBefore = notBefore; }
        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
        public Map<String, Object> getClaims() { return claims; }
        public void setClaims(Map<String, Object> claims) { this.claims = claims; }
    }

    public static class GenerateResult {
        private String token;
        private String algorithm;

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    }

    public static class ParseResult {
        private String header;
        private String payload;
        private String subject;
        private String issuer;
        private String audience;
        private Date issuedAt;
        private Date expiration;
        private Date notBefore;
        private Boolean valid;
        private String error;

        public String getHeader() { return header; }
        public void setHeader(String header) { this.header = header; }
        public String getPayload() { return payload; }
        public void setPayload(String payload) { this.payload = payload; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
        public String getAudience() { return audience; }
        public void setAudience(String audience) { this.audience = audience; }
        public Date getIssuedAt() { return issuedAt; }
        public void setIssuedAt(Date issuedAt) { this.issuedAt = issuedAt; }
        public Date getExpiration() { return expiration; }
        public void setExpiration(Date expiration) { this.expiration = expiration; }
        public Date getNotBefore() { return notBefore; }
        public void setNotBefore(Date notBefore) { this.notBefore = notBefore; }
        public Boolean getValid() { return valid; }
        public void setValid(Boolean valid) { this.valid = valid; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }

    public static class VerifyResult {
        private Boolean valid;
        private String message;
        private String subject;
        private String issuer;
        private Date expiration;

        public Boolean getValid() { return valid; }
        public void setValid(Boolean valid) { this.valid = valid; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
        public Date getExpiration() { return expiration; }
        public void setExpiration(Date expiration) { this.expiration = expiration; }
    }

    public static class DecodeResult {
        private String header;
        private String payload;
        private String signature;
        private Integer partsCount;

        public String getHeader() { return header; }
        public void setHeader(String header) { this.header = header; }
        public String getPayload() { return payload; }
        public void setPayload(String payload) { this.payload = payload; }
        public String getSignature() { return signature; }
        public void setSignature(String signature) { this.signature = signature; }
        public Integer getPartsCount() { return partsCount; }
        public void setPartsCount(Integer partsCount) { this.partsCount = partsCount; }
    }
}
