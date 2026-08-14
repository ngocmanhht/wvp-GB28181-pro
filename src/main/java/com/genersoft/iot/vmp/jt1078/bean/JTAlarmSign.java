package com.genersoft.iot.vmp.jt1078.bean;

import com.genersoft.iot.vmp.jt1078.bean.config.JTDeviceSubConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Alarm sign
 */
@Data
@Schema(description = "Alarm sign")
public class JTAlarmSign implements JTDeviceSubConfig {

    @Schema(description = "Emergency alarm, triggered after touching the alarm switch")
    private boolean urgent;
    @Schema(description = "speed alarm")
    private boolean alarmSpeeding;
    @Schema(description = "Fatigue driving alarm")
    private boolean alarmTired;
    @Schema(description = "Dangerous driving behavior warning")
    private boolean alarmDangerous;
    @Schema(description = "GNSS Module failure alarm")
    private boolean alarmGnssFault;
    @Schema(description = "GNSS Alarm if antenna is not connected or cut off")
    private boolean alarmGnssBreak;
    @Schema(description = "GNSS Antenna short circuit alarm")
    private boolean alarmGnssShortCircuited;
    @Schema(description = "Terminal main power undervoltage alarm")
    private boolean alarmUnderVoltage;
    @Schema(description = "Terminal main power failure alarm")
    private boolean alarmPowerOff;
    @Schema(description = "Terminal LCD or display failure alarm")
    private boolean alarmLCD;
    @Schema(description = "TTS Module fault alarm")
    private boolean alarmTtsFault;
    @Schema(description = "Camera failure alarm")
    private boolean alarmCameraFault;
    @Schema(description = "Road Transport Certificate IC Card Module Failure Alarm")
    private boolean alarmIcFault;
    @Schema(description = "speed warning")
    private boolean warningSpeeding;
    @Schema(description = "Fatigue driving warning")
    private boolean warningTired;
    @Schema(description = "Violation driving alarm")
    private boolean alarmWrong;
    @Schema(description = "Tire pressure warning")
    private boolean warningTirePressure;
    @Schema(description = "Right turn blind spot abnormality alarm")
    private boolean alarmBlindZone;
    @Schema(description = "Accumulated driving overtime alarm for the day")
    private boolean alarmDrivingTimeout;
    @Schema(description = "Overtime parking alarm")
    private boolean alarmParkingTimeout;
    @Schema(description = "Alarm for entry and exit areas")
    private boolean alarmRegion;
    @Schema(description = "Alarm for entry and exit routes")
    private boolean alarmRoute;
    @Schema(description = "Insufficient travel time on the road section/Alarm if too long")
    private boolean alarmTravelTime;
    @Schema(description = "Route deviation alarm")
    private boolean alarmRouteDeviation;
    @Schema(description = "Vehicle VSS failure")
    private boolean alarmVSS;
    @Schema(description = "Vehicle oil level abnormality alarm")
    private boolean alarmOil;
    @Schema(description = "vehicle stolen alarm(Via vehicle immobilizer)")
    private boolean alarmStolen;
    @Schema(description = "Illegal vehicle ignition alarm")
    private boolean alarmIllegalIgnition;
    @Schema(description = "Vehicle illegal displacement alarm")
    private boolean alarmIllegalDisplacement;
    @Schema(description = "Collision and rollover alarm")
    private boolean alarmRollover;
    @Schema(description = "Rollover warning")
    private boolean warningRollover;

    public JTAlarmSign() {
    }

