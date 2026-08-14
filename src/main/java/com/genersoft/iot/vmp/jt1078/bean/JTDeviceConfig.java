package com.genersoft.iot.vmp.jt1078.bean;

import com.genersoft.iot.vmp.jt1078.bean.common.ConfigAttribute;
import com.genersoft.iot.vmp.jt1078.bean.config.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * JT Terminal parameter settings
 */
@Schema(description = "JTTerminal parameter settings")
@Data
public class JTDeviceConfig {

    @ConfigAttribute(id = 0x1, type="Long", description = "Terminal heartbeat sending interval, unit is seconds(s)")
    private Long keepaliveInterval;

    @ConfigAttribute(id = 0x2, type="Long", description = "TCPMessage response timeout, unit is seconds(s)")
    private Long tcpResponseTimeout;

    @ConfigAttribute(id = 0x3, type="Long", description = "TCPNumber of message retransmissions")
    private Long tcpRetransmissionCount;

    @ConfigAttribute(id = 0x4, type="Long", description = "UDPMessage response timeout, unit is seconds(s)")
    private Long udpResponseTimeout;

    @ConfigAttribute(id = 0x5, type="Long", description = "UDPNumber of message retransmissions")
    private Long udpRetransmissionCount;

    @ConfigAttribute(id = 0x6, type="Long", description = "SMS Message response timeout, unit is seconds(s)")
    private Long smsResponseTimeout;

    @ConfigAttribute(id = 0x7, type="Long", description = "SMS Number of message retransmissions")
    private Long smsRetransmissionCount;

    @ConfigAttribute(id = 0x10, type="String", description = "Main server APN wireless communication dial-up access point. If the network standard is CDMA, this is the PPP dial-up number.")
    private String apnMaster;

    @ConfigAttribute(id = 0x11, type="String", description = "Main server wireless communication dial-up user name")
    private String dialingUsernameMaster;

    @ConfigAttribute(id = 0x12, type="String", description = "Primary server wireless communication dial-up password")
    private String dialingPasswordMaster;

    @ConfigAttribute(id = 0x13, type="String", description = "The main server address IP or domain name, separate the host and port with a colon. Use semicolons to separate multiple servers.")
    private String addressMaster;

    @ConfigAttribute(id = 0x14, type="String", description = "Backup serverAPN")
    private String apnBackup;

    @ConfigAttribute(id = 0x15, type="String", description = "Backup server wireless communication dial-up user name")
    private String dialingUsernameBackup;

    @ConfigAttribute(id = 0x16, type="String", description = "Backup server wireless communication dial-up password")
    private String dialingPasswordBackup;

    @ConfigAttribute(id = 0x17, type="String", description = "Backup server backup address IP or domain name, separate the host and port with a colon. Use semicolons to separate multiple servers.")
    private String addressBackup;

    @ConfigAttribute(id = 0x1a, type="String", description = "Road transport certificate IC card authentication main server IP address or domain name")
    private String addressIcMaster;

    @ConfigAttribute(id = 0x1b, type="Long", description = "Road Transport Certificate IC Card Authentication Main Server TCP Port")
    private Long tcpPortIcMaster;

    @ConfigAttribute(id = 0x1c, type="Long", description = "Road transport certificate IC card authentication main server UDP port")
    private Long udpPortIcMaster;

    @ConfigAttribute(id = 0x1d, type="String", description = "Road transport certificate IC card authentication backup server IP address or domain name, the port is the same as the main server")
    private String addressIcBackup;

    @ConfigAttribute(id = 0x20, type="Long", description = "Location reporting strategy, 0 scheduled reporting 1 fixed interval reporting 2 scheduled and fixed interval reporting")
    private Long locationReportingStrategy;

    @ConfigAttribute(id = 0x21, type="Long", description = "Location reporting scheme, 0 based on ACC status 1 based on login status and ACC status, first determine login status, if logged in then based on ACC status")
    private Long locationReportingPlan;

    @ConfigAttribute(id = 0x22, type="Long", description = "The driver is not logged in reporting time interval, the unit is seconds, the value is greater than zero")
    private Long reportingIntervalOffline;

    @ConfigAttribute(id = 0x23, type="String", description = "Slave server APN# When this value is empty! The terminal should use the same configuration as the master server")
    private String apnSlave;

    @ConfigAttribute(id = 0x24, type="String", description = "Slave server wireless communication dial-up user name # When this value is empty! The terminal should use the same configuration as the main server")
    private String dialingUsernameSlave;

    @ConfigAttribute(id = 0x25, type="String", description = "Slave server wireless communication dial-up password # When this value is empty! The terminal should use the same configuration as the main server")
    private String dialingPasswordSlave;

