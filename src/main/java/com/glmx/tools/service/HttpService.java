package com.glmx.tools.service;

import com.glmx.tools.common.BusinessException;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class HttpService {

    private final OkHttpClient client;

    public HttpService() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .build();
    }

    public HttpResponse sendRequest(HttpRequest request) {
        if (request.getUrl() == null || request.getUrl().isEmpty()) {
            throw new BusinessException("URL不能为空");
        }

        try {
            Request.Builder requestBuilder = new Request.Builder().url(request.getUrl());

            if (request.getHeaders() != null) {
                for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                    requestBuilder.addHeader(header.getKey(), header.getValue());
                }
            }

            String method = request.getMethod() != null ? request.getMethod().toUpperCase() : "GET";

            switch (method) {
                case "GET":
                    requestBuilder.get();
                    break;
                case "POST":
                    requestBuilder.post(createRequestBody(request));
                    break;
                case "PUT":
                    requestBuilder.put(createRequestBody(request));
                    break;
                case "PATCH":
                    requestBuilder.patch(createRequestBody(request));
                    break;
                case "DELETE":
                    if (request.getBody() != null && !request.getBody().isEmpty()) {
                        requestBuilder.delete(createRequestBody(request));
                    } else {
                        requestBuilder.delete();
                    }
                    break;
                case "HEAD":
                    requestBuilder.head();
                    break;
                case "OPTIONS":
                    requestBuilder.method("OPTIONS", null);
                    break;
                default:
                    requestBuilder.get();
            }

            long startTime = System.currentTimeMillis();
            Response response = client.newCall(requestBuilder.build()).execute();
            long endTime = System.currentTimeMillis();

            HttpResponse result = new HttpResponse();
            result.setStatusCode(response.code());
            result.setStatusMessage(response.message());
            result.setResponseTime(endTime - startTime);

            Map<String, String> responseHeaders = new HashMap<>();
            for (String name : response.headers().names()) {
                responseHeaders.put(name, String.join(", ", response.headers().values(name)));
            }
            result.setHeaders(responseHeaders);

            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                String contentType = responseBody.contentType() != null ?
                    responseBody.contentType().toString() : "";
                result.setContentType(contentType);

                String body = responseBody.string();
                result.setBody(body);
                result.setSize((long) body.length());
            }

            result.setSuccess(response.isSuccessful());
            return result;

        } catch (IOException e) {
            HttpResponse result = new HttpResponse();
            result.setSuccess(false);
            result.setError("请求失败: " + e.getMessage());
            return result;
        }
    }

    private RequestBody createRequestBody(HttpRequest request) {
        String body = request.getBody() != null ? request.getBody() : "";
        String contentType = request.getContentType() != null ? request.getContentType() : "application/json";

        MediaType mediaType = MediaType.parse(contentType);
        return RequestBody.create(body, mediaType);
    }

    public static class HttpRequest {
        private String url;
        private String method;
        private Map<String, String> headers;
        private String body;
        private String contentType;

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        public Map<String, String> getHeaders() { return headers; }
        public void setHeaders(Map<String, String> headers) { this.headers = headers; }
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
    }

    public static class HttpResponse {
        private Boolean success;
        private Integer statusCode;
        private String statusMessage;
        private Long responseTime;
        private String contentType;
        private Long size;
        private Map<String, String> headers;
        private String body;
        private String error;

        public Boolean getSuccess() { return success; }
        public void setSuccess(Boolean success) { this.success = success; }
        public Integer getStatusCode() { return statusCode; }
        public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }
        public String getStatusMessage() { return statusMessage; }
        public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }
        public Long getResponseTime() { return responseTime; }
        public void setResponseTime(Long responseTime) { this.responseTime = responseTime; }
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        public Long getSize() { return size; }
        public void setSize(Long size) { this.size = size; }
        public Map<String, String> getHeaders() { return headers; }
        public void setHeaders(Map<String, String> headers) { this.headers = headers; }
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}
