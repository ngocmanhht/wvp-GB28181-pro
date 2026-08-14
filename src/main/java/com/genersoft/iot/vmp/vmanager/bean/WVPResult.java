package com.genersoft.iot.vmp.vmanager.bean;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Schema(description = "Return results uniformly")
public class WVPResult<T> implements Cloneable{

    public WVPResult() {
    }

    public WVPResult(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }


    @Schema(description = "Error code, 0 means success")
    private int code;
    @Schema(description = "Description, when an error occurs, describe the cause of the error.")
    private String msg;
    @Schema(description = "data")
    private T data;


    public static <T> WVPResult<T> success(T t, String msg) {
        return new WVPResult<>(ErrorCode.SUCCESS.getCode(), msg, t);
    }

    public static <T> WVPResult<T> success() {
        return new WVPResult<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), null);
    }

    public static <T> WVPResult<T> success(T t) {
        return success(t, ErrorCode.SUCCESS.getMsg());
    }

    public static <T> WVPResult<T> fail(int code, String msg) {
        return new WVPResult<>(code, msg, null);
    }

    public static <T> WVPResult<T> fail(ErrorCode errorCode) {
        return fail(errorCode.getCode(), errorCode.getMsg());
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
