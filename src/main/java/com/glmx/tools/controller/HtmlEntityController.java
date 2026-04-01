package com.glmx.tools.controller;

import com.glmx.tools.common.Result;
import com.glmx.tools.service.HtmlEntityService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api(tags = "HTML实体编解码工具")
@RestController
@RequestMapping("/api/html-entity")
public class HtmlEntityController {

    @Autowired
    private HtmlEntityService htmlEntityService;

    @ApiOperation("HTML实体编码")
    @PostMapping("/encode")
    public Result<HtmlEntityResult> encode(@RequestBody HtmlEntityRequest request) {
        try {
            String encoded;
            String mode = request.getMode() != null ? request.getMode() : "named";
            
            switch (mode.toLowerCase()) {
                case "decimal":
                    encoded = htmlEntityService.encodeDecimal(request.getText());
                    break;
                case "hex":
                    encoded = htmlEntityService.encodeHex(request.getText());
                    break;
                case "all":
                    encoded = htmlEntityService.encode(request.getText(), true);
                    break;
                default:
                    encoded = htmlEntityService.encodeNamed(request.getText());
            }
            
            HtmlEntityResult result = new HtmlEntityResult();
            result.setOriginal(request.getText());
            result.setResult(encoded);
            result.setMode(mode);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("HTML实体解码")
    @PostMapping("/decode")
    public Result<HtmlEntityResult> decode(@RequestBody HtmlEntityRequest request) {
        try {
            String decoded = htmlEntityService.decode(request.getText());
            
            HtmlEntityResult result = new HtmlEntityResult();
            result.setOriginal(request.getText());
            result.setResult(decoded);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("获取常用HTML实体")
    @GetMapping("/common-entities")
    public Result<Map<String, String>> getCommonEntities() {
        try {
            Map<String, String> entities = htmlEntityService.getCommonEntities();
            return Result.success(entities);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}

class HtmlEntityRequest {
    private String text;
    private String mode;

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
}

class HtmlEntityResult {
    private String original;
    private String result;
    private String mode;

    public String getOriginal() { return original; }
    public void setOriginal(String original) { this.original = original; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
}
