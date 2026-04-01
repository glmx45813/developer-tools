package com.glmx.tools.controller;

import com.glmx.tools.common.Result;
import com.glmx.tools.service.XmlService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "XML工具")
@RestController
@RequestMapping("/api/xml")
public class XmlController {

    @Autowired
    private XmlService xmlService;

    @ApiOperation("XML格式化")
    @PostMapping("/format")
    public Result<XmlService.XmlResult> format(@RequestBody XmlRequest request) {
        try {
            int indent = request.getIndent() != null ? request.getIndent() : 2;
            XmlService.XmlResult result = xmlService.format(request.getXml(), indent);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("XML压缩")
    @PostMapping("/compress")
    public Result<XmlService.XmlResult> compress(@RequestBody XmlRequest request) {
        try {
            XmlService.XmlResult result = xmlService.compress(request.getXml());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("XML校验")
    @PostMapping("/validate")
    public Result<XmlService.XmlResult> validate(@RequestBody XmlRequest request) {
        try {
            XmlService.XmlResult result = xmlService.validate(request.getXml());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("XPath查询")
    @PostMapping("/xpath")
    public Result<XmlService.XmlResult> xpath(@RequestBody XmlXpathRequest request) {
        try {
            XmlService.XmlResult result = xmlService.xpath(request.getXml(), request.getXpath());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("XML转JSON")
    @PostMapping("/to-json")
    public Result<XmlService.XmlResult> toJson(@RequestBody XmlRequest request) {
        try {
            XmlService.XmlResult result = xmlService.toJson(request.getXml());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("XML转义")
    @PostMapping("/escape")
    public Result<XmlService.XmlResult> escape(@RequestBody XmlRequest request) {
        try {
            XmlService.XmlResult result = xmlService.escape(request.getXml());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("XML反转义")
    @PostMapping("/unescape")
    public Result<XmlService.XmlResult> unescape(@RequestBody XmlRequest request) {
        try {
            XmlService.XmlResult result = xmlService.unescape(request.getXml());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}

class XmlRequest {
    private String xml;
    private Integer indent;

    public String getXml() { return xml; }
    public void setXml(String xml) { this.xml = xml; }
    public Integer getIndent() { return indent; }
    public void setIndent(Integer indent) { this.indent = indent; }
}

class XmlXpathRequest {
    private String xml;
    private String xpath;

    public String getXml() { return xml; }
    public void setXml(String xml) { this.xml = xml; }
    public String getXpath() { return xpath; }
    public void setXpath(String xpath) { this.xpath = xpath; }
}
