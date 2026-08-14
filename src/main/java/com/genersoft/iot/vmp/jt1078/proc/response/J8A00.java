package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

/**
 * Platform RSA public key
 */
@Setter
@Getter
@MsgId(id = "8A00")
public class J8A00 extends Rs {

    /**
     * Platform RSA public key{e ,n}in e
     */
    private Long e;

    /**
     * RSApublic key{e ,n}in n
     */
    private byte[] n;

    @Override
    public ByteBuf encode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeInt((int) (e & 0xffffffffL));
        byteBuf.writeBytes(n);
        return byteBuf;
    }

}
