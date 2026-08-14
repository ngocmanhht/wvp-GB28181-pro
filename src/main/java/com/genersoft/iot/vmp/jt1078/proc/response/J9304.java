package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

/**
 * PTZ control instructions-PTZ wiper control
 *
 */
@Setter
@Getter
@MsgId(id = "9304")
public class J9304 extends Rs {
    // Logical channel number
    private int channel;

    // Start and stop identification: 0: Stop; 1: Start
    private int on;

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(channel);
        buffer.writeByte(on);
        return buffer;
    }

    @Override
    public String toString() {
        return "J9304{" +
                "channel=" + channel +
                ", on=" + on +
                '}';
    }
}
