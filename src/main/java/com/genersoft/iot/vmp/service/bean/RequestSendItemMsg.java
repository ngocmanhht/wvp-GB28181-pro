package com.genersoft.iot.vmp.service.bean;

/**
 * redisMessage: Requesting subordinates to reply to push information
 * @author lin
 */
public class RequestSendItemMsg {

    /**
     * Lower level servicesID
     */
    private String serverId;

    /**
     * Lower level servicesID
     */
    private String mediaServerId;

    /**
     * flowID
     */
    private String app;

    /**
     * Application name
     */
    private String stream;

    /**
     * targetIP
     */
    private String ip;

    /**
     * target port
     */
    private int port;

    /**
     * ssrc
     */
    private String ssrc;

    /**
     * Platform national standard number
     */
    private String platformId;

    /**
     * Platform name
     */
    private String platformName;

    /**
     * channelID
     */
    private String channelId;


    /**
     * Whether to useTCP
     */
    private Boolean isTcp;


    /**
     * Whether to useTCP
     */
    private Boolean rtcp;




    public static RequestSendItemMsg getInstance(String serverId, String mediaServerId, String app, String stream, String ip, int port,
                                                          String ssrc, String platformId, String channelId, Boolean isTcp, Boolean rtcp, String platformName) {
        RequestSendItemMsg requestSendItemMsg = new RequestSendItemMsg();
        requestSendItemMsg.setServerId(serverId);
        requestSendItemMsg.setMediaServerId(mediaServerId);
        requestSendItemMsg.setApp(app);
        requestSendItemMsg.setStream(stream);
        requestSendItemMsg.setIp(ip);
        requestSendItemMsg.setPort(port);
        requestSendItemMsg.setSsrc(ssrc);
        requestSendItemMsg.setPlatformId(platformId);
        requestSendItemMsg.setPlatformName(platformName);
        requestSendItemMsg.setChannelId(channelId);
        requestSendItemMsg.setTcp(isTcp);
        requestSendItemMsg.setRtcp(rtcp);

        return  requestSendItemMsg;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getMediaServerId() {
        return mediaServerId;
    }

    public void setMediaServerId(String mediaServerId) {
        this.mediaServerId = mediaServerId;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getSsrc() {
        return ssrc;
    }

    public void setSsrc(String ssrc) {
        this.ssrc = ssrc;
    }

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public Boolean getTcp() {
        return isTcp;
    }

    public void setTcp(Boolean tcp) {
        isTcp = tcp;
    }

    public Boolean getRtcp() {
        return rtcp;
    }

    public void setRtcp(Boolean rtcp) {
        this.rtcp = rtcp;
    }
}
