package com.genersoft.iot.vmp.jt1078.bean.config;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

/**
 * GNSS Positioning mode
 */
@Setter
@Getter
public class JTGnssPositioningMode implements JTDeviceSubConfig{

    /**
     * GPS Positioning true: on, false: off
     */
    private boolean gps;
    /**
     * Beidou positioning true: on, false: off
     */
    private boolean beidou;
    /**
     * GLONASSPositioning true: on, false: off
     */
    private boolean glonass;
    /**
     * GaLiLeoPositioning true: on, false: off
     */
    private boolean gaLiLeo;

    @Override
    public ByteBuf encode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byte[] bytes = new byte[1];
        bytes[0] = 0;
        if (gps) {
            bytes[0] = (byte)(bytes[0] | 1);
        }
        if (beidou) {
            bytes[0] = (byte)(bytes[0] | 2);
        }
        if (glonass) {
            bytes[0] = (byte)(bytes[0] | 4);
        }
        if (gaLiLeo) {
            bytes[0] = (byte)(bytes[0] | 8);
        }
        byteBuf.writeBytes(bytes);
        return byteBuf;
    }
}
