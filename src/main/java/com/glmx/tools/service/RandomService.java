package com.glmx.tools.service;

import com.glmx.tools.common.BusinessException;
import com.glmx.tools.dto.RandomRequest;
import com.glmx.tools.dto.RandomResponse;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Service
public class RandomService {

    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String NUMBERS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*()_+-=[]{}|;:,.<>?";

    private final SecureRandom random = new SecureRandom();

    public RandomResponse generate(RandomRequest request) {
        String charset = buildCharset(request);
        
        if (charset.isEmpty()) {
            throw new BusinessException("请至少选择一种字符类型");
        }

        charset = removeExcludedChars(charset, request.getExcludeChars());

        if (charset.isEmpty()) {
            throw new BusinessException("排除字符后可用字符集为空");
        }

        List<String> values = new ArrayList<>();
        for (int i = 0; i < request.getCount(); i++) {
            values.add(generateRandomString(charset, request.getLength()));
        }

        RandomResponse response = new RandomResponse();
        response.setValues(values);
        response.setCount(request.getCount());
        response.setLength(request.getLength());
        response.setType(request.getType());

        return response;
    }

    public String generateUuid() {
        return java.util.UUID.randomUUID().toString();
    }

    public String generateUuidWithoutDash() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    private String buildCharset(RandomRequest request) {
        StringBuilder charset = new StringBuilder();

        if ("NUMBER".equals(request.getType())) {
            charset.append(NUMBERS);
        } else if ("LETTER".equals(request.getType())) {
            if (Boolean.TRUE.equals(request.getIncludeUppercase())) {
                charset.append(UPPERCASE);
            }
            if (Boolean.TRUE.equals(request.getIncludeLowercase())) {
                charset.append(LOWERCASE);
            }
            if (charset.length() == 0) {
                charset.append(UPPERCASE).append(LOWERCASE);
            }
        } else if ("MIXED".equals(request.getType()) || "SPECIAL".equals(request.getType())) {
            if (Boolean.TRUE.equals(request.getIncludeUppercase())) {
                charset.append(UPPERCASE);
            }
            if (Boolean.TRUE.equals(request.getIncludeLowercase())) {
                charset.append(LOWERCASE);
            }
            if (Boolean.TRUE.equals(request.getIncludeNumber())) {
                charset.append(NUMBERS);
            }
            if ("SPECIAL".equals(request.getType()) || Boolean.TRUE.equals(request.getIncludeSpecial())) {
                charset.append(SPECIAL);
            }
            if (charset.length() == 0) {
                charset.append(UPPERCASE).append(LOWERCASE).append(NUMBERS);
            }
        }

        return charset.toString();
    }

    private String removeExcludedChars(String charset, String excludeChars) {
        if (excludeChars == null || excludeChars.isEmpty()) {
            return charset;
        }

        StringBuilder result = new StringBuilder();
        for (char c : charset.toCharArray()) {
            if (excludeChars.indexOf(c) == -1) {
                result.append(c);
            }
        }
        return result.toString();
    }

    private String generateRandomString(String charset, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(charset.length());
            sb.append(charset.charAt(index));
        }
        return sb.toString();
    }
}
