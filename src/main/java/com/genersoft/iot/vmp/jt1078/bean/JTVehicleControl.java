package com.genersoft.iot.vmp.jt1078.bean;

import com.genersoft.iot.vmp.jt1078.bean.common.ConfigAttribute;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * Vehicle control type
 */
@Setter
@Getter
public class JTVehicleControl {

    private int length;

    private void setLength(Object value) {
        if (Objects.isNull(value)) {
            length--;
        }else {
            length ++;
        }
    }

    @ConfigAttribute(id = 0X0001, type="Byte", description = "Door, 0: Door locked 1: Door open")
    private Integer controlCarDoor;

    public void setControlCarDoor(Integer controlCarDoor) {
        this.controlCarDoor = controlCarDoor;
        setLength(controlCarDoor);
    }
}
