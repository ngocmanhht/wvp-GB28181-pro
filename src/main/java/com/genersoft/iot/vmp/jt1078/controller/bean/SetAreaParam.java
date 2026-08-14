package com.genersoft.iot.vmp.jt1078.controller.bean;

import com.genersoft.iot.vmp.jt1078.bean.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@Schema(description = "Set zone parameters")
public class SetAreaParam {

    @Schema(description = "Terminal mobile phone number")
    private String phoneNumber;

    @Schema(description = "circular area item")
    private List<JTCircleArea> circleAreaList;

    @Schema(description = "Rectangular area item")
    private List<JTRectangleArea> rectangleAreas;

    @Schema(description = "polygon area")
    private JTPolygonArea polygonArea;

    @Schema(description = "route")
    private JTRoute route;


    @Override
    public String toString() {
        return "SetAreaParam{" +
                "Device mobile phone number='" + phoneNumber + '\'' +
                ", circleAreaList=" + circleAreaList +
                ", rectangleAreas=" + rectangleAreas +
                ", polygonArea=" + polygonArea +
                ", route=" + route +
                '}';
    }
}
