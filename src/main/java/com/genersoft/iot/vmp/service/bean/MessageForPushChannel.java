package com.genersoft.iot.vmp.service.bean;

import lombok.Data;

/**
 * Be the superior platform
 * @author lin
 */

@Data
public class MessageForPushChannel {
    /**
     * Message type
     * 0 Stream Logout 1 Stream Register
     */
    private int type;

    /**
     * Streaming application name
     */
    private String app;

    /**
     * flowId
     */
    private String stream;

    /**
     * National standardID
     */
    private String gbId;

    /**
     * Requested platform national standard number
     */
    private String platFormId;

    /**
     * The requested platform is incrementedID
     */
    private int platFormIndex;

    /**
     * Request platform name
     */
    private String platFormName;

    /**
     * WVPserviceID
     */
    private String serverId;

    /**
     * Target streaming nodeID
     */
    private String mediaServerId;



    public static MessageForPushChannel getInstance(int type, String app, String stream, String gbId,
                                                    String platFormId, String platFormName, String serverId,
                                                    String mediaServerId){
        MessageForPushChannel messageForPushChannel = new MessageForPushChannel();
        messageForPushChannel.setType(type);
        messageForPushChannel.setGbId(gbId);
        messageForPushChannel.setApp(app);
        messageForPushChannel.setStream(stream);
        messageForPushChannel.setServerId(serverId);
        messageForPushChannel.setMediaServerId(mediaServerId);
        messageForPushChannel.setPlatFormId(platFormId);
        messageForPushChannel.setPlatFormName(platFormName);
        return messageForPushChannel;
    }
}
