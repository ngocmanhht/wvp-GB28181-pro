package com.genersoft.iot.vmp.service.bean;


import com.genersoft.iot.vmp.gb28181.bean.DeviceAlarmNotify;
import com.genersoft.iot.vmp.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Alarm information")
public class Alarm {

    @Schema(description = "databaseid")
    private Long id;

    @Schema(description = "Database of associated channelsid")
    private int channelId;

    @Schema(description = "Associated channel national standard number")
    private String channelDeviceId;

    @Schema(description = "National standard name of associated channel")
    private String channelName;

    @Schema(description = "Alarm description")
    private String description;

    @Schema(description = "Alarm snapshot path")
    private String snapPath;

    @Schema(description = "Alarm recording path")
    private String recordPath;

    @Schema(description = "The longitude attached to the alarm")
    private Double longitude;

    @Schema(description = "Latitude attached to alarm")
    private Double latitude;

    @Schema(description = "Alarm category")
    private AlarmType alarmType;

    @Schema(description = "Alarm time")
    private Long alarmTime;

    public static Alarm buildFromDeviceAlarmNotify(DeviceAlarmNotify notify) {
        Alarm alarm = new Alarm();
        alarm.setDescription(notify.getAlarmDescription());
        alarm.setAlarmType(notify.getAlarmTypeEnum());
        alarm.setAlarmTime(DateUtil.yyyy_MM_dd_HH_mm_ssToTimestampMs(notify.getAlarmTime()));
        alarm.setLongitude(notify.getLongitude());
        alarm.setLatitude(notify.getLatitude());
        return alarm;
    }



}
