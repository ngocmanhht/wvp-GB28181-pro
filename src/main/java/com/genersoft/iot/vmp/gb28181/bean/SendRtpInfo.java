package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.common.enums.MediaStreamUtil;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.service.bean.RequestPushStreamMsg;
import lombok.Data;

@Data
public class SendRtpInfo {

    /**
     * Push streamingip
     */
    private String ip;

    /**
     * Push port
     */
    private int port;

    /**
     * Push logo
     */
    private String ssrc;

    /**
     * Target platform or device number
     */
    private String targetId;

    /**
     * The name of the target platform or device
     */
    private String targetName;

    /**
     * Whether it is sent to the superior platform
     */
    private boolean sendToPlatform;

    /**
     * Live streaming application name
     */
    private String app;

   /**
     * channelid
     */
    private Integer channelId;

    /**
     * Push status
     * 0 Wait for the device to push the stream
     * 1 Waiting for reply from superior platformack
     * 2 Pushing
     */
    private int status = 0;


    /**
     * device pushstreamId
     */
    private String stream;

    /**
     * Is ittcp
     */
    private boolean tcp;

    /**
     * Whether it is tcp active mode
     */
    private boolean tcpActive;

    /**
     * Used for self-pushIP
     */
    private String localIp;

    /**
     * The port used to push the stream yourself
     */
    private int localPort;

    /**
     * Streaming media used
     */
    private String mediaServerId;

    /**
     * of services usedID
     */
    private String serverId;

    /**
     *  invite of callId
     */
    private String callId;

    /**
     *  invite of fromTag
     */
    private String fromTag;

    /**
     *  invite of toTag
     */
    private String toTag;

    /**
     * When sending, rtp's pt (uint8_t), defaults to96
     */
    private int pt = 96;

    /**
     * When sending, the payload type of rtp. When it is true, the load is ps; when it is false, it ises；
     */
    private boolean usePs = true;

    /**
     * Valid when usePs is false. When it is 1, audio is sent; when it is 0, video is sent; when not transmitted, the default is0
     */
    private boolean onlyAudio = false;

    /**
     * Whether to enable rtcp keepalive
     */
    private boolean rtcp = false;


    /**
     * Play type
     */
    private InviteStreamType playType;

    /**
     * Send out flow and receive flow at the same time
     */
    private String receiveStream;

    /**
     * Superior on-demand type
     */
    private String sessionName;

    public static SendRtpInfo getInstance(RequestPushStreamMsg requestPushStreamMsg) {
        SendRtpInfo sendRtpItem = new SendRtpInfo();
        sendRtpItem.setMediaServerId(requestPushStreamMsg.getMediaServerId());
        sendRtpItem.setApp(requestPushStreamMsg.getApp());
        sendRtpItem.setStream(requestPushStreamMsg.getStream());
        sendRtpItem.setIp(requestPushStreamMsg.getIp());
        sendRtpItem.setPort(requestPushStreamMsg.getPort());
        sendRtpItem.setSsrc(requestPushStreamMsg.getSsrc());
        sendRtpItem.setTcp(requestPushStreamMsg.isTcp());
        sendRtpItem.setLocalPort(requestPushStreamMsg.getSrcPort());
        sendRtpItem.setPt(requestPushStreamMsg.getPt());
        sendRtpItem.setUsePs(requestPushStreamMsg.isPs());
        sendRtpItem.setOnlyAudio(requestPushStreamMsg.isOnlyAudio());
        return sendRtpItem;

    }

    public static SendRtpInfo getInstance(String app, String stream, String ssrc, String dstIp, Integer dstPort, boolean tcp, int sendLocalPort, Integer pt) {
        SendRtpInfo sendRtpItem = new SendRtpInfo();
        sendRtpItem.setApp(app);
        sendRtpItem.setStream(stream);
        sendRtpItem.setSsrc(ssrc);
        sendRtpItem.setTcp(tcp);
        sendRtpItem.setLocalPort(sendLocalPort);
        sendRtpItem.setIp(dstIp);
        sendRtpItem.setPort(dstPort);
        if (pt != null) {
            sendRtpItem.setPt(pt);
        }

        return sendRtpItem;
    }

    public static SendRtpInfo getInstance(Integer localPort, MediaServer mediaServer, String ip, Integer port, String ssrc,
                                          String deviceId, String platformId, Integer channelId, Boolean isTcp, Boolean rtcp,
                                          String serverId) {
        if (localPort == 0) {
            return null;
        }
        SendRtpInfo sendRtpItem = new SendRtpInfo();
        sendRtpItem.setIp(ip);
        if(port != null) {
            sendRtpItem.setPort(port);
        }

        sendRtpItem.setSsrc(ssrc);
        if (deviceId != null) {
            sendRtpItem.setTargetId(deviceId);
            sendRtpItem.setSendToPlatform(false);
        }else {
            sendRtpItem.setTargetId(platformId);
            sendRtpItem.setSendToPlatform(true);
        }
        sendRtpItem.setChannelId(channelId);
        sendRtpItem.setTcp(isTcp);
        sendRtpItem.setRtcp(rtcp);
        sendRtpItem.setApp(MediaStreamUtil.RTP_APP);
        sendRtpItem.setLocalPort(localPort);
        sendRtpItem.setServerId(serverId);
        sendRtpItem.setMediaServerId(mediaServer.getId());
        return sendRtpItem;
    }

    @Override
    public String toString() {
        return "SendRtpItem{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                ", ssrc='" + ssrc + '\'' +
                ", targetId='" + targetId + '\'' +
                ", app='" + app + '\'' +
                ", channelId='" + channelId + '\'' +
                ", status=" + status +
                ", stream='" + stream + '\'' +
                ", tcp=" + tcp +
                ", tcpActive=" + tcpActive +
                ", localIp='" + localIp + '\'' +
                ", localPort=" + localPort +
                ", mediaServerId='" + mediaServerId + '\'' +
                ", serverId='" + serverId + '\'' +
                ", CallId='" + callId + '\'' +
                ", fromTag='" + fromTag + '\'' +
                ", toTag='" + toTag + '\'' +
                ", pt=" + pt +
                ", usePs=" + usePs +
                ", onlyAudio=" + onlyAudio +
                ", rtcp=" + rtcp +
                ", playType=" + playType +
                ", receiveStream='" + receiveStream + '\'' +
                ", sessionName='" + sessionName + '\'' +
                '}';
    }


    public void setPlayTypeByChannelDataType(Integer dataType, String sessionName) {
        if (dataType == ChannelDataType.STREAM_PUSH) {
            this.setPlayType(InviteStreamType.PUSH);
        }else if (dataType == ChannelDataType.STREAM_PROXY){
            this.setPlayType(InviteStreamType.PROXY);
        }else {
            this.setPlayType("Play".equalsIgnoreCase(sessionName) ? InviteStreamType.PLAY : InviteStreamType.PLAYBACK);
        }
    }


}
