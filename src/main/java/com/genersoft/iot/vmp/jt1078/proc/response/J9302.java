package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

/**
 * PTZ control instructions-Focus control
 *
 */
@Setter
@Getter
@MsgId(id = "9302")
public class J9302 extends Rs {
    // Logical channel number
    private int channel;

    // Direction: 0: Increase the focal length; 1: Decrease the focal length
    private int focalDirection;

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(channel);
        buffer.writeByte(focalDirection);
        return buffer;
    }

    @Override
    public String toString() {
        return "J9302{" +
                "channel=" + channel +
                ", zoomDirection=" + focalDirection +
                '}';
    }
}
