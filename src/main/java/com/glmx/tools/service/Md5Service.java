package com.glmx.tools.service;

import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import java.nio.charset.StandardCharsets;

@Service
public class Md5Service {

    public String encryptStandard(String text, boolean uppercase) {
        String md5 = DigestUtils.md5DigestAsHex(text.getBytes(StandardCharsets.UTF_8));
        return uppercase ? md5.toUpperCase() : md5.toLowerCase();
    }

    public String encryptBit16(String text, boolean uppercase) {
        String md5 = encryptStandard(text, false);
        String bit16 = md5.substring(8, 24);
        return uppercase ? bit16.toUpperCase() : bit16.toLowerCase();
    }

    public String encryptBit32(String text, boolean uppercase) {
        return encryptStandard(text, uppercase);
    }

    public String encrypt(String text, String type, boolean uppercase) {
        switch (type.toUpperCase()) {
            case "BIT16":
                return encryptBit16(text, uppercase);
            case "BIT32":
            case "STANDARD":
            default:
                return encryptStandard(text, uppercase);
        }
    }
}
