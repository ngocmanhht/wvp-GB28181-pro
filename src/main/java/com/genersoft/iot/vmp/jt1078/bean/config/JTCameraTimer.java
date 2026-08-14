package com.genersoft.iot.vmp.jt1078.bean.config;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

/**
 * Timing photo control
 */
@Setter
@Getter
public class JTCameraTimer implements JTDeviceSubConfig{
    /**
     * Camera channel 1 timing photo switch sign
     */
    private boolean switchForChannel1;
    /**
     * Camera channel 2 timing photo switch sign
     */
    private boolean switchForChannel2;
    /**
     * Camera channel 3 timing photo switch sign
     */
    private boolean switchForChannel3;
    /**
     * Camera channel 4 timing photo switch sign
     */
    private boolean switchForChannel4;
    /**
     * Camera channel 5 timing photo switch sign
     */
    private boolean switchForChannel5;

    /**
     * Camera channel 1 scheduled photo storage flag, true: upload, false: storage
     */
    private boolean storageFlagsForChannel1;

    /**
     * Camera channel 2 scheduled photo storage flag true: upload, false: storage
     */
    private boolean storageFlagsForChannel2;

    /**
     * Camera channel 3 scheduled photo storage flag true: upload, false: storage
     */
    private boolean storageFlagsForChannel3;

    /**
     * Camera channel 4 scheduled photo storage flag true: upload, false: storage
     */
    private boolean storageFlagsForChannel4;

    /**
     * Camera channel 5 scheduled photo storage flag true: upload, false: storage
     */
    private boolean storageFlagsForChannel5;

    /**
     * Timing time unit, true: minutes, false: seconds, when the value is less than 5s, the terminal processes it as 5s
     */
    private boolean timeUnit;

    /**
     * scheduled time interval
     */
    private Integer timeInterval;

    @Override
    public ByteBuf encode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byte[] bytes = new byte[4];
        bytes[0] = 0;
        if (switchForChannel1) {
            bytes[0] = (byte)(bytes[0] | 1);
        }
        if (switchForChannel2) {
            bytes[0] = (byte)(bytes[0] | 2);
        }
        if (switchForChannel3) {
            bytes[0] = (byte)(bytes[0] | 4);
        }
        if (switchForChannel4) {
            bytes[0] = (byte)(bytes[0] | 8);
        }
        if (switchForChannel5) {
            bytes[0] = (byte)(bytes[0] | 16);
        }
        bytes[1] = 0;
        if (storageFlagsForChannel1) {
            bytes[1] = (byte)(bytes[1] | 1);
        }
        if (storageFlagsForChannel2) {
            bytes[1] = (byte)(bytes[1] | 2);
        }
        if (storageFlagsForChannel3) {
            bytes[1] = (byte)(bytes[1] | 4);
        }
        if (storageFlagsForChannel4) {
            bytes[1] = (byte)(bytes[1] | 8);
        }
        if (storageFlagsForChannel5) {
            bytes[1] = (byte)(bytes[1] | 16);
        }
        bytes[3] = (byte)(timeInterval & 0xfe);
        if (timeUnit) {
            bytes[3] = (byte)(bytes[3] | 1);
        }
        byteBuf.writeBytes(bytes);
        return byteBuf;
    }
}
