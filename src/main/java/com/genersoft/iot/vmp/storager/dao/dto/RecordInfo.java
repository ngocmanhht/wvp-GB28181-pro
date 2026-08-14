package com.genersoft.iot.vmp.storager.dao.dto;

/**
 * Video recording
 */
public class RecordInfo {

    /**
     * ID
     */
    private int id;

    /**
     * Application name
     */
    private String app;

    /**
     * flowID
     */
    private String stream;

    /**
     * Corresponding zlm streaming mediaID
     */
    private String mediaServerId;

    /**
     * creation time
     */
    private String createTime;

    /**
     * Type corresponds to zlm originType
     * unknown = 0,
     * rtmp_push=1,
     * rtsp_push=2,
     * rtp_push=3,
     * pull=4,
     * ffmpeg_pull=5,
     * mp4_vod=6,
     * device_chn=7,
     * rtc_push=8
     */
    private int type;

    /**
     * Equipment for national standard video recordingID
     */
    private String deviceId;

    /**
     * Channel during national standard recordingID
     */
    private String channelId;

    /**
     * The name when streaming proxy recording
     */
    private String name;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getMediaServerId() {
        return mediaServerId;
    }

    public void setMediaServerId(String mediaServerId) {
        this.mediaServerId = mediaServerId;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
