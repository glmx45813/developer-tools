package com.glmx.tools.service;

import com.glmx.tools.dto.TimestampResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TimestampServiceTest {

    @Autowired
    private TimestampService timestampService;

    @Test
    void testFromTimestampSeconds() {
        long timestampSec = 1609459200L;
        TimestampResponse response = timestampService.fromTimestamp(timestampSec, "yyyy-MM-dd HH:mm:ss", "GMT+8");
        
        assertNotNull(response);
        assertEquals(timestampSec * 1000, response.getTimestampMs());
        assertEquals(timestampSec, response.getTimestampSec());
        assertNotNull(response.getFormatted());
    }

    @Test
    void testFromTimestampMillis() {
        long timestampMs = 1609459200000L;
        TimestampResponse response = timestampService.fromTimestamp(timestampMs, "yyyy-MM-dd HH:mm:ss", "GMT+8");
        
        assertNotNull(response);
        assertEquals(timestampMs, response.getTimestampMs());
    }

    @Test
    void testGetCurrentTime() {
        TimestampResponse response = timestampService.getCurrentTime("yyyy-MM-dd HH:mm:ss", "GMT+8");
        
        assertNotNull(response);
        assertNotNull(response.getTimestampMs());
        assertNotNull(response.getTimestampSec());
        assertNotNull(response.getFormatted());
        assertNotNull(response.getYear());
        assertNotNull(response.getMonth());
        assertNotNull(response.getDay());
        assertNotNull(response.getWeekday());
    }

    @Test
    void testFromDatetime() {
        String datetime = "2021-01-01 00:00:00";
        TimestampResponse response = timestampService.fromDatetime(datetime, "yyyy-MM-dd HH:mm:ss", "GMT+8");
        
        assertNotNull(response);
        assertNotNull(response.getTimestampMs());
        assertEquals(2021, response.getYear());
        assertEquals(1, response.getMonth());
        assertEquals(1, response.getDay());
    }

    @Test
    void testDifferentPatterns() {
        TimestampResponse response = timestampService.fromTimestamp(1609459200L, "yyyy年MM月dd日", "GMT+8");
        
        assertNotNull(response);
        assertTrue(response.getFormatted().contains("年"));
        assertTrue(response.getFormatted().contains("月"));
        assertTrue(response.getFormatted().contains("日"));
    }

    @Test
    void testIso8601Format() {
        TimestampResponse response = timestampService.getCurrentTime("yyyy-MM-dd HH:mm:ss", "GMT+8");
        
        assertNotNull(response.getIso8601());
        assertTrue(response.getIso8601().contains("T"));
    }
}
