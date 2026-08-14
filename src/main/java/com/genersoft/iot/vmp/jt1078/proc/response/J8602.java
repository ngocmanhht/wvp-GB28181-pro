package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTCircleArea;
import com.genersoft.iot.vmp.jt1078.bean.JTRectangleArea;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Set rectangular area
 */
@Setter
@Getter
@MsgId(id = "8602")
public class J8602 extends Rs {

    /**
     * Set attributes, 0: update area; 1: add area; 2: modify area
     */
    private int attribute;

    /**
     * area item
     */
    private List<JTRectangleArea> rectangleAreas;


    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(attribute);
        buffer.writeByte(rectangleAreas.size());
        if (rectangleAreas.isEmpty()) {
            return buffer;
        }
        for (JTRectangleArea area : rectangleAreas) {
            buffer.writeBytes(area.encode());
        }
        return buffer;
    }

}
