package com.glmx.tools.service;

import com.glmx.tools.dto.RegexRequest;
import com.glmx.tools.dto.RegexResponse;
import com.glmx.tools.dto.RegexTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RegexServiceTest {

    @Autowired
    private RegexService regexService;

    @Test
    void testFindMode() {
        RegexRequest request = new RegexRequest();
        request.setPattern("\\d+");
        request.setText("abc123def456");
        request.setMode("FIND");
        
        RegexResponse response = regexService.test(request);
        
        assertTrue(response.getValidPattern());
        assertTrue(response.getMatched());
        assertEquals(2, response.getMatchCount());
    }

    @Test
    void testMatchesMode() {
        RegexRequest request = new RegexRequest();
        request.setPattern("\\d+");
        request.setText("123");
        request.setMode("MATCHES");
        
        RegexResponse response = regexService.test(request);
        
        assertTrue(response.getValidPattern());
        assertTrue(response.getMatched());
    }

    @Test
    void testMatchesModeFail() {
        RegexRequest request = new RegexRequest();
        request.setPattern("\\d+");
        request.setText("abc123");
        request.setMode("MATCHES");
        
        RegexResponse response = regexService.test(request);
        
        assertTrue(response.getValidPattern());
        assertFalse(response.getMatched());
    }

    @Test
    void testIgnoreCase() {
        RegexRequest request = new RegexRequest();
        request.setPattern("HELLO");
        request.setText("hello world");
        request.setMode("FIND");
        request.setIgnoreCase(true);
        
        RegexResponse response = regexService.test(request);
        
        assertTrue(response.getMatched());
    }

    @Test
    void testInvalidPattern() {
        RegexRequest request = new RegexRequest();
        request.setPattern("[invalid");
        request.setText("test");
        request.setMode("FIND");
        
        RegexResponse response = regexService.test(request);
        
        assertFalse(response.getValidPattern());
        assertNotNull(response.getError());
    }

    @Test
    void testGroups() {
        RegexRequest request = new RegexRequest();
        request.setPattern("(\\d+)-(\\d+)");
        request.setText("abc123-456def");
        request.setMode("FIND");
        
        RegexResponse response = regexService.test(request);
        
        assertTrue(response.getMatched());
        assertEquals(1, response.getMatches().size());
        assertEquals(3, response.getMatches().get(0).getGroups().size());
    }

    @Test
    void testGetTemplates() {
        List<RegexTemplate> templates = regexService.getTemplates();
        
        assertNotNull(templates);
        assertTrue(templates.size() >= 20);
    }

    @Test
    void testGetTemplatesByCategory() {
        List<RegexTemplate> templates = regexService.getTemplatesByCategory("常用验证");
        
        assertNotNull(templates);
        assertTrue(templates.size() > 0);
    }

    @Test
    void testEmailTemplate() {
        RegexTemplate template = regexService.getTemplateById("email");
        
        assertNotNull(template);
        assertEquals("邮箱地址", template.getName());
    }
}
