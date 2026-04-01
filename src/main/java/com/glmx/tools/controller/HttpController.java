package com.glmx.tools.controller;

import com.glmx.tools.common.Result;
import com.glmx.tools.service.HttpService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api(tags = "HTTP请求测试工具")
@RestController
@RequestMapping("/api/http")
public class HttpController {

    @Autowired
    private HttpService httpService;

    @ApiOperation("发送HTTP请求")
    @PostMapping("/request")
    public Result<HttpService.HttpResponse> sendRequest(@RequestBody HttpService.HttpRequest request) {
        try {
            HttpService.HttpResponse response = httpService.sendRequest(request);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("GET请求")
    @GetMapping("/get")
    public Result<HttpService.HttpResponse> get(
            @RequestParam String url,
            @RequestParam(required = false) Map<String, String> headers) {
        try {
            HttpService.HttpRequest request = new HttpService.HttpRequest();
            request.setUrl(url);
            request.setMethod("GET");
            request.setHeaders(headers);

            HttpService.HttpResponse response = httpService.sendRequest(request);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("POST请求")
    @PostMapping("/post")
    public Result<HttpService.HttpResponse> post(@RequestBody HttpPostRequest request) {
        try {
            HttpService.HttpRequest httpRequest = new HttpService.HttpRequest();
            httpRequest.setUrl(request.getUrl());
            httpRequest.setMethod("POST");
            httpRequest.setHeaders(request.getHeaders());
            httpRequest.setBody(request.getBody());
            httpRequest.setContentType(request.getContentType() != null ? request.getContentType() : "application/json");

            HttpService.HttpResponse response = httpService.sendRequest(httpRequest);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}

class HttpPostRequest {
    private String url;
    private Map<String, String> headers;
    private String body;
    private String contentType;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
}
