package com.glmx.tools.service;

import com.glmx.tools.common.BusinessException;
import com.glmx.tools.dto.UrlEncodeResponse;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class UrlEncodeService {

    public UrlEncodeResponse encode(String text, String charset) {
        validateCharset(charset);
        
        UrlEncodeResponse response = new UrlEncodeResponse();
        response.setOriginal(text);
        response.setCharset(charset);
        
        try {
            response.setEncoded(URLEncoder.encode(text, charset));
        } catch (UnsupportedEncodingException e) {
            throw new BusinessException("不支持的编码格式: " + charset);
        }
        
        return response;
    }

    public UrlEncodeResponse decode(String text, String charset) {
        validateCharset(charset);
        
        UrlEncodeResponse response = new UrlEncodeResponse();
        response.setOriginal(text);
        response.setCharset(charset);
        
        try {
            response.setDecoded(URLDecoder.decode(text, charset));
        } catch (UnsupportedEncodingException e) {
            throw new BusinessException("不支持的编码格式: " + charset);
        }
        
        return response;
    }

    public UrlEncodeResponse encodeDecode(String text, String charset) {
        validateCharset(charset);
        
        UrlEncodeResponse response = new UrlEncodeResponse();
        response.setOriginal(text);
        response.setCharset(charset);
        
        try {
            String encoded = URLEncoder.encode(text, charset);
            response.setEncoded(encoded);
            response.setDecoded(URLDecoder.decode(encoded, charset));
        } catch (UnsupportedEncodingException e) {
            throw new BusinessException("不支持的编码格式: " + charset);
        }
        
        return response;
    }

    public List<UrlEncodeResponse> batchEncode(List<String> texts, String charset) {
        validateCharset(charset);
        List<UrlEncodeResponse> results = new ArrayList<>();
        for (String text : texts) {
            results.add(encode(text, charset));
        }
        return results;
    }

    public List<UrlEncodeResponse> batchDecode(List<String> texts, String charset) {
        validateCharset(charset);
        List<UrlEncodeResponse> results = new ArrayList<>();
        for (String text : texts) {
            results.add(decode(text, charset));
        }
        return results;
    }

    private void validateCharset(String charset) {
        if (!Charset.isSupported(charset)) {
            throw new BusinessException("不支持的编码格式: " + charset);
        }
    }
}
