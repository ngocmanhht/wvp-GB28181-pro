package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import lombok.Setter;

/**
 * @author QingtaiJiang
 * @date 2023/4/27 18:40
 * @email qingtaij@163.com
 */
@Setter
@MsgId(id = "8100")
public class J8100 extends Rs {
    /**
     * 0 success
     * 1 Vehicle has been registered
     * 2 The vehicle does not exist in the database
     * 3 The terminal has been registered
     * 4 The terminal does not exist in the database
     */
    public static final Integer SUCCESS = 0;
    public static final Integer FAIL = 4;

    Integer respNo;
    Integer result;
    String code;

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeShort(respNo);
        buffer.writeByte(result);
        buffer.writeCharSequence(code, CharsetUtil.UTF_8);
        return buffer;
    }

}
