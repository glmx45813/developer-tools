package com.glmx.tools.common;

import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "方法不允许"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    PARAM_ERROR(1001, "参数校验失败"),
    FILE_TOO_LARGE(1002, "文件大小超过限制"),
    INVALID_FILE_TYPE(1003, "不支持的文件类型"),
    ENCODE_ERROR(1004, "编码转换失败"),
    DECODE_ERROR(1005, "解码转换失败"),
    ENCRYPTION_ERROR(1006, "加密失败"),
    QRCODE_GENERATE_ERROR(1007, "二维码生成失败"),
    QRCODE_PARSE_ERROR(1008, "二维码解析失败"),
    JSON_PARSE_ERROR(1009, "JSON解析失败"),
    REGEX_ERROR(1010, "正则表达式错误"),
    MQTT_CONNECT_ERROR(1011, "MQTT连接失败"),
    MQTT_PUBLISH_ERROR(1012, "MQTT消息发布失败"),
    TCP_CONNECT_ERROR(1013, "TCP连接失败"),
    TCP_SEND_ERROR(1014, "TCP数据发送失败");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
