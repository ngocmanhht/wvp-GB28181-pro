package com.genersoft.iot.vmp.media.abl.bean.hook;

public class ABLHookParam {
    private String mediaServerId;

    /**
     * Application name
     */
    private String app;

    /**
     * flowid
     */
    private String stream;

    /**
     * The source number of the media stream. You can close the streaming media based on this key. You can call the delMediaStream or close_streams function to close it.
     */
    private String key;

    /**
     * Media stream source network number, please refer to the attached table
     */
    private Integer networkType;

    public String getMediaServerId() {
        return mediaServerId;
    }

    public void setMediaServerId(String mediaServerId) {
        this.mediaServerId = mediaServerId;
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

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Integer getNetworkType() {
        return networkType;
    }

    public void setNetworkType(Integer networkType) {
        this.networkType = networkType;
    }
}
