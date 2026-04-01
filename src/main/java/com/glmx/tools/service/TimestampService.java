package com.glmx.tools.service;

import com.glmx.tools.common.BusinessException;
import com.glmx.tools.dto.TimestampRequest;
import com.glmx.tools.dto.TimestampResponse;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

@Service
public class TimestampService {

    private static final String[] WEEKDAYS = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
    private static final Map<String, String> COMMON_PATTERNS = new HashMap<>();

    static {
        COMMON_PATTERNS.put("default", "yyyy-MM-dd HH:mm:ss");
        COMMON_PATTERNS.put("date", "yyyy-MM-dd");
        COMMON_PATTERNS.put("time", "HH:mm:ss");
        COMMON_PATTERNS.put("datetime", "yyyy-MM-dd HH:mm:ss");
        COMMON_PATTERNS.put("compact", "yyyyMMddHHmmss");
        COMMON_PATTERNS.put("chinese", "yyyy年MM月dd日 HH时mm分ss秒");
    }

    public TimestampResponse fromTimestamp(Long timestamp, String pattern, String timezone) {
        long timestampMs = normalizeTimestamp(timestamp);
        Instant instant = Instant.ofEpochMilli(timestampMs);
        ZoneId zoneId = parseZoneId(timezone);
        ZonedDateTime zdt = instant.atZone(zoneId);

        return buildResponse(zdt, pattern);
    }

    public TimestampResponse fromDatetime(String datetime, String pattern, String timezone) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        ZoneId zoneId = parseZoneId(timezone);

        LocalDateTime ldt;
        try {
            ldt = LocalDateTime.parse(datetime, formatter);
        } catch (DateTimeParseException e) {
            throw new BusinessException("日期格式解析失败，请检查格式是否正确");
        }

        ZonedDateTime zdt = ldt.atZone(zoneId);
        return buildResponse(zdt, pattern);
    }

    public TimestampResponse getCurrentTime(String pattern, String timezone) {
        ZoneId zoneId = parseZoneId(timezone);
        ZonedDateTime zdt = ZonedDateTime.now(zoneId);
        return buildResponse(zdt, pattern);
    }

    public Map<String, Object> getCommonPatterns() {
        Map<String, Object> result = new HashMap<>();
        result.put("patterns", COMMON_PATTERNS);
        return result;
    }

    public TimestampResponse convert(TimestampRequest request) {
        if (request.getTimestamp() != null) {
            return fromTimestamp(request.getTimestamp(), request.getPattern(), request.getTimezone());
        } else if (request.getDatetime() != null && !request.getDatetime().isEmpty()) {
            return fromDatetime(request.getDatetime(), request.getPattern(), request.getTimezone());
        } else {
            return getCurrentTime(request.getPattern(), request.getTimezone());
        }
    }

    private long normalizeTimestamp(Long timestamp) {
        if (timestamp == null) {
            throw new BusinessException("时间戳不能为空");
        }

        String tsStr = timestamp.toString();
        if (tsStr.length() <= 10) {
            return timestamp * 1000;
        } else if (tsStr.length() <= 13) {
            return timestamp;
        } else {
            return timestamp / (long) Math.pow(10, tsStr.length() - 13);
        }
    }

    private ZoneId parseZoneId(String timezone) {
        if (timezone == null || timezone.isEmpty()) {
            return ZoneId.of("GMT+8");
        }

        try {
            if (timezone.startsWith("GMT")) {
                return ZoneId.of(timezone);
            }
            return ZoneId.of(timezone);
        } catch (Exception e) {
            return ZoneId.of("GMT+8");
        }
    }

    private TimestampResponse buildResponse(ZonedDateTime zdt, String pattern) {
        TimestampResponse response = new TimestampResponse();

        response.setTimestampMs(zdt.toInstant().toEpochMilli());
        response.setTimestampSec(zdt.toInstant().toEpochMilli() / 1000);

        String actualPattern = pattern != null && !pattern.isEmpty() ? pattern : "yyyy-MM-dd HH:mm:ss";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(actualPattern);
        response.setFormatted(zdt.format(formatter));

        response.setYear(zdt.getYear());
        response.setMonth(zdt.getMonthValue());
        response.setDay(zdt.getDayOfMonth());
        response.setHour(zdt.getHour());
        response.setMinute(zdt.getMinute());
        response.setSecond(zdt.getSecond());
        response.setWeekday(WEEKDAYS[zdt.getDayOfWeek().getValue() % 7]);

        response.setIso8601(zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        return response;
    }
}
