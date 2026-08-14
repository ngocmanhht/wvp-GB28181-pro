package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

/**
 * PTZ control instructions-PTZ rotation
 *
 */
@Setter
@Getter
@MsgId(id = "9301")
public class J9301 extends Rs {
    // Logical channel number
    private int channel;

    // Direction: 0: Stop; 1: Up; 2: Down; 3: Left; 4: Right
    private int direction;

    // speed：0 ～ 255
    private int speed;

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(channel);
        buffer.writeByte(direction);
        buffer.writeByte(speed);
        return buffer;
    }

    @Override
    public String toString() {
        return "J9301{" +
                "channel=" + channel +
                ", direction=" + direction +
                ", speed=" + speed +
                '}';
    }
}
