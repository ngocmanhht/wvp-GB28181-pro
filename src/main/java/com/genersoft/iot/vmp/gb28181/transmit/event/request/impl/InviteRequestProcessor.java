package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.service.*;
import com.genersoft.iot.vmp.gb28181.session.AudioBroadcastManager;
import com.genersoft.iot.vmp.gb28181.session.SendSsrcFactory;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.ISendRtpServerService;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import gov.nist.javax.sdp.TimeDescriptionImpl;
import gov.nist.javax.sdp.fields.TimeField;
import gov.nist.javax.sdp.fields.URIField;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sdp.*;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.List;
import java.util.Vector;

/**
 * SIPCommand type: INVITE request
 */
@Slf4j
@SuppressWarnings("rawtypes")
@Component
public class InviteRequestProcessor extends SIPRequestProcessorParent implements InitializingBean, ISIPRequestProcessor {

    private final String method = "INVITE";

    @Autowired
    private ISIPCommanderForPlatform cmderFroPlatform;

    @Autowired
    private IDeviceChannelService deviceChannelService;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private IGbChannelService channelService;

    @Autowired
    private IGbChannelPlayService channelPlayService;

    @Autowired
    private ISendRtpServerService sendRtpServerService;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private IPlayService playService;

    @Autowired
    private IPlatformService platformService;

    @Autowired
    private AudioBroadcastManager audioBroadcastManager;

    @Autowired
    private SIPProcessorObserver sipProcessorObserver;

    @Autowired
    private SipConfig config;

    @Autowired
    private SipInviteSessionManager sessionManager;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private SendSsrcFactory sendSsrcFactory;


    @Override
    public void afterPropertiesSet() throws Exception {
        // Add message processing subscription
        sipProcessorObserver.addRequestProcessor(method, this);
    }

