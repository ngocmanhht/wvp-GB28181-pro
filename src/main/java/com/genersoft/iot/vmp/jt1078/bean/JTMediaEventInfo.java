package com.genersoft.iot.vmp.jt1078.bean;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Multimedia event information")
public class JTMediaEventInfo {

    @Schema(description = "multimedia data ID")
    private long id;

    @Schema(description = "Multimedia type, 0: image; 1: audio; 2: video")
    private int type;

    @Schema(description = "Multimedia format encoding, 0：JPEG；1：TIF；2：MP3；3：WAV；4：WMV；Other reservations")
    private int code;

    @Schema(description = "Event item coding: 0: Platform issues instructions; 1: Scheduled action; 2: Robbery alarm triggered; 3: Collision rollover alarm triggered; 4: Door open to take photos; 5: Door closed to take photos; 6: Car door changes from open to closed, vehicle speed from less than 20km to over 20km; 7: Fixed distance photo")
    private int eventCode;

    @Schema(description = "channel ID")
    private int channelId;

    @Schema(description = "media data")
    private byte[] mediaData;

    @Schema(description = "Location information reporting")
    private JTPositionBaseInfo positionBaseInfo;


    public static JTMediaEventInfo decode(ByteBuf buf) {
        JTMediaEventInfo jtMediaEventInfo = new JTMediaEventInfo();
        jtMediaEventInfo.setId(buf.readUnsignedInt());
        jtMediaEventInfo.setType(buf.readUnsignedByte());
        jtMediaEventInfo.setCode(buf.readUnsignedByte());
        jtMediaEventInfo.setEventCode(buf.readUnsignedByte());
        jtMediaEventInfo.setChannelId(buf.readUnsignedByte());
        if (buf.readableBytes() > 28) {
            ByteBuf byteBuf = buf.readSlice(28);
            jtMediaEventInfo.setPositionBaseInfo(JTPositionBaseInfo.decode(byteBuf));
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            jtMediaEventInfo.setMediaData(bytes);
        }
        return jtMediaEventInfo;
    }

    @Override
    public String toString() {
        return "JTMediaEventInfo{" +
                "id=" + id +
                ", type=" + type +
                ", code=" + code +
                ", eventCode=" + eventCode +
                ", channelId=" + channelId +
                ", fileSize=" + (mediaData == null ? 0 : mediaData.length) +
                '}';
    }
}
