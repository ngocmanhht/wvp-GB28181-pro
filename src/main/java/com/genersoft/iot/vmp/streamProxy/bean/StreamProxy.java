package com.genersoft.iot.vmp.streamProxy.bean;

import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.util.ObjectUtils;

/**
 * @author lin
 */
@Data
@Schema(description = "Streaming agent information")
@EqualsAndHashCode(callSuper = true)
public class StreamProxy extends CommonGBChannel {

    /**
     * Database auto-incrementID
     */
    @Schema(description = "Database auto-incrementID")
    private int id;

    @Schema(description = "Type, value, default: Streaming media pulls streams directly (default），ffmpeg： ffmpegRealize pull flow")
    private String type;

    @Schema(description = "Application name")
    private String app;

    @Schema(description = "flowID")
    private String stream;

    @Schema(description = "Streaming media service currently used for streamingID")
    private String mediaServerId;

    @Schema(description = "Fixed selection of streaming servicesID")
    private String relatesMediaServerId;

    @Schema(description = "serviceID")
    private String serverId;

    @Schema(description = "Pull address")
    private String srcUrl;

    @Schema(description = "Timeout: seconds")
    private int timeout;

    @Schema(description = "ffmpegTemplateKEY")
    private String ffmpegCmdKey;

    @Schema(description = "rtspWhen pulling streams, stream pulling method, 0: tcp, 1: udp, 2: multicast")
    private String rtspType;

    @Schema(description = "Whether to enable")
    private boolean enable;

    @Schema(description = "Whether to enable audio")
    private boolean enableAudio;

    @Schema(description = "Whether to enableMP4")
    private boolean enableMp4;

    @Schema(description = "Whether to automatically deactivate when no one is watching")
    private boolean enableDisableNoneReader;

    @Schema(description = "The key returned by zlm when pulling the streaming agent, used to stop the streaming agent")
    private String streamKey;

    @Schema(description = "Pull state")
    private Boolean pulling;

    public CommonGBChannel buildCommonGBChannel() {
        if (ObjectUtils.isEmpty(this.getGbDeviceId())) {
            return null;
        }
        if (ObjectUtils.isEmpty(this.getGbName())) {
            this.setGbName( app+ "-" +stream);
        }
        this.setDataType(ChannelDataType.STREAM_PROXY);
        this.setDataDeviceId(this.getId());
        return this;
    }
}
