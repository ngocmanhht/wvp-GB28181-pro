package com.genersoft.iot.vmp.jt1078.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * JT terminal properties
 */
@Setter
@Getter
@Schema(description = "JTterminal properties")
public class JTDeviceAttribute {

    @Schema(description = "terminal type")
    private JTDeviceType type;

    @Schema(description = "manufacturer ID")
    private String makerId;

    @Schema(description = "Terminal model")
    private String deviceModel;

    @Schema(description = "terminal ID")
    private String terminalId;

    @Schema(description = "Terminal SIM card ICCID")
    private String iccId;

    @Schema(description = "Terminal hardware version number")
    private String hardwareVersion;

    @Schema(description = "Firmware version number")
    private String firmwareVersion ;

    @Schema(description = "GNSS module properties")
    private JTGnssAttribute gnssAttribute ;

    @Schema(description = "Communication module properties")
    private JTCommunicationModuleAttribute communicationModuleAttribute ;

    @Override
    public String toString() {
        return "JTDeviceAttribute{" +
                "type=" + type +
                ", makerId='" + makerId + '\'' +
                ", deviceModel='" + deviceModel + '\'' +
                ", terminalId='" + terminalId + '\'' +
                ", iccId='" + iccId + '\'' +
                ", hardwareVersion='" + hardwareVersion + '\'' +
                ", firmwareVersion='" + firmwareVersion + '\'' +
                ", gnssAttribute=" + gnssAttribute +
                ", communicationModuleAttribute=" + communicationModuleAttribute +
                '}';
    }
}
