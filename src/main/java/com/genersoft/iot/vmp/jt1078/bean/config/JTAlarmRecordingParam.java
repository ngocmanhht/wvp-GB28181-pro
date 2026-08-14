package com.genersoft.iot.vmp.jt1078.bean.config;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

/**
 * Special alarm recording parameters
 */
@Setter
@Getter
public class JTAlarmRecordingParam implements  JTDeviceSubConfig{

    /**
     * Special alarm recording storage threshold, percentage, value Special alarm recording occupies the main memory storage threshold 1 ~ 99, the default value is 20
     */
    private int storageLimit;

    /**
     * Special alarm recording duration, the maximum duration of special alarm recording, unit is minutes(min) ,The default value is 5
     */
    private int duration;

    /**
     * The starting time of the special alarm mark, the recording time marked before the special alarm occurs, unit is minutes( min) ,The default value is 1
     */
    private int startTime;

    @Override
    public ByteBuf encode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(storageLimit);
        byteBuf.writeByte(duration);
        byteBuf.writeByte(startTime);
        return byteBuf;
    }

    public static JTAlarmRecordingParam decode(ByteBuf byteBuf) {
        JTAlarmRecordingParam alarmRecordingParam = new JTAlarmRecordingParam();
        alarmRecordingParam.setStorageLimit(byteBuf.readUnsignedByte());
        alarmRecordingParam.setDuration(byteBuf.readUnsignedByte());
        alarmRecordingParam.setStartTime(byteBuf.readUnsignedByte());
        return alarmRecordingParam;
    }
}
