package com.genersoft.iot.vmp.gb28181.controller;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.SsrcTransaction;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.service.IInviteStreamService;
import com.genersoft.iot.vmp.gb28181.service.IPlayService;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.AudioBroadcastResult;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import jakarta.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.UUID;


/**
 * @author lin
 */
@Tag(name  = "National standard equipment on demand")
@Slf4j
@RestController
@RequestMapping("/api/play")
public class PlayController {

	@Autowired
	private SipInviteSessionManager sessionManager;

	@Autowired
	private IInviteStreamService inviteStreamService;

	@Autowired
	private DeferredResultHolder resultHolder;

	@Autowired
	private IPlayService playService;

	@Autowired
	private IMediaServerService mediaServerService;

	@Autowired
	private UserSetting userSetting;

	@Autowired
	private IDeviceService deviceService;

	@Autowired
	private IDeviceChannelService deviceChannelService;

	@Operation(summary = "Start on demand", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "channelId", description = "Channel national standard number", required = true)
	@GetMapping("/start/{deviceId}/{channelId}")
	public DeferredResult<WVPResult<StreamContent>> play(HttpServletRequest request, @PathVariable String deviceId,
														 @PathVariable String channelId) {

		log.info("[Start on demand] deviceId：{}, channelId：{}, ", deviceId, channelId);
		Assert.notNull(deviceId, "The equipment national standard number cannot beNULL");
		Assert.notNull(channelId, "The national standard number of the channel cannot beNULL");
		// Get availablezlm
		Device device = deviceService.getDeviceByDeviceId(deviceId);
		Assert.notNull(device, "Device does not exist");
		DeviceChannel channel = deviceChannelService.getOne(deviceId, channelId);
		Assert.notNull(channel, "Channel does not exist");

		DeferredResult<WVPResult<StreamContent>> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());

		result.onTimeout(()->{
			log.info("[On-demand waiting timeout] deviceId：{}, channelId：{}, ", deviceId, channelId);
			// releasertpserver
			WVPResult<StreamContent> wvpResult = new WVPResult<>();
			wvpResult.setCode(ErrorCode.ERROR100.getCode());
			wvpResult.setMsg("On-demand timeout");
			result.setResult(wvpResult);

			inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
			deviceChannelService.stopPlay(channel.getId());
		});

