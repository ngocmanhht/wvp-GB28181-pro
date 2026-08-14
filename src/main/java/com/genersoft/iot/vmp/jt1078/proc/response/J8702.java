package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Report driver identification information request
 */
@MsgId(id = "8702")
public class J8702 extends Rs {

    @Override
    public ByteBuf encode() {
        return Unpooled.buffer();
    }

}
