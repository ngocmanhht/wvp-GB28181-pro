package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.gb28181.utils.XmlUtil;
import com.genersoft.iot.vmp.service.bean.AlarmType;
import com.genersoft.iot.vmp.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dom4j.Element;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.*;

/**
 * @author lin
 */
@Schema(description = "Alarm notification")
@Data
public class DeviceAlarmNotify {

    @Schema(description = "National standard number of equipment")
    private String deviceId;

	@Schema(description = "Device name")
	private String deviceName;

    /**
     * channelId
     */
    @Schema(description = "The national standard number of the channel")
    private String channelId;

    /**
     * Alarm level, 1 is the first level alarm, 2 is the second level alarm, 3 is the third level alarm, 4 is the fourth level alarm
     */
    @Schema(description = "Alarm level, 1 is the first level alarm, 2 is the second level alarm, 3 is the third level alarm, 4 is the fourth level alarm")
    private String alarmPriority;

    @Schema(description = "Alarm level, 1 is the first level alarm, 2 is the second level alarm, 3 is the third level alarm, 4 is the fourth level alarm")
    private String alarmPriorityDescription;

    /**
     * Alarm mode, 1 is phone alarm, 2 is equipment alarm, 3 is SMS alarm, 4 is GPS alarm, 5 is video alarm, 6 is equipment failure alarm,
     * 7Other alarms;It can be a direct combination such as 12 for telephone alarm or equipment alarm.-
     */
    @Schema(description = "Alarm mode, 1 is phone alarm, 2 is equipment alarm, 3 is SMS alarm, 4 is GPS alarm, 5 is video alarm, 6 is equipment failure alarm,\n" +
            "\t * 7Other alarms;It can be a direct combination such as 12 for telephone alarm or equipment alarm.")
    private Integer alarmMethod;


    private String alarmMethodDescription;


    /**
     * Alarm time
     */
    @Schema(description = "Alarm time")
    private String alarmTime;

    /**
     * Alarm content description
     */
    @Schema(description = "Alarm content description")
    private String alarmDescription;

    /**
     * longitude
     */
    @Schema(description = "longitude")
    private double longitude;

    /**
     * Latitude
     */
    @Schema(description = "Latitude")
    private double latitude;

    /**
     * Alarm type,
     * When the alarm mode is 2, if AlarmType is not carried, it is the default alarm device alarm.,
     * The value of AlarmType carried and the corresponding alarm type are as follows::
     * 1-Video loss alarm;
     * 2-Equipment anti-tamper alarm;
     * 3-Storage device disk full alarm;
     * 4-Equipment high temperature alarm;
     * 5-Equipment low temperature alarm。
     * When the alarm mode is 5, the values are as follows:
     * 1-Manual video alarm;
     * 2-Moving target detection alarm;
     * 3-Remaining object detection alarm;
     * 4-Object removal detection alarm;
     * 5-Tripwire detection alarm;
     * 6-Intrusion detection alarm;
     * 7-Retrograde detection alarm;
     * 8-Wandering detection alarm;
     * 9-Traffic statistics alarm;
     * 10-Density detection alarm;
     * 11-Video anomaly detection and alarm;
     * 12-Fast moving alarm。
     * When the alarm mode is 6, the value is:
     * 1-Storage device disk failure alarm;
     * 2-Storage device fan failure alarm。
     */
    @Schema(description = "Alarm type")
    private Integer alarmType;

    @Schema(description = "Event type, portable when intrusion detection alarm occurs")
    private Integer eventType;

