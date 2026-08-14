package com.genersoft.iot.vmp.vmanager.bean;

import lombok.Getter;
import lombok.Setter;

/**
 * @author lin
 */
@Setter
@Getter
public class AudioBroadcastResult {
    /**
     * Streaming addresses for various methods of streaming
     */
    private StreamContent streamInfo;

    /**
     * encoding format
     */
    private String codec;

    /**
     * Application name to push to zlm
     */
    private String app;

    /**
     * Stream pushed to zlmID
     */
    private String stream;

    /**
     * Playback stream address (device audio is played to the browser through ZLM), set during intercom
     */
    private StreamContent playStreamInfo;


}
