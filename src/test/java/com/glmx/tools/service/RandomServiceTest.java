package com.glmx.tools.service;

import com.glmx.tools.dto.RandomRequest;
import com.glmx.tools.dto.RandomResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RandomServiceTest {

    @Autowired
    private RandomService randomService;

    @Test
    void testGenerateNumber() {
        RandomRequest request = new RandomRequest();
        request.setLength(10);
        request.setType("NUMBER");
        request.setCount(1);
        
        RandomResponse response = randomService.generate(request);
        
        assertNotNull(response);
        assertEquals(1, response.getValues().size());
        assertEquals(10, response.getValues().get(0).length());
        assertTrue(response.getValues().get(0).matches("\\d+"));
    }

    @Test
    void testGenerateLetter() {
        RandomRequest request = new RandomRequest();
        request.setLength(10);
        request.setType("LETTER");
        request.setCount(1);
        request.setIncludeUppercase(true);
        request.setIncludeLowercase(true);
        
        RandomResponse response = randomService.generate(request);
        
        assertNotNull(response);
        assertEquals(10, response.getValues().get(0).length());
        assertTrue(response.getValues().get(0).matches("[a-zA-Z]+"));
    }

    @Test
    void testGenerateMixed() {
        RandomRequest request = new RandomRequest();
        request.setLength(16);
        request.setType("MIXED");
        request.setCount(1);
        
        RandomResponse response = randomService.generate(request);
        
        assertNotNull(response);
        assertEquals(16, response.getValues().get(0).length());
    }

    @Test
    void testGenerateMultiple() {
        RandomRequest request = new RandomRequest();
        request.setLength(8);
        request.setType("NUMBER");
        request.setCount(5);
        
        RandomResponse response = randomService.generate(request);
        
        assertEquals(5, response.getValues().size());
    }

    @Test
    void testGenerateWithExclude() {
        RandomRequest request = new RandomRequest();
        request.setLength(10);
        request.setType("NUMBER");
        request.setCount(1);
        request.setExcludeChars("012345");
        
        RandomResponse response = randomService.generate(request);
        
        String result = response.getValues().get(0);
        assertFalse(result.contains("0"));
        assertFalse(result.contains("1"));
        assertFalse(result.contains("2"));
        assertFalse(result.contains("3"));
        assertFalse(result.contains("4"));
        assertFalse(result.contains("5"));
    }

    @Test
    void testGenerateUuid() {
        String uuid = randomService.generateUuid();
        
        assertNotNull(uuid);
        assertEquals(36, uuid.length());
        assertTrue(uuid.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }

    @Test
    void testGenerateUuidWithoutDash() {
        String uuid = randomService.generateUuidWithoutDash();
        
        assertNotNull(uuid);
        assertEquals(32, uuid.length());
        assertTrue(uuid.matches("[0-9a-f]{32}"));
    }
}
