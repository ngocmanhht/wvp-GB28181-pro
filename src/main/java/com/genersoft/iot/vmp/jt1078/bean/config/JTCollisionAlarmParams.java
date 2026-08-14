package com.genersoft.iot.vmp.jt1078.bean.config;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

/**
 * Collision alarm parameter settings
 */
@Setter
@Getter
public class JTCollisionAlarmParams implements JTDeviceSubConfig{

    /**
     * Collision time in milliseconds(ms)
     */
    private int collisionAlarmTime;

    /**
     * Collision acceleration unit is 0.1g, setting range is 0~79, default is10
     */
    private int collisionAcceleration;

    @Override
    public ByteBuf encode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byte[] bytes = new byte[2];
        bytes[0] = (byte) (collisionAlarmTime & 0xff);
        bytes[1] = (byte) (collisionAcceleration & 0xff);
        byteBuf.writeBytes(bytes);
        return byteBuf;
    }
}