    /**
     * Handle invite requests
     *
     * @param evt request message
     */
    @Override
    public void process(RequestEvent evt) {

        SIPRequest request = (SIPRequest) evt.getRequest();
        InviteMessageInfo inviteInfo = null;
        try {
            inviteInfo = decode(evt);

            // Check whether the request comes from the upper-level platform\device
            Platform platform = platformService.queryPlatformByServerGBId(inviteInfo.getRequesterId());
            if (platform == null) {
                inviteFromDeviceHandle(request, inviteInfo);
            } else {
                // Check whether the channel exists under the platform
                CommonGBChannel channel = channelService.queryOneWithPlatform(platform.getId(), inviteInfo.getTargetChannelId());
                if (channel == null) {
                    log.info("[SuperiorINVITE] The channel does not exist, return404: {}", inviteInfo.getTargetChannelId());
                    try {
                        // The channel does not exist, 404 is sent, the resource does not exist
                        responseAck(request, Response.NOT_FOUND);
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        log.error("[Command sending failed] invite Channel does not exist: {}", e.getMessage());
                    }
                    return;
                }
                log.info("[SuperiorINVITE] platform：{}， channel：{}({}), Receiving address：{}:{}，Flow collection method：{}, On demand type：{},  SSRC：{}",
                        platform.getName(), channel.getGbName(), channel.getGbDeviceId(), inviteInfo.getIp(),
                        inviteInfo.getPort(), inviteInfo.isTcp() ? (inviteInfo.isTcpActive() ? "TCPTake the initiative" : "TCPPassive") : "UDP",
                        inviteInfo.getSessionName(), inviteInfo.getSsrc());
                if (!userSetting.getUseCustomSsrcForParentInvite() && ObjectUtils.isEmpty(inviteInfo.getSsrc())) {
                    log.warn("[SuperiorINVITE] On-demand failed, the upper level did not carry SSRC, and this level was not set to use customSSRC");
                    // Channel exists, hair100，TRYING
                    try {
                        responseAck(request, Response.BAD_REQUEST);
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        log.error("[Command sending failed] SuperiorINVITE TRYING: {}", e.getMessage());
                    }
                    return;
                }
                // Channel exists, hair100，TRYING
                try {
                    responseAck(request, Response.TRYING);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[Command sending failed] SuperiorINVITE TRYING: {}", e.getMessage());
                }

                InviteMessageInfo finalInviteInfo = inviteInfo;
                channelPlayService.startInvite(channel, inviteInfo, platform, ((code, msg, streamInfo) -> {
                    if (code != InviteErrorCode.SUCCESS.getCode()) {
                        try {
                            responseAck(request, Response.BUSY_HERE, msg);
                        } catch (SipException | InvalidArgumentException | ParseException e) {
                            log.error("[Command sending failed] Superior INVITE on-demand failed: {}", e.getMessage());
                        }
                    } else {
                        // The on-demand broadcast is successful. TODO can check whether the cancel command exists here. If it exists, it will not be sent.
                        if (userSetting.getUseCustomSsrcForParentInvite()) {
                            // The upper-level platform does not use the ssrc specified by the upper-level platform when on-demand. Use the customized ssrc. Please refer to the national standard document.-On-demand external domain device media stream SSRC processing method
                            String sendSsrc = sendSsrcFactory.getSendSsrc(
                                    "Play".equalsIgnoreCase(finalInviteInfo.getSessionName()) ? "0" : "1");
                            finalInviteInfo.setSsrc(sendSsrc);
                            log.info("[SuperiorINVITE] Use customSSRC: {}", sendSsrc);
                        }
                        // Build sendRTP content
                        SendRtpInfo sendRtpItem = sendRtpServerService.createSendRtpInfo(streamInfo.getMediaServer(),
                                finalInviteInfo.getIp(), finalInviteInfo.getPort(), finalInviteInfo.getSsrc(), platform.getServerGBId(),
                                streamInfo.getApp(), streamInfo.getStream(),
                                channel.getGbId(), finalInviteInfo.isTcp(), platform.isRtcp());
                        if (finalInviteInfo.isTcp() && finalInviteInfo.isTcpActive()) {
                            sendRtpItem.setTcpActive(true);
                        }
                        sendRtpItem.setStatus(1);
                        sendRtpItem.setCallId(finalInviteInfo.getCallId());

                        sendRtpItem.setPlayTypeByChannelDataType(channel.getDataType(), finalInviteInfo.getSessionName());
                        sendRtpItem.setServerId(streamInfo.getServerId());
                        sendRtpServerService.update(sendRtpItem);
                        String sdpIp = streamInfo.getMediaServer().getSdpIp();
                        if (!ObjectUtils.isEmpty(platform.getSendStreamIp())) {
                            sdpIp = platform.getSendStreamIp();
                        }
                        String content = createSendSdp(sendRtpItem, finalInviteInfo, sdpIp);

                        // tcpActive mode, turn on monitoring after replying to sdp
                        if (sendRtpItem.isTcpActive()) {
                            MediaServer mediaServer = mediaServerService.getOne(sendRtpItem.getMediaServerId());
                            try {
                                mediaServerService.startSendRtpPassive(mediaServer, sendRtpItem, 10000);
                                DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(sendRtpItem.getChannelId());
                                if (deviceChannel != null) {
                                    redisCatchStorage.sendPlatformStartPlayMsg(sendRtpItem, deviceChannel, platform);
                                }
                            } catch (ControllerException e) {
                                log.warn("[SuperiorINVITE] tcpActive mode streaming failed", e);
                                sendBye(platform, finalInviteInfo.getCallId());
                            }
                        }

                        // If the Ack is not received after timeout, you should reply bye. The current waiting time is 10 seconds.
                        dynamicTask.startDelay(finalInviteInfo.getCallId(), () -> {
                            log.info("[Ack ] Wait timeout, {}/{}", finalInviteInfo.getCallId(), channel.getGbDeviceId());
                            // Replybye
                            sendBye(platform, finalInviteInfo.getCallId());
                        }, 60 * 1000);
                        try {
                            responseSdpAck(request, content, platform);
                        } catch (SipException | InvalidArgumentException | ParseException e) {
                            log.error("[Command sending failed] Superior INVITE sent 200（SDP）: {}", e.getMessage());
                        }
                    }
                }));
            }
        } catch (SdpException e) {
            // Incomplete parameters, sending 400, request error
            try {
                responseAck(request, Response.BAD_REQUEST);
            } catch (SipException | InvalidArgumentException | ParseException sendException) {
                log.error("[Command sending failed] invite BAD_REQUEST: {}", sendException.getMessage());
            }
        } catch (InviteDecodeException e) {
            try {
                responseAck(request, e.getCode(), e.getMsg());
            } catch (SipException | InvalidArgumentException | ParseException sendException) {
                log.error("[Command sending failed] invite BAD_REQUEST: {}", sendException.getMessage());
            }
        } catch (PlayException e) {
            try {
                responseAck(request, e.getCode(), e.getMsg());
            } catch (SipException | InvalidArgumentException | ParseException sendException) {
                log.error("[Command sending failed] invite On-demand failed: {}", sendException.getMessage());
            }
        } catch (Exception e) {
            log.error("[InviteHandle exceptions] ", e);
            try {
                responseAck(request, Response.SERVER_INTERNAL_ERROR, "");
            } catch (SipException | InvalidArgumentException | ParseException sendException) {
                log.error("[Command sending failed] invite On-demand failed: {}", sendException.getMessage());
            }
        }
    }

