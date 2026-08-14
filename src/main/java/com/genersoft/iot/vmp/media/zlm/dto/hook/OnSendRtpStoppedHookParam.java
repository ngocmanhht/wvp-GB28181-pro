package com.genersoft.iot.vmp.media.zlm.dto.hook;

/**
 * zlm hookin the eventon_send_rtp_stoppedevent parameters
 * @author lin
 */
public class OnSendRtpStoppedHookParam extends HookParam{
    private String app;
    private String stream;


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

    @Override
    public String toString() {
        return "OnSendRtpStoppedHookParam{" +
                "app='" + app + '\'' +
                ", stream='" + stream + '\'' +
                '}';
    }
}
