package com.genersoft.iot.vmp.media.abl.bean.hook;

import com.genersoft.iot.vmp.media.abl.bean.ABLUrls;
import lombok.Getter;
import lombok.Setter;

/**
 * stream of incoming events
 */
@Getter
@Setter
public class OnStreamArriveABLHookParam extends ABLHookParam{



    /**
     * Push authenticationId
     */
    private String callId;

    /**
     * Status
     */
    private Boolean status;


    /**
     *
     */
    private Boolean enableHls;


    /**
     *
     */
    private Boolean transcodingStatus;


    /**
     *
     */
    private String sourceURL;


    /**
     *
     */
    private Integer readerCount;


    /**
     *
     */
    private Integer noneReaderDuration;


    /**
     *
     */
    private String videoCodec;


    /**
     *
     */
    private Integer videoFrameSpeed;


    /**
     *
     */
    private Integer width;


    /**
     *
     */
    private Integer height;


    /**
     *
     */
    private Integer videoBitrate;


    /**
     *
     */
    private String audioCodec;


    /**
     *
     */
    private Integer audioChannels;


    /**
     *
     */
    private Integer audioSampleRate;


    /**
     *
     */
    private Integer audioBitrate;


    private ABLUrls url;
}