    private InviteMessageInfo decode(RequestEvent evt) throws SdpException {

        InviteMessageInfo inviteInfo = new InviteMessageInfo();
        SIPRequest request = (SIPRequest)evt.getRequest();
        String[] channelIdArrayFromSub = SipUtils.getChannelIdFromRequest(request);

        // To parse sdp messages, use jainsip’s own sdp parsing method.
        String contentString = new String(request.getRawContent());
        Gb28181Sdp gb28181Sdp = SipUtils.parseSDP(contentString);
        SessionDescription sdp = gb28181Sdp.getBaseSdb();
        String sessionName = sdp.getSessionName().getValue();
        String channelIdFromSdp = null;
        if(StringUtils.equalsIgnoreCase("Playback", sessionName)){
            URIField uriField = (URIField)sdp.getURI();
            channelIdFromSdp = uriField.getURI().split(":")[0];
        }
        final String channelId = StringUtils.isNotBlank(channelIdFromSdp) ? channelIdFromSdp :
                (channelIdArrayFromSub != null? channelIdArrayFromSub[0]: null);
        String requesterId = SipUtils.getUserIdFromFromHeader(request);
        CallIdHeader callIdHeader = (CallIdHeader) request.getHeader(CallIdHeader.NAME);

        if (requesterId == null || channelId == null) {
            log.warn("[Parse the INVITE message] Unable to obtain the source id from the request, returning a 400 error");
            throw new InviteDecodeException(Response.BAD_REQUEST, "request decode fail");
        }
        log.info("[INVITE] SourceID: {}, callId: {}, from：{}：{}",
                requesterId, callIdHeader.getCallId(), request.getRemoteAddress(), request.getRemotePort());
        inviteInfo.setRequesterId(requesterId);
        inviteInfo.setTargetChannelId(channelId);
        if (channelIdArrayFromSub != null && channelIdArrayFromSub.length == 2) {
            inviteInfo.setSourceChannelId(channelIdArrayFromSub[1]);
        }
        inviteInfo.setSessionName(sessionName);
        inviteInfo.setSsrc(gb28181Sdp.getSsrc());
        inviteInfo.setCallId(callIdHeader.getCallId());

        // If it is video playback, there will be the start time and end time of the video.
        Long startTime = null;
        Long stopTime = null;
        if (sdp.getTimeDescriptions(false) != null && !sdp.getTimeDescriptions(false).isEmpty()) {
            TimeDescriptionImpl timeDescription = (TimeDescriptionImpl) (sdp.getTimeDescriptions(false).get(0));
            TimeField startTimeFiled = (TimeField) timeDescription.getTime();
            startTime = startTimeFiled.getStartTime();
            stopTime = startTimeFiled.getStopTime();
        }
        //  Get supported formats
        Vector mediaDescriptions = sdp.getMediaDescriptions(true);
        // Check if PS payload is supported96
        //String ip = null;
        int port = -1;
        boolean mediaTransmissionTCP = false;
        Boolean tcpActive = null;
        for (Object description : mediaDescriptions) {
            MediaDescription mediaDescription = (MediaDescription) description;
            Media media = mediaDescription.getMedia();

            Vector mediaFormats = media.getMediaFormats(false);
            if (mediaFormats.contains("96") || mediaFormats.contains("8")) {
                port = media.getMediaPort();
                //String mediaType = media.getMediaType();
                String protocol = media.getProtocol();

                // Distinguish between TCP streaming and UDP, currently the defaultudp
                if ("TCP/RTP/AVP".equalsIgnoreCase(protocol)) {
                    String setup = mediaDescription.getAttribute("setup");
                    if (setup != null) {
                        mediaTransmissionTCP = true;
                        if ("active".equalsIgnoreCase(setup)) {
                            tcpActive = true;
                        } else if ("passive".equalsIgnoreCase(setup)) {
                            tcpActive = false;
                        }
                    }
                }
                break;
            }
        }
        if (port == -1) {
            log.info("[Parse the INVITE message]  Unsupported media format, return415");
            throw new InviteDecodeException(Response.UNSUPPORTED_MEDIA_TYPE, "unsupported media type");
        }
        inviteInfo.setTcp(mediaTransmissionTCP);
        inviteInfo.setTcpActive(tcpActive != null? tcpActive: false);
        inviteInfo.setStartTime(startTime);
        inviteInfo.setStopTime(stopTime);

        Vector sdpMediaDescriptions = sdp.getMediaDescriptions(true);
        MediaDescription mediaDescription = null;
        String downloadSpeed = "1";
        if (!sdpMediaDescriptions.isEmpty()) {
            mediaDescription = (MediaDescription) sdpMediaDescriptions.get(0);
        }
        if (mediaDescription != null) {
            downloadSpeed = mediaDescription.getAttribute("downloadspeed");
        }
        inviteInfo.setIp(sdp.getConnection().getAddress());
        inviteInfo.setPort(port);
        inviteInfo.setDownloadSpeed(downloadSpeed);

        return inviteInfo;

    }

