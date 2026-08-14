package com.genersoft.iot.vmp.gb28181.bean;

import lombok.Data;

@Data
public class TalkRtpInfo {

    /**
     * Application name, the name of the streaming application to be pushed to the device
     */
    private String app;

    /**
     * Stream id, the stream to be pushed to the deviceid
     */
    private String stream;

    /**
     * rtppushed outssrc
     */
    private String ssrc;

    /**
     * The stream pushed up by the other party's rtpid
     */
    private String receiveStreamId;

    /**
     * Whether to push local MP4 recordings, this parameter is not required.
     */
    private Integer fromMp4;

    /**
     * Type： 0(ESflow)、1(PSflow)、2(TSflow)，Default1(PSflow)；This parameter is not required
     */
    private Integer type;

    /**
     * rtp payload type，Default is 96; this parameter is not required
     */
    private Integer pt;

    /**
     * rtp esWhen packaging, whether to only package audio; this parameter is not required.
     */
    private Integer onlyAudio;

    /**
     * forwardrtp(tcpmode)If the data cannot be sent out, whether to limit the source end's traffic collection speed. This parameter is more effective in multi-speed rtp forwarding.
     */
    private Integer enableOriginReceiveLimit;

}