    @ConfigAttribute(id = 0x26, type="String", description = "Backup address from server IP or domain name! Host and port are separated by colon! Multiple servers are separated by semicolon")
    private String addressSlave;

    @ConfigAttribute(id = 0x27, type="Long", description = "Reporting interval during sleep. The unit is seconds. The value is greater than0")
    private Long reportingIntervalDormancy;

    @ConfigAttribute(id = 0x28, type="Long", description = "Reporting interval during emergency alarm. The unit is seconds. The value is greater than0")
    private Long reportingIntervalEmergencyAlarm;

    @ConfigAttribute(id = 0x29, type="Long", description = "Default time reporting interval in seconds The value is greater than0")
    private Long reportingIntervalDefault;

    @ConfigAttribute(id = 0x2c, type="Long", description = "The default distance reporting interval is in meters and the value is greater than0")
    private Long reportingDistanceDefault;

    @ConfigAttribute(id = 0x2d, type="Long", description = "The driver is not logged in and reports the distance interval. The unit is meters. The value is greater than0")
    private Long reportingDistanceOffline;

    @ConfigAttribute(id = 0x2e, type="Long", description = "Report distance interval when sleeping. The unit is meters. The value is greater than0")
    private Long reportingDistanceDormancy;

    @ConfigAttribute(id = 0x2f, type="Long", description = "Report distance interval during emergency alarm. The unit is meters. The value is greater than0")
    private Long reportingDistanceEmergencyAlarm;

    @ConfigAttribute(id = 0x30, type="Long", description = "Inflection point supplementary transmission angle, the value is less than180")
    private Long inflectionPointAngle;

    @ConfigAttribute(id = 0x31, type="Integer", description = "Electronic fence radius(Illegal displacement threshold) ,Unit is meter(m)")
    private Integer fenceRadius;

    @ConfigAttribute(id = 0x32, type="IllegalDrivingPeriods", description = "The range of illegal driving periods, accurate to the minute")
    private JTIllegalDrivingPeriods illegalDrivingPeriods;

    @ConfigAttribute(id = 0x40, type="String", description = "Monitoring platform phone number")
    private String platformPhoneNumber;

    @ConfigAttribute(id = 0x41, type="String", description = "Reset phone number, you can use this phone number to call the terminal to reset the terminal")
    private String phoneNumberForReset;

    @ConfigAttribute(id = 0x42, type="String", description = "Restore factory settings phone number. You can use this phone number to call the terminal to restore the terminal to factory settings.")
    private String phoneNumberForFactoryReset;

    @ConfigAttribute(id = 0x42, type="String", description = "Monitoring platform SMS phone number")
    private String phoneNumberForSms;

    @ConfigAttribute(id = 0x44, type="String", description = "Receive terminal SMS text alarm number")
    private String phoneNumberForReceiveTextAlarm;

    @ConfigAttribute(id = 0x45, type="Long", description = "Terminal call answering strategy. 0: Answer automatically; 1: Answer automatically when ACC is ON, answer manually when ACC is OFF")
    private Long phoneAnsweringPolicy;

    @ConfigAttribute(id = 0x46, type="Long", description = "The maximum call time per call, in seconds(s) ,0 Calls are not allowed, 0xFFFFFFFF means no restrictions")
    private Long longestCallTimeForPerSession;

    @ConfigAttribute(id = 0x47, type="Long", description = "The longest call time in the month, in seconds(s) ,0 Calling is not allowed, 0xFFFFFFFF means no limit")
    private Long longestCallTimeInMonth;

    @ConfigAttribute(id = 0x48, type="String", description = "Monitor phone number")
    private String phoneNumbersForListen;

    @ConfigAttribute(id = 0x49, type="String", description = "Supervision platform privileged SMS number")
    private String privilegedSMSNumber;

    @ConfigAttribute(id = 0x50, type="AlarmSign", description = "The alarm mask word corresponds to the alarm flag in the location information report message. If the corresponding bit is 1, the corresponding alarm is masked.")
    private JTAlarmSign alarmMaskingWord;

    @ConfigAttribute(id = 0x51, type="AlarmSign", description = "Alarm sending text SMS switch corresponds to the alarm flag in the position information reporting message. If the corresponding bit is 1, the text will be sent when the corresponding alarm occurs. SMS")
    private JTAlarmSign alarmSendsTextSmsSwitch;

    @ConfigAttribute(id = 0x52, type="AlarmSign", description = "The alarm shooting switch corresponds to the alarm flag in the location information report message. If the corresponding bit is 1, the camera will shoot when the corresponding alarm occurs.")
    private JTAlarmSign alarmShootingSwitch;

