/**
 * Device control command API interface
 *
 * @author lawrencehj
 * @date 2021February 1
 */

package com.genersoft.iot.vmp.gb28181.controller;

import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

@Tag(name  = "National standard equipment control")
@Slf4j
@RestController
@RequestMapping("/api/device/control")
public class DeviceControl {

    @Autowired
    private IDeviceService deviceService;


	@Operation(summary = "remote start", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
    @GetMapping("/teleboot/{deviceId}")
    public void teleBootApi(@PathVariable String deviceId) {
        if (log.isDebugEnabled()) {
            log.debug("Device remote start API call");
        }
        Device device = deviceService.getDeviceByDeviceId(deviceId);
		Assert.notNull(device, "Device does not exist");
		deviceService.teleboot(device);
    }


	@Operation(summary = "Video control", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "channelId", description = "Channel national standard number", required = true)
	@Parameter(name = "recordCmdStr", description = "Command, optional values: Record (manual recording), StopRecord (stop manual recording)）", required = true)
    @GetMapping("/record")
    public DeferredResult<WVPResult<String>> recordApi(String deviceId, String recordCmdStr, String channelId) {
        if (log.isDebugEnabled()) {
            log.debug("start/Stop recording API call");
        }
        Device device = deviceService.getDeviceByDeviceId(deviceId);
		Assert.notNull(device, "Device does not exist");
		DeferredResult<WVPResult<String>> deferredResult = new DeferredResult<>();

		deviceService.record(device, channelId, recordCmdStr, (code, msg, data) -> {
			deferredResult.setResult(new WVPResult<>(code, msg, data));
		});
		deferredResult.onTimeout(() -> {
			log.warn("[start/Stop recording] The operation timed out and the device did not return a response command., {}", deviceId);
			deferredResult.setResult(WVPResult.fail(ErrorCode.ERROR100.getCode(), "The operation timed out and the device did not respond."));
		});
		return deferredResult;
	}

	@Operation(summary = "arm/disarm", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "guardCmd", description = "Command, optional values: SetGuard (arm), ResetGuard (disarm)）", required = true)
	@GetMapping("/guard")
	public DeferredResult<WVPResult<String>> guardApi(String deviceId, String guardCmd) {
		log.info("[arm/disarm] APIcall, deviceId: {}, guardCmd: {}", deviceId, guardCmd);
		Device device = deviceService.getDeviceByDeviceId(deviceId);
		Assert.notNull(device, "Device does not exist");
		DeferredResult<WVPResult<String>> result = new DeferredResult<>();
		deviceService.guard(device, guardCmd, (code, msg, data) -> {
			result.setResult(new WVPResult<>(code, msg, data));
		});
		result.onTimeout(() -> {
			log.warn("[arm/disarm] The operation timed out and the device did not return a response command., {}", deviceId);
			result.setResult(WVPResult.fail(ErrorCode.ERROR100.getCode(), "The operation timed out and the device did not respond."));
		});
		return result;
	}

	@Operation(summary = "Alarm reset", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "channelId", description = "Channel national standard number", required = true)
	@Parameter(name = "alarmMethod", description = "alarm mode, alarm mode conditions(Optional),The value 0 is all, 1 is phone alarm, 2 is device alarm, 3 is text message alarm, 4 is\n" +
			"GPSAlarm, 5 is video alarm, 6 is equipment failure alarm, 7 is other alarm;It can be a direct combination such as 12 for telephone alarm or equipment alarm.")
	@Parameter(name = "alarmType", description = "Alarm type, " +
			"Alarm type。" +
			"When the alarm mode is 2, if AlarmType is not carried, it is the default alarm device alarm.," +
			"The value of AlarmType carried and the corresponding alarm type are as follows::" +
			"1-Video loss alarm;2-Equipment anti-tamper alarm;3-Storage device disk full alarm;4-Equipment high temperature alarm;5-Equipment low temperature alarm。" +
			"When the alarm mode is 5, the values are as follows:" +
			"1-Manual video alarm;2-Moving target detection alarm;3-Remaining object detection alarm;4-Object removal detection alarm;5-Tripwire detection alarm;" +
			"6-Intrusion detection alarm;7-Retrograde detection alarm;8-Wandering detection alarm;9-Traffic statistics alarm;10-Density detection alarm;" +
			"11-Video anomaly detection and alarm;12-Fast moving alarm。" +
			"When the alarm mode is 6, the values are as follows:" +
			"1-Storage device disk failure alarm;2-Storage device fan failure alarm")
	@GetMapping("/reset_alarm")
	public DeferredResult<WVPResult<String>> resetAlarm(String deviceId, String channelId,
																@RequestParam(required = false) String alarmMethod,
																@RequestParam(required = false) String alarmType) {
		log.info("[Alarm reset] deviceId: {}, channelId: {}, alarmMethod: {}, alarmType: {}", deviceId, channelId, alarmMethod, alarmType);
		Device device = deviceService.getDeviceByDeviceId(deviceId);
		Assert.notNull(device, "Device does not exist");
		DeferredResult<WVPResult<String>> result = new DeferredResult<>();
		deviceService.resetAlarm(device, channelId, alarmMethod, alarmType, (code, msg, data) -> {
			result.setResult(new WVPResult<>(code, msg, data));
		});
		result.onTimeout(() -> {
			log.warn("[arm/disarm] The operation timed out and the device did not return a response command., {}", deviceId);
			result.setResult(WVPResult.fail(ErrorCode.ERROR100.getCode(), "The operation timed out and the device did not respond."));
		});
		return result;
	}

	@Operation(summary = "Force keyframe", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "channelId", description = "Channel national standard number")
	@GetMapping("/i_frame")
	public void iFrame(String deviceId, @RequestParam(required = false) String channelId) {
		if (log.isDebugEnabled()) {
			log.debug("Force keyframe API call");
		}
		Device device = deviceService.getDeviceByDeviceId(deviceId);
		Assert.notNull(device, "Device does not exist");
		deviceService.iFrame(device, channelId);
	}

	@Operation(summary = "Watch bit setting", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "channelId", description = "Channel national standard number", required = true)
	@Parameter(name = "enabled", description = "Whether to enable guard position", required = true)
	@Parameter(name = "presetIndex", description = "Call preset number")
	@Parameter(name = "resetTime", description = "Automatic homing time interval unit: seconds")
	@GetMapping("/home_position")
	public DeferredResult<WVPResult<String>> homePositionApi(String deviceId, String channelId, Boolean enabled,
												  @RequestParam(required = false) Integer resetTime,
												  @RequestParam(required = false) Integer presetIndex) {
        if (log.isDebugEnabled()) {
			log.debug("Guard bit setting API call");
		}
		Device device = deviceService.getDeviceByDeviceId(deviceId);
		Assert.notNull(device, "Device does not exist");
		DeferredResult<WVPResult<String>> result = new DeferredResult<>();
		deviceService.homePosition(device, channelId, enabled, resetTime, presetIndex, (code, msg, data) -> {
			result.setResult(new WVPResult<>(code, msg, data));
		});
		result.onTimeout(() -> {
			log.warn("[Watch bit setting] The operation timed out and the device did not return a response command., {}", deviceId);
			result.setResult(WVPResult.fail(ErrorCode.ERROR100.getCode(), "The operation timed out and the device did not respond."));
		});
		return result;
	}

	@Operation(summary = "Scroll down to enlarge", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "channelId", description = "Channel national standard number", required = true)
	@Parameter(name = "length", description = "Play window length pixel value", required = true)
	@Parameter(name = "width", description = "Play window width pixel value", required = true)
	@Parameter(name = "midPointX", description = "The horizontal axis coordinate pixel value of the center of the pull box", required = true)
	@Parameter(name = "midPointY", description = "The vertical axis coordinate pixel value of the center of the pull box", required = true)
	@Parameter(name = "lengthX", description = "Frame length in pixels", required = true)
	@Parameter(name = "lengthY", description = "Pull box width pixel value", required = true)
	@GetMapping("drag_zoom/zoom_in")
	public void dragZoomIn(@RequestParam String deviceId, String channelId,
											 @RequestParam int length,
											 @RequestParam int width,
											 @RequestParam int midPointX,
											 @RequestParam int midPointY,
											 @RequestParam int lengthX,
											 @RequestParam int lengthY) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Device pull box zoom API call，deviceId：%s ，channelId：%s ，length：%d ，width：%d ，midPointX：%d ，midPointY：%d ，lengthX：%d ，lengthY：%d",deviceId, channelId, length, width, midPointX, midPointY,lengthX, lengthY));
		}
		Device device = deviceService.getDeviceByDeviceId(deviceId);
		Assert.notNull(device, "Device does not exist");
		deviceService.dragZoomIn(device, channelId, length, width, midPointX, midPointY, lengthX, lengthY);
	}

	@Operation(summary = "Zoom out", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "channelId", description = "Channel national standard number")
	@Parameter(name = "length", description = "Play window length pixel value", required = true)
	@Parameter(name = "width", description = "Play window width pixel value", required = true)
	@Parameter(name = "midPointX", description = "The horizontal axis coordinate pixel value of the center of the pull box", required = true)
	@Parameter(name = "midPointY", description = "The vertical axis coordinate pixel value of the center of the pull box", required = true)
	@Parameter(name = "lengthX", description = "Frame length in pixels", required = true)
	@Parameter(name = "lengthY", description = "Pull box width pixel value", required = true)
	@GetMapping("/drag_zoom/zoom_out")
	public void dragZoomOut(@RequestParam String deviceId,
											  @RequestParam(required = false) String channelId,
											  @RequestParam int length,
											  @RequestParam int width,
											  @RequestParam int midPointX,
											  @RequestParam int midPointY,
											  @RequestParam int lengthX,
											  @RequestParam int lengthY){

		if (log.isDebugEnabled()) {
			log.debug(String.format("Device pull box shrink API call，deviceId：%s ，channelId：%s ，length：%d ，width：%d ，midPointX：%d ，midPointY：%d ，lengthX：%d ，lengthY：%d",deviceId, channelId, length, width, midPointX, midPointY,lengthX, lengthY));
		}
		Device device = deviceService.getDeviceByDeviceId(deviceId);
		Assert.notNull(device, "Device does not exist");
		deviceService.dragZoomOut(device, channelId, length, width, midPointX, midPointY, lengthX,lengthY);
	}
}
