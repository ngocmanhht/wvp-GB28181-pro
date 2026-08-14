package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

/**
 * Audio and video real-time transmission control
 *
 * @author QingtaiJiang
 * @date 2023/4/27 18:49
 * @email qingtaij@163.com
 */
@Setter
@Getter
@MsgId(id = "9102")
public class J9102 extends Rs {

    // Channel number
    Integer channel;

    // control instructions
    /**
     * 0：Close audio and video transmission command；
     * 1：Switch code stream(Add pause and resume)；
     * 2：Pause the sending of all streams on this channel；
     * 3：Resumes the sending of the stream before the pause, which is the same as the stream type before the pause.；
     * 4：Turn off two-way intercom
     */
    Integer command;

    // data type
    /**
     * 0：Close the audio and video data related to this channel；
     * 1：Only close the audio related to the channel and keep the channel
     * Related videos；
     * 2：Only close the video related to the channel and keep the channel
     * Related audio
     */
    Integer closeType;

    // data type
    /**
     * 0：main stream；
     * 1：substream
     */
    Integer streamType;

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(channel);
        buffer.writeByte(command);
        buffer.writeByte(closeType);
        buffer.writeByte(streamType);
        return buffer;
    }


    @Override
    public String toString() {
        return "J9102{" +
                "channel=" + channel +
                ", command=" + command +
                ", closeType=" + closeType +
                ", streamType=" + streamType +
                '}';
    }
}
