package com.genersoft.iot.vmp.common;

import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.service.bean.DownloadFileInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

@Data
@Schema(description = "flow information")
public class StreamInfo implements Serializable, Cloneable{

    @Schema(description = "Application name")
    private String app;
    @Schema(description = "flowID")
    private String stream;
    @Schema(description = "Device number")
    private String deviceId;
    @Schema(description = "channelID")
    private Integer channelId;

    @Schema(description = "IP")
    private String ip;

    @Schema(description = "HTTP-FLVstream address")
    private StreamURL flv;

    @Schema(description = "HTTPS-FLVstream address")
    private StreamURL https_flv;
    @Schema(description = "Websocket-FLVstream address")
    private StreamURL ws_flv;
    @Schema(description = "Websockets-FLVstream address")
    private StreamURL wss_flv;
    @Schema(description = "HTTP-FMP4stream address")
    private StreamURL fmp4;
    @Schema(description = "HTTPS-FMP4stream address")
    private StreamURL https_fmp4;
    @Schema(description = "Websocket-FMP4stream address")
    private StreamURL ws_fmp4;
    @Schema(description = "Websockets-FMP4stream address")
    private StreamURL wss_fmp4;
    @Schema(description = "HLSstream address")
    private StreamURL hls;
    @Schema(description = "HTTPS-HLSstream address")
    private StreamURL https_hls;
    @Schema(description = "Websocket-HLSstream address")
    private StreamURL ws_hls;
    @Schema(description = "Websockets-HLSstream address")
    private StreamURL wss_hls;
    @Schema(description = "HTTP-TSstream address")
    private StreamURL ts;
    @Schema(description = "HTTPS-TSstream address")
    private StreamURL https_ts;
    @Schema(description = "Websocket-TSstream address")
    private StreamURL ws_ts;
    @Schema(description = "Websockets-TSstream address")
    private StreamURL wss_ts;
    @Schema(description = "RTMPstream address")
    private StreamURL rtmp;
    @Schema(description = "RTMPSstream address")
    private StreamURL rtmps;
    @Schema(description = "RTSPstream address")
    private StreamURL rtsp;
    @Schema(description = "RTSPSstream address")
    private StreamURL rtsps;
    @Schema(description = "RTCstream address")
    private StreamURL rtc;

    @Schema(description = "RTCSstream address")
    private StreamURL rtcs;
    @Schema(description = "Streaming media node")
    private MediaServer mediaServer;
    @Schema(description = "Stream encoding information")
    private MediaInfo mediaInfo;
    @Schema(description = "start time")
    private String startTime;
    @Schema(description = "end time")
    private String endTime;
    @Schema(description = "duration(used during playback)")
    private Double duration;
    @Schema(description = "Progress (video download and use）")
    private double progress;
    @Schema(description = "File download address (use for video downloads）")
    private DownloadFileInfo downLoadFilePath;
    @Schema(description = "on demand requestcallId")
    private String callId;

    @Schema(description = "Whether to pause (video playback uses）")
    private boolean pause;

    @Schema(description = "Generate source type, including unknown = 0,rtmp_push=1,rtsp_push=2,rtp_push=3,pull=4,ffmpeg_pull=5,mp4_vod=6,device_chn=7")
    private int originType;

    @Schema(description = "originTypetext description of")
    private String originTypeStr;

    @Schema(description = "Transcoded video stream")
    private StreamInfo transcodeStream;

    @Schema(description = "usedWVP ID")
    private String serverId;

    @Schema(description = "Streaming operations for stream bindingkey")
    private String key;

    public void setRtmp(String host, Integer port, Integer sslPort, String app, String stream, String callIdParam) {
        String file = String.format("%s/%s%s", app, stream, callIdParam);
        if (port != null && port > 0) {
            this.rtmp = new StreamURL("rtmp", host, port, file);
        }
        if (sslPort != null && sslPort > 0) {
            this.rtmps = new StreamURL("rtmps", host, sslPort, file);
        }
    }

