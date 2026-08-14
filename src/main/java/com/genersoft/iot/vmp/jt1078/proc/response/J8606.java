package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTPolygonArea;
import com.genersoft.iot.vmp.jt1078.bean.JTRoute;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

/**
 * Set route
 */
@Setter
@Getter
@MsgId(id = "8606")
public class J8606 extends Rs {

    /**
     * route
     */
    private JTRoute route;


    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeBytes(route.encode());
        return buffer;
    }

}
