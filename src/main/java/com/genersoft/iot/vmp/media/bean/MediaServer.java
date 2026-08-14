package com.genersoft.iot.vmp.media.bean;


import com.genersoft.iot.vmp.media.abl.bean.AblServerConfig;
import com.genersoft.iot.vmp.media.zlm.dto.ZLMServerConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.util.ObjectUtils;

@Schema(description = "Streaming service information")
@Data
public class MediaServer {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "IP")
    private String ip;

    @Schema(description = "hookIP used (used by zlm to access WVPIP）")
    private String hookIp = "127.0.0.1";

    @Schema(description = "SDP IP")
    private String sdpIp;

    @Schema(description = "flowIP")
    private String streamIp;

    @Schema(description = "HTTPport")
    private int httpPort;

    @Schema(description = "HTTPSport")
    private int httpSSlPort;

    @Schema(description = "RTMPport")
    private int rtmpPort;

    @Schema(description = "flvport")
    private int flvPort;

    @Schema(description = "https-flvport")
    private int flvSSLPort;

    @Schema(description = "mp4port")
    private int mp4Port;

    @Schema(description = "ws-flvport")
    private int wsFlvPort;

    @Schema(description = "wss-flvport")
    private int wsFlvSSLPort;

    @Schema(description = "RTMPSport")
    private int rtmpSSlPort;

    @Schema(description = "RTPTraffic collection port (useful in single port mode）")
    private int rtpProxyPort;

    @Schema(description = "1078Traffic collection port (useful in single port mode）")
    private int jttProxyPort;

    @Schema(description = "RTSPport")
    private int rtspPort;

    @Schema(description = "RTSPSport")
    private int rtspSSLPort;

    @Schema(description = "Whether to enable automatic configurationZLM")
    private boolean autoConfig;

    @Schema(description = "ZLMAuthentication parameters")
    private String secret;

    @Schema(description = "keepalive hookTrigger interval, unit seconds")
    private Float hookAliveInterval;

    @Schema(description = "Whether to use multi-port mode")
    private boolean rtpEnable;

    @Schema(description = "Status")
    private boolean status;

    @Schema(description = "Multi-port RTP traffic collection port range")
    private String rtpPortRange;

    @Schema(description = "RTPSending port range")
    private String sendRtpPortRange;

    @Schema(description = "assistservice port")
    private int recordAssistPort;

    @Schema(description = "creation time")
    private String createTime;

    @Schema(description = "Update time")
    private String updateTime;

    @Schema(description = "last heartbeat time")
    private String lastKeepaliveTime;

    @Schema(description = "Is it the defaultZLM")
    private boolean defaultServer;

    @Schema(description = "Video storage duration")
    private int recordDay;

    @Schema(description = "Video storage path")
    private String recordPath;
    @Schema(description = "Type： zlm/abl")
    private String type;

    @Schema(description = "Transcoding prefix")
    private String transcodeSuffix;

    @Schema(description = "serviceId")
    private String serverId;

    public MediaServer() {
    }

    public MediaServer(ZLMServerConfig zlmServerConfig, String sipIp) {
        id = zlmServerConfig.getGeneralMediaServerId();
        ip = zlmServerConfig.getIp();
        hookIp = ObjectUtils.isEmpty(zlmServerConfig.getHookIp())? sipIp: zlmServerConfig.getHookIp();
        sdpIp = ObjectUtils.isEmpty(zlmServerConfig.getSdpIp())? zlmServerConfig.getIp(): zlmServerConfig.getSdpIp();
        streamIp = ObjectUtils.isEmpty(zlmServerConfig.getStreamIp())? zlmServerConfig.getIp(): zlmServerConfig.getStreamIp();
        httpPort = zlmServerConfig.getHttpPort();
        httpSSlPort = zlmServerConfig.getHttpSSLport();
        rtmpPort = zlmServerConfig.getRtmpPort();
        rtmpSSlPort = zlmServerConfig.getRtmpSslPort();
        rtpProxyPort = zlmServerConfig.getRtpProxyPort();
        rtspPort = zlmServerConfig.getRtspPort();
        rtspSSLPort = zlmServerConfig.getRtspSSlport();
        autoConfig = true; // Default valuetrue;
        secret = zlmServerConfig.getApiSecret();
        hookAliveInterval = zlmServerConfig.getHookAliveInterval();
        rtpEnable = false; // Use single port by default;Until the user sets up multiple ports by himself
        rtpPortRange = zlmServerConfig.getPortRange().replace("_",","); // By default, 30000 and 30500 are used as the port numbers for sending streams during cascading.
        recordAssistPort = 0; // Off by default
        transcodeSuffix = zlmServerConfig.getTranscodeSuffix();

    }

    public MediaServer(AblServerConfig config, String sipIp) {
        id = config.getMediaServerId();
        ip = config.getServerIp();
        hookIp = sipIp;
        sdpIp = config.getServerIp();
        streamIp = config.getServerIp();
        httpPort = config.getHttpServerPort();
        flvPort = config.getHttpFlvPort();
        mp4Port = config.getHttpMp4Port();
        wsFlvPort = config.getWsFlvPort();
        rtmpPort = config.getRtmpPort();
        rtpProxyPort = config.getJtt1078RecvPort();
        rtspPort = config.getRtspPort();
        autoConfig = true; // Default valuetrue;
        secret = config.getSecret();
        rtpEnable = false; // Use single port by default;Until the user sets up multiple ports by himself
        rtpPortRange = "30000,30500"; // By default, 30000 and 30500 are used as the port numbers for sending streams during cascading.
        recordAssistPort = 0; // Off by default
    }
}
