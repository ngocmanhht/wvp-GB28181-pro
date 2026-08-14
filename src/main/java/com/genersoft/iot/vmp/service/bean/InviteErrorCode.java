package com.genersoft.iot.vmp.service.bean;

/**
 * Global error code
 */
public enum InviteErrorCode {
    SUCCESS(0, "success"),
    FAIL(-100, "failed"),
    ERROR_FOR_SIGNALLING_TIMEOUT(-1, "Signaling timeout"),
    ERROR_FOR_STREAM_TIMEOUT(-2, "Traffic collection timeout"),
    ERROR_FOR_RESOURCE_EXHAUSTION(-3, "resources exhausted"),
    ERROR_FOR_CATCH_DATA(-4, "Cache data exception"),
    ERROR_FOR_SIGNALLING_ERROR(-5, "Signaling error received"),
    ERROR_FOR_STREAM_PARSING_EXCEPTIONS(-6, "Stream address parsing error"),
    ERROR_FOR_SDP_PARSING_EXCEPTIONS(-7, "SDPInformation parsing failed"),
    ERROR_FOR_SSRC_UNAVAILABLE(-8, "SSRCNot available"),
    ERROR_FOR_RESET_SSRC(-9, "Failed to reset traffic collection information"),
    ERROR_FOR_SIP_SENDING_FAILED(-10, "Command sending failed"),
    ERROR_FOR_ASSIST_NOT_READY(-11, "No assist service available"),
    ERROR_FOR_PARAMETER_ERROR(-13, "Parameter exception"),
    ERROR_FOR_TCP_ACTIVE_CONNECTION_REFUSED_ERROR(-14, "TCPActive connection failed"),
    ERROR_FOR_FINISH(-20, "ended"),
    ;

    private final int code;
    private final String msg;

    InviteErrorCode(int code, String msg) {
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
