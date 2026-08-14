package com.genersoft.iot.vmp.service.bean;

/**
 * When redis replies to the upper-level platform of push results
 * @author lin
 */
public class MessageForPushChannelResponse {
    /**
     * Wrong horse
     * 0 Success 1 Failure
     */
    private int code;
    /**
     * Error content
     */
    private String msg;

    /**
     * Streaming application name
     */
    private String app;

    /**
     * flowId
     */
    private String stream;



    public static MessageForPushChannelResponse getInstance(int code, String msg, String app, String stream){
        MessageForPushChannelResponse messageForPushChannel = new MessageForPushChannelResponse();
        messageForPushChannel.setCode(code);
        messageForPushChannel.setMsg(msg);
        messageForPushChannel.setApp(app);
        messageForPushChannel.setStream(stream);
        return messageForPushChannel;
    }


    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
