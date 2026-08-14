package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTQueryMediaDataCommand;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

/**
 * Recording starts/stop command
 */
@Setter
@Getter
@MsgId(id = "8804")
public class J8804 extends Rs {

    /**
     * Recording command, 0: stop recording; 0X01: start recording
     */
    private int commond;

    /**
     * Recording duration in seconds(s) ,0 Indicates always recording
     */
    private int duration;

    /**
     * Save flag, 0: real-time upload; 1: save
     */
    private int save;

    /**
     * Audio sample rate， 0:8K；1:11K；2:23K；3:32K；Other reservations
     */
    private int samplingRate;

    @Override
    public ByteBuf encode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(commond);
        byteBuf.writeShort((short)(duration & 0xffff));
        byteBuf.writeByte(save);
        byteBuf.writeByte(samplingRate);
        return byteBuf;
    }

}
