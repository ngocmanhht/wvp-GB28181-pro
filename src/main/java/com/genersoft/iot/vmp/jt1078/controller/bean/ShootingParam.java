package com.genersoft.iot.vmp.jt1078.controller.bean;

import com.genersoft.iot.vmp.jt1078.bean.JTShootingCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Schema(description = "Camera immediately captures command parameters")
public class ShootingParam {

    @Schema(description = "Terminal mobile phone number")
    private String phoneNumber;

    @Schema(description = "Shooting command parameters")
    private JTShootingCommand shootingCommand;

    @Override
    public String toString() {
        return "ShootingParam{" +
                "Device mobile phone number='" + phoneNumber + '\'' +
                ", shootingCommand=" + shootingCommand +
                '}';
    }
}
