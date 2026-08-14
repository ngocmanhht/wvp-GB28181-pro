package com.genersoft.iot.vmp.jt1078.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Schema(description = "Video alarm reporting")
public class JTVideoAlarm {

    @Schema(description = "Video signal loss alarm channel")
    private List<Integer> videoLossChannels;

    @Schema(description = "Video signal blocking alarm channel")
    private List<Integer> videoOcclusionChannels;

    @Schema(description = "Memory fault alarm status, section 1-12 main memory，12-15 Respectively represent the 1-4 disaster recovery storage device")
    private List<Integer> storageFaultAlarm;

    @Schema(description = "Abnormal driving behavior-fatigue")
    private boolean drivingForFatigue;

    @Schema(description = "Abnormal driving behavior-call")
    private boolean drivingForCall;

    @Schema(description = "Abnormal driving behavior-Smoking")
    private boolean drivingSmoking;

    @Schema(description = "Other video equipment failure")
    private boolean otherDeviceFailure;

    @Schema(description = "Bus overcrowding alarm")
    private boolean overcrowding;

    @Schema(description = "Special alarm: The recording reaches the storage threshold alarm.")
    private boolean specialRecordFull;

    public JTVideoAlarm() {
    }

    public static JTVideoAlarm getInstance(int alarm, int loss, int occlusion, short storageFault, short driving) {
        JTVideoAlarm jtVideoAlarm = new JTVideoAlarm();
        if (alarm == 0) {
            return jtVideoAlarm;
        }
        boolean lossAlarm = (alarm & 1) == 1;
        boolean occlusionAlarm = (alarm >>> 1 & 1) == 1;
        boolean storageFaultAlarm = (alarm >>> 2 & 1) == 1;
        jtVideoAlarm.setOtherDeviceFailure((alarm >>> 3 & 1) == 1);
        jtVideoAlarm.setOvercrowding((alarm >>> 4 & 1) == 1);
        boolean drivingAlarm = (alarm >>> 5 & 1) == 1;
        jtVideoAlarm.setSpecialRecordFull((alarm >>> 6 & 1) == 1);
        if (lossAlarm && loss > 0) {
            List<Integer> videoLossChannels = new ArrayList<>();
            for (int i = 0; i < 32; i++) {
                if ((loss >>> i & 1) == 1 ) {
                    videoLossChannels.add(i);
                }
            }
            jtVideoAlarm.setVideoLossChannels(videoLossChannels);
        }
        if (occlusionAlarm && occlusion > 0) {
            List<Integer> videoOcclusionChannels = new ArrayList<>();
            for (int i = 0; i < 32; i++) {
                if ((occlusion >>> i & 1) == 1) {
                    videoOcclusionChannels.add(i);
                }
            }
            jtVideoAlarm.setVideoOcclusionChannels(videoOcclusionChannels);
        }
        if (storageFaultAlarm && storageFault > 0) {
            List<Integer> storageFaultAlarmContent = new ArrayList<>();
            for (int i = 0; i < 16; i++) {
                if ((storageFault >>> i & 1) == 1) {
                    storageFaultAlarmContent.add(i);
                }
            }
            jtVideoAlarm.setStorageFaultAlarm(storageFaultAlarmContent);
        }
        if (drivingAlarm && driving > 0) {
            jtVideoAlarm.setDrivingForFatigue((driving & 1) == 1 );
            jtVideoAlarm.setDrivingForCall((driving >>> 1 & 1) == 1 );
            jtVideoAlarm.setDrivingSmoking((driving >>> 2 & 1) == 1 );
        }
        return jtVideoAlarm;
    }

}
