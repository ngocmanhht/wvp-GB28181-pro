package com.genersoft.iot.vmp.jt1078.bean.config;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Video alarm flag
 */
public class JTVideoAlarmBit implements JTDeviceSubConfig{

    /**
     * Video signal loss alarm
     */
    private boolean lossSignal;
    /**
     * Video signal blocking alarm
     */
    private boolean occlusionSignal;
    /**
     * Storage unit failure alarm
     */
    private boolean storageFault;
    /**
     * Other video equipment failure alarms
     */
    private boolean otherDeviceFailure;
    /**
     * Bus overcrowding alarm
     */
    private boolean overcrowding;
    /**
     * Abnormal driving behavior alarm
     */
    private boolean abnormalDriving;
    /**
     * Special alarm: The recording reaches the storage threshold alarm.
     */
    private boolean storageLimit;

    public boolean isLossSignal() {
        return lossSignal;
    }

    public void setLossSignal(boolean lossSignal) {
        this.lossSignal = lossSignal;
    }

    public boolean isOcclusionSignal() {
        return occlusionSignal;
    }

    public void setOcclusionSignal(boolean occlusionSignal) {
        this.occlusionSignal = occlusionSignal;
    }

    public boolean isStorageFault() {
        return storageFault;
    }

    public void setStorageFault(boolean storageFault) {
        this.storageFault = storageFault;
    }

    public boolean isOtherDeviceFailure() {
        return otherDeviceFailure;
    }

    public void setOtherDeviceFailure(boolean otherDeviceFailure) {
        this.otherDeviceFailure = otherDeviceFailure;
    }

    public boolean isOvercrowding() {
        return overcrowding;
    }

    public void setOvercrowding(boolean overcrowding) {
        this.overcrowding = overcrowding;
    }

    public boolean isAbnormalDriving() {
        return abnormalDriving;
    }

    public void setAbnormalDriving(boolean abnormalDriving) {
        this.abnormalDriving = abnormalDriving;
    }

    public boolean isStorageLimit() {
        return storageLimit;
    }

    public void setStorageLimit(boolean storageLimit) {
        this.storageLimit = storageLimit;
    }

    @Override
    public ByteBuf encode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byte content = 0;
        if (lossSignal) {
            content = content |= 1;
        }
        if (occlusionSignal) {
            content = content |= (1 << 1);
        }
        if (storageFault) {
            content = content |= (1 << 2);
        }
        if (otherDeviceFailure) {
            content = content |= (1 << 3);
        }
        if (overcrowding) {
            content = content |= (1 << 4);
        }
        if (abnormalDriving) {
            content = content |= (1 << 5);
        }
        if (storageLimit) {
            content = content |= (1 << 6);
        }
        byteBuf.writeByte(content);
        byteBuf.writeByte(0);
        byteBuf.writeByte(0);
        byteBuf.writeByte(0);
        return byteBuf;
    }

    public static JTVideoAlarmBit decode(ByteBuf byteBuf) {
        JTVideoAlarmBit videoAlarmBit = new JTVideoAlarmBit();
        byte content = byteBuf.readByte();
        videoAlarmBit.setLossSignal((content & 1) == 1);
        videoAlarmBit.setOcclusionSignal((content >>> 1 & 1) == 1);
        videoAlarmBit.setStorageFault((content >>> 2 & 1) == 1);
        videoAlarmBit.setOtherDeviceFailure((content >>> 3 & 1) == 1);
        videoAlarmBit.setOvercrowding((content >>> 4 & 1) == 1);
        videoAlarmBit.setAbnormalDriving((content >>> 5 & 1) == 1);
        videoAlarmBit.setStorageLimit((content >>> 6 & 1) == 1);
        byteBuf.readByte();
        byteBuf.readByte();
        byteBuf.readByte();
        return videoAlarmBit;
    }
}