    public JTAlarmSign(long alarmSignInt) {
        if (alarmSignInt == 0) {
            return;
        }
        // Parse alarm parameters
        this.urgent = (alarmSignInt & 1) == 1;
        this.alarmSpeeding = (alarmSignInt >>> 1 & 1) == 1;
        this.alarmTired = (alarmSignInt >>> 2 & 1) == 1;
        this.alarmDangerous = (alarmSignInt >>> 3 & 1) == 1;
        this.alarmGnssFault = (alarmSignInt >>> 4 & 1) == 1;
        this.alarmGnssBreak = (alarmSignInt >>> 5 & 1) == 1;
        this.alarmGnssShortCircuited = (alarmSignInt >>> 6 & 1) == 1;
        this.alarmUnderVoltage = (alarmSignInt >>> 7 & 1) == 1;
        this.alarmPowerOff = (alarmSignInt >>> 8 & 1) == 1;
        this.alarmLCD = (alarmSignInt >>> 9 & 1) == 1;
        this.alarmTtsFault = (alarmSignInt >>> 10 & 1) == 1;
        this.alarmCameraFault = (alarmSignInt >>> 11 & 1) == 1;
        this.alarmIcFault = (alarmSignInt >>> 12 & 1) == 1;
        this.warningSpeeding = (alarmSignInt >>> 13 & 1) == 1;
        this.warningTired = (alarmSignInt >>> 14 & 1) == 1;
        this.alarmWrong = (alarmSignInt >>> 15 & 1) == 1;
        this.warningTirePressure = (alarmSignInt >>> 16 & 1) == 1;
        this.alarmBlindZone = (alarmSignInt >>> 17 & 1) == 1;
        this.alarmDrivingTimeout = (alarmSignInt >>> 18 & 1) == 1;
        this.alarmParkingTimeout = (alarmSignInt >>> 19 & 1) == 1;
        this.alarmRegion = (alarmSignInt >>> 20 & 1) == 1;
        this.alarmRoute = (alarmSignInt >>> 21 & 1) == 1;
        this.alarmTravelTime = (alarmSignInt >>> 22 & 1) == 1;
        this.alarmRouteDeviation = (alarmSignInt >>> 23 & 1) == 1;
        this.alarmVSS = (alarmSignInt >>> 24 & 1) == 1;
        this.alarmOil = (alarmSignInt >>> 25 & 1) == 1;
        this.alarmStolen = (alarmSignInt >>> 26 & 1) == 1;
        this.alarmIllegalIgnition = (alarmSignInt >>> 27 & 1) == 1;
        this.alarmIllegalDisplacement = (alarmSignInt >>> 28 & 1) == 1;
        this.alarmRollover = (alarmSignInt >>> 29 & 1) == 1;
        this.warningRollover = (alarmSignInt >>> 30 & 1) == 1;
    }

    public static JTAlarmSign decode(ByteBuf byteBuf) {
        long alarmSignByte = byteBuf.readUnsignedInt();
        return new JTAlarmSign(alarmSignByte);
    }

    @Override
    public ByteBuf encode() {
        // Limited capacity to avoid affecting subsequent space occupation
        ByteBuf byteBuf = Unpooled.buffer();
        int alarmSignValue = 0;
        if (urgent) {
            alarmSignValue = alarmSignValue | 1;
        }
        if (alarmSpeeding) {
            alarmSignValue = alarmSignValue | 1 << 1;
        }
        if (alarmTired) {
            alarmSignValue = alarmSignValue | 1 << 2;
        }
        if (alarmDangerous) {
            alarmSignValue = alarmSignValue | 1 << 3;
        }
        if (alarmGnssFault) {
            alarmSignValue = alarmSignValue | 1 << 4;
        }
        if (alarmGnssBreak) {
            alarmSignValue = alarmSignValue | 1 << 5;
        }
        if (alarmGnssShortCircuited) {
            alarmSignValue = alarmSignValue | 1 << 6;
        }
        if (alarmUnderVoltage) {
            alarmSignValue = alarmSignValue | 1 << 7;
        }
        if (alarmPowerOff) {
            alarmSignValue = alarmSignValue | 1 << 8;
        }
        if (alarmLCD) {
            alarmSignValue = alarmSignValue | 1 << 9;
        }
        if (alarmTtsFault) {
            alarmSignValue = alarmSignValue | 1 << 10;
        }
        if (alarmCameraFault) {
            alarmSignValue = alarmSignValue | 1 << 11;
        }
        if (alarmIcFault) {
            alarmSignValue = alarmSignValue | 1 << 12;
        }
        if (warningSpeeding) {
            alarmSignValue = alarmSignValue | 1 << 13;
        }
        if (warningTired) {
            alarmSignValue = alarmSignValue | 1 << 14;
        }
        if (alarmWrong) {
            alarmSignValue = alarmSignValue | 1 << 15;
        }
        if (warningTirePressure) {
            alarmSignValue = alarmSignValue | 1 << 16;
        }
        if (alarmBlindZone) {
            alarmSignValue = alarmSignValue | 1 << 17;
        }
        if (alarmDrivingTimeout) {
            alarmSignValue = alarmSignValue | 1 << 18;
        }
        if (alarmParkingTimeout) {
            alarmSignValue = alarmSignValue | 1 << 19;
        }
        if (alarmRegion) {
            alarmSignValue = alarmSignValue | 1 << 20;
        }
        if (alarmRoute) {
            alarmSignValue = alarmSignValue | 1 << 21;
        }
        if (alarmTravelTime) {
            alarmSignValue = alarmSignValue | 1 << 22;
        }
        if (alarmRouteDeviation) {
            alarmSignValue = alarmSignValue | 1 << 23;
        }
        if (alarmVSS) {
            alarmSignValue = alarmSignValue | 1 << 24;
        }
        if (alarmOil) {
            alarmSignValue = alarmSignValue | 1 << 25;
        }
        if (alarmStolen) {
            alarmSignValue = alarmSignValue | 1 << 26;
        }
        if (alarmIllegalIgnition) {
            alarmSignValue = alarmSignValue | 1 << 27;
        }
        if (alarmIllegalDisplacement) {
            alarmSignValue = alarmSignValue | 1 << 28;
        }
        if (alarmRollover) {
            alarmSignValue = alarmSignValue | 1 << 29;
        }
        if (warningRollover) {
            alarmSignValue = alarmSignValue | 1 << 30;
        }
        byteBuf.writeInt(alarmSignValue);
        return byteBuf;
    }

