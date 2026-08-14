package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTShootingCommand;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Camera shooting command immediately
 */
@Setter
@Getter
@MsgId(id = "8801")
public class J8801 extends Rs {

    JTShootingCommand command;

    @Override
    public ByteBuf encode() {
        return command.decode();
    }

}
