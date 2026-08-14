package com.genersoft.iot.vmp.service.bean;

import com.genersoft.iot.vmp.media.bean.MediaServer;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RTPServerParam {

    /**
     * Streaming media used
     */
    private MediaServer mediaServer;
    private String app;
    private String streamId;
    /**
     * Whether to pass ssrc to zlm for verification
     */
    private boolean ssrcCheck;
    /**
     * Used when starting rtpServerssrc
     */
    private Long ssrc;
    private Integer port;
    private boolean onlyAuto;
    private boolean disableAudio;
    private boolean reUsePort;

    /**
     * tcpMode, when 0 is to disable tcp monitoring, when 1 is to enable tcp monitoring, and when 2 is tcp active connection mode
     */
    private Integer tcpMode;

    public RTPServerParam(MediaServer mediaServer, String app, String streamId, Long ssrc, Integer port,
                          boolean onlyAuto, boolean disableAudio, boolean reUsePort, Integer tcpMode) {
        this.mediaServer = mediaServer;
        this.app = app;
        this.streamId = streamId;
        this.ssrc = ssrc;
        this.port = port;
        this.onlyAuto = onlyAuto;
        this.disableAudio = disableAudio;
        this.reUsePort = reUsePort;
        this.tcpMode = tcpMode;
    }
}
