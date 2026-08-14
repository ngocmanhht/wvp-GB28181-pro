package com.genersoft.iot.vmp.vmanager.bean;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.service.bean.DownloadFileInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "flow information")
public class StreamContent {

    @Schema(description = "Application name")
    private String app;

    @Schema(description = "flowID")
    private String stream;

    @Schema(description = "IP")
    private String ip;

    @Schema(description = "HTTP-FLVstream address")
    private String flv;

    @Schema(description = "HTTPS-FLVstream address")
    private String https_flv;

    @Schema(description = "Websocket-FLVstream address")
    private String ws_flv;

    @Schema(description = "Websockets-FLVstream address")
    private String wss_flv;

    @Schema(description = "HTTP-FMP4stream address")
    private String fmp4;

    @Schema(description = "HTTPS-FMP4stream address")
    private String https_fmp4;

    @Schema(description = "Websocket-FMP4stream address")
    private String ws_fmp4;

    @Schema(description = "Websockets-FMP4stream address")
    private String wss_fmp4;

    @Schema(description = "HLSstream address")
    private String hls;

    @Schema(description = "HTTPS-HLSstream address")
    private String https_hls;

    @Schema(description = "Websocket-HLSstream address")
    private String ws_hls;

    @Schema(description = "Websockets-HLSstream address")
    private String wss_hls;

    @Schema(description = "HTTP-TSstream address")
    private String ts;

    @Schema(description = "HTTPS-TSstream address")
    private String https_ts;

    @Schema(description = "Websocket-TSstream address")
    private String ws_ts;

    @Schema(description = "Websockets-TSstream address")
    private String wss_ts;

    @Schema(description = "RTMPstream address")
    private String rtmp;

    @Schema(description = "RTMPSstream address")
    private String rtmps;

    @Schema(description = "RTSPstream address")
    private String rtsp;

    @Schema(description = "RTSPSstream address")
    private String rtsps;

    @Schema(description = "RTCstream address")
    private String rtc;

    @Schema(description = "RTCSstream address")
    private String rtcs;

    @Schema(description = "streaming mediaID")
    private String mediaServerId;

    @Schema(description = "Stream encoding information")
    private MediaInfo mediaInfo;

    @Schema(description = "start time")
    private String startTime;

    @Schema(description = "end time")
    private String endTime;

    @Schema(description = "duration(used during playback)")
    private Double duration;

    @Schema(description = "File download address (use for video downloads）")
    private DownloadFileInfo downLoadFilePath;

    @Schema(description = "Transcoded video stream")
    private StreamContent transcodeStream;

    private double progress;

    @Schema(description = "Returned by the streaming agentKEY")
    private String key;

    @Schema(description = "usedWVP ID")
    private String serverId;

    public StreamContent(StreamInfo streamInfo) {
        if (streamInfo == null) {
            return;
        }
        this.app = streamInfo.getApp();
        this.stream = streamInfo.getStream();
        if (streamInfo.getFlv() != null) {
            this.flv = streamInfo.getFlv().getUrl();
        }
        if (streamInfo.getHttps_flv() != null) {
            this.https_flv = streamInfo.getHttps_flv().getUrl();
        }
        if (streamInfo.getWs_flv() != null) {
            this.ws_flv = streamInfo.getWs_flv().getUrl();
        }
        if (streamInfo.getWss_flv() != null) {
            this.wss_flv = streamInfo.getWss_flv().getUrl();
        }
        if (streamInfo.getFmp4() != null) {
            this.fmp4 = streamInfo.getFmp4().getUrl();
        }
        if (streamInfo.getHttps_fmp4() != null) {
            this.https_fmp4 = streamInfo.getHttps_fmp4().getUrl();
        }
        if (streamInfo.getWs_fmp4() != null) {
            this.ws_fmp4 = streamInfo.getWs_fmp4().getUrl();
        }
        if (streamInfo.getWss_fmp4() != null) {
            this.wss_fmp4 = streamInfo.getWss_fmp4().getUrl();
        }
        if (streamInfo.getHls() != null) {
            this.hls = streamInfo.getHls().getUrl();
        }
        if (streamInfo.getHttps_hls() != null) {
            this.https_hls = streamInfo.getHttps_hls().getUrl();
        }
        if (streamInfo.getWs_hls() != null) {
            this.ws_hls = streamInfo.getWs_hls().getUrl();
        }
        if (streamInfo.getWss_hls() != null) {
            this.wss_hls = streamInfo.getWss_hls().getUrl();
        }
        if (streamInfo.getTs() != null) {
            this.ts = streamInfo.getTs().getUrl();
        }
        if (streamInfo.getHttps_ts() != null) {
            this.https_ts = streamInfo.getHttps_ts().getUrl();
        }
        if (streamInfo.getWs_ts() != null) {
            this.ws_ts = streamInfo.getWs_ts().getUrl();
        }
        if (streamInfo.getRtmp() != null) {
            this.rtmp = streamInfo.getRtmp().getUrl();
        }
        if (streamInfo.getRtmps() != null) {
            this.rtmps = streamInfo.getRtmps().getUrl();
        }
        if (streamInfo.getRtsp() != null) {
            this.rtsp = streamInfo.getRtsp().getUrl();
        }
        if (streamInfo.getRtsps() != null) {
            this.rtsps = streamInfo.getRtsps().getUrl();
        }
        if (streamInfo.getRtc() != null) {
            this.rtc = streamInfo.getRtc().getUrl();
        }
        if (streamInfo.getRtcs() != null) {
            this.rtcs = streamInfo.getRtcs().getUrl();
        }
        if (streamInfo.getMediaServer() != null) {
            this.mediaServerId = streamInfo.getMediaServer().getId();
        }

        this.mediaInfo = streamInfo.getMediaInfo();
        this.startTime = streamInfo.getStartTime();
        this.endTime = streamInfo.getEndTime();
        this.progress = streamInfo.getProgress();
        this.duration = streamInfo.getDuration();
        this.key = streamInfo.getKey();
        this.serverId = streamInfo.getServerId();

        if (streamInfo.getDownLoadFilePath() != null) {
            this.downLoadFilePath = streamInfo.getDownLoadFilePath();
        }
        if (streamInfo.getTranscodeStream() != null) {
            this.transcodeStream = new StreamContent(streamInfo.getTranscodeStream());
        }
    }

}
