package com.glmx.tools.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.glmx.tools.common.BusinessException;
import com.glmx.tools.dto.JsonRequest;
import com.glmx.tools.dto.JsonResponse;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JsonService {

    private final ObjectMapper objectMapper;

    public JsonService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public JsonResponse format(JsonRequest request) {
        JsonResponse response = new JsonResponse();
        response.setOriginalSize(request.getJson().getBytes().length);

        try {
            JsonNode jsonNode = objectMapper.readTree(request.getJson());
            
            ObjectMapper formatter = new ObjectMapper();
            formatter.enable(SerializationFeature.INDENT_OUTPUT);
            
            String formatted = formatter.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
            
            response.setResult(formatted);
            response.setValid(true);
            response.setResultSize(formatted.getBytes().length);
            response.setCompressionRatio(calculateCompressionRatio(response.getOriginalSize(), response.getResultSize()));
        } catch (JsonProcessingException e) {
            response.setValid(false);
            response.setError(e.getOriginalMessage());
            response.setErrorLine(extractErrorLine(e));
            response.setErrorColumn(extractErrorColumn(e));
        }

        return response;
    }

    public JsonResponse compress(JsonRequest request) {
        JsonResponse response = new JsonResponse();
        response.setOriginalSize(request.getJson().getBytes().length);

        try {
            JsonNode jsonNode = objectMapper.readTree(request.getJson());
            
            ObjectMapper compressor = new ObjectMapper();
            compressor.disable(SerializationFeature.INDENT_OUTPUT);
            
            String compressed = compressor.writeValueAsString(jsonNode);
            
            response.setResult(compressed);
            response.setValid(true);
            response.setResultSize(compressed.getBytes().length);
            response.setCompressionRatio(calculateCompressionRatio(response.getOriginalSize(), response.getResultSize()));
        } catch (JsonProcessingException e) {
            response.setValid(false);
            response.setError(e.getOriginalMessage());
            response.setErrorLine(extractErrorLine(e));
            response.setErrorColumn(extractErrorColumn(e));
        }

        return response;
    }

    public JsonResponse validate(JsonRequest request) {
        JsonResponse response = new JsonResponse();

        try {
            objectMapper.readTree(request.getJson());
            response.setValid(true);
            response.setResult("JSON格式有效");
        } catch (JsonProcessingException e) {
            response.setValid(false);
            response.setError(e.getOriginalMessage());
            response.setErrorLine(extractErrorLine(e));
            response.setErrorColumn(extractErrorColumn(e));
        }

        return response;
    }

    public JsonResponse escape(JsonRequest request) {
        JsonResponse response = new JsonResponse();
        response.setOriginalSize(request.getJson().getBytes().length);

        try {
            String escaped = escapeJson(request.getJson());
            response.setResult(escaped);
            response.setValid(true);
            response.setResultSize(escaped.getBytes().length);
        } catch (Exception e) {
            response.setValid(false);
            response.setError(e.getMessage());
        }

        return response;
    }

    public JsonResponse unescape(JsonRequest request) {
        JsonResponse response = new JsonResponse();
        response.setOriginalSize(request.getJson().getBytes().length);

        try {
            String unescaped = unescapeJson(request.getJson());
            response.setResult(unescaped);
            response.setValid(true);
            response.setResultSize(unescaped.getBytes().length);
        } catch (Exception e) {
            response.setValid(false);
            response.setError(e.getMessage());
        }

        return response;
    }

    private String escapeJson(String json) {
        StringBuilder sb = new StringBuilder();
        for (char c : json.toCharArray()) {
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    private String unescapeJson(String json) {
        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) {
                switch (c) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        if (i + 4 < json.length()) {
                            String hex = json.substring(i + 1, i + 5);
                            sb.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                        }
                        break;
                    default:
                        sb.append(c);
                }
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private Integer extractErrorLine(JsonProcessingException e) {
        String message = e.getMessage();
        Pattern pattern = Pattern.compile("at line (\\d+)");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    private Integer extractErrorColumn(JsonProcessingException e) {
        String message = e.getMessage();
        Pattern pattern = Pattern.compile("column (\\d+)");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    private String calculateCompressionRatio(int original, int result) {
        if (original == 0) return "0%";
        double ratio = ((double) (original - result) / original) * 100;
        return String.format("%.2f%%", ratio);
    }
}
