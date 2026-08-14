package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.common.InviteSessionType;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.Data;

@Data
public class SsrcTransaction {

    /**
     * Device number
     */
    private String deviceId;

    /**
     * The number of the upper level platform
     */
    private String platformId;

    /**
     * channel databaseID
     */
    private Integer channelId;

    /**
     * ConversationalCALL ID
     */
    private String callId;

    /**
     * Associated streaming application name
     */
    private String app;

    /**
     * associated streamsID
     */
    private String stream;

    /**
     * Streaming media used
     */
    private String mediaServerId;

    /**
     * usedSSRC
     */
    private String ssrc;

    /**
     * transaction information
     */
    private SipTransactionInfo sipTransactionInfo;

    /**
     * Type
     */
    private InviteSessionType type;

    public static SsrcTransaction buildForDevice(String deviceId, Integer channelId, String callId, String app, String stream,
                                                 String ssrc, String mediaServerId, SIPResponse response, InviteSessionType type) {
        SsrcTransaction ssrcTransaction = new SsrcTransaction();
        ssrcTransaction.setDeviceId(deviceId);
        ssrcTransaction.setChannelId(channelId);
        ssrcTransaction.setCallId(callId);
        ssrcTransaction.setApp(app);
        ssrcTransaction.setStream(stream);
        ssrcTransaction.setMediaServerId(mediaServerId);
        ssrcTransaction.setSsrc(ssrc);
        ssrcTransaction.setSipTransactionInfo(new SipTransactionInfo(response));
        ssrcTransaction.setType(type);
        return ssrcTransaction;
    }
    public static SsrcTransaction buildForPlatform(String platformId, Integer channelId, String callId, String app,String stream,
                                                 String ssrc, String mediaServerId, SIPResponse response, InviteSessionType type) {
        SsrcTransaction ssrcTransaction = new SsrcTransaction();
        ssrcTransaction.setPlatformId(platformId);
        ssrcTransaction.setChannelId(channelId);
        ssrcTransaction.setCallId(callId);
        ssrcTransaction.setStream(stream);
        ssrcTransaction.setApp(app);
        ssrcTransaction.setMediaServerId(mediaServerId);
        ssrcTransaction.setSsrc(ssrc);
        ssrcTransaction.setSipTransactionInfo(new SipTransactionInfo(response));
        ssrcTransaction.setType(type);
        return ssrcTransaction;
    }

    public SsrcTransaction() {
    }


}
