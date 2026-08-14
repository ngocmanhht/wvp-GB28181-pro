package com.genersoft.iot.vmp.web.custom.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Polygon retrieval camera parameters")
public class PolygonQueryParam {

    @Schema(description = "polygon position, format： [{'lng':116.32, 'lat': 39: 39.2}, {'lng':115.32, 'lat': 39: 38.2}, {'lng':125.32, 'lat': 39: 38.2}]")
    private List<Point> position;

    @Schema(description = "map level")
    private Integer level;

    @Schema(description = "Group alias")
    private String groupAlias;

    @Schema(description = "Coordinate system type：WGS84,GCJ02、BD09")
    private String geoCoordSys;
}
