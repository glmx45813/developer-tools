package com.glmx.tools.service;

import com.glmx.tools.dto.Md5Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class Md5ServiceTest {

    @Autowired
    private Md5Service md5Service;

    @Test
    void testEncryptStandard() {
        String text = "hello world";
        String result = md5Service.encryptStandard(text, false);
        
        assertNotNull(result);
        assertEquals(32, result.length());
        assertEquals("5eb63bbbe01eeed093cb22bb8f5acdc3", result);
    }

    @Test
    void testEncryptStandardUppercase() {
        String text = "hello world";
        String result = md5Service.encryptStandard(text, true);
        
        assertNotNull(result);
        assertEquals(32, result.length());
        assertEquals("5EB63BBBE01EEED093CB22BB8F5ACDC3", result);
    }

    @Test
    void testEncryptBit16() {
        String text = "hello world";
        String result = md5Service.encryptBit16(text, false);
        
        assertNotNull(result);
        assertEquals(16, result.length());
        assertEquals("e01eeed093cb22bb", result);
    }

    @Test
    void testEncryptBit32() {
        String text = "hello world";
        String result = md5Service.encryptBit32(text, false);
        
        assertNotNull(result);
        assertEquals(32, result.length());
        assertEquals("5eb63bbbe01eeed093cb22bb8f5acdc3", result);
    }

    @Test
    void testEncryptWithType() {
        String text = "test";
        
        String standard = md5Service.encrypt(text, "STANDARD", false);
        assertEquals(32, standard.length());
        
        String bit16 = md5Service.encrypt(text, "BIT16", false);
        assertEquals(16, bit16.length());
        
        String bit32 = md5Service.encrypt(text, "BIT32", false);
        assertEquals(32, bit32.length());
    }

    @Test
    void testEmptyString() {
        String result = md5Service.encryptStandard("", false);
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", result);
    }

    @Test
    void testChineseCharacters() {
        String text = "中文测试";
        String result = md5Service.encryptStandard(text, false);
        
        assertNotNull(result);
        assertEquals(32, result.length());
    }
}
