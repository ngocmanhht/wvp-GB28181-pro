package com.genersoft.iot.vmp.jt1078.controller.bean;

import com.genersoft.iot.vmp.jt1078.bean.JTDeviceConnectionControl;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ConnectionControlParam {

    @Schema(description = "Terminal mobile phone number")
    private String phoneNumber;
    private JTDeviceConnectionControl control;

    @Override
    public String toString() {
        return "ConnectionControlParam{" +
                "deviceId='" + phoneNumber + '\'' +
                ", control=" + control +
                '}';
    }
}
