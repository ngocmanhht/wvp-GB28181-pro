package com.genersoft.iot.vmp.service.bean;

import com.genersoft.iot.vmp.gb28181.bean.SendRtpInfo;

/**
 * redisMessage: Requesting lower-level push flow information
 * @author lin
 */
public class RequestPushStreamMsg {


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
     * Whether to use TCP mode
     */
    private boolean tcp;

    /**
     * Port used locally
     */
    private int srcPort;

    /**
     * When sending, rtp's pt (uint8_t), defaults to96
     */
    private int pt;

    /**
     * When sending, the payload type of rtp. When it is true, the load is ps; when it is false, it ises；
     */
    private boolean ps;

    /**
     * Is it audio only?
     */
    private boolean onlyAudio;


    public static RequestPushStreamMsg getInstance(String mediaServerId, String app, String stream, String ip, int port, String ssrc,
                                boolean tcp, int srcPort, int pt, boolean ps, boolean onlyAudio) {
        RequestPushStreamMsg requestPushStreamMsg = new RequestPushStreamMsg();
        requestPushStreamMsg.setMediaServerId(mediaServerId);
        requestPushStreamMsg.setApp(app);
        requestPushStreamMsg.setStream(stream);
        requestPushStreamMsg.setIp(ip);
        requestPushStreamMsg.setPort(port);
        requestPushStreamMsg.setSsrc(ssrc);
        requestPushStreamMsg.setTcp(tcp);
        requestPushStreamMsg.setSrcPort(srcPort);
        requestPushStreamMsg.setPt(pt);
        requestPushStreamMsg.setPs(ps);
        requestPushStreamMsg.setOnlyAudio(onlyAudio);
        return requestPushStreamMsg;
    }

    public static RequestPushStreamMsg getInstance(SendRtpInfo sendRtpItem) {
        RequestPushStreamMsg requestPushStreamMsg = new RequestPushStreamMsg();
        requestPushStreamMsg.setMediaServerId(sendRtpItem.getMediaServerId());
        requestPushStreamMsg.setApp(sendRtpItem.getApp());
        requestPushStreamMsg.setStream(sendRtpItem.getStream());
        requestPushStreamMsg.setIp(sendRtpItem.getIp());
        requestPushStreamMsg.setPort(sendRtpItem.getPort());
        requestPushStreamMsg.setSsrc(sendRtpItem.getSsrc());
        requestPushStreamMsg.setTcp(sendRtpItem.isTcp());
        requestPushStreamMsg.setSrcPort(sendRtpItem.getLocalPort());
        requestPushStreamMsg.setPt(sendRtpItem.getPt());
        requestPushStreamMsg.setPs(sendRtpItem.isUsePs());
        requestPushStreamMsg.setOnlyAudio(sendRtpItem.isOnlyAudio());
        return requestPushStreamMsg;
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

    public boolean isTcp() {
        return tcp;
    }

    public void setTcp(boolean tcp) {
        this.tcp = tcp;
    }

    public int getSrcPort() {
        return srcPort;
    }

    public void setSrcPort(int srcPort) {
        this.srcPort = srcPort;
    }

    public int getPt() {
        return pt;
    }

    public void setPt(int pt) {
        this.pt = pt;
    }

    public boolean isPs() {
        return ps;
    }

    public void setPs(boolean ps) {
        this.ps = ps;
    }

    public boolean isOnlyAudio() {
        return onlyAudio;
    }

    public void setOnlyAudio(boolean onlyAudio) {
        this.onlyAudio = onlyAudio;
    }
}
