package com.genersoft.iot.vmp.gb28181.bean;


import com.genersoft.iot.vmp.gb28181.controller.bean.AudioBroadcastEvent;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.Data;

/**
 * Caching the status of voice broadcasts
 * @author lin
 */
@Data
public class AudioBroadcastCatch {


    public AudioBroadcastCatch(
            String deviceId,
            Integer channelId,
            MediaServer mediaServerItem,
            String app,
            String stream,
            AudioBroadcastEvent event,
            AudioBroadcastCatchStatus status,
            boolean isFromPlatform
    ) {
        this.deviceId = deviceId;
        this.channelId = channelId;
        this.status = status;
        this.event = event;
        this.isFromPlatform = isFromPlatform;
        this.app = app;
        this.stream = stream;
        this.mediaServerItem = mediaServerItem;
    }

    public AudioBroadcastCatch() {
    }

    /**
     * Device number
     */
    private String deviceId;

    /**
     * Channel number
     */
    private Integer channelId;

    /**
     * Streaming media information
     */
    private MediaServer mediaServerItem;

    /**
     * associated streamsAPP
     */
    private String app;

    /**
     * associated streamsSTREAM
     */
    private String stream;

    /**
     *  Whether it is a cascade voice call
     */
    private boolean isFromPlatform;

    /**
     * Voice broadcast status
     */
    private AudioBroadcastCatchStatus status;

    /**
     * request information
     */
    private SipTransactionInfo sipTransactionInfo;

    /**
     * Request result callback
     */
    private AudioBroadcastEvent event;


    public void setSipTransactionInfoByRequest(SIPResponse sipResponse) {
        this.sipTransactionInfo = new SipTransactionInfo(sipResponse);
    }
}
