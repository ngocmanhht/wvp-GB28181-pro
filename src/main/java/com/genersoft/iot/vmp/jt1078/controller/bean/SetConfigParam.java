package com.genersoft.iot.vmp.jt1078.controller.bean;

import com.genersoft.iot.vmp.jt1078.bean.JTDeviceConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Schema(description = "Terminal parameter settings")
public class SetConfigParam {

    @Schema(description = "Terminal mobile phone number")
    private String phoneNumber;

    @Schema(description = "Terminal parameter settings")
    private JTDeviceConfig config;

    @Override
    public String toString() {
        return "SetConfigParam{" +
                "phoneNumber='" + phoneNumber + '\'' +
                ", config=" + config +
                '}';
    }
}
