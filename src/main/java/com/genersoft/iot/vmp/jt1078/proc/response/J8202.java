package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

/**
 * Temporary location tracking control
 */
@Setter
@Getter
@MsgId(id = "8202")
public class J8202 extends Rs {

    /**
     * Time interval, unit is second, stop tracking when the time interval is 0, no subsequent fields are required to stop tracking.
     */
    private int timeInterval;

    /**
     * Location tracking validity period, in seconds. After receiving the location tracking control message, the terminal will send a location report according to the time interval in the message before the expiration date of the validity period.
     */
    private long validityPeriod;

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeShort((short)(timeInterval & 0xffff));
        if (timeInterval > 0) {
            buffer.writeInt((int) (validityPeriod & 0xffffffffL));
        }
        return buffer;
    }

}
