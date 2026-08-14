package com.genersoft.iot.vmp.gb28181.controller;


import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.service.IPTZService;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

@Tag(name  = "Front-end device control")
@Slf4j
@RestController
@RequestMapping("/api/front-end")
public class PtzController {

	@Autowired
	private SIPCommander cmder;

	@Autowired
	private IDeviceService deviceService;

	@Autowired
	private IPTZService ptzService;

	@Autowired
	private DeferredResultHolder resultHolder;

	@Operation(summary = "Common front-end control commands(Refer to the national standard document A.3.1 instruction format)", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "channelId", description = "Channel national standard number", required = true)
	@Parameter(name = "cmdCode", description = "Script code(Corresponding to the bytes in the national standard document instruction format4)", required = true)
	@Parameter(name = "parameter1", description = "Data one(Corresponds to byte 5 in the national standard document instruction format, range0-255)", required = true)
	@Parameter(name = "parameter2", description = "Data two(Corresponds to byte 6 in the national standard document instruction format, range0-255)", required = true)
	@Parameter(name = "combindCode2", description = "Combination code two(Corresponds to byte 7 in the national standard document instruction format, range0-15)", required = true)
	@GetMapping("/common/{deviceId}/{channelId}")
	public void frontEndCommand(@PathVariable String deviceId,@PathVariable String channelId,Integer cmdCode, Integer parameter1, Integer parameter2, Integer combindCode2){

		if (log.isDebugEnabled()) {
			log.debug(String.format("Device PTZ control API call，deviceId：%s ，channelId：%s ，cmdCode：%d parameter1：%d parameter2：%d",deviceId, channelId, cmdCode, parameter1, parameter2));
		}

		if (parameter1 == null || parameter1 < 0 || parameter1 > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "parameter1 for 0-255number");
		}
		if (parameter2 == null || parameter2 < 0 || parameter2 > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "parameter2 for 0-255number");
		}
		if (combindCode2 == null || combindCode2 < 0 || combindCode2 > 15) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "combindCode2 for 0-15number");
		}

		Device device = deviceService.getDeviceByDeviceId(deviceId);

		Assert.notNull(device, "Equipment[" + deviceId + "]does not exist");

		ptzService.frontEndCommand(device, channelId, cmdCode, parameter1, parameter2, combindCode2);
	}

	@Operation(summary = "PTZ control", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "channelId", description = "Channel national standard number", required = true)
	@Parameter(name = "command", description = "control instructions, allowable values: left, right, up, down, upleft, upright, downleft, downright, zoomin, zoomout, stop", required = true)
	@Parameter(name = "horizonSpeed", description = "horizontal speed(0-255)", required = true)
	@Parameter(name = "verticalSpeed", description = "vertical speed(0-255)", required = true)
	@Parameter(name = "zoomSpeed", description = "Zoom speed(0-15)", required = true)
	@GetMapping("/ptz/{deviceId}/{channelId}")
	public void ptz(@PathVariable String deviceId,@PathVariable String channelId, String command, Integer horizonSpeed, Integer verticalSpeed, Integer zoomSpeed){

		if (log.isDebugEnabled()) {
			log.debug(String.format("Device PTZ control API call，deviceId：%s ，channelId：%s ，command：%s ，horizonSpeed：%d ，verticalSpeed：%d ，zoomSpeed：%d",deviceId, channelId, command, horizonSpeed, verticalSpeed, zoomSpeed));
		}
		if (horizonSpeed == null) {
			horizonSpeed = 100;
		}else if (horizonSpeed < 0 || horizonSpeed > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "horizonSpeed for 0-255number");
		}
		if (verticalSpeed == null) {
			verticalSpeed = 100;
		}else if (verticalSpeed < 0 || verticalSpeed > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "verticalSpeed for 0-255number");
		}
		if (zoomSpeed == null) {
			zoomSpeed = 16;
		}else if (zoomSpeed < 0 || zoomSpeed > 15) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "zoomSpeed for 0-15number");
		}

		int cmdCode = 0;
		switch (command){
			case "left":
				cmdCode = 2;
				break;
			case "right":
				cmdCode = 1;
				break;
			case "up":
				cmdCode = 8;
				break;
			case "down":
				cmdCode = 4;
				break;
			case "upleft":
				cmdCode = 10;
				break;
			case "upright":
				cmdCode = 9;
				break;
			case "downleft":
				cmdCode = 6;
				break;
			case "downright":
				cmdCode = 5;
				break;
			case "zoomin":
				cmdCode = 16;
				break;
			case "zoomout":
				cmdCode = 32;
				break;
			case "stop":
				horizonSpeed = 0;
				verticalSpeed = 0;
				zoomSpeed = 0;
				break;
			default:
				break;
		}
		frontEndCommand(deviceId, channelId, cmdCode, horizonSpeed, verticalSpeed, zoomSpeed);
	}


	@Operation(summary = "Aperture control", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "channelId", description = "Channel national standard number", required = true)
	@Parameter(name = "command", description = "control instructions, allowable values: in, out, stop", required = true)
	@Parameter(name = "speed", description = "aperture speed(0-255)", required = true)
	@GetMapping("/fi/iris/{deviceId}/{channelId}")
	public void iris(@PathVariable String deviceId,@PathVariable String channelId, String command, Integer speed){

		if (log.isDebugEnabled()) {
			log.debug("Device aperture control API call，deviceId：{} ，channelId：{} ，command：{} ，speed：{} ",deviceId, channelId, command, speed);
		}

		if (speed == null) {
			speed = 100;
		}else if (speed < 0 || speed > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "speed for 0-255number");
		}

		int cmdCode = 0x40;
		switch (command){
			case "in":
				cmdCode = 0x44;
				break;
			case "out":
				cmdCode = 0x48;
				break;
			case "stop":
				speed = 0;
				break;
			default:
				break;
		}
		frontEndCommand(deviceId, channelId, cmdCode, 0, speed, 0);
	}

	@Operation(summary = "focus control", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "channelId", description = "Channel national standard number", required = true)
	@Parameter(name = "command", description = "control instructions, allowable values: near, far, stop", required = true)
	@Parameter(name = "speed", description = "focus speed(0-255)", required = true)
	@GetMapping("/fi/focus/{deviceId}/{channelId}")
	public void focus(@PathVariable String deviceId,@PathVariable String channelId, String command, Integer speed){

		if (log.isDebugEnabled()) {
			log.debug("Device focus control API call，deviceId：{} ，channelId：{} ，command：{} ，speed：{} ",deviceId, channelId, command, speed);
		}

		if (speed == null) {
			speed = 100;
		}else if (speed < 0 || speed > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "speed for 0-255number");
		}

		int cmdCode = 0x40;
		switch (command){
			case "near":
				cmdCode = 0x42;
				break;
			case "far":
				cmdCode = 0x41;
				break;
			case "stop":
				speed = 0;
				break;
			default:
				break;
		}
		frontEndCommand(deviceId, channelId, cmdCode, speed, 0, 0);
	}

	@Operation(summary = "Query preset position", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "channelId", description = "Channel national standard number", required = true)
	@GetMapping("/preset/query/{deviceId}/{channelId}")
	public DeferredResult<WVPResult<Object>> queryPreset(@PathVariable String deviceId, @PathVariable String channelId) {
		if (log.isDebugEnabled()) {
			log.debug("Device preset position query API call");
		}
		Device device = deviceService.getDeviceByDeviceId(deviceId);
		Assert.notNull(device, "Device does not exist");
		DeferredResult<WVPResult<Object>> deferredResult = new DeferredResult<> (3 * 1000L);
		deviceService.queryPreset(device, channelId, (code, msg, data) -> {
			deferredResult.setResult(new WVPResult<>(code, msg, data));
		});

		deferredResult.onTimeout(()->{
			log.warn("[Get device preset position] timeout, {}", device.getDeviceId());
			deferredResult.setResult(WVPResult.fail(ErrorCode.ERROR100.getCode(), "timeout"));
		});
		return deferredResult;
	}

	@Operation(summary = "Preset command-Set preset position", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "channelId", description = "Channel national standard number", required = true)
	@Parameter(name = "presetId", description = "Preset number(1-255)", required = true)
	@GetMapping("/preset/add/{deviceId}/{channelId}")
	public void addPreset(@PathVariable String deviceId, @PathVariable String channelId, Integer presetId) {
		if (presetId == null || presetId < 1 || presetId > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "The preset number must be1-255numbers between");
		}
		frontEndCommand(deviceId, channelId, 0x81, 1, presetId, 0);
	}

	@Operation(summary = "Preset command-Call preset position", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "channelId", description = "Channel national standard number", required = true)
	@Parameter(name = "presetId", description = "Preset number(1-255)", required = true)
	@GetMapping("/preset/call/{deviceId}/{channelId}")
	public void callPreset(@PathVariable String deviceId, @PathVariable String channelId, Integer presetId) {
		if (presetId == null || presetId < 1 || presetId > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "The preset number must be1-255numbers between");
		}
		frontEndCommand(deviceId, channelId, 0x82, 1, presetId, 0);
	}

	@Operation(summary = "Preset command-Delete preset position", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "channelId", description = "Channel national standard number", required = true)
	@Parameter(name = "presetId", description = "Preset number(1-255)", required = true)
	@GetMapping("/preset/delete/{deviceId}/{channelId}")
	public void deletePreset(@PathVariable String deviceId, @PathVariable String channelId, Integer presetId) {
		if (presetId == null || presetId < 1 || presetId > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "The preset number must be1-255numbers between");
		}
		frontEndCommand(deviceId, channelId, 0x83, 1, presetId, 0);
	}

	@Operation(summary = "cruise command-Join a cruise spot", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "channelId", description = "Channel national standard number", required = true)
	@Parameter(name = "cruiseId", description = "Cruise group number(0-255)", required = true)
	@Parameter(name = "presetId", description = "Preset number(1-255)", required = true)
	@GetMapping("/cruise/point/add/{deviceId}/{channelId}")
	public void addCruisePoint(@PathVariable String deviceId, @PathVariable String channelId, Integer cruiseId, Integer presetId) {
		if (presetId == null || cruiseId == null || presetId < 1 || presetId > 255 || cruiseId < 0 || cruiseId > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "The number must be1-255numbers between");
		}
		frontEndCommand(deviceId, channelId, 0x84, cruiseId, presetId, 0);
	}

	@Operation(summary = "cruise command-Delete a cruise point", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "channelId", description = "Channel national standard number", required = true)
	@Parameter(name = "cruiseId", description = "Cruise group number(1-255)", required = true)
	@Parameter(name = "presetId", description = "Preset number(0-255, When 0, delete the entire cruise)", required = true)
	@GetMapping("/cruise/point/delete/{deviceId}/{channelId}")
	public void deleteCruisePoint(@PathVariable String deviceId, @PathVariable String channelId, Integer cruiseId, Integer presetId) {
		if (presetId == null || presetId < 0 || presetId > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "The preset number must be0-255The number between, if it is 0, delete the entire cruise");
		}
		if (cruiseId == null || cruiseId < 0 || cruiseId > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "The cruise group number must be0-255numbers between");
		}
		frontEndCommand(deviceId, channelId, 0x85, cruiseId, presetId, 0);
	}

	@Operation(summary = "cruise command-Set cruise speed", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "channelId", description = "Channel national standard number", required = true)
	@Parameter(name = "cruiseId", description = "Cruise group number(0-255)", required = true)
	@Parameter(name = "speed", description = "cruising speed(1-4095)", required = true)
	@GetMapping("/cruise/speed/{deviceId}/{channelId}")
	public void setCruiseSpeed(@PathVariable String deviceId, @PathVariable String channelId, Integer cruiseId, Integer speed) {
		if (cruiseId == null || cruiseId < 0 || cruiseId > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "The cruise group number must be0-255numbers between");
		}
		if (speed == null || speed < 1 || speed > 4095) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "The cruising speed must be1-4095numbers between");
		}
		int parameter2 = speed & 0xFF;
		int combindCode2 =  speed >> 8;
		frontEndCommand(deviceId, channelId, 0x86, cruiseId, parameter2, combindCode2);
	}

	@Operation(summary = "cruise command-Set cruise stop time", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "channelId", description = "Channel national standard number", required = true)
	@Parameter(name = "cruiseId", description = "Cruise group number", required = true)
	@Parameter(name = "time", description = "Cruise stop time(1-4095)", required = true)
	@GetMapping("/cruise/time/{deviceId}/{channelId}")
	public void setCruiseTime(@PathVariable String deviceId, @PathVariable String channelId, Integer cruiseId, Integer time) {
		if (cruiseId == null || cruiseId < 0 || cruiseId > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "The cruise group number must be0-255numbers between");
		}
		if (time == null || time < 1 || time > 4095) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "Cruise stop time must be1-4095numbers between");
		}
		int parameter2 = time & 0xFF;
		int combindCode2 =  time >> 8;
		frontEndCommand(deviceId, channelId, 0x87, cruiseId, parameter2, combindCode2);
	}

	@Operation(summary = "cruise command-start cruise", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "channelId", description = "Channel national standard number", required = true)
	@Parameter(name = "cruiseId", description = "Cruise group number)", required = true)
	@GetMapping("/cruise/start/{deviceId}/{channelId}")
	public void startCruise(@PathVariable String deviceId, @PathVariable String channelId, Integer cruiseId) {
		if (cruiseId == null || cruiseId < 0 || cruiseId > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "The cruise group number must be0-255numbers between");
		}
		frontEndCommand(deviceId, channelId, 0x88, cruiseId, 0, 0);
	}

	@Operation(summary = "cruise command-Stop cruising", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "channelId", description = "Channel national standard number", required = true)
	@Parameter(name = "cruiseId", description = "Cruise group number", required = true)
	@GetMapping("/cruise/stop/{deviceId}/{channelId}")
	public void stopCruise(@PathVariable String deviceId, @PathVariable String channelId, Integer cruiseId) {
		if (cruiseId == null || cruiseId < 0 || cruiseId > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "The cruise group number must be0-255numbers between");
		}
		frontEndCommand(deviceId, channelId, 0, 0, 0, 0);
	}

	@Operation(summary = "scan command-Start automatic scan", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "channelId", description = "Channel national standard number", required = true)
	@Parameter(name = "scanId", description = "Scan group number(0-255)", required = true)
	@GetMapping("/scan/start/{deviceId}/{channelId}")
	public void startScan(@PathVariable String deviceId, @PathVariable String channelId, Integer scanId) {
		if (scanId == null || scanId < 0 || scanId > 255 ) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "The scan group number must be0-255numbers between");
		}
		frontEndCommand(deviceId, channelId, 0x89, scanId, 0, 0);
	}

	@Operation(summary = "scan command-Stop automatic scanning", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "channelId", description = "Channel national standard number", required = true)
	@Parameter(name = "scanId", description = "Scan group number(0-255)", required = true)
	@GetMapping("/scan/stop/{deviceId}/{channelId}")
	public void stopScan(@PathVariable String deviceId, @PathVariable String channelId, Integer scanId) {
		if (scanId == null || scanId < 0 || scanId > 255 ) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "The scan group number must be0-255numbers between");
		}
		frontEndCommand(deviceId, channelId, 0, 0, 0, 0);
	}

	@Operation(summary = "scan command-Set up automatic scanning of the left border", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "channelId", description = "Channel national standard number", required = true)
	@Parameter(name = "scanId", description = "Scan group number(0-255)", required = true)
	@GetMapping("/scan/set/left/{deviceId}/{channelId}")
	public void setScanLeft(@PathVariable String deviceId, @PathVariable String channelId, Integer scanId) {
		if (scanId == null || scanId < 0 || scanId > 255 ) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "The scan group number must be0-255numbers between");
		}
		frontEndCommand(deviceId, channelId, 0x89, scanId, 1, 0);
	}

	@Operation(summary = "scan command-Set up automatic scanning of the right border", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "channelId", description = "Channel national standard number", required = true)
	@Parameter(name = "scanId", description = "Scan group number(0-255)", required = true)
	@GetMapping("/scan/set/right/{deviceId}/{channelId}")
	public void setScanRight(@PathVariable String deviceId, @PathVariable String channelId, Integer scanId) {
		if (scanId == null || scanId < 0 || scanId > 255 ) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "The scan group number must be0-255numbers between");
		}
		frontEndCommand(deviceId, channelId, 0x89, scanId, 2, 0);
	}


	@Operation(summary = "scan command-Set automatic scan speed", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "channelId", description = "Channel national standard number", required = true)
	@Parameter(name = "scanId", description = "Scan group number(0-255)", required = true)
	@Parameter(name = "speed", description = "Auto scan speed(1-4095)", required = true)
	@GetMapping("/scan/set/speed/{deviceId}/{channelId}")
	public void setScanSpeed(@PathVariable String deviceId, @PathVariable String channelId, Integer scanId, Integer speed) {
		if (scanId == null || scanId < 0 || scanId > 255 ) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "The scan group number must be0-255numbers between");
		}
		if (speed == null || speed < 1 || speed > 4095) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "Auto scan speed must be1-4095numbers between");
		}
		int parameter2 = speed & 0xFF;
		int combindCode2 =  speed >> 8;
		frontEndCommand(deviceId, channelId, 0x8A, scanId, parameter2, combindCode2);
	}


	@Operation(summary = "Auxiliary switch control instructions-Wiper control", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "channelId", description = "Channel national standard number", required = true)
	@Parameter(name = "command", description = "control instructions, allowable values: on, off", required = true)
	@GetMapping("/wiper/{deviceId}/{channelId}")
	public void wiper(@PathVariable String deviceId,@PathVariable String channelId, String command){

		if (log.isDebugEnabled()) {
			log.debug("Auxiliary switch control instructions-Wiper control API call，deviceId：{} ，channelId：{} ，command：{}",deviceId, channelId, command);
		}

		int cmdCode = 0;
		switch (command){
			case "on":
				cmdCode = 0x8c;
				break;
			case "off":
				cmdCode = 0x8d;
				break;
			default:
				break;
		}
		frontEndCommand(deviceId, channelId, cmdCode, 1, 0, 0);
	}

	@Operation(summary = "Auxiliary switch control instructions", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "channelId", description = "Channel national standard number", required = true)
	@Parameter(name = "command", description = "control instructions, allowable values: on, off", required = true)
	@Parameter(name = "switchId", description = "Switch number", required = true)
	@GetMapping("/auxiliary/{deviceId}/{channelId}")
	public void auxiliarySwitch(@PathVariable String deviceId,@PathVariable String channelId, String command, Integer switchId){

		if (log.isDebugEnabled()) {
			log.debug("Auxiliary switch control instructions-Wiper control API call，deviceId：{} ，channelId：{} ，command：{}, switchId: {}",deviceId, channelId, command, switchId);
		}

		int cmdCode = 0;
		switch (command){
			case "on":
				cmdCode = 0x8c;
				break;
			case "off":
				cmdCode = 0x8d;
				break;
			default:
				break;
		}
		frontEndCommand(deviceId, channelId, cmdCode, switchId, 0, 0);
	}
}
