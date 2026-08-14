package com.genersoft.iot.vmp.jt1078.bean.config;

import com.genersoft.iot.vmp.jt1078.util.BCDUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

/**
 * Terminal sleep wakeup mode setting
 */
@Setter
@Getter
public class JTAwakenParam implements JTDeviceSubConfig{

    /**
     * sleep wake mode-Conditional wake-up
     */
    private boolean wakeUpModeByCondition;

    /**
     * sleep wake mode-Wake up regularly
     */
    private boolean wakeUpModeByTime;

    /**
     * sleep wake mode-Manual wake up
     */
    private boolean wakeUpModeByManual;

    /**
     * wake condition type-emergency alarm
     */
    private boolean wakeUpConditionsByAlarm;

    /**
     * wake condition type-Collision and rollover alarm
     */
    private boolean wakeUpConditionsByRollover;

    /**
     * wake condition type-vehicle door opening
     */
    private boolean wakeUpConditionsByOpenTheDoor;

    /**
     * Scheduled wake-up day settings-Monday
     */
    private boolean awakeningDayForMonday;

    /**
     * Scheduled wake-up day settings-Tuesday
     */
    private boolean awakeningDayForTuesday;

    /**
     * Scheduled wake-up day settings-wednesday
     */
    private boolean awakeningDayForWednesday;

    /**
     * Scheduled wake-up day settings-Thursday
     */
    private boolean awakeningDayForThursday;

    /**
     * Scheduled wake-up day settings-Friday
     */
    private boolean awakeningDayForFriday;

    /**
     * Scheduled wake-up day settings-Saturday
     */
    private boolean awakeningDayForSaturday;

    /**
     * Scheduled wake-up day settings-Sunday
     */
    private boolean awakeningDayForSunday;

    /**
     * Daily wake up time-Enable time period1
     */
    private boolean time1Enable;

    /**
     * Daily wake up time-Time period 1 start time
     */
    private String time1StartTime;

    /**
     * Daily wake up time-Time period 1 end time
     */
    private String time1EndTime;

    /**
     * Daily wake up time-Enable time period2
     */
    private boolean time2Enable;

    /**
     * Daily wake up time-Time period 2 start time
     */
    private String time2StartTime;

    /**
     * Daily wake up time-Time period 2 end time
     */
    private String time2EndTime;

    /**
     * Daily wake up time-Enable time period3
     */
    private boolean time3Enable;

    /**
     * Daily wake up time-Time period 3 start time
     */
    private String time3StartTime;

    /**
     * Daily wake up time-Time period 3 end time
     */
    private String time3EndTime;

    /**
     * Daily wake up time-Enable time period4
     */
    private boolean time4Enable;

    /**
     * Daily wake up time-Time period 4 start time
     */
    private String time4StartTime;

    /**
     * Daily wake up time-Time period 4 end time
     */
    private String time4EndTime;

