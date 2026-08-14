package com.genersoft.iot.vmp.jt1078.controller.bean;

import com.genersoft.iot.vmp.jt1078.bean.JTConfirmationAlarmMessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * Manually confirm alarm message parameters
 */
@Setter
@Getter
@Schema(description = "Manually confirm alarm message parameters")
public class ConfirmationAlarmMessageParam {

    @Schema(description = "Terminal mobile phone number")
    private String phoneNumber;
    @Schema(description = "Alarm message serial number")
    private int alarmPackageNo;
    @Schema(description = "Manual confirmation alarm type")
    private JTConfirmationAlarmMessageType alarmMessageType;

    @Override
    public String toString() {
        return "ConfirmationAlarmMessageParam{" +
                "PhoneNumber='" + phoneNumber + '\'' +
                ", alarmPackageNo=" + alarmPackageNo +
                ", alarmMessageType=" + alarmMessageType +
                '}';
    }
}
