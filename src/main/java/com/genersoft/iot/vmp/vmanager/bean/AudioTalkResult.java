package com.genersoft.iot.vmp.vmanager.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "intercom information")
public class AudioTalkResult {

    @Schema(description = "Push address (browser WebRTC push toZLM）")
    private StreamContent pushStream;

    @Schema(description = "Playback address (the device audio is played to the browser through ZLM), when callingnull")
    private StreamContent playStream;
}