    @ConfigAttribute(id = 0x53, type="AlarmSign", description = "The alarm shooting storage flag corresponds to the alarm flag in the location information report message. If the corresponding bit is 1, the photo taken during the corresponding alarm will be stored, otherwise it will be uploaded in real time.")
    private JTAlarmSign alarmShootingStorageFlags;

    @ConfigAttribute(id = 0x54, type="AlarmSign", description = "The key flag corresponds to the alarm flag in the location information report message. If the corresponding bit is 1, the corresponding alarm is a key alarm.")
    private JTAlarmSign KeySign;

    @ConfigAttribute(id = 0x55, type="Long", description = "Maximum speed in kilometers per hour(km/h)")
    private Long maxSpeed;

    @ConfigAttribute(id = 0x56, type="Long", description = "Overspeed duration, unit is seconds(s)")
    private Long overSpeedDuration;

    @ConfigAttribute(id = 0x57, type="Long", description = "Continuous driving time threshold in seconds(s)")
    private Long continuousDrivingTimeThreshold;

    @ConfigAttribute(id = 0x58, type="Long", description = "Cumulative driving time threshold for the day in seconds(s)")
    private Long cumulativeDrivingTimeThresholdForTheDay;

    @ConfigAttribute(id = 0x59, type="Long", description = "Minimum rest time in seconds(s)")
    private Long minimumBreakTime;

    @ConfigAttribute(id = 0x5a, type="Long", description = "Maximum parking time in seconds(s)")
    private Long maximumParkingTime;

    @ConfigAttribute(id = 0x5b, type="Integer", description = "Speed warning difference unit is1/10 kilometers per hour(1/10km/h)")
    private Integer overSpeedWarningDifference;

    @ConfigAttribute(id = 0x5c, type="Integer", description = "Difference value of fatigue driving warning in seconds. The value is greater than zero.")
    private Integer drowsyDrivingWarningDifference;

    @ConfigAttribute(id = 0x5d, type="CollisionAlarmParams", description = "Collision alarm parameter settings")
    private JTCollisionAlarmParams collisionAlarmParams;

    @ConfigAttribute(id = 0x5e, type="Integer", description = "Rollover alarm parameter setting: rollover angle, unit is degree, default is30")
    private Integer rolloverAlarm;

    @ConfigAttribute(id = 0x64, type="CameraTimer", description = "Timing photo control")
    private JTCameraTimer cameraTimer;

    @ConfigAttribute(id = 0x70, type="Long", description = "image/Video quality setting range is 1~10, 1 means the best quality")
    private Long qualityForVideo;

    @ConfigAttribute(id = 0x71, type="Long", description = "Brightness, the setting range is0 ~ 255")
    private Long brightness;

    @ConfigAttribute(id = 0x72, type="Long", description = "Contrast, the setting range is0 ~ 127")
    private Long contrastRatio;

    @ConfigAttribute(id = 0x73, type="Long", description = "Saturation, the setting range is0 ~ 127")
    private Long saturation;

    @ConfigAttribute(id = 0x74, type="Long", description = "Chroma, the setting range is0 ~ 255")
    private Long chroma;

    @ConfigAttribute(id = 0x75, type="VideoParam", description = "Audio and video parameter settings")
    private JTVideoParam videoParam;

    @ConfigAttribute(id = 0x76, type="ChannelListParam", description = "Audio and video channel list settings")
    private JTChannelListParam channelListParam;

    @ConfigAttribute(id = 0x77, type="ChannelParam", description = "Individual video channel parameter settings")
    private JTChannelParam channelParam;

    @ConfigAttribute(id = 0x79, type="AlarmRecordingParam", description = "Special alarm recording parameter settings")
    private JTAlarmRecordingParam alarmRecordingParam;

    @ConfigAttribute(id = 0x7a, type="VideoAlarmBit", description = "Video related alarm blocking words")
    private JTVideoAlarmBit videoAlarmBit;

    @ConfigAttribute(id = 0x7b, type="AnalyzeAlarmParam", description = "Image analysis alarm parameter settings")
    private JTAnalyzeAlarmParam analyzeAlarmParam;

    @ConfigAttribute(id = 0x7c, type="AwakenParam", description = "Terminal sleep wakeup mode setting")
    private JTAwakenParam awakenParam;

    @ConfigAttribute(id = 0x80, type="Long", description = "Vehicle odometer reading, unit'1/10km")
    private Long mileage;

    @ConfigAttribute(id = 0x81, type="Integer", description = "The province where the vehicle is locatedID")
    private Integer provincialId;

