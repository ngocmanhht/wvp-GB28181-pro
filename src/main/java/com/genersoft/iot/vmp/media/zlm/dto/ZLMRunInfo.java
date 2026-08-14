package com.genersoft.iot.vmp.media.zlm.dto;

/**
 * Record some parameters during zlm operation
 */
public class ZLMRunInfo {

    /**
     * zlmCurrent number of streams
     */
    private int mediaCount;

    /**
     * online status
     */
    private boolean online;

    public int getMediaCount() {
        return mediaCount;
    }

    public void setMediaCount(int mediaCount) {
        this.mediaCount = mediaCount;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }
}
