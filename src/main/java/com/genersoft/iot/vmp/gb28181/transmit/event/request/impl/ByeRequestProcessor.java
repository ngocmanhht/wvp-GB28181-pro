package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.service.*;
import com.genersoft.iot.vmp.gb28181.session.AudioBroadcastManager;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IReceiveRtpServerService;
import com.genersoft.iot.vmp.service.ISendRtpServerService;
import com.genersoft.iot.vmp.service.redisMsg.IRedisRpcService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Response;
import java.text.ParseException;

/**
 * SIPCommand type: BYE request
 */
@Slf4j
@Component
public class ByeRequestProcessor extends SIPRequestProcessorParent implements InitializingBean, ISIPRequestProcessor {

	private final String method = "BYE";

	@Autowired
	private ISIPCommander cmder;

	@Autowired
	private ISendRtpServerService sendRtpServerService;

	@Autowired
	private IRedisCatchStorage redisCatchStorage;

	@Autowired
	private IInviteStreamService inviteStreamService;

	@Autowired
	private IPlatformService platformService;

	@Autowired
	private IDeviceService deviceService;

	@Autowired
	private IDeviceChannelService deviceChannelService;

	@Autowired
	private AudioBroadcastManager audioBroadcastManager;

	@Autowired
	private IGbChannelService channelService;

	@Autowired
	private IMediaServerService mediaServerService;

	@Autowired
	private SIPProcessorObserver sipProcessorObserver;

	@Autowired
	private SipInviteSessionManager sessionManager;

	@Autowired
	private IPlayService playService;

	@Autowired
	private UserSetting userSetting;

	@Autowired
	private IRedisRpcService redisRpcService;

	@Autowired
	private IReceiveRtpServerService receiveRtpServerService;


	@Override
	public void afterPropertiesSet() throws Exception {
		// Add message processing subscription
		sipProcessorObserver.addRequestProcessor(method, this);
	}