    private String createSendSdp(SendRtpInfo sendRtpItem, InviteMessageInfo inviteInfo, String sdpIp) {
        StringBuilder content = new StringBuilder(200);
        content.append("v=0\r\n");
        content.append("o=" + inviteInfo.getTargetChannelId() + " 0 0 IN IP4 " + sdpIp + "\r\n");
        content.append("s=" + inviteInfo.getSessionName() + "\r\n");
        content.append("c=IN IP4 " + sdpIp + "\r\n");
        if ("Playback".equalsIgnoreCase(inviteInfo.getSessionName())) {
            content.append("t=" + inviteInfo.getStartTime() + " " + inviteInfo.getStopTime() + "\r\n");
        } else {
            content.append("t=0 0\r\n");
        }
        if (sendRtpItem.isTcp()) {
            content.append("m=video " + sendRtpItem.getLocalPort() + " TCP/RTP/AVP 96\r\n");
            if (!sendRtpItem.isTcpActive()) {
                content.append("a=setup:active\r\n");
            } else {
                content.append("a=setup:passive\r\n");
            }
        }else {
            content.append("m=video " + sendRtpItem.getLocalPort() + " RTP/AVP 96\r\n");
        }
        content.append("a=sendonly\r\n");
        content.append("a=rtpmap:96 PS/90000\r\n");
        content.append("y=" + sendRtpItem.getSsrc() + "\r\n");
        content.append("f=\r\n");
        return content.toString();
    }

    private void sendBye(Platform platform, String callId) {
        try {
            SendRtpInfo sendRtpItem = sendRtpServerService.queryByCallId(callId);
            if (sendRtpItem == null) {
                return;
            }
            CommonGBChannel channel = channelService.getOne(sendRtpItem.getChannelId());
            if (channel == null) {
                return;
            }
            cmderFroPlatform.streamByeCmd(platform, sendRtpItem, channel);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[Command sending failed] Superior INVITE sentBYE: {}", e.getMessage());
        }
    }

