package com.glmx.tools.service;

import com.glmx.tools.common.BusinessException;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class UnicodeService {

    private static final Pattern UNICODE_PATTERN = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
    private static final Pattern UNICODE_PATTERN2 = Pattern.compile("%u([0-9a-fA-F]{4})");
    private static final Pattern UNICODE_PATTERN3 = Pattern.compile("&#x([0-9a-fA-F]+);");
    private static final Pattern UNICODE_PATTERN4 = Pattern.compile("&#(\\d+);");

    public String encode(String text, String format) {
        if (text == null || text.isEmpty()) {
            throw new BusinessException("待编码文本不能为空");
        }
        
        StringBuilder result = new StringBuilder();
        
        for (char c : text.toCharArray()) {
            int codePoint = c;
            
            if (Character.isSupplementaryCodePoint(codePoint)) {
                char[] surrogates = Character.toChars(codePoint);
                for (char surrogate : surrogates) {
                    result.append(formatCodePoint(surrogate, format));
                }
            } else {
                result.append(formatCodePoint(c, format));
            }
        }
        
        return result.toString();
    }

    public String encodeChinese(String text, String format) {
        if (text == null || text.isEmpty()) {
            throw new BusinessException("待编码文本不能为空");
        }
        
        StringBuilder result = new StringBuilder();
        
        for (char c : text.toCharArray()) {
            if (c > 127) {
                result.append(formatCodePoint(c, format));
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }

    public String decode(String text) {
        if (text == null || text.isEmpty()) {
            throw new BusinessException("待解码文本不能为空");
        }
        
        String result = text;
        
        result = decodePattern(result, UNICODE_PATTERN);
        result = decodePattern(result, UNICODE_PATTERN2);
        result = decodeHexPattern(result, UNICODE_PATTERN3);
        result = decodeDecimalPattern(result, UNICODE_PATTERN4);
        
        return result;
    }

    public String decodePattern(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String hex = matcher.group(1);
            int codePoint = Integer.parseInt(hex, 16);
            matcher.appendReplacement(sb, String.valueOf((char) codePoint));
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }

    private String decodeHexPattern(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String hex = matcher.group(1);
            int codePoint = Integer.parseInt(hex, 16);
            matcher.appendReplacement(sb, String.valueOf((char) codePoint));
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }

    private String decodeDecimalPattern(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String decimal = matcher.group(1);
            int codePoint = Integer.parseInt(decimal, 10);
            matcher.appendReplacement(sb, String.valueOf((char) codePoint));
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }

    private String formatCodePoint(char c, String format) {
        String hex = String.format("%04X", (int) c);
        
        if (format == null || format.isEmpty()) {
            format = "U+";
        }
        
        switch (format.toLowerCase()) {
            case "\\u":
                return "\\u" + hex.toLowerCase();
            case "%u":
                return "%u" + hex.toLowerCase();
            case "u+":
                return "U+" + hex.toUpperCase();
            case "&#x":
                return "&#x" + hex.toLowerCase() + ";";
            case "&#":
                return "&#" + (int) c + ";";
            default:
                return "\\u" + hex.toLowerCase();
        }
    }

    public CodePointInfo getCodePointInfo(String text) {
        if (text == null || text.isEmpty()) {
            throw new BusinessException("文本不能为空");
        }
        
        CodePointInfo info = new CodePointInfo();
        
        if (text.length() == 1 || (text.length() == 2 && Character.isSurrogatePair(text.charAt(0), text.charAt(1)))) {
            int codePoint = text.codePointAt(0);
            info.setCodePoint(codePoint);
            info.setHex("U+" + String.format("%04X", codePoint));
            info.setDecimal(codePoint);
            info.setBinary(Integer.toBinaryString(codePoint));
            info.setOctal(Integer.toOctalString(codePoint));
            info.setName(getCharacterName(codePoint));
            info.setBlock(Character.UnicodeBlock.of(codePoint).toString());
            info.setType(getCharacterType(codePoint));
        } else {
            throw new BusinessException("请输入单个字符");
        }
        
        return info;
    }

    private String getCharacterName(int codePoint) {
        if (Character.isLetter(codePoint)) {
            return "LETTER";
        } else if (Character.isDigit(codePoint)) {
            return "DIGIT";
        } else if (Character.isWhitespace(codePoint)) {
            return "WHITESPACE";
        } else if (Character.isUpperCase(codePoint)) {
            return "UPPERCASE_LETTER";
        } else if (Character.isLowerCase(codePoint)) {
            return "LOWERCASE_LETTER";
        } else if (Character.isISOControl(codePoint)) {
            return "CONTROL";
        } else {
            int type = Character.getType(codePoint);
            if (type == Character.CONNECTOR_PUNCTUATION || type == Character.DASH_PUNCTUATION ||
                type == Character.START_PUNCTUATION || type == Character.END_PUNCTUATION ||
                type == Character.INITIAL_QUOTE_PUNCTUATION || type == Character.FINAL_QUOTE_PUNCTUATION ||
                type == Character.OTHER_PUNCTUATION) {
                return "PUNCTUATION";
            } else if (type == Character.MATH_SYMBOL || type == Character.CURRENCY_SYMBOL ||
                       type == Character.MODIFIER_SYMBOL || type == Character.OTHER_SYMBOL) {
                return "SYMBOL";
            } else {
                return "OTHER";
            }
        }
    }

    private String getCharacterType(int codePoint) {
        switch (Character.getType(codePoint)) {
            case Character.UPPERCASE_LETTER: return "大写字母";
            case Character.LOWERCASE_LETTER: return "小写字母";
            case Character.TITLECASE_LETTER: return "标题字母";
            case Character.MODIFIER_LETTER: return "修饰字母";
            case Character.OTHER_LETTER: return "其他字母";
            case Character.DECIMAL_DIGIT_NUMBER: return "十进制数字";
            case Character.LETTER_NUMBER: return "字母数字";
            case Character.OTHER_NUMBER: return "其他数字";
            case Character.SPACE_SEPARATOR: return "空白分隔符";
            case Character.LINE_SEPARATOR: return "行分隔符";
            case Character.PARAGRAPH_SEPARATOR: return "段落分隔符";
            case Character.CONNECTOR_PUNCTUATION: return "连接标点";
            case Character.DASH_PUNCTUATION: return "破折号";
            case Character.START_PUNCTUATION: return "开始标点";
            case Character.END_PUNCTUATION: return "结束标点";
            case Character.INITIAL_QUOTE_PUNCTUATION: return "前引号";
            case Character.FINAL_QUOTE_PUNCTUATION: return "后引号";
            case Character.OTHER_PUNCTUATION: return "其他标点";
            case Character.MATH_SYMBOL: return "数学符号";
            case Character.CURRENCY_SYMBOL: return "货币符号";
            case Character.MODIFIER_SYMBOL: return "修饰符号";
            case Character.OTHER_SYMBOL: return "其他符号";
            case Character.CONTROL: return "控制字符";
            case Character.FORMAT: return "格式字符";
            case Character.PRIVATE_USE: return "私用字符";
            case Character.SURROGATE: return "代理字符";
            default: return "未知类型";
        }
    }

    public static class CodePointInfo {
        private Integer codePoint;
        private String hex;
        private Integer decimal;
        private String binary;
        private String octal;
        private String name;
        private String block;
        private String type;

        public Integer getCodePoint() { return codePoint; }
        public void setCodePoint(Integer codePoint) { this.codePoint = codePoint; }
        public String getHex() { return hex; }
        public void setHex(String hex) { this.hex = hex; }
        public Integer getDecimal() { return decimal; }
        public void setDecimal(Integer decimal) { this.decimal = decimal; }
        public String getBinary() { return binary; }
        public void setBinary(String binary) { this.binary = binary; }
        public String getOctal() { return octal; }
        public void setOctal(String octal) { this.octal = octal; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getBlock() { return block; }
        public void setBlock(String block) { this.block = block; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }
}
