package com.genersoft.iot.vmp.gb28181.bean;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The live stream is associated with the national standard superior platform
 * @author lin
 */
@Schema(description = "The live stream is associated with the national standard superior platform")
public class GbStream extends PlatformGbStream{

    @Schema(description = "ID")
    private int gbStreamId;
    @Schema(description = "Application name")
    private String app;
    @Schema(description = "flowID")
    private String stream;
    @Schema(description = "National standardID")
    private String gbId;
    @Schema(description = "Name")
    private String name;
    @Schema(description = "streaming mediaID")
    private String mediaServerId;
    @Schema(description = "longitude")
    private double longitude;
    @Schema(description = "Latitude")
    private double latitude;
    @Schema(description = "Stream type (pull stream/Push streaming）")
    private String streamType;
    @Schema(description = "Status")
    private boolean status;

    @Schema(description = "creation time")
    public String createTime;

    @Override
    public Integer getGbStreamId() {
        return gbStreamId;
    }

    @Override
    public void setGbStreamId(Integer gbStreamId) {
        this.gbStreamId = gbStreamId;
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

    public String getGbId() {
        return gbId;
    }

    public void setGbId(String gbId) {
        this.gbId = gbId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public String getStreamType() {
        return streamType;
    }

    public void setStreamType(String streamType) {
        this.streamType = streamType;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
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
}
