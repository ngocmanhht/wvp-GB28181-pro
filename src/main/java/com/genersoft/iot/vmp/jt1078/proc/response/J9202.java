package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

/**
 * Remote video playback control issued by the platform
 *
 * @author QingtaiJiang
 * @date 2023/4/28 10:37
 * @email qingtaij@163.com
 */
@Setter
@Getter
@MsgId(id = "9202")
public class J9202 extends Rs {
    // Logical channel number
    private int channel;

    // Playback control: 0. Start playback 1. Pause playback 2. End playback 3. Fast forward playback 4. Keyframe fast rewind playback 5. Drag playback 6. Keyframe playback
    private int playbackType;

    // Fast forward or rewind multiple: 0. Invalid 1.1 times 2.2 times 3.4 times 4.8 times 5.16 times (When the playback control is 3 and 4, the content of this field is valid, otherwise it is set0)
    private int playbackSpeed;

    // Drag playback position(YYMMDDHHMMSS,This field is valid when the playback control is 5)
    private String playbackTime;

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(channel);
        buffer.writeByte(playbackType);
        buffer.writeByte(playbackSpeed);
        if (playbackType == 5) {
            buffer.writeBytes(ByteBufUtil.decodeHexDump(playbackTime));
        }

        return buffer;
    }

    @Override
    public String toString() {
        return "J9202{" +
                "channel=" + channel +
                ", playbackType=" + playbackType +
                ", playbackSpeed=" + playbackSpeed +
                ", playbackTime='" + playbackTime + '\'' +
                '}';
    }
}
