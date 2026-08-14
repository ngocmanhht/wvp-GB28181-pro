package com.genersoft.iot.vmp.web.custom.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Channel information")
public class ChannelParam {

    @Schema(description = "National standard number of camera equipment. This parameter does not need to be set for non-national standard cameras.")
    private String deviceCode;

    @Schema(description = "Channel number")
    private String deviceId;
}
