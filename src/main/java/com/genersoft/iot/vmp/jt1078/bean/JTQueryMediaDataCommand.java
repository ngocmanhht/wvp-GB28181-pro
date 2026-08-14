package com.genersoft.iot.vmp.jt1078.bean;

import com.genersoft.iot.vmp.jt1078.util.BCDUtil;
import com.genersoft.iot.vmp.utils.DateUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Schema(description = "Store multimedia data")
public class JTQueryMediaDataCommand {

    @Schema(description = "Multimedia type: 0: image; 1: audio; 2: video")
    private int type;

    @Schema(description = "Channel ID, 0 means to retrieve all channels of this media type")
    private int chanelId;

    @Schema(description = "Event item coding: 0: Instructions issued by the platform; 1: Timing action; 2: Robbery alarm triggered; 3: Collision and rollover alarm triggered; others reserved")
    private int event;

    @Schema(description = "start time")
    private String startTime;

    @Schema(description = "end time")
    private String endTime;

    @Schema(description = "Delete flag, 0: reserved; 1: deleted, used in storage multimedia data upload command")
    private Integer delete;


    public ByteBuf decode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(type);
        byteBuf.writeByte(chanelId);
        byteBuf.writeByte(event);
        if (startTime == null) {
            byteBuf.writeBytes(BCDUtil.strToBcd("000000000000"));
        }else {
            byteBuf.writeBytes(BCDUtil.strToBcd(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(startTime)));
        }
        if (endTime == null) {
            byteBuf.writeBytes(BCDUtil.strToBcd("000000000000"));
        }else {
            byteBuf.writeBytes(BCDUtil.strToBcd(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(endTime)));
        }
        if (delete != null) {
            byteBuf.writeByte(delete);
        }
        return byteBuf;
    }

    @Override
    public String toString() {
        return "JTQueryMediaDataCommand{" +
                "type=" + type +
                ", chanelId=" + chanelId +
                ", event=" + event +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", delete='" + delete + '\'' +
                '}';
    }
}
