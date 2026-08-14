package com.genersoft.iot.vmp.web.custom.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Get camera list based on multiple IDs")
public class IdsQueryParam {

    @Schema(description = "Channel number list")
    private List<String> deviceIds;

    @Schema(description = "Coordinate system type：WGS84,GCJ02、BD09")
    private String geoCoordSys;
}
