package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTCircleArea;
import com.genersoft.iot.vmp.jt1078.bean.JTVehicleControl;
import com.genersoft.iot.vmp.jt1078.bean.common.ConfigAttribute;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Set circular area
 */
@Setter
@Getter
@MsgId(id = "8600")
public class J8600 extends Rs {

    /**
     * Set attributes, 0: update area; 1: add area; 2: modify area
     */
    private int attribute;

    /**
     * area item
     */
    private List<JTCircleArea> circleAreaList;


    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(attribute);
        buffer.writeByte(circleAreaList.size());
        if (circleAreaList.isEmpty()) {
            return buffer;
        }
        for (JTCircleArea circleArea : circleAreaList) {
            buffer.writeBytes(circleArea.encode());
        }
        return buffer;
    }

}
