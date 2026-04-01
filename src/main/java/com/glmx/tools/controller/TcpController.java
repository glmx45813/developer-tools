package com.glmx.tools.controller;

import com.glmx.tools.common.Result;
import com.glmx.tools.dto.*;
import com.glmx.tools.service.TcpService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Api(tags = "TCP连接工具")
@RestController
@RequestMapping("/api/tcp")
public class TcpController {

    @Autowired
    private TcpService tcpService;

    @ApiOperation("建立TCP连接")
    @PostMapping("/connect")
    public Result<TcpConnectionStatus> connect(Authentication authentication, @Valid @RequestBody TcpConnectRequest request) {
        String username = getUsername(authentication);
        TcpConnectionStatus status = tcpService.connect(username, request);
        return Result.success(status);
    }

    @ApiOperation("断开TCP连接")
    @PostMapping("/disconnect/{connectionId}")
    public Result<Void> disconnect(Authentication authentication, @PathVariable String connectionId) {
        String username = getUsername(authentication);
        tcpService.disconnect(username, connectionId);
        return Result.success();
    }

    @ApiOperation("发送数据")
    @PostMapping("/send")
    public Result<Void> send(Authentication authentication, @Valid @RequestBody TcpSendRequest request) {
        String username = getUsername(authentication);
        tcpService.send(username, request);
        return Result.success();
    }

    @ApiOperation("获取连接状态")
    @GetMapping("/status/{connectionId}")
    public Result<TcpConnectionStatus> getStatus(Authentication authentication, @PathVariable String connectionId) {
        String username = getUsername(authentication);
        TcpConnectionStatus status = tcpService.getStatus(username, connectionId);
        if (status == null) {
            return Result.error("连接不存在");
        }
        return Result.success(status);
    }

    @ApiOperation("获取当前用户的所有连接")
    @GetMapping("/connections")
    public Result<List<TcpConnectionStatus>> getMyConnections(Authentication authentication) {
        String username = getUsername(authentication);
        List<TcpConnectionStatus> connections = tcpService.getUserConnections(username);
        return Result.success(connections);
    }

    @ApiOperation("清除数据日志")
    @DeleteMapping("/logs/{connectionId}")
    public Result<Void> clearLogs(Authentication authentication, @PathVariable String connectionId) {
        String username = getUsername(authentication);
        tcpService.clearLogs(username, connectionId);
        return Result.success();
    }

    private String getUsername(Authentication authentication) {
        if (authentication != null && authentication.getName() != null && !authentication.getName().isEmpty()) {
            return authentication.getName();
        }
        return "anonymous";
    }
}
