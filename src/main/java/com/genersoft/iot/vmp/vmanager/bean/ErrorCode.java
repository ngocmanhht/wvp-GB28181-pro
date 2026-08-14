package com.genersoft.iot.vmp.vmanager.bean;

/**
 * Global error code
 */
public enum ErrorCode {
    SUCCESS(0, "success"),
    ERROR100(100, "failed"),
    ERROR400(400, "Parameter or method error"),
    ERROR404(404, "Resource not found"),
    ERROR403(403, "Operation without permission"),
    ERROR486(486, "Timeout or no response"),
    ERROR401(401, "Please log in and request again"),
    ERROR408(408, "Request timeout"),
    ERROR500(500, "System exception");

    private final int code;
    private final String msg;

    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
