package com.genersoft.iot.vmp.jt1078.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * JT Equipment
 */
@Data
@Schema(description = "jt808Equipment")
public class JTDevice {

    private int id;

    @Schema(description = "Provincial areaID")
    private String provinceId;

    @Schema(description = "Provincial text description")
    private String provinceText;

    @Schema(description = "City and countyID")
    private String cityId;

    @Schema(description = "City and county text description")
    private String cityText;

    @Schema(description = "manufacturerID")
    private String makerId;

    @Schema(description = "Terminal model")
    private String model;

    @Schema(description = "Terminal mobile phone number")
    private String phoneNumber;

    @Schema(description = "terminalID")
    private String terminalId;

    @Schema(description = "license plate color")
    private int plateColor;

    @Schema(description = "license plate")
    private String plateNo;

    @Schema(description = "longitude")
    private Double longitude;

    @Schema(description = "Latitude")
    private Double latitude;

    @Schema(description = "Registration time")
    private String registerTime;

    @Schema(description = "creation time")
    private String createTime;

    @Schema(description = "Update time")
    private String updateTime;

    @Schema(description = "Status")
    private boolean status;

    @Schema(description = "Media ID used by the device, default isnull")
    private String mediaServerId;

    @Schema(description = "Geographic coordinate system, currently supported WGS84,GCJ02")
    private String geoCoordSys;

    @Schema(description = "collect flowIP")
    private String sdpIp;

    @Override
    public String toString() {
        return "JTDevice{" +
                "  Terminal mobile phone number='" + phoneNumber + '\'' +
                ", Provincial areaID='" + provinceId + '\'' +
                ", Provincial text description='" + provinceText + '\'' +
                ", City and countyID='" + cityId + '\'' +
                ", City and county text description='" + cityText + '\'' +
                ", manufacturerID='" + makerId + '\'' +
                ", Terminal model='" + model + '\'' +
                ", EquipmentID='" + terminalId + '\'' +
                ", license plate color=" + plateColor +
                ", license plate='" + plateNo + '\'' +
                ", Registration time='" + registerTime + '\'' +
                ", status=" + status +
                '}';
    }
}
