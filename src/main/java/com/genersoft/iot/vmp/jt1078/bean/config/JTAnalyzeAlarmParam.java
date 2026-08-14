package com.genersoft.iot.vmp.jt1078.bean.config;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

/**
 * Video analysis alarm parameters
 */
@Setter
@Getter
public class JTAnalyzeAlarmParam implements JTDeviceSubConfig{

    /**
     * Number of people on board the vehicle
     */
    private int numberForPeople;


    /**
     * Fatigue level threshold
     */
    private int fatigueThreshold;


    @Override
    public ByteBuf encode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(numberForPeople);
        byteBuf.writeByte(fatigueThreshold);
        return byteBuf;
    }

    public static JTAnalyzeAlarmParam decode(ByteBuf byteBuf) {
        JTAnalyzeAlarmParam analyzeAlarmParam = new JTAnalyzeAlarmParam();
        analyzeAlarmParam.setNumberForPeople(byteBuf.readUnsignedByte());
        analyzeAlarmParam.setFatigueThreshold(byteBuf.readUnsignedByte());
        return analyzeAlarmParam;
    }
}
