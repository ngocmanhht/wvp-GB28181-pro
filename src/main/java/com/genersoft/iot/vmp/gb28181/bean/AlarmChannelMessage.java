package com.genersoft.iot.vmp.gb28181.bean;


import lombok.Data;

/**
 * Distribute alarm messages through redis
 */
@Data
public class AlarmChannelMessage {
    /**
     * Channel national standard number
     */
    private String gbId;

    /**
     * Alarm number
     */
    private Integer alarmSn;

    /**
     * Alarm type
     */
    private Integer alarmType;

    /**
     * Alarm description
     */
    private String alarmDescription;
}
