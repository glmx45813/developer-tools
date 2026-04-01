package com.glmx.tools.service;

import com.glmx.tools.dto.JsonRequest;
import com.glmx.tools.dto.JsonResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JsonServiceTest {

    @Autowired
    private JsonService jsonService;

    @Test
    void testFormat() {
        JsonRequest request = new JsonRequest();
        request.setJson("{\"name\":\"test\",\"value\":123}");
        
        JsonResponse response = jsonService.format(request);
        
        assertTrue(response.getValid());
        assertNotNull(response.getResult());
        assertTrue(response.getResult().contains("\n"));
    }

    @Test
    void testCompress() {
        JsonRequest request = new JsonRequest();
        request.setJson("{\n  \"name\": \"test\",\n  \"value\": 123\n}");
        
        JsonResponse response = jsonService.compress(request);
        
        assertTrue(response.getValid());
        assertNotNull(response.getResult());
        assertFalse(response.getResult().contains("\n"));
        assertFalse(response.getResult().contains(" "));
    }

    @Test
    void testValidateValid() {
        JsonRequest request = new JsonRequest();
        request.setJson("{\"name\":\"test\"}");
        
        JsonResponse response = jsonService.validate(request);
        
        assertTrue(response.getValid());
    }

    @Test
    void testValidateInvalid() {
        JsonRequest request = new JsonRequest();
        request.setJson("{name:test}");
        
        JsonResponse response = jsonService.validate(request);
        
        assertFalse(response.getValid());
        assertNotNull(response.getError());
    }

    @Test
    void testEscape() {
        JsonRequest request = new JsonRequest();
        request.setJson("hello\nworld");
        
        JsonResponse response = jsonService.escape(request);
        
        assertTrue(response.getValid());
        assertEquals("hello\\nworld", response.getResult());
    }

    @Test
    void testUnescape() {
        JsonRequest request = new JsonRequest();
        request.setJson("hello\\nworld");
        
        JsonResponse response = jsonService.unescape(request);
        
        assertTrue(response.getValid());
        assertEquals("hello\nworld", response.getResult());
    }

    @Test
    void testComplexJson() {
        JsonRequest request = new JsonRequest();
        request.setJson("{\"users\":[{\"name\":\"Alice\",\"age\":25},{\"name\":\"Bob\",\"age\":30}]}");
        
        JsonResponse response = jsonService.format(request);
        
        assertTrue(response.getValid());
        assertNotNull(response.getResult());
    }
}
