package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTConfirmationAlarmMessageType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

/**
 * Manually confirm alarm message
 */
@Setter
@Getter
@MsgId(id = "8203")
public class J8203 extends Rs {

    /**
     * Alarm message serial number
     */
    private int alarmPackageNo;
    /**
     * Manual confirmation alarm type
     */
    private JTConfirmationAlarmMessageType alarmMessageType;

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeShort((short)(alarmPackageNo & 0xffff));
        if (alarmMessageType != null) {
            buffer.writeInt((int) (alarmMessageType.encode() & 0xffffffffL));
        }
        return buffer;
    }

}