    @Override
    public String toString() {
        return "status alarm flag bit：" +
                "\n      emergency alarm：" + (urgent?"open":"close") +
                "\n      speed alarm：" + (alarmSpeeding?"open":"close") +
                "\n      Fatigue driving alarm：" + (alarmTired?"open":"close") +
                "\n      Dangerous driving behavior warning：" + (alarmDangerous?"open":"close") +
                "\n      GNSS Module failure alarm：" + (alarmGnssFault?"open":"close") +
                "\n      GNSS Alarm if antenna is not connected or cut off：" + (alarmGnssBreak?"open":"close") +
                "\n      GNSS Antenna short circuit alarm：" + (alarmGnssShortCircuited?"open":"close") +
                "\n      Terminal main power undervoltage alarm：" + (alarmUnderVoltage?"open":"close") +
                "\n      Terminal main power failure alarm：" + (alarmPowerOff?"open":"close") +
                "\n      Terminal LCD or display failure alarm：" + (alarmLCD?"open":"close") +
                "\n      TTS Module fault alarm：" + (alarmTtsFault?"open":"close") +
                "\n      Camera failure alarm：" + (alarmCameraFault?"open":"close") +
                "\n      Road transport certificate IC card module failure alarm：" + (alarmIcFault?"open":"close") +
                "\n      speed warning：" + (warningSpeeding?"open":"close") +
                "\n      Fatigue driving warning：" + (warningTired?"open":"close") +
                "\n      Violation driving alarm：" + (alarmWrong ?"open":"close") +
                "\n      Tire pressure warning：" + (warningTirePressure?"open":"close") +
                "\n      Right turn blind spot abnormality alarm：" + (alarmBlindZone?"open":"close") +
                "\n      Accumulated driving overtime alarm for the day：" + (alarmDrivingTimeout?"open":"close") +
                "\n      Overtime parking alarm：" + (alarmParkingTimeout?"open":"close") +
                "\n      Alarm for entry and exit areas：" + (alarmRegion?"open":"close") +
                "\n      Alarm for entry and exit routes：" + (alarmRoute?"open":"close") +
                "\n      Insufficient travel time on the road section/Alarm if too long：" + (alarmTravelTime?"open":"close") +
                "\n      Route deviation alarm：" + (alarmRouteDeviation?"open":"close") +
                "\n      Vehicle VSS failure：" + (alarmVSS?"open":"close") +
                "\n      Vehicle oil level abnormality alarm：" + (alarmOil?"open":"close") +
                "\n      vehicle stolen alarm(Via vehicle immobilizer)：" + (alarmStolen?"open":"close") +
                "\n      Illegal vehicle ignition alarm：" + (alarmIllegalIgnition?"open":"close") +
                "\n      Vehicle illegal displacement alarm：" + (alarmIllegalDisplacement?"open":"close") +
                "\n      Collision and rollover alarm：" + (alarmRollover?"open":"close") +
                "\n      Rollover warning：" + (warningRollover?"open":"close") +
                "\n       ";
    }

}
