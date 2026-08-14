package com.genersoft.iot.vmp.gb28181.bean;

/**
 * Alarm mode
 * @author lin
 * 12 is the phone alarm, 2 is the equipment alarm, 3 is the SMS alarm, 4 is the GPS alarm, 5 is the video alarm, and 6 is the equipment failure alarm.,
 * 7Other alarms;It can be a direct combination such as 12 for telephone alarm or equipment alarm.-
 */
public enum DeviceAlarmMethod {
    // 1Call the police
    Telephone(1),

    // 2Alarm the device
    Device(2),

    // 3Alarm for SMS
    SMS(3),

    // 4Alarm for GPS
    GPS(4),

    // 5Alarm for video
    Video(5),

    // 6Alarm for equipment failure
    DeviceFailure(6),

    // 7Other alarms
    Other(7);

    private final int val;

    DeviceAlarmMethod(int val) {
        this.val=val;
    }

    public int getVal() {
        return val;
    }

    /**
     * Check if type matches
     * @param code
     * @return
     */
    public static DeviceAlarmMethod typeOf(int code) {
        for (DeviceAlarmMethod item : DeviceAlarmMethod.values()) {
            if (code==item.getVal()) {
                return item;
            }
        }
        return null;
    }
}
