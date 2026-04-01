package com.glmx.tools.controller;

import com.glmx.tools.common.Result;
import com.glmx.tools.dto.*;
import com.glmx.tools.service.MqttService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Api(tags = "MQTT连接工具")
@RestController
@RequestMapping("/api/mqtt")
public class MqttController {

    @Autowired
    private MqttService mqttService;

    @ApiOperation("连接MQTT服务器")
    @PostMapping("/connect")
    public Result<MqttConnectionStatus> connect(Authentication authentication, @Valid @RequestBody MqttConnectRequest request) {
        String username = getUsername(authentication);
        MqttConnectionStatus status = mqttService.connect(username, request);
        return Result.success(status);
    }

    @ApiOperation("断开MQTT连接")
    @PostMapping("/disconnect/{connectionId}")
    public Result<Void> disconnect(Authentication authentication, @PathVariable String connectionId) {
        String username = getUsername(authentication);
        mqttService.disconnect(username, connectionId);
        return Result.success();
    }

    @ApiOperation("发布消息")
    @PostMapping("/publish")
    public Result<Void> publish(Authentication authentication, @Valid @RequestBody MqttPublishRequest request) {
        String username = getUsername(authentication);
        mqttService.publish(username, request);
        return Result.success();
    }

    @ApiOperation("订阅主题(支持单主题或批量订阅)")
    @PostMapping("/subscribe")
    public Result<Void> subscribe(Authentication authentication, @Valid @RequestBody MqttSubscribeRequest request) {
        String username = getUsername(authentication);
        mqttService.subscribe(username, request);
        return Result.success();
    }

    @ApiOperation("取消订阅单个主题")
    @PostMapping("/unsubscribe")
    public Result<Void> unsubscribe(Authentication authentication, @RequestParam String connectionId, @RequestParam String topic) {
        String username = getUsername(authentication);
        mqttService.unsubscribe(username, connectionId, topic);
        return Result.success();
    }

    @ApiOperation("批量取消订阅")
    @PostMapping("/unsubscribe/batch")
    public Result<Void> unsubscribeBatch(Authentication authentication, @RequestParam String connectionId, @RequestBody List<String> topics) {
        String username = getUsername(authentication);
        mqttService.unsubscribeMultiple(username, connectionId, topics);
        return Result.success();
    }

    @ApiOperation("获取连接状态")
    @GetMapping("/status/{connectionId}")
    public Result<MqttConnectionStatus> getStatus(Authentication authentication, @PathVariable String connectionId) {
        String username = getUsername(authentication);
        MqttConnectionStatus status = mqttService.getStatus(username, connectionId);
        if (status == null) {
            return Result.error("连接不存在");
        }
        return Result.success(status);
    }

    @ApiOperation("获取消息日志")
    @GetMapping("/logs/{connectionId}")
    public Result<List<MqttMessage>> getMessageLogs(Authentication authentication, @PathVariable String connectionId) {
        String username = getUsername(authentication);
        List<MqttMessage> logs = mqttService.getMessageLogs(username, connectionId);
        return Result.success(logs);
    }

    @ApiOperation("清除消息日志")
    @DeleteMapping("/logs/{connectionId}")
    public Result<Void> clearMessageLogs(Authentication authentication, @PathVariable String connectionId) {
        String username = getUsername(authentication);
        mqttService.clearMessageLogs(username, connectionId);
        return Result.success();
    }

    @ApiOperation("获取当前用户的所有连接")
    @GetMapping("/connections")
    public Result<List<MqttConnectionStatus>> getMyConnections(Authentication authentication) {
        String username = getUsername(authentication);
        List<MqttConnectionStatus> connections = mqttService.getUserConnections(username);
        return Result.success(connections);
    }

    private String getUsername(Authentication authentication) {
        if (authentication != null && authentication.getName() != null && !authentication.getName().isEmpty()) {
            return authentication.getName();
        }
        return "anonymous";
    }
}
