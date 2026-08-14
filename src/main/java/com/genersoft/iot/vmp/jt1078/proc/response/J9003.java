package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Query terminal audio and video attributes
 */
@MsgId(id = "9003")
public class J9003 extends Rs {

    @Override
    public ByteBuf encode() {
        return Unpooled.buffer();
    }

}
