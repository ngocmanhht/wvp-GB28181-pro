package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import lombok.Getter;
import lombok.Setter;

/**
 * File upload control
 *
 */
@Setter
@Getter
@MsgId(id = "9207")
public class J9207 extends Rs {

    // Serial number corresponding to the platform file upload message
    Integer respNo;

    // Control: 0: Pause; 1: Continue; 2: Cancel
    private int control;

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeShort(respNo);
        buffer.writeByte(control);
        return buffer;
    }


    @Override
    public String toString() {
        return "J9207{" +
                "respNo=" + respNo +
                ", control=" + control +
                '}';
    }
}
