package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.Charset;

/**
 * File upload command
 *
 */
@Setter
@Getter
@MsgId(id = "9206")
public class J9206 extends Rs {

    // Server address
    private String serverIp;
    // Server port
    private int port;
    // Username
    private String username;
    // Password
    private String password;
    // File upload path
    private String path;
    // Logical channel number
    private int channelId;

    // Start time YYMMDDHHMMSS, all 0 means no start time
    private String startTime;

    // End time YYMMDDHHMMSS, all 0 means no end time
    private String endTime;

    // Alarm sign
    private int alarmSign = 0;

    // Audio and video resource types: 0. Audio and video 1. Audio 2. Video 3. Video or audio and video
    private int mediaType;

    // Stream type: 0. All streams 1. Main stream 2. Sub-stream
    private int streamType = 0;

    // Storage type: 0. All storage 1. Main storage 2. Disaster recovery storage
    private int storageType = 0;

    // Task execution conditions，
    // 1：onlyWI-FI Downloadable below，
    // 2： Downloading is only possible when connected via LAN；
    // 3： WI-FI + LAN Downloadable when connected；
    // 4： only3G/ 4G Downloadable when connected
    // 5： WI-FI + 3G/ 4G Downloadable when connected
    // 6： WI-FI + LAN Downloadable when connected
    // 7： WI-FI + LAN + 3G/ 4G Downloadable when connected
    private int taskConditions = 7;

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();

        buffer.writeByte(serverIp.getBytes(Charset.forName("GBK")).length);
        buffer.writeCharSequence(serverIp, Charset.forName("GBK"));
        buffer.writeShort(port);
        buffer.writeByte(username.getBytes(Charset.forName("GBK")).length);
        buffer.writeCharSequence(username, Charset.forName("GBK"));
        buffer.writeByte(password.getBytes(Charset.forName("GBK")).length);
        buffer.writeCharSequence(password, Charset.forName("GBK"));
        buffer.writeByte(path.getBytes(Charset.forName("GBK")).length);
        buffer.writeCharSequence(path, Charset.forName("GBK"));
        buffer.writeByte(channelId);
        buffer.writeBytes(ByteBufUtil.decodeHexDump(startTime));
        buffer.writeBytes(ByteBufUtil.decodeHexDump(endTime));
        buffer.writeLong(alarmSign);
        buffer.writeByte(mediaType);
        buffer.writeByte(streamType);
        buffer.writeByte(storageType);
        buffer.writeByte(taskConditions);
        return buffer;
    }


    @Override
    public String toString() {
        return "J9206{" +
                "serverIp='" + serverIp + '\'' +
                ", port=" + port +
                ", user='" + username + '\'' +
                ", password='" + password + '\'' +
                ", path='" + path + '\'' +
                ", channelId=" + channelId +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", warnType=" + alarmSign +
                ", mediaType=" + mediaType +
                ", streamType=" + streamType +
                ", storageType=" + storageType +
                ", taskConditions=" + taskConditions +
                '}';
    }
}