    public void setRtsp(String host, Integer port, Integer sslPort, String app, String stream, String callIdParam) {
        String file = String.format("%s/%s%s", app, stream, callIdParam);
        if (port != null && port > 0) {
            this.rtsp = new StreamURL("rtsp", host, port, file);
        }
        if (sslPort != null && sslPort > 0) {
            this.rtsps = new StreamURL("rtsps", host, sslPort, file);
        }
    }

    public void setFlv(String host, Integer port, Integer sslPort, String file) {
        if (port != null && port > 0) {
            this.flv = new StreamURL("http", host, port, file);
        }
        if (sslPort != null && sslPort > 0) {
            this.https_flv = new StreamURL("https", host, sslPort, file);
        }
    }

    public void setWsFlv(String host, Integer port, Integer sslPort, String file) {
        if (port != null && port > 0) {
            this.ws_flv = new StreamURL("ws", host, port, file);
        }
        if (sslPort != null && sslPort > 0) {
            this.wss_flv = new StreamURL("wss", host, sslPort, file);
        }
    }

    public void setFmp4(String host, Integer port, Integer sslPort, String file) {
        if (port != null &&  port > 0) {
            this.fmp4 = new StreamURL("http", host, port, file);
        }
        if (sslPort != null &&  sslPort > 0) {
            this.https_fmp4 = new StreamURL("https", host, sslPort, file);
        }
    }

    public void setWsMp4(String host, Integer port, Integer sslPort, String file) {
        if (port != null && port > 0) {
            this.ws_fmp4 = new StreamURL("ws", host, port, file);
        }
        if (sslPort != null && sslPort > 0) {
            this.wss_fmp4 = new StreamURL("wss", host, sslPort, file);
        }
    }

    public void setHls(String host, Integer port, Integer sslPort, String app, String stream, String callIdParam) {
        String file = String.format("%s/%s/hls.m3u8%s", app, stream, callIdParam);
        if (port != null && port > 0) {
            this.hls = new StreamURL("http", host, port, file);
        }
        if (sslPort != null && sslPort > 0) {
            this.https_hls = new StreamURL("https", host, sslPort, file);
        }
    }

    public void setWsHls(String host, Integer port, Integer sslPort, String app, String stream, String callIdParam) {
        String file = String.format("%s/%s/hls.m3u8%s", app, stream, callIdParam);
        if (port != null && port > 0) {
            this.ws_hls = new StreamURL("ws", host, port, file);
        }
        if (sslPort != null && sslPort > 0) {
            this.wss_hls = new StreamURL("wss", host, sslPort, file);
        }
    }

    public void setTs(String host, Integer port, Integer sslPort, String app, String stream, String callIdParam) {
        String file = String.format("%s/%s.live.ts%s", app, stream, callIdParam);

        if (port != null && port > 0) {
            this.ts = new StreamURL("http", host, port, file);
        }
        if (sslPort != null && sslPort > 0) {
            this.https_ts = new StreamURL("https", host, sslPort, file);
        }
    }

    public void setWsTs(String host, Integer port, Integer sslPort, String app, String stream, String callIdParam) {
        String file = String.format("%s/%s.live.ts%s", app, stream, callIdParam);

        if (port != null && port > 0) {
            this.ws_ts = new StreamURL("ws", host, port, file);
        }
        if (sslPort != null && sslPort > 0) {
            this.wss_ts = new StreamURL("wss", host, sslPort, file);
        }
    }

    public void setRtc(String host, Integer port, Integer sslPort, String app, String stream, String callIdParam, boolean isPlay) {
        if (callIdParam != null) {
            callIdParam = Objects.equals(callIdParam, "") ? callIdParam : callIdParam.replace("?", "&");
        }
//        String file = String.format("%s/%s?type=%s%s", app, stream, isPlay?"play":"push", callIdParam);
        String file = String.format("index/api/webrtc?app=%s&stream=%s&type=%s%s", app, stream, isPlay?"play":"push", callIdParam);
        if (port > 0) {
            this.rtc = new StreamURL("http", host, port, file);
        }
        if (sslPort > 0) {
            this.rtcs = new StreamURL("https", host, sslPort, file);
        }
    }

