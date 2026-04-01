package com.glmx.tools.service;

import com.glmx.tools.dto.UrlEncodeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UrlEncodeServiceTest {

    @Autowired
    private UrlEncodeService urlEncodeService;

    @Test
    void testEncode() {
        String text = "hello world";
        UrlEncodeResponse response = urlEncodeService.encode(text, "UTF-8");
        
        assertNotNull(response);
        assertEquals(text, response.getOriginal());
        assertEquals("hello+world", response.getEncoded());
    }

    @Test
    void testDecode() {
        String text = "hello+world";
        UrlEncodeResponse response = urlEncodeService.decode(text, "UTF-8");
        
        assertNotNull(response);
        assertEquals(text, response.getOriginal());
        assertEquals("hello world", response.getDecoded());
    }

    @Test
    void testEncodeChinese() {
        String text = "中文测试";
        UrlEncodeResponse response = urlEncodeService.encode(text, "UTF-8");
        
        assertNotNull(response);
        assertEquals(text, response.getOriginal());
        assertTrue(response.getEncoded().contains("%"));
    }

    @Test
    void testEncodeDecodeRoundTrip() {
        String text = "test=value&name=张三";
        UrlEncodeResponse encoded = urlEncodeService.encode(text, "UTF-8");
        UrlEncodeResponse decoded = urlEncodeService.decode(encoded.getEncoded(), "UTF-8");
        
        assertEquals(text, decoded.getDecoded());
    }

    @Test
    void testBatchEncode() {
        List<String> texts = Arrays.asList("hello", "world", "test");
        List<UrlEncodeResponse> responses = urlEncodeService.batchEncode(texts, "UTF-8");
        
        assertEquals(3, responses.size());
        assertEquals("hello", responses.get(0).getEncoded());
        assertEquals("world", responses.get(1).getEncoded());
        assertEquals("test", responses.get(2).getEncoded());
    }

    @Test
    void testBatchDecode() {
        List<String> texts = Arrays.asList("hello+world", "test%20123");
        List<UrlEncodeResponse> responses = urlEncodeService.batchDecode(texts, "UTF-8");
        
        assertEquals(2, responses.size());
        assertEquals("hello world", responses.get(0).getDecoded());
        assertEquals("test 123", responses.get(1).getDecoded());
    }
}
