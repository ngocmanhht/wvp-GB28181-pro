package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.Charset;

/**
 * Replay request
 *
 * @author QingtaiJiang
 * @date 2023/4/28 10:37
 * @email qingtaij@163.com
 */
@Setter
@Getter
@MsgId(id = "9201")
public class J9201 extends Rs {
    // Server IP address
    private String ip;

    // Real-time video server TCP port number
    private int tcpPort;

    // Real-time video server UDP port number
    private int udpPort;

    // Logical channel number
    private int channel;

    // Audio and video resource types: 0. Audio and video 1. Audio 2. Video 3. Video or audio and video
    private int type;

    // Stream type: 0. All streams 1. Main stream 2. Sub-stream(If this channel only transmits audio, this field is set to0)
    private int rate;

    // Storage type: 0. All storage 1. Main storage 2. Disaster recovery storage"
    private int storageType;

    // Playback mode: 0. Normal playback 1. Fast forward playback 2. Key frame fast rewind playback 3. Key frame playback 4. Single frame upload
    private int playbackType;

    // Fast forward or rewind multiple: 0. Invalid 1.1 times 2.2 times 3.4 times 4.8 times 5.16 times (When the playback control is 1 and 2, the content of this field is valid, otherwise it is set0)
    private int playbackSpeed;

    // Start time YYMMDDHHMMSS, when the playback mode is 4, this field indicates the single frame upload time
    private String startTime;

    // End time YYMMDDHHMMSS. When the playback mode is 4, this field is invalid. If it is 0, it means always playing back.
    private String endTime;

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(ip.getBytes(Charset.forName("GBK")).length);
        buffer.writeCharSequence(ip, Charset.forName("GBK"));
        buffer.writeShort(tcpPort);
        buffer.writeShort(udpPort);
        buffer.writeByte(channel);
        buffer.writeByte(type);
        buffer.writeByte(rate);
        buffer.writeByte(storageType);
        buffer.writeByte(playbackType);
        buffer.writeByte(playbackSpeed);
        buffer.writeBytes(ByteBufUtil.decodeHexDump(startTime));
        buffer.writeBytes(ByteBufUtil.decodeHexDump(endTime));
        return buffer;
    }

    @Override
    public String toString() {
        return "J9201{" +
                "ip='" + ip + '\'' +
                ", tcpPort=" + tcpPort +
                ", udpPort=" + udpPort +
                ", channel=" + channel +
                ", type=" + type +
                ", rate=" + rate +
                ", storageType=" + storageType +
                ", playbackType=" + playbackType +
                ", playbackSpeed=" + playbackSpeed +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                '}';
    }
}
