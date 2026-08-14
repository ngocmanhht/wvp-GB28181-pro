package com.genersoft.iot.vmp.gb28181.controller.bean;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class DrawThinParam {
    private Map<Integer, Double> zoomParam;
    private Extent extent;

    /**
     * geographical coordinate system， WGS84/GCJ02， The coordinate system used to identify the extent parameter
     */
    private String geoCoordSys;
}
