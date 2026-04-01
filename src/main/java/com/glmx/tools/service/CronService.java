package com.glmx.tools.service;

import com.glmx.tools.common.BusinessException;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CronService {

    private static final Pattern CRON_PATTERN = Pattern.compile(
        "^(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)(?:\\s+(\\S+))?$"
    );

    public CronResult generate(CronGenerateRequest request) {
        StringBuilder cron = new StringBuilder();
        
        String second = request.getSecond() != null ? request.getSecond() : "0";
        String minute = request.getMinute() != null ? request.getMinute() : "*";
        String hour = request.getHour() != null ? request.getHour() : "*";
        String dayOfMonth = request.getDayOfMonth() != null ? request.getDayOfMonth() : "*";
        String month = request.getMonth() != null ? request.getMonth() : "*";
        String dayOfWeek = request.getDayOfWeek() != null ? request.getDayOfWeek() : "*";
        
        cron.append(second).append(" ")
            .append(minute).append(" ")
            .append(hour).append(" ")
            .append(dayOfMonth).append(" ")
            .append(month).append(" ")
            .append(dayOfWeek);
        
        CronResult result = new CronResult();
        result.setExpression(cron.toString().trim());
        result.setDescription(generateDescription(cron.toString().trim()));
        result.setNextExecutions(getNextExecutions(cron.toString().trim(), 5));
        return result;
    }

    public CronResult parse(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new BusinessException("Cron表达式不能为空");
        }

        Matcher matcher = CRON_PATTERN.matcher(expression.trim());
        if (!matcher.matches()) {
            throw new BusinessException("Cron表达式格式无效");
        }

        CronResult result = new CronResult();
        result.setExpression(expression.trim());
        result.setSecond(matcher.group(1));
        result.setMinute(matcher.group(2));
        result.setHour(matcher.group(3));
        result.setDayOfMonth(matcher.group(4));
        result.setMonth(matcher.group(5));
        result.setDayOfWeek(matcher.groupCount() >= 6 ? matcher.group(6) : "*");
        result.setDescription(generateDescription(expression.trim()));
        result.setValid(true);
        result.setNextExecutions(getNextExecutions(expression.trim(), 5));
        return result;
    }

    public CronResult validate(String expression) {
        try {
            CronResult result = parse(expression);
            result.setValid(true);
            result.setMessage("Cron表达式有效");
            return result;
        } catch (Exception e) {
            CronResult result = new CronResult();
            result.setValid(false);
            result.setError(e.getMessage());
            return result;
        }
    }

    public List<String> getNextExecutions(String expression, int count) {
        List<String> executions = new ArrayList<>();
        
        try {
            String[] parts = expression.trim().split("\\s+");
            if (parts.length < 5 || parts.length > 6) {
                return executions;
            }

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            cal.add(Calendar.MINUTE, 1);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            int maxIterations = 366 * 24 * 60;
            int iterations = 0;

            while (executions.size() < count && iterations < maxIterations) {
                if (matchesCron(cal, parts)) {
                    executions.add(sdf.format(cal.getTime()));
                }
                cal.add(Calendar.MINUTE, 1);
                iterations++;
            }
        } catch (Exception e) {
            // ignore
        }

        return executions;
    }

    private boolean matchesCron(Calendar cal, String[] parts) {
        int secondIndex = parts.length == 6 ? 0 : -1;
        int minuteIndex = parts.length == 6 ? 1 : 0;
        int hourIndex = parts.length == 6 ? 2 : 1;
        int dayOfMonthIndex = parts.length == 6 ? 3 : 2;
        int monthIndex = parts.length == 6 ? 4 : 3;
        int dayOfWeekIndex = parts.length == 6 ? 5 : 4;

        if (secondIndex >= 0 && !matchesField(cal.get(Calendar.SECOND), parts[secondIndex], 0, 59)) {
            return false;
        }
        if (!matchesField(cal.get(Calendar.MINUTE), parts[minuteIndex], 0, 59)) {
            return false;
        }
        if (!matchesField(cal.get(Calendar.HOUR_OF_DAY), parts[hourIndex], 0, 23)) {
            return false;
        }
        if (!matchesField(cal.get(Calendar.MONTH) + 1, parts[monthIndex], 1, 12)) {
            return false;
        }
        
        String dayOfMonthPart = parts[dayOfMonthIndex];
        String dayOfWeekPart = parts[dayOfWeekIndex];
        
        if (!dayOfMonthPart.equals("*") && !dayOfWeekPart.equals("*")) {
            boolean dayOfMonthMatch = matchesField(cal.get(Calendar.DAY_OF_MONTH), dayOfMonthPart, 1, 31);
            boolean dayOfWeekMatch = matchesField(cal.get(Calendar.DAY_OF_WEEK) - 1, dayOfWeekPart, 0, 6);
            if (!dayOfMonthMatch && !dayOfWeekMatch) {
                return false;
            }
        } else {
            if (!dayOfMonthPart.equals("*") && !matchesField(cal.get(Calendar.DAY_OF_MONTH), dayOfMonthPart, 1, 31)) {
                return false;
            }
            if (!dayOfWeekPart.equals("*") && !matchesField(cal.get(Calendar.DAY_OF_WEEK) - 1, dayOfWeekPart, 0, 6)) {
                return false;
            }
        }

        return true;
    }

    private boolean matchesField(int value, String field, int min, int max) {
        if (field.equals("*")) {
            return true;
        }

        if (field.equals("?")) {
            return true;
        }

        if (field.contains("/")) {
            String[] stepParts = field.split("/");
            int start = stepParts[0].equals("*") ? min : Integer.parseInt(stepParts[0]);
            int step = Integer.parseInt(stepParts[1]);
            return (value - start) % step == 0 && value >= start;
        }

        if (field.contains("-")) {
            String[] rangeParts = field.split("-");
            int start = Integer.parseInt(rangeParts[0]);
            int end = Integer.parseInt(rangeParts[1]);
            return value >= start && value <= end;
        }

        if (field.contains(",")) {
            String[] values = field.split(",");
            for (String v : values) {
                if (Integer.parseInt(v.trim()) == value) {
                    return true;
                }
            }
            return false;
        }

        try {
            return Integer.parseInt(field) == value;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String generateDescription(String expression) {
        String[] parts = expression.trim().split("\\s+");
        StringBuilder desc = new StringBuilder();

        int secondIndex = parts.length == 6 ? 0 : -1;
        int minuteIndex = parts.length == 6 ? 1 : 0;
        int hourIndex = parts.length == 6 ? 2 : 1;
        int dayOfMonthIndex = parts.length == 6 ? 3 : 2;
        int monthIndex = parts.length == 6 ? 4 : 3;
        int dayOfWeekIndex = parts.length == 6 ? 5 : 4;

        if (secondIndex >= 0 && !parts[secondIndex].equals("0") && !parts[secondIndex].equals("*")) {
            desc.append("每").append(describeField(parts[secondIndex], "秒", 0, 59));
        }

        if (!parts[minuteIndex].equals("*")) {
            desc.append("每").append(describeField(parts[minuteIndex], "分", 0, 59));
        }

        if (!parts[hourIndex].equals("*")) {
            desc.append(describeField(parts[hourIndex], "时", 0, 23));
        }

        if (!parts[dayOfMonthIndex].equals("*") && !parts[dayOfMonthIndex].equals("?")) {
            desc.append("每月").append(describeField(parts[dayOfMonthIndex], "日", 1, 31));
        }

        if (!parts[monthIndex].equals("*")) {
            desc.append(describeMonth(parts[monthIndex]));
        }

        if (!parts[dayOfWeekIndex].equals("*") && !parts[dayOfWeekIndex].equals("?")) {
            desc.append("每").append(describeDayOfWeek(parts[dayOfWeekIndex]));
        }

        return desc.length() > 0 ? desc.toString() : "每分钟执行";
    }

    private String describeField(String field, String unit, int min, int max) {
        if (field.equals("*")) {
            return unit;
        }
        if (field.contains("/")) {
            String[] parts = field.split("/");
            return parts[1] + unit + "执行一次";
        }
        if (field.contains("-")) {
            String[] parts = field.split("-");
            return parts[0] + unit + "到" + parts[1] + unit;
        }
        return field + unit;
    }

    private String describeMonth(String field) {
        String[] months = {"", "一月", "二月", "三月", "四月", "五月", "六月", 
                          "七月", "八月", "九月", "十月", "十一月", "十二月"};
        if (field.equals("*")) {
            return "";
        }
        try {
            int month = Integer.parseInt(field);
            return months[month];
        } catch (NumberFormatException e) {
            return field;
        }
    }

    private String describeDayOfWeek(String field) {
        String[] days = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        if (field.equals("*")) {
            return "";
        }
        try {
            int day = Integer.parseInt(field);
            return days[day];
        } catch (NumberFormatException e) {
            return field;
        }
    }

    public List<CronPreset> getPresets() {
        List<CronPreset> presets = new ArrayList<>();
        
        presets.add(new CronPreset("每分钟", "0 * * * * ?", "每分钟执行一次"));
        presets.add(new CronPreset("每小时", "0 0 * * * ?", "每小时整点执行"));
        presets.add(new CronPreset("每天零点", "0 0 0 * * ?", "每天凌晨0点执行"));
        presets.add(new CronPreset("每天中午", "0 0 12 * * ?", "每天中午12点执行"));
        presets.add(new CronPreset("每天早晚", "0 0 9,18 * * ?", "每天9点和18点执行"));
        presets.add(new CronPreset("每周一", "0 0 0 ? * MON", "每周一凌晨执行"));
        presets.add(new CronPreset("每周工作日", "0 0 9 ? * MON-FRI", "工作日早上9点执行"));
        presets.add(new CronPreset("每月1号", "0 0 0 1 * ?", "每月1号凌晨执行"));
        presets.add(new CronPreset("每月最后一天", "0 0 0 L * ?", "每月最后一天凌晨执行"));
        presets.add(new CronPreset("每5分钟", "0 0/5 * * * ?", "每5分钟执行一次"));
        presets.add(new CronPreset("每30分钟", "0 0/30 * * * ?", "每30分钟执行一次"));
        presets.add(new CronPreset("每季度", "0 0 0 1 1,4,7,10 ?", "每季度第一天执行"));
        
        return presets;
    }

    public static class CronGenerateRequest {
        private String second;
        private String minute;
        private String hour;
        private String dayOfMonth;
        private String month;
        private String dayOfWeek;

        public String getSecond() { return second; }
        public void setSecond(String second) { this.second = second; }
        public String getMinute() { return minute; }
        public void setMinute(String minute) { this.minute = minute; }
        public String getHour() { return hour; }
        public void setHour(String hour) { this.hour = hour; }
        public String getDayOfMonth() { return dayOfMonth; }
        public void setDayOfMonth(String dayOfMonth) { this.dayOfMonth = dayOfMonth; }
        public String getMonth() { return month; }
        public void setMonth(String month) { this.month = month; }
        public String getDayOfWeek() { return dayOfWeek; }
        public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    }

    public static class CronResult {
        private String expression;
        private String second;
        private String minute;
        private String hour;
        private String dayOfMonth;
        private String month;
        private String dayOfWeek;
        private String description;
        private Boolean valid;
        private String message;
        private String error;
        private List<String> nextExecutions;

        public String getExpression() { return expression; }
        public void setExpression(String expression) { this.expression = expression; }
        public String getSecond() { return second; }
        public void setSecond(String second) { this.second = second; }
        public String getMinute() { return minute; }
        public void setMinute(String minute) { this.minute = minute; }
        public String getHour() { return hour; }
        public void setHour(String hour) { this.hour = hour; }
        public String getDayOfMonth() { return dayOfMonth; }
        public void setDayOfMonth(String dayOfMonth) { this.dayOfMonth = dayOfMonth; }
        public String getMonth() { return month; }
        public void setMonth(String month) { this.month = month; }
        public String getDayOfWeek() { return dayOfWeek; }
        public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Boolean getValid() { return valid; }
        public void setValid(Boolean valid) { this.valid = valid; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public List<String> getNextExecutions() { return nextExecutions; }
        public void setNextExecutions(List<String> nextExecutions) { this.nextExecutions = nextExecutions; }
    }

    public static class CronPreset {
        private String name;
        private String expression;
        private String description;

        public CronPreset(String name, String expression, String description) {
            this.name = name;
            this.expression = expression;
            this.description = description;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getExpression() { return expression; }
        public void setExpression(String expression) { this.expression = expression; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
