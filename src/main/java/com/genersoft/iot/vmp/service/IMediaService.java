package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.media.bean.ResultForOnPublish;
import com.genersoft.iot.vmp.media.bean.MediaServer;

/**
 * Media information business
 */
public interface IMediaService {

    /**
     * Playback authentication
     */
    boolean authenticatePlay(String app, String stream, String callId);

    ResultForOnPublish authenticatePublish(MediaServer mediaServer, String app, String stream, String params);

    boolean closeStreamOnNoneReader(String mediaServerId, String app, String stream, String schema);
}
