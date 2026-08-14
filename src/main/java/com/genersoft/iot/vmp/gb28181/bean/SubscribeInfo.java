package com.genersoft.iot.vmp.gb28181.bean;

import gov.nist.javax.sip.message.SIPResponse;
import lombok.Data;

import javax.sip.header.EventHeader;
import java.util.UUID;

@Data
public class SubscribeInfo {

    private String id;
    private int expires;
    private String eventId;
    private String eventType;
    private SipTransactionInfo transactionInfo;

    /**
     * The following are optional fields
     */
    private String sn;

    private int gpsInterval;

    /**
     * simulatedFromTag
     */
    private String simulatedFromTag;

    /**
     * simulatedToTag
     */
    private String simulatedToTag;

    /**
     * simulatedCallID
     */
    private String simulatedCallId;

    /**
     * SourceserverId
     */
    private String serverId;


    public static SubscribeInfo getInstance(SIPResponse response, String id, int expires, EventHeader eventHeader){
        SubscribeInfo subscribeInfo = new SubscribeInfo();
        subscribeInfo.id = id;
        subscribeInfo.transactionInfo = new SipTransactionInfo(response);

        subscribeInfo.expires = expires;
        subscribeInfo.eventId = eventHeader.getEventId();
        subscribeInfo.eventType = eventHeader.getEventType();
        return subscribeInfo;
    }
    public static SubscribeInfo buildSimulated(String platFormServerId, String platFormServerIp){
        SubscribeInfo subscribeInfo = new SubscribeInfo();
        subscribeInfo.setId(platFormServerId);
        subscribeInfo.setExpires(-1);
        subscribeInfo.setEventType("Catalog");
        int random = (int) Math.floor(Math.random() * 10000);
        subscribeInfo.setEventId(random + "");
        subscribeInfo.setSimulatedCallId(UUID.randomUUID().toString().replace("-", "") + "@" + platFormServerIp);
        subscribeInfo.setSimulatedFromTag(UUID.randomUUID().toString().replace("-", ""));
        subscribeInfo.setSimulatedToTag(UUID.randomUUID().toString().replace("-", ""));
        return subscribeInfo;
    }

}
