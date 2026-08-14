package com.genersoft.iot.vmp.jt1078.bean;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Schema(description = "vertices of polygon area")
public class JTPolygonPoint {

    @Schema(description = "vertex latitude")
    private Double latitude;

    @Schema(description = "vertex longitude")
    private Double longitude;

    public ByteBuf encode(){
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeInt((int) (Math.round((latitude * 1000000)) & 0xffffffffL));
        byteBuf.writeInt((int) (Math.round((longitude * 1000000)) & 0xffffffffL));
        return byteBuf;
    }

}