	/**
	 * Handling BYE requests
	 */
	@Override
	public void process(RequestEvent evt) {
		SIPRequest request = (SIPRequest) evt.getRequest();
		try {
			responseAck(request, Response.OK);
		} catch (SipException | InvalidArgumentException | ParseException e) {
			log.error("[Failed to reply BYE message]，{}", e.getMessage());
		}
		CallIdHeader callIdHeader = (CallIdHeader)evt.getRequest().getHeader(CallIdHeader.NAME);
		SendRtpInfo sendRtpItem =  sendRtpServerService.queryByCallId(callIdHeader.getCallId());

		// Stop sending at the receiving end
		if (sendRtpItem != null){
			CommonGBChannel channel = channelService.getOne(sendRtpItem.getChannelId());
			log.info("[receivedbye] from{}，stop channel：{}, Type： {}, callId: {}", sendRtpItem.getTargetId(), channel.getGbDeviceId(), sendRtpItem.getPlayType(), callIdHeader.getCallId());

			String streamId = sendRtpItem.getStream();
			log.info("[receivedbye] Stop pushing：{}, media node： {}", streamId, sendRtpItem.getMediaServerId());

			if (sendRtpItem.getPlayType().equals(InviteStreamType.PUSH)) {
				// If you are not from this platform, send a redis message to ask other wvp to stop streaming.
				Platform platform = platformService.queryPlatformByServerGBId(sendRtpItem.getTargetId());
				if (platform != null) {
					redisCatchStorage.sendPlatformStopPlayMsg(sendRtpItem, platform, channel);
					if (!userSetting.getServerId().equals(sendRtpItem.getServerId())) {
						redisRpcService.stopSendRtp(sendRtpItem.getCallId());
						sendRtpServerService.deleteByCallId(sendRtpItem.getCallId());
					}else {
						MediaServer mediaServer = mediaServerService.getOne(sendRtpItem.getMediaServerId());
						sendRtpServerService.deleteByCallId(callIdHeader.getCallId());
						if (mediaServer != null) {
							mediaServerService.stopSendRtp(mediaServer, sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getSsrc());
						}
					}
				}else {
					log.info("[Stop watching on the superior platform] Platform not found{}Information, failed to send redis message", sendRtpItem.getTargetId());
				}
			}else {
				MediaServer mediaInfo = mediaServerService.getOne(sendRtpItem.getMediaServerId());
				sendRtpServerService.delete(sendRtpItem);
				mediaServerService.stopSendRtp(mediaInfo, sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getSsrc());
			}
			if (sendRtpItem.getServerId().equals(userSetting.getServerId())) {
				MediaServer mediaServer = mediaServerService.getOne(sendRtpItem.getMediaServerId());
				if (mediaServer != null) {
					AudioBroadcastCatch audioBroadcastCatch = audioBroadcastManager.get(sendRtpItem.getChannelId());
					if (audioBroadcastCatch != null && audioBroadcastCatch.getSipTransactionInfo().getCallId().equals(callIdHeader.getCallId())) {
						// Stop intercom from superior platform
						log.info("[Stop intercom] From superiors, platform：{}, channel：{}", sendRtpItem.getTargetId(), sendRtpItem.getChannelId());
						audioBroadcastManager.del(sendRtpItem.getChannelId());
					}

					MediaInfo mediaInfo = mediaServerService.getMediaInfo(mediaServer, sendRtpItem.getApp(), streamId);

					if (mediaInfo != null && mediaInfo.getReaderCount() <= 0) {
						log.info("[receivedbye] {} If there are no other viewers, notify the device to stop streaming.", streamId);
						if (sendRtpItem.getPlayType().equals(InviteStreamType.PLAY)) {
							Device device = deviceService.getDeviceByDeviceId(sendRtpItem.getTargetId());
							if (device == null) {
								log.info("[receivedbye] {} No device information found when notifying the device to stop streaming", streamId);
								return;
							}
							DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(sendRtpItem.getChannelId());
							if (deviceChannel == null) {
								log.info("[receivedbye] {} Channel information not found when notifying the device to stop streaming", streamId);
								return;
							}
							try {
								log.info("[Stop on demand] {}/{}", sendRtpItem.getTargetId(), sendRtpItem.getChannelId());
								cmder.streamByeCmd(device, deviceChannel.getDeviceId(), sendRtpItem.getApp(), sendRtpItem.getStream(), null, null);
							} catch (InvalidArgumentException | ParseException | SipException |
									 SsrcTransactionNotFoundException e) {
								log.error("[receivedbye] {} There are no other viewers. Notify the device to stop streaming. Failed to send BYE. {}",streamId, e.getMessage());
							}
						}
					}
				}
			} else {
				// TODO When streaming on other wvp, this wvp should be notified to stop pushing and sending.BYE

			}
		}
		// It may be that the device has stopped sending
		SsrcTransaction ssrcTransaction = sessionManager.getSsrcTransactionByCallId(callIdHeader.getCallId());
		if (ssrcTransaction == null) {
			return;
		}
		log.info("[receivedbye] from：{}, channel: {}, Type： {}", ssrcTransaction.getDeviceId(), ssrcTransaction.getChannelId(), ssrcTransaction.getType());
		// TODO End on-demand and avoid waiting

		if (ssrcTransaction.getPlatformId() != null ) {
			Platform platform = platformService.queryPlatformByServerGBId(ssrcTransaction.getPlatformId());
			if (ssrcTransaction.getType().equals(InviteSessionType.BROADCAST)) {
				log.info("[receivedbye] The superior stopped the voice intercom from：{}, The channel has stopped streaming: {}", ssrcTransaction.getPlatformId(), ssrcTransaction.getChannelId());
				CommonGBChannel channel = channelService.getOne(ssrcTransaction.getChannelId());
				if (channel == null) {
					log.info("[receivedbye] Passage not found, superior：{}， channel：{}", ssrcTransaction.getPlatformId(), ssrcTransaction.getChannelId());
					return;
				}
				String mediaServerId = ssrcTransaction.getMediaServerId();
				platformService.stopBroadcast(platform, channel, ssrcTransaction.getApp(), ssrcTransaction.getStream(), false,
						mediaServerService.getOne(mediaServerId));
				DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(channel.getGbId());
				Device device = deviceService.getDevice(channel.getDataDeviceId());
				playService.stopAudioBroadcast(device, deviceChannel);
			}

		}else {
			Device device = deviceService.getDeviceByDeviceId(ssrcTransaction.getDeviceId());
			if (device == null) {
				log.info("[receivedbye] Device not found：{} ", ssrcTransaction.getDeviceId());
				return;
			}
			DeviceChannel channel = deviceChannelService.getOneForSourceById(ssrcTransaction.getChannelId());
			if (channel == null) {
				log.info("[receivedbye] Channel not found, device：{}， channel：{}", ssrcTransaction.getDeviceId(), ssrcTransaction.getChannelId());
				return;
			}
			switch (ssrcTransaction.getType()){
				case PLAY:
				case PLAYBACK:
				case DOWNLOAD:
					try {
						InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(ssrcTransaction.getType(), channel.getId());
						if (inviteInfo != null) {
							deviceChannelService.stopPlay(channel.getId());
							inviteStreamService.removeInviteInfo(inviteInfo);
							if (inviteInfo.getStreamInfo() != null) {
								receiveRtpServerService.closeRTPServer(inviteInfo.getStreamInfo().getMediaServer(), inviteInfo.getStreamInfo().getApp(), inviteInfo.getStreamInfo().getStream());
							}
						}
					} catch (Exception e) {
						log.error("[BYEProcess] Clean up Invite exception: type={}, channelId={}", ssrcTransaction.getType(), channel.getId(), e);
					}
					break;
				case BROADCAST:
				case TALK:
					// Find source intercom device, send stop
					Device sourceDevice = deviceService.getDeviceByChannelId(ssrcTransaction.getChannelId());
					AudioBroadcastCatch audioBroadcastCatch = audioBroadcastManager.get(channel.getId());
					if (sourceDevice != null) {
						playService.stopAudioBroadcast(sourceDevice, channel);
					}
					if (audioBroadcastCatch != null) {
						// Stop intercom from superior platform
						log.info("[Stop intercom] From superiors, platform：{}, channel：{}", ssrcTransaction.getDeviceId(), channel.getDeviceId());
						audioBroadcastManager.del(channel.getId());
					}
					break;
			}
			sessionManager.removeByCallId(ssrcTransaction.getCallId());
		}
	}
}
