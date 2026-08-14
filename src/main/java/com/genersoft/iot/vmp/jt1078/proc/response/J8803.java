package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTQueryMediaDataCommand;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;

/**
 * Store multimedia data upload commands
 */
@Setter
@Getter
@MsgId(id = "8803")
public class J8803 extends Rs {

    JTQueryMediaDataCommand command;

    @Override
    public ByteBuf encode() {
        return command.decode();
    }

}