    public void inviteFromDeviceHandle(SIPRequest request, InviteMessageInfo inviteInfo) {

        if (inviteInfo.getSourceChannelId() == null) {
            log.warn("Invite request from the device, the channel the request came from cannot be determined from the request information, ignored，requesterId： {}", inviteInfo.getRequesterId());
            try {
                responseAck(request, Response.FORBIDDEN);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[Command sending failed] Invite request from a device, the device cannot be determined from the request information FORBIDDEN: {}", e.getMessage());
            }
            return;
        }
        // Non-upper-level platform request, query whether the device requested it (usually a device that receives voice broadcasts）
        Device device = redisCatchStorage.getDevice(inviteInfo.getRequesterId());
        // Determine whether requesterId is a device or channel
        if (device == null) {
            device = deviceService.getDeviceBySourceChannelDeviceId(inviteInfo.getRequesterId());
        }
        if (device == null) {
            // Check if channelID is available
            device = deviceService.getDeviceBySourceChannelDeviceId(inviteInfo.getSourceChannelId());
        }

        if (device == null) {
            log.warn("Invite request from a device. The device cannot be determined from the request information and has been ignored.，requesterId： {}/{}", inviteInfo.getRequesterId(),
                    inviteInfo.getSourceChannelId());
            try {
                responseAck(request, Response.FORBIDDEN);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[Command sending failed] Invite request from a device, the device cannot be determined from the request information FORBIDDEN: {}", e.getMessage());
            }
            return;
        }
        DeviceChannel deviceChannel = deviceChannelService.getOne(device.getDeviceId(), inviteInfo.getSourceChannelId());
        if (deviceChannel == null) {
            List<AudioBroadcastCatch> audioBroadcastCatchList = audioBroadcastManager.getByDeviceId(device.getDeviceId());
            if (audioBroadcastCatchList.isEmpty()) {
                log.warn("Invite request from the device, the channel to which it belongs cannot be determined from the request information and has been ignored.，requesterId： {}/{}", inviteInfo.getRequesterId(), inviteInfo.getSourceChannelId());
                try {
                    responseAck(request, Response.FORBIDDEN);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[Command sending failed] Invite request from a device, the device cannot be determined from the request information FORBIDDEN: {}", e.getMessage());
                }
                return;
            }else {
                deviceChannel = deviceChannelService.getOneForSourceById(audioBroadcastCatchList.get(0).getChannelId());
            }
        }
        AudioBroadcastCatch broadcastCatch = audioBroadcastManager.get(deviceChannel.getId());
        if (broadcastCatch == null) {
            log.warn("Invite request from device, non-voice broadcast, ignored，requesterId： {}/{}", inviteInfo.getRequesterId(), inviteInfo.getSourceChannelId());
            try {
                responseAck(request, Response.FORBIDDEN);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[Command sending failed] Invite request non-voice broadcast from device FORBIDDEN: {}", e.getMessage());
            }
            return;
        }
        log.info("device received" + inviteInfo.getRequesterId() + "Voice Broadcast Invite Request");
        String key = VideoManagerConstants.BROADCAST_WAITE_INVITE + device.getDeviceId();
        if (!SipUtils.isFrontEnd(device.getDeviceId())) {
            key += broadcastCatch.getChannelId();
        }
        dynamicTask.stop(key);
        try {
            responseAck(request, Response.TRYING);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[Command sending failed] invite BAD_REQUEST: {}", e.getMessage());
            playService.stopAudioBroadcast(device, deviceChannel);
            return;
        }
        String contentString = new String(request.getRawContent());

        try {
            Gb28181Sdp gb28181Sdp = SipUtils.parseSDP(contentString);
            SessionDescription sdp = gb28181Sdp.getBaseSdb();

            if (ObjectUtils.isEmpty(gb28181Sdp.getSsrc()) ) {
                String ssrc =  sendSsrcFactory.getSendSsrc("0");
                log.warn("The Invite request from the device does not carry SSRC and generates a randomssrc: {}，requesterId： {}/{}", ssrc, inviteInfo.getRequesterId(), inviteInfo.getSourceChannelId());
                gb28181Sdp.setSsrc(ssrc);
            }

            //  Get supported formats
            Vector mediaDescriptions = sdp.getMediaDescriptions(true);

            // Check if PS payload is supported96
            int port = -1;
            boolean mediaTransmissionTCP = false;
            Boolean tcpActive = null;
            for (int i = 0; i < mediaDescriptions.size(); i++) {
                MediaDescription mediaDescription = (MediaDescription) mediaDescriptions.get(i);
                Media media = mediaDescription.getMedia();

                Vector mediaFormats = media.getMediaFormats(false);
//                    if (mediaFormats.contains("8")) {
                port = media.getMediaPort();
                String protocol = media.getProtocol();
                // Distinguish between TCP streaming and UDP, currently the defaultudp
                if ("TCP/RTP/AVP".equals(protocol)) {
                    String setup = mediaDescription.getAttribute("setup");
                    if (setup != null) {
                        mediaTransmissionTCP = true;
                        if ("active".equals(setup)) {
                            tcpActive = true;
                        } else if ("passive".equals(setup)) {
                            tcpActive = false;
                        }
                    }
                }
                break;
//                    }
            }
            if (port == -1) {
                log.info("Unsupported media format, return415");
                // Reply to unsupported format
                try {
                    responseAck(request, Response.UNSUPPORTED_MEDIA_TYPE); // Unsupported format, send415
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[Command sending failed] invite Unsupported media format: {}", e.getMessage());
                    playService.stopAudioBroadcast(device, deviceChannel);
                    return;
                }
                return;
            }
            String addressStr = sdp.getOrigin().getAddress();
            log.info("Equipment{}Request voice stream, address：{}:{}，ssrc：{}, {}", inviteInfo.getRequesterId(), addressStr, port, gb28181Sdp.getSsrc(),
                    mediaTransmissionTCP ? (tcpActive ? "TCPTake the initiative" : "TCPPassive") : "UDP");

            MediaServer mediaServerItem = broadcastCatch.getMediaServerItem();
            if (mediaServerItem == null) {
                log.warn("The voice call was not found.zlm");
                try {
                    responseAck(request, Response.BUSY_HERE);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[Command sending failed] invite Not found availablezlm: {}", e.getMessage());
                    playService.stopAudioBroadcast(device, deviceChannel);
                }
                return;
            }
            log.info("Equipment{}Request voice stream, receive stream address：{}:{}，ssrc：{}, {}, Intercom mode：{}", inviteInfo.getRequesterId(), addressStr, port, gb28181Sdp.getSsrc(),
                    mediaTransmissionTCP ? (tcpActive ? "TCPTake the initiative" : "TCPPassive") : "UDP", sdp.getSessionName().getValue());
            CallIdHeader callIdHeader = (CallIdHeader) request.getHeader(CallIdHeader.NAME);

            SendRtpInfo sendRtpItem = sendRtpServerService.createSendRtpInfo(mediaServerItem, addressStr, port, gb28181Sdp.getSsrc(), inviteInfo.getRequesterId(),
                    device.getDeviceId(), deviceChannel.getId(),
                    mediaTransmissionTCP, false);

            if (sendRtpItem == null) {
                log.warn("Insufficient server port resources");
                try {
                    responseAck(request, Response.BUSY_HERE);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[Command sending failed] invite Insufficient server port resources: {}", e.getMessage());
                    playService.stopAudioBroadcast(device, deviceChannel);
                    return;
                }
                return;
            }

            sendRtpItem.setPlayType(InviteStreamType.BROADCAST);
            sendRtpItem.setCallId(callIdHeader.getCallId());
            sendRtpItem.setStatus(1);
            sendRtpItem.setApp(broadcastCatch.getApp());
            sendRtpItem.setStream(broadcastCatch.getStream());
            sendRtpItem.setPt(8);
            sendRtpItem.setUsePs(false);
            sendRtpItem.setRtcp(false);
            sendRtpItem.setOnlyAudio(true);
            sendRtpItem.setTcp(mediaTransmissionTCP);
            if (tcpActive != null) {
                sendRtpItem.setTcpActive(tcpActive);
            }

            sendRtpServerService.update(sendRtpItem);

            Boolean streamReady = mediaServerService.isStreamReady(mediaServerItem, broadcastCatch.getApp(), broadcastCatch.getStream());
            if (streamReady) {
                sendOk(device, deviceChannel, sendRtpItem, sdp, request, mediaServerItem, mediaTransmissionTCP, gb28181Sdp.getSsrc());
            } else {
                log.warn("[voice call]， No stream found to be pushed,app={},stream={}", broadcastCatch.getApp(), broadcastCatch.getStream());
                try {
                    responseAck(request, Response.GONE);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[Command sending failed] Voice call reply 410 failed， {}", e.getMessage());
                    return;
                }
                playService.stopAudioBroadcast(device, deviceChannel);
            }
        } catch (SdpException e) {
            log.error("[voice call] SDPparsing exception", e);
            try {
                responseAck(request, Response.BAD_REQUEST);
            } catch (SipException | InvalidArgumentException | ParseException exception) {
                log.error("[Command sending failed] Invite request non-voice broadcast from device FORBIDDEN: {}", exception.getMessage());
            }
            playService.stopAudioBroadcast(device, deviceChannel);
        }
    }

