package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

/**
 * Query resource list
 *
 * @author QingtaiJiang
 * @date 2023/4/28 10:36
 * @email qingtaij@163.com
 */
@MsgId(id = "9205")
public class J9205 extends Rs {
    // Logical channel number
    private int channelId;

    // Start time YYMMDDHHMMSS, all 0 means no start time
    private String startTime;

    // End time YYMMDDHHMMSS, all 0 means no end time
    private String endTime;

    // Alarm sign
    private final int warnType = 0;

    // Audio and video resource types: 0. Audio and video 1. Audio 2. Video 3. Video or audio and video
    private int mediaType;

    // Stream type: 0. All streams 1. Main stream 2. Sub-stream
    private int streamType = 0;

    // Storage type: 0. All storage 1. Main storage 2. Disaster recovery storage
    private int storageType = 0;

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();

        buffer.writeByte(channelId);
        buffer.writeBytes(ByteBufUtil.decodeHexDump(startTime));
        buffer.writeBytes(ByteBufUtil.decodeHexDump(endTime));
        buffer.writeLong(warnType);
        buffer.writeByte(mediaType);
        buffer.writeByte(streamType);
        buffer.writeByte(storageType);

        return buffer;
    }


    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public void setMediaType(int mediaType) {
        this.mediaType = mediaType;
    }

    public void setStreamType(int streamType) {
        this.streamType = streamType;
    }

    public void setStorageType(int storageType) {
        this.storageType = storageType;
    }

    public int getWarnType() {
        return warnType;
    }

    @Override
    public String toString() {
        return "J9205{" +
                "channelId=" + channelId +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", warnType=" + warnType +
                ", mediaType=" + mediaType +
                ", streamType=" + streamType +
                ", storageType=" + storageType +
                '}';
    }
}
