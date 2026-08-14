package com.genersoft.iot.vmp.gb28181.controller.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description="Submit parameters for multiple channels associated with administrative divisions")
public class ChannelToRegionParam {

    @Schema(description = "Administrative division number")
    private String civilCode;

    @Schema(description = "The selected channel, and the all parameter, choose one of the two")
    private List<Integer> channelIds;

    @Schema(description = "All channels, choose one of the channelIds parameters")
    private Boolean all;

}