	public AlarmType getAlarmTypeEnum() {
		if (alarmType == null) {
			return null;
		}

		if (alarmMethod == DeviceAlarmMethod.Device.getVal()) {
			// 2Alarm the device,
			// When the alarm mode is 2,
			// If AlarmType is not carried, it is the default alarm device alarm.,
			// The value of AlarmType carried and the corresponding alarm type are as follows::
			// 1-Video loss alarm;2-Equipment anti-tamper alarm;3-Storage device disk full alarm;4-Equipment high temperature alarm;5-Equipment low temperature alarm
			switch (alarmType) {
				case 1:
					return AlarmType.VideoLoss;
				case 2:
					return AlarmType.DeviceTamper;
				case 3:
					return AlarmType.StorageFull;
				case 4:
					return AlarmType.DeviceHighTemperature;
				case 5:
					return AlarmType.DeviceLowTemperature;
			}
		}
		if (alarmMethod == DeviceAlarmMethod.Video.getVal()) {
			// 5Alarm for video
			// Alarm mode is 5 hours,
			// The values are as follows:
			// 1-Manual video alarm;2-Moving target detection alarm;3-Remaining object detection alarm;4-Object removal detection alarm;5-Tripwire detection alarm;
			// 6-Intrusion detection alarm;7-Retrograde detection alarm;8-Wandering detection alarm;9-Traffic statistics alarm;
			// 10-Density detection alarm;11-Video anomaly detection and alarm;12-Fast moving alarm。
			switch (alarmType) {
				case 1:
					return AlarmType.ManualVideo;
				case 2:
					return AlarmType.MotionDetection;
				case 3:
					return AlarmType.LeftObjectDetection;
				case 4:
					return AlarmType.ObjectRemovalDetection;
				case 5:
					return AlarmType.TripwireDetection;
				case 6:
					return AlarmType.IntrusionDetection;
				case 7:
					return AlarmType.ReverseDetection;
				case 8:
					return AlarmType.LoiteringDetection;
				case 9:
					return AlarmType.FlowStatistics;
				case 10:
					return AlarmType.DensityDetection;
				case 11:
					return AlarmType.VideoAbnormal;
				case 12:
					return AlarmType.RapidMovement;
			}
		}
		if (alarmMethod == DeviceAlarmMethod.DeviceFailure.getVal()) {
			switch (alarmType) {
				case 1:
					return AlarmType.StorageFault;
				case 2:
					return AlarmType.StorageFanFault;
			}
		}
		return null;
	}

	@Schema(description = "Alarm type description")
    private String alarmTypeDescription;

    @Schema(description = "creation time")
    private String createTime;


	public static DeviceAlarmNotify fromXml(Element rootElement) {
		Element deviceIdElement = rootElement.element("DeviceID");
		String channelId = deviceIdElement.getText();

		DeviceAlarmNotify deviceAlarm = new DeviceAlarmNotify();
		deviceAlarm.setCreateTime(DateUtil.getNow());
		deviceAlarm.setChannelId(channelId);
		deviceAlarm.setAlarmPriority(getText(rootElement, "AlarmPriority"));
		deviceAlarm.setAlarmMethod(getInteger(rootElement, "AlarmMethod"));
		String alarmTime = XmlUtil.getText(rootElement, "AlarmTime");
		deviceAlarm.setAlarmTime(DateUtil.ISO8601Toyyyy_MM_dd_HH_mm_ss(alarmTime));
		deviceAlarm.setAlarmDescription(getText(rootElement, "AlarmDescription"));

		Double longitude = getDouble(rootElement, "Longitude");
		deviceAlarm.setLongitude(longitude != null ? longitude: 0.00D);
		Double latitude = getDouble(rootElement, "Latitude");
		deviceAlarm.setLatitude(latitude != null ? latitude: 0.00D);
		deviceAlarm.setAlarmType(getInteger(rootElement, "AlarmType"));
		Element info = rootElement.element("Info");
		if (info != null) {
			deviceAlarm.setAlarmType(getInteger(info, "AlarmType"));
			Element alarmTypeParam = info.element("AlarmTypeParam");
			if (alarmTypeParam != null) {
				deviceAlarm.setAlarmDescription(alarmTypeParam.elementText("AlarmDescription"));
			}
		}
		deviceAlarm.setCreateTime(DateUtil.getNow());
		return deviceAlarm;
	}

}
