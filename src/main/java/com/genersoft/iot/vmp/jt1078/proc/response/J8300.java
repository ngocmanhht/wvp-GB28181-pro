package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTTextSign;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.nio.charset.Charset;

/**
 * Text message delivery
 */
@EqualsAndHashCode(callSuper = true)
@Data
@MsgId(id = "8300")
public class J8300 extends Rs {

    /**
     * logo
     */
    private JTTextSign sign;

    /**
     * text type1 = Notification ，2 = service
     */
    private int textType;

    /**
     * text message
     */
    private String content;

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(sign.encode());
        buffer.writeByte(textType);
        buffer.writeCharSequence(content, Charset.forName("GBK"));
        return buffer;
    }
}