    public void changeStreamIp(String localAddr) {
        if (this.flv != null) {
            this.flv.setHost(localAddr);
        }
        if (this.ws_flv != null ){
            this.ws_flv.setHost(localAddr);
        }
        if (this.hls != null ) {
            this.hls.setHost(localAddr);
        }
        if (this.ws_hls != null ) {
            this.ws_hls.setHost(localAddr);
        }
        if (this.ts != null ) {
            this.ts.setHost(localAddr);
        }
        if (this.ws_ts != null ) {
            this.ws_ts.setHost(localAddr);
        }
        if (this.fmp4 != null ) {
            this.fmp4.setHost(localAddr);
        }
        if (this.ws_fmp4 != null ) {
            this.ws_fmp4.setHost(localAddr);
        }
        if (this.rtc != null ) {
            this.rtc.setHost(localAddr);
        }
        if (this.https_flv != null) {
            this.https_flv.setHost(localAddr);
        }
        if (this.wss_flv != null) {
            this.wss_flv.setHost(localAddr);
        }
        if (this.https_hls != null) {
            this.https_hls.setHost(localAddr);
        }
        if (this.wss_hls != null) {
            this.wss_hls.setHost(localAddr);
        }
        if (this.wss_ts != null) {
            this.wss_ts.setHost(localAddr);
        }
        if (this.https_fmp4 != null) {
            this.https_fmp4.setHost(localAddr);
        }
        if (this.wss_fmp4 != null) {
            this.wss_fmp4.setHost(localAddr);
        }
        if (this.rtcs != null) {
            this.rtcs.setHost(localAddr);
        }
        if (this.rtsp != null) {
            this.rtsp.setHost(localAddr);
        }
        if (this.rtsps != null) {
            this.rtsps.setHost(localAddr);
        }
        if (this.rtmp != null) {
            this.rtmp.setHost(localAddr);
        }
        if (this.rtmps != null) {
            this.rtmps.setHost(localAddr);
        }
    }


    public static class TransactionInfo{
        public String callId;
        public String localTag;
        public String remoteTag;
        public String branch;
    }

    private TransactionInfo transactionInfo;


    @Override
    public StreamInfo clone() {
        StreamInfo instance = null;
        try{
            instance = (StreamInfo)super.clone();
            if (this.flv != null) {
                instance.flv=this.flv.clone();
            }
            if (this.ws_flv != null ){
                instance.ws_flv= this.ws_flv.clone();
            }
            if (this.hls != null ) {
                instance.hls= this.hls.clone();
            }
            if (this.ws_hls != null ) {
                instance.ws_hls= this.ws_hls.clone();
            }
            if (this.ts != null ) {
                instance.ts= this.ts.clone();
            }
            if (this.ws_ts != null ) {
                instance.ws_ts= this.ws_ts.clone();
            }
            if (this.fmp4 != null ) {
                instance.fmp4= this.fmp4.clone();
            }
            if (this.ws_fmp4 != null ) {
                instance.ws_fmp4= this.ws_fmp4.clone();
            }
            if (this.rtc != null ) {
                instance.rtc= this.rtc.clone();
            }
            if (this.https_flv != null) {
                instance.https_flv= this.https_flv.clone();
            }
            if (this.wss_flv != null) {
                instance.wss_flv= this.wss_flv.clone();
            }
            if (this.https_hls != null) {
                instance.https_hls= this.https_hls.clone();
            }
            if (this.wss_hls != null) {
                instance.wss_hls= this.wss_hls.clone();
            }
            if (this.wss_ts != null) {
                instance.wss_ts= this.wss_ts.clone();
            }
            if (this.https_fmp4 != null) {
                instance.https_fmp4= this.https_fmp4.clone();
            }
            if (this.wss_fmp4 != null) {
                instance.wss_fmp4= this.wss_fmp4.clone();
            }
            if (this.rtcs != null) {
                instance.rtcs= this.rtcs.clone();
            }
            if (this.rtsp != null) {
                instance.rtsp= this.rtsp.clone();
            }
            if (this.rtsps != null) {
                instance.rtsps= this.rtsps.clone();
            }
            if (this.rtmp != null) {
                instance.rtmp= this.rtmp.clone();
            }
            if (this.rtmps != null) {
                instance.rtmps= this.rtmps.clone();
            }
        }catch(CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return instance;
    }

}
