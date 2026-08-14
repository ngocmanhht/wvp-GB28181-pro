package com.genersoft.iot.vmp.jt1078.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Schema(description = "Manual confirmation alarm type")
public class JTConfirmationAlarmMessageType {
    @Schema(description = "Confirm emergency alarm")
    private boolean urgent;
    @Schema(description = "Confirm danger warning")
    private boolean alarmDangerous;
    @Schema(description = "Confirm entry and exit area alarm")
    private boolean alarmRegion;
    @Schema(description = "Confirm entry and exit route alarm")
    private boolean alarmRoute;
    @Schema(description = "Confirm that the road segment travel time is insufficient/Alarm if too long")
    private boolean alarmTravelTime;
    @Schema(description = "Confirm vehicle illegal ignition alarm")
    private boolean alarmIllegalIgnition;
    @Schema(description = "Confirm vehicle illegal displacement alarm")
    private boolean alarmIllegalDisplacement;

    public long encode(){
        long result = 0L;
        if (urgent) {
            result |= 0x01;
        }
        if (alarmDangerous) {
            result |= (0x01 << 3);
        }
        if (alarmRegion) {
            result |= (0x01 << 20);
        }
        if (alarmRoute) {
            result |= (0x01 << 21);
        }
        if (alarmTravelTime) {
            result |= (0x01 << 22);
        }
        if (alarmIllegalIgnition) {
            result |= (0x01 << 27);
        }
        if (alarmIllegalDisplacement) {
            result |= (0x01 << 28);
        }
        return result;
    }


    @Override
    public String toString() {
        return "JConfirmationAlarmMessageType{" +
                "urgent=" + urgent +
                ", alarmDangerous=" + alarmDangerous +
                ", alarmRegion=" + alarmRegion +
                ", alarmRoute=" + alarmRoute +
                ", alarmTravelTime=" + alarmTravelTime +
                ", alarmIllegalIgnition=" + alarmIllegalIgnition +
                ", alarmIllegalDisplacement=" + alarmIllegalDisplacement +
                '}';
    }
}