    @ConfigAttribute(id = 0x82, type="Integer", description = "The city where the vehicle is locatedID")
    private Integer cityId;

    @ConfigAttribute(id = 0x83, type="String", description = "Motor vehicle license plate issued by the public security and traffic management department")
    private String licensePlate;

    @ConfigAttribute(id = 0x84, type="Short", description = "License plate color, value according toJT/T697-7.2014For unlicensed vehicles, please fill in the0")
    private Short licensePlateColor;

    @ConfigAttribute(id = 0x90, type="GnssPositioningMode", description = "GNSSPositioning mode")
    private JTGnssPositioningMode gnssPositioningMode;

    @ConfigAttribute(id = 0x91, type="Short", description = "GNSS Baud rate, defined as follows: 0: 4800, 1:9600,  2：19200, 3:38400,  4:57600, 5:115200")
    private Short gnssBaudRate;

    @ConfigAttribute(id = 0x92, type="Short", description = "GNSS The detailed positioning data output frequency of the module is defined as follows: 0: 500ms, 1:1000ms(Default value),  2：2000ms, 3:3000ms,  4:4000ms")
    private Short gnssOutputFrequency;

        @ConfigAttribute(id = 0x93, type="Long", description = "GNSS Module detailed positioning data collection frequency, unit is second(s) ,Default is1")
    private Long gnssCollectionFrequency;

    @ConfigAttribute(id = 0x94, type="Short", description = "GNSS Module detailed positioning data upload method:, defined as follows: " +
            "0: Local storage, no upload(Default value) , " +
            "1:Upload by time interval,  " +
            "2：Upload by distance interval, " +
            "11:Upload according to the accumulated time, and automatically stop uploading after the transmission time is reached,  " +
            "12:Upload according to the cumulative distance, and automatically stop uploading after reaching the distance,  " +
            "13:Upload according to the cumulative number of items, and automatically stop uploading when the number of uploads is reached.")
    private Short gnssDataUploadMethod;

    @ConfigAttribute(id = 0x95, type="Long", description = "GNSS Module detailed positioning data upload settings:, defined as follows: " +
            "1:Unit is seconds(s),  " +
            "2：Unit is meter(m) , " +
            "11:The unit is seconds(s),  " +
            "12:Unit is meter(m),  " +
            "13:Unit is bar")
    private Long gnssDataUploadMethodUnit;

    @ConfigAttribute(id = 0x100, type="Long", description = "CANBus channel 1 acquisition time interval, unit is milliseconds(ms) ,0 Indicates no collection")
    private Long canCollectionTimeForChannel1;

    @ConfigAttribute(id = 0x101, type="Integer", description = "CANBus channel 1 upload time interval, unit is seconds(s) ,0 Indicates not uploading")
    private Integer canUploadIntervalForChannel1;

    @ConfigAttribute(id = 0x102, type="Long", description = "CANBus channel 2 acquisition time interval, unit is milliseconds(ms) ,0 Indicates no collection")
    private Long canCollectionTimeForChannel2;

    @ConfigAttribute(id = 0x103, type="Integer", description = "CANBus channel 2 upload time interval, unit is seconds(s) ,0 Indicates not uploading")
    private Integer canUploadIntervalForChannel2;