		ErrorCallback<StreamInfo> callback  = (code, msg, streamInfo) -> {
			WVPResult<StreamContent> wvpResult = new WVPResult<>();
			if (code == InviteErrorCode.SUCCESS.getCode()) {
				wvpResult.setCode(ErrorCode.SUCCESS.getCode());
				wvpResult.setMsg(ErrorCode.SUCCESS.getMsg());

				if (streamInfo != null) {
					if (userSetting.getUseSourceIpAsStreamIp()) {
						streamInfo=streamInfo.clone();//deep copy
						String host;
						try {
							URL url=new URL(request.getRequestURL().toString());
							host=url.getHost();
						} catch (MalformedURLException e) {
							host=request.getLocalAddr();
						}
						streamInfo.changeStreamIp(host);
					}
					if (!ObjectUtils.isEmpty(streamInfo.getMediaServer().getTranscodeSuffix()) && !"null".equalsIgnoreCase(streamInfo.getMediaServer().getTranscodeSuffix())) {
						streamInfo.setStream(streamInfo.getStream() + "_" + streamInfo.getMediaServer().getTranscodeSuffix());
					}
					wvpResult.setData(new StreamContent(streamInfo));
				}else {
					wvpResult.setCode(code);
					wvpResult.setMsg(msg);
				}
			}else {
				wvpResult.setCode(code);
				wvpResult.setMsg(msg);
			}
			result.setResult(wvpResult);
		};
		playService.play(device, channel, callback);
		return result;
	}

	@Operation(summary = "Stop on demand", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "channelId", description = "Channel national standard number", required = true)
	@GetMapping("/stop/{deviceId}/{channelId}")
	public JSONObject playStop(@PathVariable String deviceId, @PathVariable String channelId) {

		log.debug(String.format("Device preview/Playback stop API call，streamId：%s_%s", deviceId, channelId ));

		if (deviceId == null || channelId == null) {
			throw new ControllerException(ErrorCode.ERROR400);
		}

		Device device = deviceService.getDeviceByDeviceId(deviceId);
		DeviceChannel channel = deviceChannelService.getOneForSource(deviceId, channelId);
		Assert.notNull(device, "Device does not exist");
		Assert.notNull(channel, "Channel does not exist");
		String streamId = String.format("%s_%s", device.getDeviceId(), channel.getDeviceId());
		playService.stop(InviteSessionType.PLAY, device, channel, streamId);
		JSONObject json = new JSONObject();
		json.put("deviceId", deviceId);
		json.put("channelId", channelId);
		return json;
	}
	/**
	 * End transcoding
	 */
	@Operation(summary = "End transcoding", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "key", description = "video streamingkey", required = true)
	@Parameter(name = "mediaServerId", description = "streaming servicesID", required = true)
	@PostMapping("/convertStop/{key}")
	public void playConvertStop(@PathVariable String key, String mediaServerId) {
		if (mediaServerId == null) {
			throw new ControllerException(ErrorCode.ERROR400.getCode(), "streaming media：" + mediaServerId + "does not exist" );
		}
		MediaServer mediaInfo = mediaServerService.getOne(mediaServerId);
		if (mediaInfo == null) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "The streamer being used has stopped running" );
		}else {
			Boolean deleted = mediaServerService.delFFmpegSource(mediaInfo, key);
			if (!deleted) {
				throw new ControllerException(ErrorCode.ERROR100 );
			}
		}
	}

	@Operation(summary = "Voice broadcast command", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "deviceId", description = "Channel national standard number", required = true)
	@Parameter(name = "timeout", description = "Push timeout(seconds)", required = true)
	@GetMapping("/broadcast/{deviceId}/{channelId}")
	@PostMapping("/broadcast/{deviceId}/{channelId}")
    public AudioBroadcastResult broadcastApi(@PathVariable String deviceId, @PathVariable String channelId, Integer timeout, Boolean broadcastMode) {
		if (log.isDebugEnabled()) {
			log.debug("Voice broadcast API call");
		}

		return playService.audioBroadcast(deviceId, channelId, broadcastMode);

	}

	@Operation(summary = "Stop voice broadcast")
	@Parameter(name = "deviceId", description = "EquipmentId", required = true)
	@Parameter(name = "channelId", description = "channelId", required = true)
	@GetMapping("/broadcast/stop/{deviceId}/{channelId}")
	@PostMapping("/broadcast/stop/{deviceId}/{channelId}")
	public void stopBroadcast(@PathVariable String deviceId, @PathVariable String channelId) {
		if (log.isDebugEnabled()) {
			log.debug("Stop voice broadcast API call");
		}
		Device device = deviceService.getDeviceByDeviceId(deviceId);
		Assert.notNull(device, "Device does not exist");
		DeviceChannel channel = deviceChannelService.getOne(deviceId, channelId);
		Assert.notNull(channel, "Channel does not exist");
		playService.stopAudioBroadcast(device, channel);
	}

	@Operation(summary = "get allssrc", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@GetMapping("/ssrc")
	public JSONObject getSSRC() {
		if (log.isDebugEnabled()) {
			log.debug("get allssrc");
		}
		JSONArray objects = new JSONArray();
		List<SsrcTransaction> allSsrc = sessionManager.getAll();
		for (SsrcTransaction transaction : allSsrc) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("deviceId", transaction.getDeviceId());
			jsonObject.put("channelId", transaction.getChannelId());
			jsonObject.put("ssrc", transaction.getSsrc());
			jsonObject.put("streamId", transaction.getStream());
			objects.add(jsonObject);
		}

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("data", objects);
		jsonObject.put("count", objects.size());
		return jsonObject;
	}

	@Operation(summary = "Get screenshot", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
	@Parameter(name = "channelId", description = "Channel national standard number", required = true)
	@GetMapping("/snap")
	public DeferredResult<String> getSnap(String deviceId, String channelId) {
		if (log.isDebugEnabled()) {
			log.debug("Get screenshot: {}/{}", deviceId, channelId);
		}

		DeferredResult<String> result = new DeferredResult<>(3 * 1000L);
		String key  = DeferredResultHolder.CALLBACK_CMD_SNAP + deviceId;
		String uuid  = UUID.randomUUID().toString();
		resultHolder.put(key, uuid,  result);

		RequestMessage message = new RequestMessage();
		message.setKey(key);
		message.setId(uuid);

		String fileName = deviceId + "_" + channelId + "_" + DateUtil.getNowForUrl() + ".jpg";
		playService.getSnap(deviceId, channelId, fileName, (code, msg, data) -> {
			if (code == InviteErrorCode.SUCCESS.getCode()) {
				message.setData(data);
			}else {
				message.setData(WVPResult.fail(code, msg));
			}
			resultHolder.invokeResult(message);
		});
		return result;
	}

}

