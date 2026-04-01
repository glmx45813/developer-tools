package com.glmx.tools.service;

import com.glmx.tools.dto.RegexRequest;
import com.glmx.tools.dto.RegexResponse;
import com.glmx.tools.dto.RegexTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Service
public class RegexService {

    private final List<RegexTemplate> templates = Arrays.asList(
            createTemplate("email", "邮箱地址", "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
                    "验证邮箱地址格式", "test@example.com", "常用验证"),
            createTemplate("phone-cn", "中国手机号", "^1[3-9]\\d{9}$",
                    "验证中国大陆手机号", "13812345678", "常用验证"),
            createTemplate("phone-intl", "国际手机号", "^\\+?[1-9]\\d{1,14}$",
                    "验证国际手机号格式", "+8613812345678", "常用验证"),
            createTemplate("url", "URL地址", "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]",
                    "验证URL地址", "https://www.example.com", "常用验证"),
            createTemplate("ipv4", "IPv4地址", "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$",
                    "验证IPv4地址", "192.168.1.1", "网络相关"),
            createTemplate("ipv6", "IPv6地址", "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$",
                    "验证IPv6地址", "2001:0db8:85a3:0000:0000:8a2e:0370:7334", "网络相关"),
            createTemplate("mac", "MAC地址", "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$",
                    "验证MAC地址", "00:1A:2B:3C:4D:5E", "网络相关"),
            createTemplate("id-card-cn", "中国身份证号", "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$",
                    "验证中国身份证号(18位)", "11010519900307223X", "证件号码"),
            createTemplate("credit-card", "信用卡号", "^(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|3[47][0-9]{13}|6(?:011|5[0-9]{14})|(?:2131|1800|35\\d{3})\\d{11})$",
                    "验证信用卡号", "4111111111111111", "证件号码"),
            createTemplate("username", "用户名", "^[a-zA-Z][a-zA-Z0-9_-]{2,15}$",
                    "验证用户名(字母开头,3-16位)", "username123", "账号验证"),
            createTemplate("password-strong", "强密码", "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
                    "验证强密码(至少8位,包含大小写字母、数字和特殊字符)", "Passw0rd!", "账号验证"),
            createTemplate("password-medium", "中等密码", "^(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z\\d]{6,}$",
                    "验证中等密码(至少6位,包含字母和数字)", "pass123", "账号验证"),
            createTemplate("chinese", "中文字符", "^[\\u4e00-\\u9fa5]+$",
                    "验证纯中文字符", "中文测试", "字符验证"),
            createTemplate("english", "英文字母", "^[a-zA-Z]+$",
                    "验证纯英文字母", "English", "字符验证"),
            createTemplate("number", "纯数字", "^\\d+$",
                    "验证纯数字", "123456", "字符验证"),
            createTemplate("alphanumeric", "字母数字", "^[a-zA-Z0-9]+$",
                    "验证字母和数字组合", "abc123", "字符验证"),
            createTemplate("date-ymd", "日期(yyyy-MM-dd)", "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$",
                    "验证日期格式", "2024-01-01", "日期时间"),
            createTemplate("time-hms", "时间(HH:mm:ss)", "^([01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d$",
                    "验证时间格式", "23:59:59", "日期时间"),
            createTemplate("datetime", "日期时间", "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])\\s([01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d$",
                    "验证日期时间格式", "2024-01-01 23:59:59", "日期时间"),
            createTemplate("hex-color", "十六进制颜色", "^#?([a-fA-F0-9]{6}|[a-fA-F0-9]{3})$",
                    "验证十六进制颜色值", "#FF5733", "前端开发"),
            createTemplate("html-tag", "HTML标签", "<([a-zA-Z][a-zA-Z0-9]*)[^>]*>.*?</\\1>|<[a-zA-Z][a-zA-Z0-9]*/?>",
                    "匹配HTML标签", "<div>content</div>", "前端开发"),
            createTemplate("json-string", "JSON字符串", "^\\s*\\{[\\s\\S]*\\}\\s*$|^\\s*\\[[\\s\\S]*\\]\\s*$",
                    "简单验证JSON格式", "{\"key\":\"value\"}", "数据格式"),
            createTemplate("xml-tag", "XML标签", "<([a-zA-Z][a-zA-Z0-9_-]*)[^>]*>.*?</\\1>|<[a-zA-Z][a-zA-Z0-9_-]*/?>",
                    "匹配XML标签", "<root>content</root>", "数据格式"),
            createTemplate("base64", "Base64字符串", "^[A-Za-z0-9+/]+=*$",
                    "验证Base64编码", "SGVsbG8gV29ybGQ=", "编码验证"),
            createTemplate("md5", "MD5哈希", "^[a-fA-F0-9]{32}$",
                    "验证MD5哈希值", "d41d8cd98f00b204e9800998ecf8427e", "编码验证"),
            createTemplate("uuid", "UUID", "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                    "验证UUID格式", "550e8400-e29b-41d4-a716-446655440000", "编码验证"),
            createTemplate("slug", "URL Slug", "^[a-z0-9]+(?:-[a-z0-9]+)*$",
                    "验证URL Slug格式", "my-blog-post-title", "前端开发"),
            createTemplate("domain", "域名", "^(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$",
                    "验证域名格式", "example.com", "网络相关"),
            createTemplate("port", "端口号", "^([1-9]\\d{0,3}|[1-5]\\d{4}|6[0-4]\\d{3}|65[0-4]\\d{2}|655[0-2]\\d|6553[0-5])$",
                    "验证端口号(1-65535)", "8080", "网络相关"),
            createTemplate("qq", "QQ号", "^[1-9]\\d{4,10}$",
                    "验证QQ号", "12345678", "社交账号"),
            createTemplate("wechat", "微信号", "^[a-zA-Z][a-zA-Z0-9_-]{5,19}$",
                    "验证微信号格式", "wechat_id_123", "社交账号")
    );

    public RegexResponse test(RegexRequest request) {
        RegexResponse response = new RegexResponse();
        List<RegexResponse.MatchResult> matches = new ArrayList<>();

        try {
            int flags = buildFlags(request);
            Pattern pattern = Pattern.compile(request.getPattern(), flags);
            Matcher matcher = pattern.matcher(request.getText());

            response.setValidPattern(true);

            switch (request.getMode().toUpperCase()) {
                case "MATCHES":
                    response.setMatched(matcher.matches());
                    if (matcher.matches()) {
                        matches.add(buildMatchResult(matcher, 0));
                    }
                    break;
                case "MATCH":
                    response.setMatched(matcher.find());
                    if (matcher.find()) {
                        matches.add(buildMatchResult(matcher, 0));
                    }
                    break;
                case "FIND":
                default:
                    int index = 0;
                    while (matcher.find()) {
                        matches.add(buildMatchResult(matcher, index++));
                    }
                    response.setMatched(!matches.isEmpty());
                    break;
            }

            response.setMatches(matches);
            response.setMatchCount(matches.size());

        } catch (PatternSyntaxException e) {
            response.setValidPattern(false);
            response.setError("正则表达式语法错误: " + e.getDescription());
            response.setMatched(false);
        }

        return response;
    }

    public List<RegexTemplate> getTemplates() {
        return templates;
    }

    public List<RegexTemplate> getTemplatesByCategory(String category) {
        if (category == null || category.isEmpty()) {
            return templates;
        }
        List<RegexTemplate> result = new ArrayList<>();
        for (RegexTemplate template : templates) {
            if (category.equals(template.getCategory())) {
                result.add(template);
            }
        }
        return result;
    }

    public RegexTemplate getTemplateById(String id) {
        for (RegexTemplate template : templates) {
            if (id.equals(template.getId())) {
                return template;
            }
        }
        return null;
    }

    public List<String> getCategories() {
        return Arrays.asList("常用验证", "网络相关", "证件号码", "账号验证", "字符验证", "日期时间", "前端开发", "数据格式", "编码验证", "社交账号");
    }

    private int buildFlags(RegexRequest request) {
        int flags = 0;
        if (Boolean.TRUE.equals(request.getIgnoreCase())) {
            flags |= Pattern.CASE_INSENSITIVE;
        }
        if (Boolean.TRUE.equals(request.getMultiline())) {
            flags |= Pattern.MULTILINE;
        }
        if (Boolean.TRUE.equals(request.getDotAll())) {
            flags |= Pattern.DOTALL;
        }
        return flags;
    }

    private RegexResponse.MatchResult buildMatchResult(Matcher matcher, int index) {
        RegexResponse.MatchResult result = new RegexResponse.MatchResult();
        result.setValue(matcher.group());
        result.setStart(matcher.start());
        result.setEnd(matcher.end());

        List<RegexResponse.GroupInfo> groups = new ArrayList<>();
        for (int i = 0; i <= matcher.groupCount(); i++) {
            try {
                RegexResponse.GroupInfo group = new RegexResponse.GroupInfo();
                group.setIndex(i);
                group.setValue(matcher.group(i));
                groups.add(group);
            } catch (Exception ignored) {
            }
        }
        result.setGroups(groups);

        return result;
    }

    private RegexTemplate createTemplate(String id, String name, String pattern, String description, String example, String category) {
        RegexTemplate template = new RegexTemplate();
        template.setId(id);
        template.setName(name);
        template.setPattern(pattern);
        template.setDescription(description);
        template.setExample(example);
        template.setCategory(category);
        return template;
    }
}
