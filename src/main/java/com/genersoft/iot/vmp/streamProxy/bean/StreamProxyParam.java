package com.genersoft.iot.vmp.streamProxy.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author lin
 */
@Data
@Schema(description = "Streaming agent information")
public class StreamProxyParam {

    @Schema(description = "Type, value, default: Streaming media pulls streams directly (default），ffmpeg： ffmpegRealize pull flow")
    private String type;

    @Schema(description = "Application name")
    private String app;

    @Schema(description = "Name")
    private String name;

    @Schema(description = "flowID")
    private String stream;

    @Schema(description = "streaming servicesID")
    private String mediaServerId;

    @Schema(description = "Pull address")
    private String url;

    @Schema(description = "Timeout: seconds")
    private int timeoutMs;

    @Schema(description = "ffmpegTemplateKEY")
    private String ffmpegCmdKey;

    @Schema(description = "rtspWhen pulling streams, stream pulling method, 0: tcp, 1: udp, 2: multicast")
    private String rtpType;

    @Schema(description = "Whether to enable")
    private boolean enable;

    @Schema(description = "Whether to enable audio")
    private boolean enableAudio;

    @Schema(description = "Whether to enableMP4")
    private boolean enableMp4;

    @Schema(description = "Whether to automatically deactivate when no one is watching")
    private boolean enableDisableNoneReader;


    public StreamProxy buildStreamProxy(String serverId) {
        StreamProxy streamProxy = new StreamProxy();
        streamProxy.setApp(app);
        streamProxy.setStream(stream);
        streamProxy.setRelatesMediaServerId(mediaServerId);
        streamProxy.setServerId(serverId);
        streamProxy.setSrcUrl(url);
        streamProxy.setTimeout(timeoutMs/1000);
        streamProxy.setRtspType(rtpType);
        streamProxy.setEnable(enable);
        streamProxy.setEnableAudio(enableAudio);
        streamProxy.setEnableMp4(enableMp4);
        streamProxy.setEnableDisableNoneReader(enableDisableNoneReader);
        streamProxy.setFfmpegCmdKey(ffmpegCmdKey);
        streamProxy.setGbName(name);
        return streamProxy;

    }
}
