package com.genersoft.iot.vmp.jt1078.bean;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Shooting command parameters")
public class JTShootingCommand {

    @Schema(description = "channel ID")
    private int chanelId;

    @Schema(description = "0:Stop shooting; 0xFFFF: video recording; others: number of photos taken")
    private int command;

    @Schema(description = "Photo interval/Recording time, unit is seconds(s) ,0 Indicates taking pictures at minimum intervals or recording videos all the time")
    private int time;

    @Schema(description = "1:Save; 0: real-time upload")
    private int save;

    @Schema(description = "resolution: " +
            "0x00:lowest resolution" +
            "0x01:320 x240；" +
            "0x02:640 x480；" +
            "0x03:800 x600；" +
            "0x04:1024 x768；" +
            "0x05:176 x144；" +
            "0x06:352 x288；" +
            "0x07:704 x288；" +
            "0x08:704 x576；" +
            "0xff:highest resolution")
    private int resolvingPower;

    @Schema(description = "image/Video quality: The value range is 1 ~ 10, 1 represents the minimum quality loss, 10 represents the maximum compression ratio")
    private int quality;

    @Schema(description = "brightness, 0 ~ 255")
    private int brightness;

    @Schema(description = "Contrast,0 ~ 127")
    private int contrastRatio;

    @Schema(description = "saturation,0 ~ 127")
    private int saturation;

    @Schema(description = "Chroma,0 ~ 255")
    private int chroma;

    public ByteBuf decode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(chanelId);
        byteBuf.writeShort((short)(command & 0xffff));
        byteBuf.writeShort((short)(time & 0xffff));
        byteBuf.writeByte(save);
        byteBuf.writeByte(resolvingPower);
        byteBuf.writeByte(quality);
        byteBuf.writeByte(brightness);
        byteBuf.writeByte(contrastRatio);
        byteBuf.writeByte(saturation);
        byteBuf.writeByte(chroma);
        return byteBuf;
    }

    @Override
    public String toString() {
        return "JTShootingCommand{" +
                "chanelId=" + chanelId +
                ", command=" + command +
                ", time=" + time +
                ", save=" + save +
                ", resolvingPower=" + resolvingPower +
                ", quality=" + quality +
                ", brightness=" + brightness +
                ", contrastRatio=" + contrastRatio +
                ", saturation=" + saturation +
                ", chroma=" + chroma +
                '}';
    }
}
