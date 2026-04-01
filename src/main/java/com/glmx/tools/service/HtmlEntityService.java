package com.glmx.tools.service;

import com.glmx.tools.common.BusinessException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HtmlEntityService {

    private static final Map<String, String> NAMED_ENTITIES = new HashMap<>();
    private static final Pattern ENTITY_PATTERN = Pattern.compile("&(#?[xX]?(\\w+));");
    
    static {
        NAMED_ENTITIES.put("<", "&lt;");
        NAMED_ENTITIES.put(">", "&gt;");
        NAMED_ENTITIES.put("&", "&amp;");
        NAMED_ENTITIES.put("\"", "&quot;");
        NAMED_ENTITIES.put("'", "&apos;");
        NAMED_ENTITIES.put("'", "&#39;");
        NAMED_ENTITIES.put(" ", "&nbsp;");
        NAMED_ENTITIES.put("©", "&copy;");
        NAMED_ENTITIES.put("®", "&reg;");
        NAMED_ENTITIES.put("™", "&trade;");
        NAMED_ENTITIES.put("€", "&euro;");
        NAMED_ENTITIES.put("£", "&pound;");
        NAMED_ENTITIES.put("¥", "&yen;");
        NAMED_ENTITIES.put("¢", "&cent;");
        NAMED_ENTITIES.put("§", "&sect;");
        NAMED_ENTITIES.put("¶", "&para;");
        NAMED_ENTITIES.put("°", "&deg;");
        NAMED_ENTITIES.put("±", "&plusmn;");
        NAMED_ENTITIES.put("×", "&times;");
        NAMED_ENTITIES.put("÷", "&divide;");
        NAMED_ENTITIES.put("¼", "&frac14;");
        NAMED_ENTITIES.put("½", "&frac12;");
        NAMED_ENTITIES.put("¾", "&frac34;");
        NAMED_ENTITIES.put("←", "&larr;");
        NAMED_ENTITIES.put("→", "&rarr;");
        NAMED_ENTITIES.put("↑", "&uarr;");
        NAMED_ENTITIES.put("↓", "&darr;");
        NAMED_ENTITIES.put("↔", "&harr;");
        NAMED_ENTITIES.put("⇐", "&lArr;");
        NAMED_ENTITIES.put("⇒", "&rArr;");
        NAMED_ENTITIES.put("⇑", "&uArr;");
        NAMED_ENTITIES.put("⇓", "&dArr;");
        NAMED_ENTITIES.put("⇔", "&hArr;");
        NAMED_ENTITIES.put("♠", "&spades;");
        NAMED_ENTITIES.put("♣", "&clubs;");
        NAMED_ENTITIES.put("♥", "&hearts;");
        NAMED_ENTITIES.put("♦", "&diams;");
        NAMED_ENTITIES.put("α", "&alpha;");
        NAMED_ENTITIES.put("β", "&beta;");
        NAMED_ENTITIES.put("γ", "&gamma;");
        NAMED_ENTITIES.put("δ", "&delta;");
        NAMED_ENTITIES.put("ε", "&epsilon;");
        NAMED_ENTITIES.put("π", "&pi;");
        NAMED_ENTITIES.put("σ", "&sigma;");
        NAMED_ENTITIES.put("ω", "&omega;");
        NAMED_ENTITIES.put("Ω", "&Omega;");
        NAMED_ENTITIES.put("∞", "&infin;");
        NAMED_ENTITIES.put("√", "&radic;");
        NAMED_ENTITIES.put("∑", "&sum;");
        NAMED_ENTITIES.put("∫", "&int;");
        NAMED_ENTITIES.put("≈", "&asymp;");
        NAMED_ENTITIES.put("≠", "&ne;");
        NAMED_ENTITIES.put("≤", "&le;");
        NAMED_ENTITIES.put("≥", "&ge;");
        NAMED_ENTITIES.put("…", "&hellip;");
        NAMED_ENTITIES.put("–", "&ndash;");
        NAMED_ENTITIES.put("—", "&mdash;");
        NAMED_ENTITIES.put("·", "&middot;");
        NAMED_ENTITIES.put("•", "&bull;");
        NAMED_ENTITIES.put("‰", "&permil;");
        NAMED_ENTITIES.put("′", "&prime;");
        NAMED_ENTITIES.put("″", "&Prime;");
    }

    private static final Map<String, String> ENTITY_TO_CHAR = new HashMap<>();
    
    static {
        for (Map.Entry<String, String> entry : NAMED_ENTITIES.entrySet()) {
            ENTITY_TO_CHAR.put(entry.getValue(), entry.getKey());
            if (entry.getValue().startsWith("&#")) {
                continue;
            }
            String named = entry.getValue().substring(1, entry.getValue().length() - 1);
            ENTITY_TO_CHAR.put(named, entry.getKey());
        }
    }

    public String encode(String text, boolean encodeAll) {
        if (text == null || text.isEmpty()) {
            throw new BusinessException("待编码文本不能为空");
        }
        
        StringBuilder result = new StringBuilder();
        
        for (char c : text.toCharArray()) {
            String entity = NAMED_ENTITIES.get(String.valueOf(c));
            
            if (entity != null && !encodeAll) {
                result.append(entity);
            } else if (encodeAll || c > 127) {
                result.append("&#").append((int) c).append(";");
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }

    public String encodeNamed(String text) {
        if (text == null || text.isEmpty()) {
            throw new BusinessException("待编码文本不能为空");
        }
        
        StringBuilder result = new StringBuilder();
        
        for (char c : text.toCharArray()) {
            String entity = NAMED_ENTITIES.get(String.valueOf(c));
            if (entity != null) {
                result.append(entity);
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }

    public String encodeDecimal(String text) {
        if (text == null || text.isEmpty()) {
            throw new BusinessException("待编码文本不能为空");
        }
        
        StringBuilder result = new StringBuilder();
        
        for (char c : text.toCharArray()) {
            if (c > 127 || c == '<' || c == '>' || c == '&' || c == '"' || c == '\'') {
                result.append("&#").append((int) c).append(";");
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }

    public String encodeHex(String text) {
        if (text == null || text.isEmpty()) {
            throw new BusinessException("待编码文本不能为空");
        }
        
        StringBuilder result = new StringBuilder();
        
        for (char c : text.toCharArray()) {
            if (c > 127 || c == '<' || c == '>' || c == '&' || c == '"' || c == '\'') {
                result.append("&#x").append(Integer.toHexString(c).toUpperCase()).append(";");
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
        
        Matcher matcher = ENTITY_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String fullEntity = matcher.group(0);
            String entityContent = matcher.group(1);
            String replacement = decodeEntity(entityContent, fullEntity);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        
        return result.toString();
    }

    private String decodeEntity(String entityContent, String fullEntity) {
        if (entityContent.startsWith("#x") || entityContent.startsWith("#X")) {
            String hex = entityContent.substring(2);
            try {
                int codePoint = Integer.parseInt(hex, 16);
                return String.valueOf((char) codePoint);
            } catch (NumberFormatException e) {
                return fullEntity;
            }
        } else if (entityContent.startsWith("#")) {
            String decimal = entityContent.substring(1);
            try {
                int codePoint = Integer.parseInt(decimal, 10);
                return String.valueOf((char) codePoint);
            } catch (NumberFormatException e) {
                return fullEntity;
            }
        } else {
            String character = ENTITY_TO_CHAR.get(entityContent);
            if (character != null) {
                return character;
            }
            String entity = "&" + entityContent + ";";
            character = ENTITY_TO_CHAR.get(entity);
            if (character != null) {
                return character;
            }
            return fullEntity;
        }
    }

    public Map<String, String> getCommonEntities() {
        Map<String, String> common = new HashMap<>();
        common.put("<", "&lt;");
        common.put(">", "&gt;");
        common.put("&", "&amp;");
        common.put("\"", "&quot;");
        common.put("'", "&#39;");
        common.put(" ", "&nbsp;");
        common.put("©", "&copy;");
        common.put("®", "&reg;");
        common.put("™", "&trade;");
        common.put("€", "&euro;");
        common.put("£", "&pound;");
        common.put("¥", "&yen;");
        common.put("←", "&larr;");
        common.put("→", "&rarr;");
        return common;
    }
}
