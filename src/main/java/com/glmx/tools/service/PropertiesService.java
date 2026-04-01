package com.glmx.tools.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.glmx.tools.common.BusinessException;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PropertiesService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final YAMLMapper yamlMapper = new YAMLMapper();

    public PropertiesResult propertiesToYaml(String properties) {
        if (properties == null || properties.trim().isEmpty()) {
            throw new BusinessException("Properties内容不能为空");
        }

        try {
            Map<String, Object> map = parseProperties(properties);
            String yaml = mapToYaml(map);

            PropertiesResult result = new PropertiesResult();
            result.setResult(yaml);
            result.setValid(true);
            result.setOriginalSize(properties.length());
            result.setResultSize(yaml.length());
            return result;
        } catch (Exception e) {
            PropertiesResult result = new PropertiesResult();
            result.setValid(false);
            result.setError("转换失败: " + e.getMessage());
            return result;
        }
    }

    public PropertiesResult propertiesToJson(String properties) {
        if (properties == null || properties.trim().isEmpty()) {
            throw new BusinessException("Properties内容不能为空");
        }

        try {
            Map<String, Object> map = parseProperties(properties);
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);

            PropertiesResult result = new PropertiesResult();
            result.setResult(json);
            result.setValid(true);
            result.setOriginalSize(properties.length());
            result.setResultSize(json.length());
            return result;
        } catch (Exception e) {
            PropertiesResult result = new PropertiesResult();
            result.setValid(false);
            result.setError("转换失败: " + e.getMessage());
            return result;
        }
    }

    public PropertiesResult yamlToProperties(String yaml) {
        if (yaml == null || yaml.trim().isEmpty()) {
            throw new BusinessException("YAML内容不能为空");
        }

        try {
            Yaml yamlParser = new Yaml();
            Map<String, Object> map = yamlParser.load(yaml);
            String properties = mapToProperties(map, "");

            PropertiesResult result = new PropertiesResult();
            result.setResult(properties);
            result.setValid(true);
            result.setOriginalSize(yaml.length());
            result.setResultSize(properties.length());
            return result;
        } catch (Exception e) {
            PropertiesResult result = new PropertiesResult();
            result.setValid(false);
            result.setError("转换失败: " + e.getMessage());
            return result;
        }
    }

    public PropertiesResult jsonToProperties(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new BusinessException("JSON内容不能为空");
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(json, LinkedHashMap.class);
            String properties = mapToProperties(map, "");

            PropertiesResult result = new PropertiesResult();
            result.setResult(properties);
            result.setValid(true);
            result.setOriginalSize(json.length());
            result.setResultSize(properties.length());
            return result;
        } catch (Exception e) {
            PropertiesResult result = new PropertiesResult();
            result.setValid(false);
            result.setError("转换失败: " + e.getMessage());
            return result;
        }
    }

    public PropertiesResult format(String properties) {
        if (properties == null || properties.trim().isEmpty()) {
            throw new BusinessException("Properties内容不能为空");
        }

        try {
            Map<String, Object> map = parseProperties(properties);
            String formatted = mapToProperties(map, "");

            PropertiesResult result = new PropertiesResult();
            result.setResult(formatted);
            result.setValid(true);
            result.setOriginalSize(properties.length());
            result.setResultSize(formatted.length());
            return result;
        } catch (Exception e) {
            PropertiesResult result = new PropertiesResult();
            result.setValid(false);
            result.setError("格式化失败: " + e.getMessage());
            return result;
        }
    }

    public PropertiesResult validate(String properties) {
        if (properties == null || properties.trim().isEmpty()) {
            throw new BusinessException("Properties内容不能为空");
        }

        try {
            parseProperties(properties);

            PropertiesResult result = new PropertiesResult();
            result.setValid(true);
            result.setMessage("Properties格式有效");
            return result;
        } catch (Exception e) {
            PropertiesResult result = new PropertiesResult();
            result.setValid(false);
            result.setError("Properties格式错误: " + e.getMessage());
            return result;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseProperties(String properties) throws Exception {
        Properties props = new Properties();
        props.load(new StringReader(properties));

        Map<String, Object> result = new LinkedHashMap<>();
        
        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key);
            setNestedValue(result, key, value);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private void setNestedValue(Map<String, Object> map, String key, String value) {
        String[] parts = key.split("\\.");
        Map<String, Object> current = map;

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            
            if (isIndexedKey(part)) {
                handleIndexedKey(current, part, parts, i, value);
                return;
            }

            if (!current.containsKey(part)) {
                current.put(part, new LinkedHashMap<String, Object>());
            }
            
            Object next = current.get(part);
            if (!(next instanceof Map)) {
                LinkedHashMap<String, Object> newMap = new LinkedHashMap<>();
                newMap.put("", next);
                current.put(part, newMap);
                next = newMap;
            }
            current = (Map<String, Object>) next;
        }

        String lastPart = parts[parts.length - 1];
        if (isIndexedKey(lastPart)) {
            handleIndexedKey(current, lastPart, parts, parts.length - 1, value);
        } else {
            current.put(lastPart, convertValue(value));
        }
    }

    private boolean isIndexedKey(String key) {
        return key.matches(".+\\[\\d+\\]");
    }

    @SuppressWarnings("unchecked")
    private void handleIndexedKey(Map<String, Object> map, String indexedKey, String[] parts, int index, String value) {
        Pattern pattern = Pattern.compile("(.+)\\[(\\d+)\\]");
        Matcher matcher = pattern.matcher(indexedKey);
        
        if (matcher.matches()) {
            String baseKey = matcher.group(1);
            int arrayIndex = Integer.parseInt(matcher.group(2));
            
            if (!map.containsKey(baseKey)) {
                map.put(baseKey, new LinkedHashMap<Integer, Object>());
            }
            
            Object obj = map.get(baseKey);
            if (obj instanceof Map) {
                Map<Integer, Object> indexedMap = (Map<Integer, Object>) obj;
                indexedMap.put(arrayIndex, convertValue(value));
            }
        }
    }

    private Object convertValue(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                long longValue = Long.parseLong(value);
                if (longValue <= Integer.MAX_VALUE && longValue >= Integer.MIN_VALUE) {
                    return (int) longValue;
                }
                return longValue;
            }
        } catch (NumberFormatException e) {
            return value;
        }
    }

    @SuppressWarnings("unchecked")
    private String mapToProperties(Map<String, Object> map, String prefix) {
        StringBuilder result = new StringBuilder();
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof Map) {
                Map<?, ?> subMap = (Map<?, ?>) value;
                boolean isArray = subMap.keySet().stream().allMatch(k -> k instanceof Integer);
                
                if (isArray) {
                    for (Map.Entry<?, ?> subEntry : subMap.entrySet()) {
                        String arrayKey = key + "[" + subEntry.getKey() + "]";
                        Object subValue = subEntry.getValue();
                        if (subValue instanceof Map) {
                            result.append(mapToProperties((Map<String, Object>) subValue, arrayKey));
                        } else {
                            result.append(arrayKey).append("=").append(subValue).append("\n");
                        }
                    }
                } else {
                    result.append(mapToProperties((Map<String, Object>) value, key));
                }
            } else {
                result.append(key).append("=").append(value).append("\n");
            }
        }
        
        return result.toString();
    }

    private String mapToYaml(Map<String, Object> map) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        
        Yaml yaml = new Yaml(options);
        return yaml.dump(map);
    }

    public static class PropertiesResult {
        private String result;
        private Boolean valid;
        private String message;
        private String error;
        private Integer originalSize;
        private Integer resultSize;

        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
        public Boolean getValid() { return valid; }
        public void setValid(Boolean valid) { this.valid = valid; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public Integer getOriginalSize() { return originalSize; }
        public void setOriginalSize(Integer originalSize) { this.originalSize = originalSize; }
        public Integer getResultSize() { return resultSize; }
        public void setResultSize(Integer resultSize) { this.resultSize = resultSize; }
    }
}
