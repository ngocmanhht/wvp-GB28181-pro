package com.genersoft.iot.vmp.gb28181.controller.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Channel related parameters")
public class UpdateChannelParam {

    @Schema(description = "Database of superior platformID")
    private Integer platformId;


    @Schema(description = "Link all channels")
    private boolean all;

    @Schema(description = "Channel to be associatedID")
    List<Integer> channelIds;

    @Schema(description = "Device to be associatedID")
    List<Integer> deviceIds;
}
