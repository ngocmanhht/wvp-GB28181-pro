package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpInfo;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.service.IPlatformService;
import com.genersoft.iot.vmp.gb28181.service.IPlayService;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.ISendRtpServerService;
import com.genersoft.iot.vmp.service.redisMsg.IRedisRpcService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;
import javax.sip.address.SipURI;
import javax.sip.header.CallIdHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderAddress;
import javax.sip.header.ToHeader;

/**
 * SIPCommand type: ACK request
 * @author lin
 */
@Slf4j
@Component
public class AckRequestProcessor extends SIPRequestProcessorParent implements InitializingBean, ISIPRequestProcessor {

	private final String method = "ACK";

	@Autowired
	private SIPProcessorObserver sipProcessorObserver;

	@Override
	public void afterPropertiesSet() throws Exception {
		// Add message processing subscription
		sipProcessorObserver.addRequestProcessor(method, this);
	}

	@Autowired
    private IRedisCatchStorage redisCatchStorage;

	@Autowired
    private IRedisRpcService redisRpcService;

	@Autowired
    private UserSetting userSetting;

	@Autowired
	private IPlatformService platformService;

	@Autowired
	private IDeviceService deviceService;

	@Autowired
	private IDeviceChannelService deviceChannelService;

	@Autowired
	private IMediaServerService mediaServerService;

	@Autowired
	private DynamicTask dynamicTask;

	@Autowired
	private IPlayService playService;

	@Autowired
	private ISendRtpServerService sendRtpServerService;


	/**   
	 * Handle ACK request
	 */
	@Override
	public void process(RequestEvent evt) {
		CallIdHeader callIdHeader = (CallIdHeader)evt.getRequest().getHeader(CallIdHeader.NAME);
		dynamicTask.stop(callIdHeader.getCallId());
		String fromUserId = ((SipURI) ((HeaderAddress) evt.getRequest().getHeader(FromHeader.NAME)).getAddress().getURI()).getUser();
		String toUserId = ((SipURI) ((HeaderAddress) evt.getRequest().getHeader(ToHeader.NAME)).getAddress().getURI()).getUser();
		log.info("[receivedACK]： from->{}", fromUserId);
		SendRtpInfo sendRtpItem =  sendRtpServerService.queryByCallId(callIdHeader.getCallId());
		if (sendRtpItem == null) {
			log.warn("[receivedACK]：Not found from{}，callId: {}", fromUserId, callIdHeader.getCallId());
			return;
		}
		// tcpWhen active, it is cascading to lower-level platforms. When replying with 200ok, the local has requested zlm to start monitoring. Skip the following steps.
		if (sendRtpItem.isTcpActive()) {
			log.info("receivedACK，rtp/{} TCPActive mode waits for the superior connection to be received and then starts streaming.", sendRtpItem.getStream());
			return;
		}
		MediaServer mediaServer = mediaServerService.getOne(sendRtpItem.getMediaServerId());
		log.info("receivedACK，rtp/{}Start pushing the flow to the superior, target={}:{}，SSRC={}, Agreement:{}",
				sendRtpItem.getStream(),
				sendRtpItem.getIp(),
				sendRtpItem.getPort(),
				sendRtpItem.getSsrc(),
				sendRtpItem.isTcp()?(sendRtpItem.isTcpActive()?"TCPTake the initiative":"TCPPassive"):"UDP"
		);
		Platform parentPlatform = platformService.queryPlatformByServerGBId(fromUserId);

		if (parentPlatform != null) {
			DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(sendRtpItem.getChannelId());
			if (!userSetting.getServerId().equals(sendRtpItem.getServerId())) {
				WVPResult<?> wvpResult = redisRpcService.startSendRtp(callIdHeader.getCallId(), sendRtpItem);
				if (wvpResult.getCode() == 0) {
					redisCatchStorage.sendPlatformStartPlayMsg(sendRtpItem, deviceChannel, parentPlatform);
				}
			} else {
				try {
					if (mediaServer != null) {
						if (sendRtpItem.isTcpActive()) {
							mediaServerService.startSendRtpPassive(mediaServer,sendRtpItem, null);
						} else {
							mediaServerService.startSendRtp(mediaServer, sendRtpItem);
						}
					}else {
						// mediaInfo In other wvp of the cluster

					}

					redisCatchStorage.sendPlatformStartPlayMsg(sendRtpItem, deviceChannel, parentPlatform);
				}catch (ControllerException e) {
					log.error("RTPPush failed: {}", e.getMessage());
					playService.startSendRtpStreamFailHand(sendRtpItem, parentPlatform, callIdHeader);
				}
			}
		}else {
			Device device = deviceService.getDeviceByDeviceId(fromUserId);
			if (device == null) {
				log.warn("[receivedACK]：from{}，The goal is({})The push information for finding the fluid service[{}]information",fromUserId, toUserId, sendRtpItem.getMediaServerId());
				return;
			}
			// The device that is set to send voice after receiving ACK is already sending 200OK to start streaming.
			if (!device.isBroadcastPushAfterAck()) {
				return;
			}
			if (mediaServer == null) {
				log.warn("[receivedACK]：from{}，The goal is({})The push information for finding the fluid service[{}]information",fromUserId, toUserId, sendRtpItem.getMediaServerId());
				return;
			}
			try {
				if (sendRtpItem.isTcpActive()) {
					mediaServerService.startSendRtpPassive(mediaServer, sendRtpItem, null);
				} else {
					mediaServerService.startSendRtp(mediaServer, sendRtpItem);
				}
			}catch (ControllerException e) {
				log.error("RTPPush failed: {}", e.getMessage());
				playService.startSendRtpStreamFailHand(sendRtpItem, null, callIdHeader);
			}
		}
	}

}
