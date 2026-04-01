package com.glmx.tools.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.glmx.tools.common.BusinessException;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;

@Service
public class YamlService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final YAMLMapper yamlMapper = new YAMLMapper();
    private final Yaml snakeYaml;

    public YamlService() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        options.setIndicatorIndent(0);
        options.setIndentWithIndicator(true);
        this.snakeYaml = new Yaml(options);
    }

    public YamlResult format(String yaml, int indent) {
        if (yaml == null || yaml.trim().isEmpty()) {
            throw new BusinessException("YAML内容不能为空");
        }

        try {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            options.setIndent(indent);
            options.setIndicatorIndent(0);
            options.setIndentWithIndicator(true);
            
            Yaml yamlParser = new Yaml(options);
            Object data = yamlParser.load(yaml);
            String formatted = yamlParser.dump(data);

            YamlResult result = new YamlResult();
            result.setResult(formatted);
            result.setValid(true);
            result.setOriginalSize(yaml.length());
            result.setResultSize(formatted.length());
            return result;
        } catch (Exception e) {
            YamlResult result = new YamlResult();
            result.setValid(false);
            result.setError("YAML格式错误: " + e.getMessage());
            return result;
        }
    }

    public YamlResult compress(String yaml) {
        if (yaml == null || yaml.trim().isEmpty()) {
            throw new BusinessException("YAML内容不能为空");
        }

        try {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.FLOW);
            options.setPrettyFlow(false);
            
            Yaml yamlParser = new Yaml(options);
            Object data = yamlParser.load(yaml);
            String compressed = yamlParser.dump(data).trim();

            YamlResult result = new YamlResult();
            result.setResult(compressed);
            result.setValid(true);
            result.setOriginalSize(yaml.length());
            result.setResultSize(compressed.length());
            result.setCompressionRatio(String.format("%.2f%%", 
                (1 - (double) compressed.length() / yaml.length()) * 100));
            return result;
        } catch (Exception e) {
            YamlResult result = new YamlResult();
            result.setValid(false);
            result.setError("YAML格式错误: " + e.getMessage());
            return result;
        }
    }

    public YamlResult validate(String yaml) {
        if (yaml == null || yaml.trim().isEmpty()) {
            throw new BusinessException("YAML内容不能为空");
        }

        try {
            snakeYaml.load(yaml);

            YamlResult result = new YamlResult();
            result.setValid(true);
            result.setMessage("YAML格式有效");
            return result;
        } catch (Exception e) {
            YamlResult result = new YamlResult();
            result.setValid(false);
            result.setError("YAML格式错误: " + e.getMessage());
            return result;
        }
    }

    public YamlResult toJson(String yaml) {
        if (yaml == null || yaml.trim().isEmpty()) {
            throw new BusinessException("YAML内容不能为空");
        }

        try {
            JsonNode jsonNode = yamlMapper.readTree(yaml);
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);

            YamlResult result = new YamlResult();
            result.setResult(json);
            result.setValid(true);
            return result;
        } catch (Exception e) {
            YamlResult result = new YamlResult();
            result.setValid(false);
            result.setError("转换失败: " + e.getMessage());
            return result;
        }
    }

    public YamlResult fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new BusinessException("JSON内容不能为空");
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(json);
            String yaml = yamlMapper.writeValueAsString(jsonNode);

            YamlResult result = new YamlResult();
            result.setResult(yaml);
            result.setValid(true);
            return result;
        } catch (Exception e) {
            YamlResult result = new YamlResult();
            result.setValid(false);
            result.setError("转换失败: " + e.getMessage());
            return result;
        }
    }

    public YamlResult toProperties(String yaml) {
        if (yaml == null || yaml.trim().isEmpty()) {
            throw new BusinessException("YAML内容不能为空");
        }

        try {
            Map<String, Object> data = snakeYaml.load(yaml);
            StringBuilder properties = new StringBuilder();
            flattenMap(data, "", properties);

            YamlResult result = new YamlResult();
            result.setResult(properties.toString());
            result.setValid(true);
            return result;
        } catch (Exception e) {
            YamlResult result = new YamlResult();
            result.setValid(false);
            result.setError("转换失败: " + e.getMessage());
            return result;
        }
    }

    @SuppressWarnings("unchecked")
    private void flattenMap(Map<String, Object> map, String prefix, StringBuilder result) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof Map) {
                flattenMap((Map<String, Object>) value, key, result);
            } else if (value instanceof Iterable) {
                int index = 0;
                for (Object item : (Iterable<?>) value) {
                    if (item instanceof Map) {
                        flattenMap((Map<String, Object>) item, key + "[" + index + "]", result);
                    } else {
                        result.append(key).append("[").append(index).append("]=").append(item).append("\n");
                    }
                    index++;
                }
            } else {
                result.append(key).append("=").append(value != null ? value.toString() : "").append("\n");
            }
        }
    }

    public static class YamlResult {
        private String result;
        private Boolean valid;
        private String message;
        private String error;
        private Integer originalSize;
        private Integer resultSize;
        private String compressionRatio;

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
        public String getCompressionRatio() { return compressionRatio; }
        public void setCompressionRatio(String compressionRatio) { this.compressionRatio = compressionRatio; }
    }
}