    @Override
    public String toString() {
        return "JTDeviceConfig{" +
                "Terminal heartbeat sending interval： " + keepaliveInterval + "seconds" +
                ", TCPMessage response timeout：" + tcpResponseTimeout + "seconds" +
                ", TCPNumber of message retransmissions： " + tcpRetransmissionCount + "seconds"  +
                ", UDPMessage response timeout： " + udpResponseTimeout +
                ", UDPNumber of message retransmissions： " + udpRetransmissionCount +
                ", SMS Message response timeout： " + smsResponseTimeout  + "seconds"  +
                ", SMS Number of message retransmissions： " + smsRetransmissionCount +
                ", Primary Server APN Wireless Communication Dial-up Access Point： " + apnMaster + '\'' +
                ", Main server wireless communication dial-up user name： " + dialingUsernameMaster  +
                ", Primary server wireless communication dial-up password： " + dialingPasswordMaster  +
                ", Main server address IP or domain name： " + addressMaster  +
                ", Backup serverAPN： " + apnBackup  +
                ", Backup server wireless communication dial-up user name： " + dialingUsernameBackup  +
                ", Backup server wireless communication dial-up password： " + dialingPasswordBackup  +
                ", Backup server backup address IP or domain name： " + addressBackup  +
                ", Road transport certificate IC card authentication main server IP address or domain name： " + addressIcMaster  +
                ", Road Transport Certificate IC Card Authentication Main Server TCP Port： " + tcpPortIcMaster +
                ", Road transport certificate IC card authentication main server UDP port： " + udpPortIcMaster +
                ", Road Transport Certificate IC Card Authentication Backup Server IP Address or Domain Name： " + addressIcBackup  +
                ", Location reporting strategy： " + locationReportingStrategy +
                ", location reporting solution： " + locationReportingPlan +
                ", Driver not logged in reporting time interval： " + reportingIntervalOffline + "seconds"   +
                ", from server APN： " + apnSlave  +
                ", Dial password from server wireless communication： " + dialingUsernameSlave  +
                ", Backup address IP or domain name from server： " + dialingPasswordSlave  +
                ", Backup address IP or domain name from server： " + addressSlave  +
                ", reportingIntervalDormancy： " + reportingIntervalDormancy +
                ", reportingIntervalEmergencyAlarm： " + reportingIntervalEmergencyAlarm +
                ", reportingIntervalDefault： " + reportingIntervalDefault +
                ", reportingDistanceDefault： " + reportingDistanceDefault +
                ", reportingDistanceOffline： " + reportingDistanceOffline +
                ", reportingDistanceDormancy： " + reportingDistanceDormancy +
                ", reportingDistanceEmergencyAlarm： " + reportingDistanceEmergencyAlarm +
                ", inflectionPointAngle： " + inflectionPointAngle +
                ", fenceRadius： " + fenceRadius +
                ", illegalDrivingPeriods： " + illegalDrivingPeriods +
                ", platformPhoneNumber： " + platformPhoneNumber  +
                ", phoneNumberForReset： " + phoneNumberForReset  +
                ", phoneNumberForFactoryReset： " + phoneNumberForFactoryReset  +
                ", phoneNumberForSms： " + phoneNumberForSms  +
                ", phoneNumberForReceiveTextAlarm： " + phoneNumberForReceiveTextAlarm  +
                ", phoneAnsweringPolicy： " + phoneAnsweringPolicy +
                ", longestCallTimeForPerSession： " + longestCallTimeForPerSession +
                ", longestCallTimeInMonth： " + longestCallTimeInMonth +
                ", phoneNumbersForListen： " + phoneNumbersForListen  +
                ", privilegedSMSNumber： " + privilegedSMSNumber  +
                ", alarmMaskingWord： " + alarmMaskingWord +
                ", alarmSendsTextSmsSwitch： " + alarmSendsTextSmsSwitch +
                ", alarmShootingSwitch： " + alarmShootingSwitch +
                ", alarmShootingStorageFlags： " + alarmShootingStorageFlags +
                ", KeySign： " + KeySign +
                ", topSpeed： " + maxSpeed +
                ", overSpeedDuration： " + overSpeedDuration +
                ", continuousDrivingTimeThreshold： " + continuousDrivingTimeThreshold +
                ", cumulativeDrivingTimeThresholdForTheDay： " + cumulativeDrivingTimeThresholdForTheDay +
                ", minimumBreakTime： " + minimumBreakTime +
                ", maximumParkingTime： " + maximumParkingTime +
                ", overSpeedWarningDifference： " + overSpeedWarningDifference +
                ", drowsyDrivingWarningDifference： " + drowsyDrivingWarningDifference +
                ", collisionAlarmParams： " + collisionAlarmParams +
                ", rolloverAlarm： " + rolloverAlarm +
                ", cameraTimer： " + cameraTimer +
                ", qualityForVideo： " + qualityForVideo +
                ", brightness： " + brightness +
                ", contrastRatio： " + contrastRatio +
                ", saturation： " + saturation +
                ", chroma： " + chroma +
                ", mileage： " + mileage +
                ", provincialId： " + provincialId +
                ", cityId： " + cityId +
                ", licensePlate： " + licensePlate  +
                ", licensePlateColor： " + licensePlateColor +
                ", gnssPositioningMode： " + gnssPositioningMode +
                ", gnssBaudRate： " + gnssBaudRate +
                ", gnssOutputFrequency： " + gnssOutputFrequency +
                ", gnssCollectionFrequency： " + gnssCollectionFrequency +
                ", gnssDataUploadMethod： " + gnssDataUploadMethod +
                ", gnssDataUploadMethodUnit： " + gnssDataUploadMethodUnit +
                ", canCollectionTimeForChannel1： " + canCollectionTimeForChannel1 +
                ", canUploadIntervalForChannel1： " + canUploadIntervalForChannel1 +
                ", canCollectionTimeForChannel2： " + canCollectionTimeForChannel2 +
                ", canUploadIntervalForChannel2： " + canUploadIntervalForChannel2 +
                '}';
    }
}
