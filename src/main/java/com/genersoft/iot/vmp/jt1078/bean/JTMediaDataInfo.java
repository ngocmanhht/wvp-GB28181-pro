package com.genersoft.iot.vmp.jt1078.bean;

import io.netty.buffer.ByteBuf;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Schema(description = "Multimedia search item data")
public class JTMediaDataInfo {

    @Schema(description = "multimedia data ID")
    private long id;

    @Schema(description = "Multimedia type, 0: image; 1: audio; 2: video")
    private int type;

    @Schema(description = "Event item coding: 0: Platform issues instructions; 1: Scheduled action; 2: Robbery alarm triggered; 3: Collision rollover alarm triggered; 4: Door open to take photos; 5: Door closed to take photos; 6: Car door changes from open to closed, vehicle speed from less than 20km to over 20km; 7: Fixed distance photo")
    private int eventCode;

    @Schema(description = "channel ID")
    private int channelId;

    @Schema(description = "Report message indicating the starting moment of shooting or recording")
    private JTPositionBaseInfo positionBaseInfo;

    public static JTMediaDataInfo decode(ByteBuf buf) {
        JTMediaDataInfo jtMediaEventInfo = new JTMediaDataInfo();
        jtMediaEventInfo.setId(buf.readUnsignedInt());
        jtMediaEventInfo.setType(buf.readUnsignedByte());
        jtMediaEventInfo.setChannelId(buf.readUnsignedByte());
        jtMediaEventInfo.setEventCode(buf.readUnsignedByte());
        jtMediaEventInfo.setPositionBaseInfo(JTPositionBaseInfo.decode(buf));
        return jtMediaEventInfo;
    }

    @Override
    public String toString() {
        return "JTMediaDataInfo{" +
                "id=" + id +
                ", type=" + type +
                ", eventCode=" + eventCode +
                ", channelId=" + channelId +
                ", positionBaseInfo=" + positionBaseInfo +
                '}';
    }
}
