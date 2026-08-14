package com.genersoft.iot.vmp.gb28181.controller;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.SyncStatus;
import com.genersoft.iot.vmp.gb28181.bean.TimeStatistics;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.service.IInviteStreamService;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.service.redisMsg.IRedisRpcService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.ibatis.annotations.Options;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

@Tag(name  = "National standard equipment inquiry", description = "National standard equipment inquiry")
@SuppressWarnings("rawtypes")
@Slf4j
@RestController
@RequestMapping("/api/device/query")
public class DeviceQuery {

	@Autowired
	private IDeviceChannelService deviceChannelService;

	@Autowired
	private IInviteStreamService inviteStreamService;

	@Autowired
	private IDeviceService deviceService;

    @Autowired
	private ISIPCommander cmder;

	@Autowired
	private DeferredResultHolder resultHolder;

	@Autowired
	private UserSetting userSetting;

	@Autowired
	private DynamicTask dynamicTask;

	@Autowired
	private IRedisRpcService redisRpcService;

	@Operation(summary = "Query national standard equipment", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@GetMapping("/devices/{deviceId}")
	public Device devices(@PathVariable String deviceId){

		return deviceService.getDeviceByDeviceId(deviceId);
	}


	@Operation(summary = "Query national standard equipment by page", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "page", description = "Current page", required = true)
	@Parameter(name = "count", description = "Number of queries per page", required = true)
	@Parameter(name = "query", description = "Search", required = false)
	@Parameter(name = "status", description = "Status", required = false)
	@GetMapping("/devices")
	@Options()
	public PageInfo<Device> devices(int page, int count, String query, Boolean status){
		if (ObjectUtils.isEmpty(query)){
			query = null;
		}
		return deviceService.getAll(page, count, query, status);
	}


	@GetMapping("/devices/{deviceId}/channels")
	@Operation(summary = "Paging query channel", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "page", description = "Current page", required = true)
	@Parameter(name = "count", description = "Number of queries per page", required = true)
	@Parameter(name = "query", description = "Query content")
	@Parameter(name = "online", description = "Is online")
	@Parameter(name = "channelType", description = "Equipment/subdirectory-> false/true")
	public PageInfo<DeviceChannel> channels(@PathVariable String deviceId,
											   int page, int count,
											   @RequestParam(required = false) String query,
											   @RequestParam(required = false) Boolean online,
											   @RequestParam(required = false) Boolean channelType) {
		if (ObjectUtils.isEmpty(query)) {
			query = null;
		}

		return deviceChannelService.queryChannelsByDeviceId(deviceId, query, channelType, online, page, count);
	}

	@GetMapping("/streams")
	@Operation(summary = "Paging query for channels with existing streams", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "page", description = "Current page", required = true)
	@Parameter(name = "count", description = "Number of queries per page", required = true)
	@Parameter(name = "query", description = "Query content")
	public PageInfo<DeviceChannel> streamChannels(int page, int count,
												  @RequestParam(required = false) String query) {
		if (ObjectUtils.isEmpty(query)) {
			query = null;
		}

		return deviceChannelService.queryChannels(query, true, null, null, true, page, count);
	}

	@Operation(summary = "Sync device channels", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@GetMapping("/devices/{deviceId}/sync")
	public WVPResult<SyncStatus> devicesSync(@PathVariable String deviceId){

		if (log.isDebugEnabled()) {
			log.debug("Device channel information synchronization API call，deviceId：" + deviceId);
		}
		Device device = deviceService.getDeviceByDeviceId(deviceId);
		Assert.notNull(device, "Device does not exist");
		if (device.getTransport() == null) {
			WVPResult<SyncStatus> wvpResult = new WVPResult<>();
			wvpResult.setCode(ErrorCode.ERROR100.getCode());
			wvpResult.setMsg("The device has not been registered yet");
			return wvpResult;
		}
		return deviceService.devicesSync(device);

	}

	@Operation(summary = "Remove device", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@DeleteMapping("/devices/{deviceId}/delete")
	public String delete(@PathVariable String deviceId){

		if (log.isDebugEnabled()) {
			log.debug("Device information deletion API call，deviceId：" + deviceId);
		}

		// Clear redis records
		deviceService.delete(deviceId);
		JSONObject json = new JSONObject();
		json.put("deviceId", deviceId);
		return json.toString();
	}

	@Operation(summary = "Paging query subdirectory channel", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "channelId", description = "Channel national standard number", required = true)
	@Parameter(name = "page", description = "Current page", required = true)
	@Parameter(name = "count", description = "Number of queries per page", required = true)
	@Parameter(name = "query", description = "Query content")
	@Parameter(name = "online", description = "Is online")
	@Parameter(name = "channelType", description = "Equipment/subdirectory-> false/true")
	@GetMapping("/sub_channels/{deviceId}/{channelId}/channels")
	public PageInfo<DeviceChannel> subChannels(@PathVariable String deviceId,
												  @PathVariable String channelId,
												  int page,
												  int count,
												  @RequestParam(required = false) String query,
												  @RequestParam(required = false) Boolean online,
												  @RequestParam(required = false) Boolean channelType){

		DeviceChannel deviceChannel = deviceChannelService.getOne(deviceId,channelId);
		if (deviceChannel == null) {
            return new PageInfo<>();
		}

		return deviceChannelService.getSubChannels(deviceChannel.getDataDeviceId(), channelId, query, channelType, online, page, count);
	}

	@Operation(summary = "turn on/Turn off channel audio", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "channelId", description = "channel databaseID", required = true)
	@Parameter(name = "audio", description = "turn on/Turn off audio", required = true)
	@PostMapping("/channel/audio")
	public void changeAudio(Integer channelId, Boolean audio){
		Assert.notNull(channelId, "The database ID of the channel cannot beNULL");
		Assert.notNull(audio, "turn on/Turning off audio is not possibleNULL");
		deviceChannelService.changeAudio(channelId, audio);
	}

	@Operation(summary = "Modify the code stream type of the channel", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@PostMapping("/channel/stream/identification/update/")
	public void updateChannelStreamIdentification(DeviceChannel channel){
		deviceChannelService.updateChannelStreamIdentification(channel);
	}
	@Operation(summary = "Get individual channel details", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "National standard code of equipment", required = true)
	@Parameter(name = "channelDeviceId", description = "The national standard code of the channel", required = true)
	@GetMapping("/channel/one")
	public DeviceChannel getChannel(String deviceId, String channelDeviceId){
		return deviceChannelService.getOne(deviceId, channelDeviceId);
	}


	@Operation(summary = "Modify data streaming mode", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "streamMode", description = "Data stream transmission mode, value：" +
			"UDP（udptransmission），TCP-ACTIVE（tcpActive mode），TCP-PASSIVE（tcppassive mode）", required = true)
	@PostMapping("/transport/{deviceId}/{streamMode}")
	public void updateTransport(@PathVariable String deviceId, @PathVariable String streamMode){
		Assert.isTrue(streamMode.equalsIgnoreCase("UDP")
				|| streamMode.equalsIgnoreCase("TCP-ACTIVE")
				|| streamMode.equalsIgnoreCase("TCP-PASSIVE"), "Data stream transmission mode, value：UDP/TCP-ACTIVE/TCP-PASSIVE");
		Device device = deviceService.getDeviceByDeviceId(deviceId);
		Assert.notNull(device, "Device does not exist");
		device.setStreamMode(streamMode.toUpperCase());
		deviceService.updateCustomDevice(device);
	}


	@Operation(summary = "Add device information", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "device", description = "Equipment", required = true)
	@PostMapping("/device/add")
	public void addDevice(@RequestBody Device device){

		if (device == null || device.getDeviceId() == null) {
			throw new ControllerException(ErrorCode.ERROR400);
		}

		// Check if deviceId exists
		boolean exist = deviceService.isExist(device.getDeviceId());
		if (exist) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "Device number already exists");
		}
		deviceService.addCustomDevice(device);
	}


	@Operation(summary = "Update device information", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "device", description = "Equipment", required = true)
	@PostMapping("/device/update")
	public void updateDevice(@RequestBody Device device){
		if (device == null || device.getDeviceId() == null || device.getId() <= 0) {
			throw new ControllerException(ErrorCode.ERROR400);
		}
		deviceService.updateCustomDevice(device);
	}

	@Operation(summary = "Equipment status query", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@GetMapping("/devices/{deviceId}/status")
	public DeferredResult<WVPResult<String>> deviceStatusApi(@PathVariable String deviceId) {
		if (log.isDebugEnabled()) {
			log.debug("Device status query API call");
		}
		Device device = deviceService.getDeviceByDeviceId(deviceId);
		Assert.notNull(device, "Device does not exist");
		DeferredResult<WVPResult<String>> result = new DeferredResult<>();
		deviceService.deviceStatus(device, (code, msg, data) -> {
			result.setResult(new WVPResult<>(code, msg, data));
		});
		result.onTimeout(() -> {
			log.warn("[Equipment status query] The operation timed out and the device did not return a response command., {}", deviceId);
			result.setResult(WVPResult.fail(ErrorCode.ERROR100.getCode(), "The operation timed out and the device did not respond."));
		});
		return result;
	}

	@Operation(summary = "Equipment alarm query", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "startPriority", description = "Alarm starting level, 0 is all, 1 is first-level alarm, 2 is second-level alarm, 3 is third-level alarm, 4 is fourth-level alarm")
	@Parameter(name = "endPriority", description = "Alarm termination level, 0 is all, 1 is first-level alarm, 2 is second-level alarm, 3 is third-level alarm, 4 is fourth-level alarm")
	@Parameter(name = "alarmMethod", description = "Alarm mode conditions, the value 0 is all, 1 is phone alarm, 2 is device alarm, 3 is SMS alarm, 4 is GPS alarm," +
			"5For video alarm, 6 for equipment failure alarm, 7 for other alarms;It can be a direct combination such as 12 for telephone alarm or equipment alarm.")
	@Parameter(name = "alarmType", description = "Alarm type")
	@Parameter(name = "startTime", description = "Alarm occurrence start time")
	@Parameter(name = "endTime", description = "Alarm occurrence end time")
	@GetMapping("/alarm")
	public DeferredResult<WVPResult<Object>> alarmApi(String deviceId,
														@RequestParam(required = false) String startPriority,
														@RequestParam(required = false) String endPriority,
														@RequestParam(required = false) String alarmMethod,
														@RequestParam(required = false) String alarmType,
														@RequestParam(required = false) String startTime,
														@RequestParam(required = false) String endTime) {
		if (log.isDebugEnabled()) {
			log.debug("Device alarm query API call");
		}
		Device device = deviceService.getDeviceByDeviceId(deviceId);
		Assert.notNull(device, "Device does not exist");
		DeferredResult<WVPResult<Object>> result = new DeferredResult<>();
		deviceService.alarm(device, startPriority,endPriority ,alarmMethod ,alarmType ,startTime ,endTime, (code, msg, data) -> {
			result.setResult(new WVPResult<>(code, msg, data));
		});
		result.onTimeout(() -> {
			log.warn("[Equipment alarm query] The operation timed out and the device did not return a response command., {}", deviceId);
			result.setResult(WVPResult.fail(ErrorCode.ERROR100.getCode(), "The operation timed out and the device did not respond."));
		});
		return result;
	}

	@Operation(summary = "Equipment information query", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@GetMapping("/info")
	public DeferredResult<WVPResult<Object>> deviceInfo(String deviceId) {
		if (log.isDebugEnabled()) {
			log.debug("Device information query API call");
		}
		Device device = deviceService.getDeviceByDeviceId(deviceId);
		Assert.notNull(device, "Device does not exist");
		DeferredResult<WVPResult<Object>> result = new DeferredResult<>();
		deviceService.deviceInfo(device, (code, msg, data) -> {
			result.setResult(new WVPResult<>(code, msg, data));
		});
		result.onTimeout(() -> {
			log.warn("[Equipment information query] The operation timed out and the device did not return a response command., {}", deviceId);
			result.setResult(WVPResult.fail(ErrorCode.ERROR100.getCode(), "The operation timed out and the device did not respond."));
		});
		return result;
	}

    /**
     * This interface is reserved for compatibility only and will be removed later. Please migrate to
     */
	@GetMapping("/{deviceId}/sync_status")
	@Operation(summary = "Get channel synchronization progress (this interface is reserved for compatibility only and will be removed later, please migrate to /sync_status?deviceId=）", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	public WVPResult<SyncStatus> getSyncStatusInPath(@PathVariable String deviceId) {
		SyncStatus channelSyncStatus = deviceService.getChannelSyncStatus(deviceId);
		WVPResult<SyncStatus> wvpResult = new WVPResult<>();
		if (channelSyncStatus == null) {
			wvpResult.setCode(ErrorCode.ERROR100.getCode());
			wvpResult.setMsg("Sync does not exist");
		}else if (channelSyncStatus.getErrorMsg() != null) {
			wvpResult.setCode(ErrorCode.ERROR100.getCode());
			wvpResult.setMsg(channelSyncStatus.getErrorMsg());
		}else if (channelSyncStatus.getTotal() == null){
			wvpResult.setCode(ErrorCode.SUCCESS.getCode());
			wvpResult.setMsg("Waiting for channel information...");
		}else if (channelSyncStatus.getTotal() == 0){
			wvpResult.setCode(ErrorCode.SUCCESS.getCode());
            wvpResult.setMsg(ErrorCode.SUCCESS.getMsg());
            wvpResult.setData(channelSyncStatus);
		}else {
			wvpResult.setCode(ErrorCode.SUCCESS.getCode());
			wvpResult.setMsg(ErrorCode.SUCCESS.getMsg());
			wvpResult.setData(channelSyncStatus);
		}
		return wvpResult;
	}

    /**
     * This interface is reserved for compatibility only and will be removed later. Please migrate to
     */
    @GetMapping("/sync_status")
    @Operation(summary = "Get channel synchronization progress", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
    public WVPResult<SyncStatus> getSyncStatus(String deviceId) {
        SyncStatus channelSyncStatus = deviceService.getChannelSyncStatus(deviceId);
        WVPResult<SyncStatus> wvpResult = new WVPResult<>();
        if (channelSyncStatus == null) {
            wvpResult.setCode(ErrorCode.ERROR100.getCode());
            wvpResult.setMsg("Sync does not exist");
        }else if (channelSyncStatus.getErrorMsg() != null) {
            wvpResult.setCode(ErrorCode.ERROR100.getCode());
            wvpResult.setMsg(channelSyncStatus.getErrorMsg());
        }else if (channelSyncStatus.getTotal() == null){
            wvpResult.setCode(ErrorCode.SUCCESS.getCode());
            wvpResult.setMsg("Waiting for channel information...");
        }else if (channelSyncStatus.getTotal() == 0){
            wvpResult.setCode(ErrorCode.SUCCESS.getCode());
            wvpResult.setMsg(ErrorCode.SUCCESS.getMsg());
            wvpResult.setData(channelSyncStatus);
        }else {
            wvpResult.setCode(ErrorCode.SUCCESS.getCode());
            wvpResult.setMsg(ErrorCode.SUCCESS.getMsg());
            wvpResult.setData(channelSyncStatus);
        }
        return wvpResult;
    }

	@GetMapping("/snap/{deviceId}/{channelId}")
	@Operation(summary = "Request a screenshot")
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "channelId", description = "Channel national standard number", required = true)
	@Parameter(name = "mark", description = "logo", required = false)
	public void getSnap(HttpServletResponse resp, @PathVariable String deviceId, @PathVariable String channelId, @RequestParam(required = false) String mark) {

		try {
			final InputStream in = Files.newInputStream(new File("snap" + File.separator + deviceId + "_" + channelId + (mark == null? ".jpg": ("_" + mark + ".jpg"))).toPath());
			resp.setContentType(MediaType.IMAGE_PNG_VALUE);
			ServletOutputStream outputStream = resp.getOutputStream();
			IOUtils.copy(in, resp.getOutputStream());
			in.close();
			outputStream.close();
		} catch (IOException e) {
			resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
		}
	}

	@GetMapping("/channel/raw")
	@Operation(summary = "Data echo when editing the national standard channel")
	@Parameter(name = "id", description = "ChannelId", required = true)
	public DeviceChannel getRawChannel(int id) {
		return deviceChannelService.getRawChannel(id);
	}

	@GetMapping("/subscribe/catalog")
	@Operation(summary = "turn on/Close directory subscription")
	@Parameter(name = "id", description = "ChannelId", required = true)
	@Parameter(name = "cycle", description = "Subscription cycle", required = true)
	public void subscribeCatalog(int id, int cycle) {
		deviceService.subscribeCatalog(id, cycle);
	}

	@GetMapping("/subscribe/mobile-position")
	@Operation(summary = "turn on/Turn off mobile location subscriptions")
	@Parameter(name = "id", description = "ChannelId", required = true)
	@Parameter(name = "cycle", description = "Subscription cycle", required = true)
	@Parameter(name = "interval", description = "Submission interval", required = true)
	public void subscribeMobilePosition(int id, int cycle, int interval) {
		deviceService.subscribeMobilePosition(id, cycle, interval);
	}

	@GetMapping("/statistics/keepalive")
	@Operation(summary = "Request heartbeat statistics")
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "count", description = "The quantity returned, sorted forward by time, the latest returned", required = true)
	public List<TimeStatistics> getKeepaliveTimeStatistics(String deviceId, Integer count) {
		if (ObjectUtils.isEmpty(deviceId)) {
			return List.of();
		}
		return deviceService.getKeepaliveTimeStatistics(deviceId, count);
	}

	@GetMapping("/statistics/register")
	@Operation(summary = "Request registration statistics")
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "count", description = "The quantity returned, sorted forward by time, the latest returned", required = true)
	public List<TimeStatistics> getRegisterTimeStatistics(String deviceId, Integer count) {
		if (ObjectUtils.isEmpty(deviceId)) {
			return List.of();
		}
		return deviceService.getRegisterTimeStatistics(deviceId, count);
	}

	@GetMapping("/subscribe/alarm")
	@Operation(summary = "turn on/Close alarm subscription")
	@Parameter(name = "id", description = "ChannelId", required = true)
	@Parameter(name = "cycle", description = "Subscription cycle", required = true)
	public void subscribeAlarm(int id, int cycle) {
		deviceService.subscribeAlarm(id, cycle);
	}
}
