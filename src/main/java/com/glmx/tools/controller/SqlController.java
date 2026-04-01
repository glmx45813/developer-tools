package com.glmx.tools.controller;

import com.glmx.tools.common.Result;
import com.glmx.tools.service.SqlService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "SQL格式化工具")
@RestController
@RequestMapping("/api/sql")
public class SqlController {

    @Autowired
    private SqlService sqlService;

    @ApiOperation("SQL格式化")
    @PostMapping("/format")
    public Result<SqlService.SqlResult> format(@RequestBody SqlRequest request) {
        try {
            String dialect = request.getDialect() != null ? request.getDialect() : "mysql";
            int indentSize = request.getIndentSize() != null ? request.getIndentSize() : 2;
            SqlService.SqlResult result = sqlService.format(request.getSql(), dialect, indentSize);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("SQL压缩")
    @PostMapping("/compress")
    public Result<SqlService.SqlResult> compress(@RequestBody SqlRequest request) {
        try {
            SqlService.SqlResult result = sqlService.compress(request.getSql());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("SQL校验")
    @PostMapping("/validate")
    public Result<SqlService.SqlResult> validate(@RequestBody SqlRequest request) {
        try {
            SqlService.SqlResult result = sqlService.validate(request.getSql());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("SQL高亮")
    @PostMapping("/highlight")
    public Result<SqlService.SqlResult> highlight(@RequestBody SqlRequest request) {
        try {
            SqlService.SqlResult result = sqlService.highlight(request.getSql());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}

class SqlRequest {
    private String sql;
    private String dialect;
    private Integer indentSize;

    public String getSql() { return sql; }
    public void setSql(String sql) { this.sql = sql; }
    public String getDialect() { return dialect; }
    public void setDialect(String dialect) { this.dialect = dialect; }
    public Integer getIndentSize() { return indentSize; }
    public void setIndentSize(Integer indentSize) { this.indentSize = indentSize; }
}