    @Override
    public ByteBuf encode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byte wakeUpTypeByte = 0;
        byte wakeUpConditionsByte = 0;
        byte wakeDayByte = 0;
        if (wakeUpModeByCondition) {
            wakeUpTypeByte = (byte)(wakeUpTypeByte | 1);
        }
        if (wakeUpModeByTime) {
            wakeUpTypeByte = (byte)(wakeUpTypeByte | (1 << 1));
        }
        if (wakeUpModeByManual) {
            wakeUpTypeByte = (byte)(wakeUpTypeByte | (1 << 2));
        }
        byteBuf.writeByte(wakeUpTypeByte);
        if (wakeUpConditionsByAlarm) {
            wakeUpConditionsByte = (byte)(wakeUpConditionsByte | 1);
        }
        if (wakeUpConditionsByRollover) {
            wakeUpConditionsByte = (byte)(wakeUpConditionsByte | (1 << 1));
        }
        if (wakeUpConditionsByOpenTheDoor) {
            wakeUpConditionsByte = (byte)(wakeUpConditionsByte | (1 << 2));
        }
        byteBuf.writeByte(wakeUpConditionsByte);
        if (awakeningDayForMonday) {
            wakeDayByte = (byte)(wakeDayByte | 1);
        }
        if (awakeningDayForTuesday) {
            wakeDayByte = (byte)(wakeDayByte | (1 << 1));
        }
        if (awakeningDayForWednesday) {
            wakeDayByte = (byte)(wakeDayByte | (1 << 2));
        }
        if (awakeningDayForThursday) {
            wakeDayByte = (byte)(wakeDayByte | (1 << 3));
        }
        if (awakeningDayForFriday) {
            wakeDayByte = (byte)(wakeDayByte | (1 << 4));
        }
        if (awakeningDayForSaturday) {
            wakeDayByte = (byte)(wakeDayByte | (1 << 5));
        }
        if (awakeningDayForSunday) {
            wakeDayByte = (byte)(wakeDayByte | (1 << 6));
        }
        byteBuf.writeByte(wakeDayByte);
        byte enableByte = 0;
        if (time1Enable) {
            enableByte = (byte)(enableByte | 1);
        }
        if (time2Enable) {
            enableByte = (byte)(enableByte | (1 << 1));
        }
        if (time3Enable) {
            enableByte = (byte)(enableByte | (1 << 2));
        }
        if (time4Enable) {
            enableByte = (byte)(enableByte | (1 << 3));
        }
        byteBuf.writeByte(enableByte);
        byteBuf.writeBytes(transportTime(time1StartTime));
        byteBuf.writeBytes(transportTime(time1EndTime));
        byteBuf.writeBytes(transportTime(time2StartTime));
        byteBuf.writeBytes(transportTime(time2EndTime));
        byteBuf.writeBytes(transportTime(time3StartTime));
        byteBuf.writeBytes(transportTime(time3EndTime));
        byteBuf.writeBytes(transportTime(time4StartTime));
        byteBuf.writeBytes(transportTime(time4EndTime));
        return byteBuf;
    }

    private byte[] transportTime(String time) {
        return BCDUtil.strToBcd(time.replace(":", ""));
    }

    public static JTAwakenParam decode(ByteBuf byteBuf) {
        JTAwakenParam awakenParam = new JTAwakenParam();
        short wakeUpTypeByte = byteBuf.readUnsignedByte();
        awakenParam.wakeUpModeByCondition = ((wakeUpTypeByte & 1) == 1);
        awakenParam.wakeUpModeByTime = ((wakeUpTypeByte >>> 1 & 1) == 1);
        awakenParam.wakeUpModeByManual = ((wakeUpTypeByte >>> 2 & 1) == 1);

        short wakeUpConditionsByte = byteBuf.readUnsignedByte();
        awakenParam.wakeUpConditionsByAlarm = ((wakeUpConditionsByte & 1) == 1);
        awakenParam.wakeUpConditionsByRollover = ((wakeUpConditionsByte >>> 1 & 1) == 1);
        awakenParam.wakeUpConditionsByOpenTheDoor = ((wakeUpConditionsByte >>> 2 & 1) == 1);

        short wakeDayByte = byteBuf.readUnsignedByte();
        awakenParam.awakeningDayForMonday = ((wakeDayByte & 1) == 1);
        awakenParam.awakeningDayForTuesday = ((wakeDayByte >>> 1 & 1) == 1);
        awakenParam.awakeningDayForWednesday = ((wakeDayByte >>> 2 & 1) == 1);
        awakenParam.awakeningDayForThursday = ((wakeDayByte >>> 3 & 1) == 1);
        awakenParam.awakeningDayForFriday = ((wakeDayByte >>> 4 & 1) == 1);
        awakenParam.awakeningDayForSaturday = ((wakeDayByte >>> 5 & 1) == 1);
        awakenParam.awakeningDayForSunday = ((wakeDayByte >>> 6 & 1) == 1);
        short enableByte = byteBuf.readUnsignedByte();
        awakenParam.time1Enable = ((enableByte & 1) == 1);
        awakenParam.time2Enable = ((enableByte >>> 1 & 1) == 1);
        awakenParam.time3Enable = ((enableByte >>> 2 & 1) == 1);
        awakenParam.time4Enable = ((enableByte >>> 3 & 1) == 1);
        byte[] timeBytes = new byte[2];
        byteBuf.readBytes(timeBytes);
        awakenParam.time1StartTime = transportTime(timeBytes);
        byteBuf.readBytes(timeBytes);
        awakenParam.time1EndTime = transportTime(timeBytes);

        byteBuf.readBytes(timeBytes);
        awakenParam.time2StartTime = transportTime(timeBytes);
        byteBuf.readBytes(timeBytes);
        awakenParam.time2EndTime = transportTime(timeBytes);

        byteBuf.readBytes(timeBytes);
        awakenParam.time3StartTime = transportTime(timeBytes);
        byteBuf.readBytes(timeBytes);
        awakenParam.time3EndTime = transportTime(timeBytes);

        byteBuf.readBytes(timeBytes);
        awakenParam.time4StartTime = transportTime(timeBytes);
        byteBuf.readBytes(timeBytes);
        awakenParam.time4EndTime = transportTime(timeBytes);
        return awakenParam;
    }

    private static String transportTime(byte[] timeBytes) {
        String time1Str = BCDUtil.transform(timeBytes);
        return time1Str.replace(time1Str.substring(0, 2), time1Str.substring(0, 2) + ":");
    }
}