    SIPResponse sendOk(Device device, DeviceChannel channel,  SendRtpInfo sendRtpItem, SessionDescription sdp, SIPRequest request, MediaServer mediaServerItem, boolean mediaTransmissionTCP, String ssrc) {
        SIPResponse sipResponse = null;
        try {
            sendRtpItem.setStatus(2);
            sendRtpServerService.update(sendRtpItem);
            StringBuffer content = new StringBuffer(200);
            content.append("v=0\r\n");
            content.append("o=" + config.getId() + " " + sdp.getOrigin().getSessionId() + " " + sdp.getOrigin().getSessionVersion() + " IN IP4 " + mediaServerItem.getSdpIp() + "\r\n");
            content.append("s=Play\r\n");
            content.append("c=IN IP4 " + mediaServerItem.getSdpIp() + "\r\n");
            content.append("t=0 0\r\n");

            if (mediaTransmissionTCP) {
                content.append("m=audio " + sendRtpItem.getLocalPort() + " TCP/RTP/AVP 8\r\n");
            } else {
                content.append("m=audio " + sendRtpItem.getLocalPort() + " RTP/AVP 8\r\n");
            }

            content.append("a=rtpmap:8 PCMA/8000/1\r\n");

            content.append("a=sendonly\r\n");
            if (sendRtpItem.isTcp()) {
                content.append("a=connection:new\r\n");
                if (!sendRtpItem.isTcpActive()) {
                    content.append("a=setup:active\r\n");
                } else {
                    content.append("a=setup:passive\r\n");
                }
            }
            if (ssrc != null) {
                content.append("y=" + ssrc + "\r\n");
            }
            content.append("f=v/////a/1/8/1\r\n");

            Platform parentPlatform = new Platform();
            parentPlatform.setServerIp(device.getIp());
            parentPlatform.setServerPort(device.getPort());
            parentPlatform.setServerGBId(device.getDeviceId());

            sipResponse = responseSdpAck(request, content.toString(), parentPlatform);

            AudioBroadcastCatch audioBroadcastCatch = audioBroadcastManager.get(sendRtpItem.getChannelId());

            audioBroadcastCatch.setStatus(AudioBroadcastCatchStatus.Ok);
            audioBroadcastCatch.setSipTransactionInfoByRequest(sipResponse);
            audioBroadcastManager.update(audioBroadcastCatch);
            SsrcTransaction ssrcTransaction = SsrcTransaction.buildForDevice(device.getDeviceId(), sendRtpItem.getChannelId(),
                    request.getCallIdHeader().getCallId(), sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getSsrc(), sendRtpItem.getMediaServerId(), sipResponse, InviteSessionType.BROADCAST);
            sessionManager.put(ssrcTransaction);
            // Turn on streaming, Dahua will start establishing connections after receiving 200OK
            if (sendRtpItem.isTcpActive() || !device.isBroadcastPushAfterAck()) {
                if (sendRtpItem.isTcpActive()) {
                    log.info("[Voice call] Listen to the port and wait for the device to connect before pushing the stream");
                }else {
                    log.info("[Voice call] Found out after replying 200OK BroadcastPushAfterAckis False, start streaming now");
                }

                playService.startPushStream(sendRtpItem, channel, sipResponse, parentPlatform, request.getCallIdHeader());
            }

        } catch (SipException | InvalidArgumentException | ParseException | SdpParseException e) {
            log.error("[Command sending failed] Voice call reply200OK（SDP）: {}", e.getMessage());
        }
        return sipResponse;
    }
}
