package com.genersoft.iot.vmp.gb28181.bean;

import lombok.Data;

// Parse the required information from the INVITE message
@Data
public class InviteMessageInfo {
    private String requesterId;
    private String targetChannelId;
    private String sourceChannelId;
    private String sessionName;
    private String ssrc;
    private boolean tcp;
    private boolean tcpActive;
    private String callId;
    private Long startTime;
    private Long stopTime;
    private String downloadSpeed;
    private String ip;
    private int port;

}
