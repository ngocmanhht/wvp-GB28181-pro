package com.genersoft.iot.vmp.jt1078.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Location extensions")
public class JTPositionAdditionalInfo {

    @Schema(description = "Mileage, in units of1/10km, Corresponding car odometer reading")
    private int mileage;

    @Schema(description = "Oil volume, unit is1/10L, Corresponds to the car’s fuel gauge reading")
    private int oil;

    @Schema(description = "The speed obtained by the driving record function, in units of1/10km/h")
    private int speed;

    @Schema(description = "alarm event ID")
    private int alarmId;
    // TODO Tire pressure is not supported yet

    @Schema(description = "Cabin temperature in degrees Celsius")
    private int carriageTemperature;
    // TODO Tire pressure is not supported yet

}
