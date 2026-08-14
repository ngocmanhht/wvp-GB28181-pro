package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Query area or line data
 */
@Setter
@Getter
@MsgId(id = "8608")
public class J8608 extends Rs {


    /**
     * Query type, 1 = Query circular area data ,2 = Query rectangular area data ,3 = Query polygon area data ,4 = Query line data
     */
    private int type;


    /**
     * The area or route to be queried ID
     */
    private List<Long> idList;


    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(type);
        if (idList == null || idList.isEmpty()) {
            buffer.writeInt(0);
            return buffer;
        }else {
            buffer.writeInt(idList.size());
        }
        for (Long id : idList) {
            buffer.writeInt((int) (id & 0xffffffffL));
        }
        return buffer;
    }

}
