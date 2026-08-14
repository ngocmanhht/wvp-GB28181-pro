package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

/**
 * PTZ control instructions-Aperture control
 *
 */
@Setter
@Getter
@MsgId(id = "9303")
public class J9303 extends Rs {
    // Logical channel number
    private int channel;

    // Adjustment method: 0: Increase; 1: Decrease
    private int iris;

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(channel);
        buffer.writeByte(iris);
        return buffer;
    }

    @Override
    public String toString() {
        return "J9303{" +
                "channel=" + channel +
                ", iris=" + iris +
                '}';
    }
}
