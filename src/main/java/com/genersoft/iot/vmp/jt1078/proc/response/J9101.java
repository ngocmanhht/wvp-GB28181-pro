package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.Charset;

/**
 * Real-time audio and video transmission request
 *
 * @author QingtaiJiang
 * @date 2023/4/27 18:25
 * @email qingtaij@163.com
 */
@Setter
@Getter
@MsgId(id = "9101")
public class J9101 extends Rs {
    String ip;

    // TCPport
    Integer tcpPort;

    // UDPport
    Integer udpPort;

    // Logical channel number
    Integer channel;

    // data type
    /**
     * 0：Audio and video, 1: video, 2: two-way intercom, 3: monitoring, 4: central broadcast, 5: transparent transmission
     */
    Integer type;

    // Stream type
    /**
     * 0：Main stream, 1: sub stream
     */
    Integer rate;

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
        return buffer;
    }

    @Override
    public String toString() {
        return "J9101{" +
                "ip='" + ip + '\'' +
                ", tcpPort=" + tcpPort +
                ", udpPort=" + udpPort +
                ", channel=" + channel +
                ", type=" + type +
                ", rate=" + rate +
                '}';
    }
}
