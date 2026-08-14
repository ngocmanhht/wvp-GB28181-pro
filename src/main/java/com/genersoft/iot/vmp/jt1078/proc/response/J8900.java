package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

/**
 * Data downlink transparent transmission
 */
@Setter
@Getter
@MsgId(id = "8900")
public class J8900 extends Rs {

    /**
     * Transparent transmission message type, 0x00: GNSS module detailed positioning data, 0X0B: Road transportation certificate IC card information, 0X41: Serial port 1 transparent transmission, 0X42: Serial port 2 transparent transmission, 0XF0 ~ 0XFF: User-defined transparent transmission
     */
    private Integer type;

    /**
     * Transparent message content
     */
    private byte[] content;

    @Override
    public ByteBuf encode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(type);
        byteBuf.writeBytes(content);
        return byteBuf;
    }

}
