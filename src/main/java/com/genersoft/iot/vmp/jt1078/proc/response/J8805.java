package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTQueryMediaDataCommand;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * Single stored multimedia data retrieval and upload command
 */
@Setter
@Getter
@MsgId(id = "8805")
public class J8805 extends Rs {

    /**
     * multimedia ID
     */
    private Long mediaId;

    /**
     * Delete flag, 0: reserved; 1: deleted, used in storage multimedia data upload command
     */
    private Integer delete;

    @Override
    public ByteBuf encode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeInt((int) (mediaId & 0xffffffffL));
        byteBuf.writeByte(delete);
        return byteBuf;
    }

}
