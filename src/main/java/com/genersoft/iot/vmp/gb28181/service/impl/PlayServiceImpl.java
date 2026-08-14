package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.*;
import com.genersoft.iot.vmp.common.enums.MediaStreamUtil;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.exception.ServiceException;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.controller.bean.AudioBroadcastEvent;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.service.*;
import com.genersoft.iot.vmp.gb28181.session.AudioBroadcastManager;
import com.genersoft.iot.vmp.gb28181.session.SSRCFactory;
import com.genersoft.iot.vmp.gb28181.session.SendSsrcFactory;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.bean.RecordInfo;
import com.genersoft.iot.vmp.media.event.hook.Hook;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.event.hook.HookType;
import com.genersoft.iot.vmp.media.event.media.MediaArrivalEvent;
import com.genersoft.iot.vmp.media.event.media.MediaDepartureEvent;
import com.genersoft.iot.vmp.media.event.media.MediaNotFoundEvent;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.media.zlm.dto.StreamAuthorityInfo;
import com.genersoft.iot.vmp.service.ICloudRecordService;
import com.genersoft.iot.vmp.service.IReceiveRtpServerService;
import com.genersoft.iot.vmp.service.ISendRtpServerService;
import com.genersoft.iot.vmp.service.bean.*;
import com.genersoft.iot.vmp.service.redisMsg.IRedisRpcPlayService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.AudioBroadcastResult;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import javax.sdp.*;
import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Response;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

@SuppressWarnings(value = {"rawtypes", "unchecked"})
@Slf4j
@Service("playService")
public class PlayServiceImpl implements IPlayService {

    @Autowired
    private ISIPCommander cmder;

    @Autowired
    private AudioBroadcastManager audioBroadcastManager;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private ISIPCommanderForPlatform sipCommanderFroPlatform;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IInviteStreamService inviteStreamService;

    @Autowired
    private HookSubscribe subscribe;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private SipInviteSessionManager sessionManager;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private IDeviceChannelService deviceChannelService;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private ISIPCommanderForPlatform commanderForPlatform;

    @Autowired
    private SSRCFactory ssrcFactory;

    @Autowired
    private SendSsrcFactory sendSsrcFactory;

    @Autowired
    private IPlatformService platformService;

    @Autowired
    private IGbChannelService channelService;

    @Autowired
    private ISendRtpServerService sendRtpServerService;

    @Autowired
    private IReceiveRtpServerService receiveRtpServerService;

    @Autowired
    private ICloudRecordService cloudRecordService;

    @Autowired
    private IRedisRpcPlayService redisRpcPlayService;

    /**
     * Processing of incoming streams
     */
    @Async
    @EventListener
    public void onApplicationEvent(MediaArrivalEvent event) {
        if (MediaStreamUtil.GB28181_BROADCAST.equals(event.getApp()) || MediaStreamUtil.GB28181_TALK.equals(event.getApp())) {
            if (event.getStream().indexOf("_") > 0) {
                String[] streamArray = event.getStream().split("_");
                if (streamArray.length == 2) {
                    String deviceId = streamArray[0];
                    String channelId = streamArray[1];
                    Device device = deviceService.getDeviceByDeviceId(deviceId);
                    DeviceChannel channel = deviceChannelService.getOneForSource(deviceId, channelId);
                    if (device == null) {
                        log.info("[Voice intercom/shout] Device not found：{}", deviceId);
                        return;
                    }
                    if (channel == null) {
                        log.info("[Voice intercom/shout] Channel not found：{}", channelId);
                        return;
                    }
                    if (MediaStreamUtil.GB28181_BROADCAST.equals(event.getApp())) {
                        if (audioBroadcastManager.exit(channel.getId())) {
                            stopAudioBroadcast(device, channel);
                        }
                        // Open the voice intercom channel
                        try {
                            audioBroadcastCmd(device, channel, event.getMediaServer(),
                                    event.getApp(), event.getStream(), 60, false, (msg) -> log.info("[Voice call] Channel established successfully, device: {}, channel: {}", deviceId, channelId));
                        } catch (InvalidArgumentException | ParseException | SipException e) {
                            log.error("[Command sending failed] Voice intercom: {}", e.getMessage());
                        }
                    }else if (MediaStreamUtil.GB28181_TALK.equals(event.getApp())) {
                        // Open the voice intercom channel
                        talkCmd(device, channel, event.getMediaServer(), event.getStream(), (msg) -> log.info("[Voice intercom] Channel established successfully, device: {}, channel: {}", deviceId, channelId));
                    }
                }
            }
        }


    }

    /**
     * Stream departure processing
     */
    @Async
    @EventListener
    public void onApplicationEvent(MediaDepartureEvent event) {
        List<SendRtpInfo> sendRtpInfos = sendRtpServerService.queryByStream(event.getStream());
        if (!sendRtpInfos.isEmpty()) {
            for (SendRtpInfo sendRtpInfo : sendRtpInfos) {
                if (sendRtpInfo != null && sendRtpInfo.isSendToPlatform() && sendRtpInfo.getApp().equals(event.getApp())) {
                    String platformId = sendRtpInfo.getTargetId();
                    Device device = deviceService.getDeviceByDeviceId(platformId);
                    DeviceChannel channel = deviceChannelService.getOneById(sendRtpInfo.getChannelId());
                    try {
                        if (device != null && channel != null) {
                            cmder.streamByeCmd(device, channel.getDeviceId(), event.getApp(), event.getStream(), sendRtpInfo.getCallId(), null);
                            if (sendRtpInfo.getPlayType().equals(InviteStreamType.BROADCAST)
                                    || sendRtpInfo.getPlayType().equals(InviteStreamType.TALK)) {
                                AudioBroadcastCatch audioBroadcastCatch = audioBroadcastManager.get(channel.getId());
                                if (audioBroadcastCatch != null) {
                                    // Stop intercom from superior platform
                                    log.info("[Stop intercom] From superiors, platform：{}, channel：{}", sendRtpInfo.getTargetId(), sendRtpInfo.getChannelId());
                                    audioBroadcastManager.del(sendRtpInfo.getChannelId());
                                }
                            }
                        }
                    } catch (SipException | InvalidArgumentException | ParseException |
                             SsrcTransactionNotFoundException e) {
                        log.error("[Command sending failed] sendBYE: {}", e.getMessage());
                    }
                }
            }
        }

        if (MediaStreamUtil.GB28181_BROADCAST.equals(event.getApp()) || MediaStreamUtil.GB28181_TALK.equals(event.getApp())) {
            if (event.getStream().indexOf("_") > 0) {
                String[] streamArray = event.getStream().split("_");
                if (streamArray.length == 2) {
                    String deviceId = streamArray[0];
                    String channelId = streamArray[1];
                    Device device = deviceService.getDeviceByDeviceId(deviceId);
                    if (device == null) {
                        log.info("[Voice intercom/shout] Device not found：{}", deviceId);
                        return;
                    }
                    DeviceChannel channel = deviceChannelService.getOneForSource(deviceId, channelId);
                    if (channel == null) {
                        log.info("[Voice intercom/shout] Channel not found：{}", channelId);
                        return;
                    }
                    if (MediaStreamUtil.GB28181_BROADCAST.equals(event.getApp())) {
                        stopAudioBroadcast(device, channel);
                    }else if (MediaStreamUtil.GB28181_TALK.equals(event.getApp())) {
                        stopTalk(device, channel, false);
                    }
                }
            }
        }else if (MediaStreamUtil.isGB28181(event.getApp(), event.getStream())) {
            // releasessrc
            InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(null, event.getStream());
            if (inviteInfo != null && inviteInfo.getStatus() == InviteSessionStatus.ok
                    && inviteInfo.getStreamInfo() != null && inviteInfo.getSsrcInfo() != null) {
                // sendbye
                stop(inviteInfo);
            }

        }
    }

    /**
     * Stream not found processing
     */
    @Async
    @EventListener
    public void onApplicationEvent(MediaNotFoundEvent event) {
        if (!MediaStreamUtil.isGB28181(event.getApp(), event.getStream())) {
            return;
        }
        String[] s = event.getStream().split("_");
        if ((s.length != 2 && s.length != 4)) {
            return;
        }
        String deviceId = s[0];
        String channelId = s[1];
        Device device = redisCatchStorage.getDevice(deviceId);
        if (device == null || !device.isOnLine()) {
            return;
        }
        DeviceChannel deviceChannel = deviceChannelService.getOne(deviceId, channelId);
        if (deviceChannel == null) {
            return;
        }
        if (s.length == 2) {
            log.info("[ZLM HOOK] Preview stream not found, initiate automatic on-demand playback：{}->{}->{}/{}", event.getMediaServer().getId(), event.getSchema(), event.getApp(), event.getStream());
            play(event.getMediaServer(), deviceId, channelId, null, (code, msg, data) -> {});
        } else if (s.length == 4) {
            // This is video playback, and the video playback format is> Device ID_Channel ID_Start time_End time
            String startTimeStr = s[2];
            String endTimeStr = s[3];
            if (startTimeStr == null || endTimeStr == null || startTimeStr.length() != 14 || endTimeStr.length() != 14) {
                return;
            }
            String startTime = DateUtil.urlToyyyy_MM_dd_HH_mm_ss(startTimeStr);
            String endTime = DateUtil.urlToyyyy_MM_dd_HH_mm_ss(endTimeStr);
            log.info("[ZLM HOOK] Playback stream not found, initiate automatic on-demand playback：{}->{}->{}/{}-{}-{}",
                    event.getMediaServer().getId(), event.getSchema(),
                    event.getApp(), event.getStream(),
                    startTime, endTime
            );

            playBack(event.getMediaServer(), device, deviceChannel, startTime, endTime, (code, msg, data) -> {});
        }
    }

    @Override
    public void play(Device device, DeviceChannel channel, ErrorCallback<StreamInfo> callback) {

        // Determine whether the device belongs to the current platform, if not, initiate an automatic call
        if (!userSetting.getServerId().equals(device.getServerId())) {
            redisRpcPlayService.play(device.getServerId(), channel.getId(), callback);
            return;
        }
        MediaServer mediaServerItem = getNewMediaServerItem(device);
        if (mediaServerItem == null) {
            log.warn("[on demand] Not found availablezlm deviceId: {},channelId:{}", device.getDeviceId(), channel.getDeviceId());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Not found availablezlm");
        }
        play(mediaServerItem, device, channel, null, userSetting.getRecordSip(), callback);
    }

    @Override
    public SSRCInfo play(MediaServer mediaServerItem, String deviceId, String channelId, String ssrc, ErrorCallback<StreamInfo> callback) {
        if (mediaServerItem == null) {
            log.warn("[on demand] Not found availablezlm deviceId: {},channelId:{}", deviceId, channelId);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Not found availablezlm");
        }
        Device device = redisCatchStorage.getDevice(deviceId);
        if (device.getStreamMode().equalsIgnoreCase("TCP-ACTIVE") && !mediaServerItem.isRtpEnable()) {
            log.warn("[on demand] Single-port traffic collection does not support TCP active traffic collection. deviceId: {},channelId:{}", deviceId, channelId);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Single-port traffic collection does not support TCP active traffic collection.");
        }
        DeviceChannel channel = deviceChannelService.getOneForSource(deviceId, channelId);
        if (channel == null) {
            log.warn("[on demand] Channel not found deviceId: {},channelId:{}", deviceId, channelId);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Channel not found");
        }

        return play(mediaServerItem, device, channel, ssrc, userSetting.getRecordSip(), callback);
    }

    private SSRCInfo play(MediaServer mediaServer, Device device, DeviceChannel channel, String ssrc, Boolean record,
                          ErrorCallback<StreamInfo> callback) {
        if (mediaServer == null ) {
            if (callback != null) {
                callback.run(InviteErrorCode.ERROR_FOR_PARAMETER_ERROR.getCode(),
                        InviteErrorCode.ERROR_FOR_PARAMETER_ERROR.getMsg(),
                        null);
            }
            return null;
        }

        InviteInfo inviteInfoInCatch = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
        if (inviteInfoInCatch != null ) {
            if (inviteInfoInCatch.getStreamInfo() == null) {
                // The on-demand broadcast has been initiated but has not yet been successful. Just register the callback and wait for the result.
                inviteStreamService.once(InviteSessionType.PLAY, channel.getId(), null, callback);
                log.info("[Start on demand] Already requested, waiting for result， deviceId: {}, channelId({}): {}", device.getDeviceId(), channel.getDeviceId(), channel.getId());
                return inviteInfoInCatch.getSsrcInfo();
            }else {
                StreamInfo streamInfo = inviteInfoInCatch.getStreamInfo();
                String streamId = streamInfo.getStream();
                if (streamId == null) {
                    callback.run(InviteErrorCode.ERROR_FOR_CATCH_DATA.getCode(), "On-demand play failed, redis cache streamId equalsnull", null);
                    inviteStreamService.call(InviteSessionType.PLAY, channel.getId(), null,
                            InviteErrorCode.ERROR_FOR_CATCH_DATA.getCode(),
                            "On-demand play failed, redis cache streamId equalsnull",
                            null);
                    return inviteInfoInCatch.getSsrcInfo();
                }
                MediaServer mediaInfo = streamInfo.getMediaServer();
                Boolean ready = mediaServerService.isStreamReady(mediaInfo, MediaStreamUtil.RTP_APP, streamId);
                if (ready != null && ready) {
                    if(callback != null) {
                        callback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
                    }
                    inviteStreamService.call(InviteSessionType.PLAY, channel.getId(), null,
                            InviteErrorCode.SUCCESS.getCode(),
                            InviteErrorCode.SUCCESS.getMsg(),
                            streamInfo);
                    log.info("[On demand already exists] Return directly, device number: {}, Channel number: {}", device.getDeviceId(), channel.getDeviceId());
                    return inviteInfoInCatch.getSsrcInfo();
                }else {
                    // The on-demand broadcast has been initiated but has not yet been successful. Just register the callback and wait for the result.
                    inviteStreamService.once(InviteSessionType.PLAY, channel.getId(), null, callback);
                    deviceChannelService.stopPlay(channel.getId());
                    inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
                }
            }
        }

        String streamId = String.format("%s_%s", device.getDeviceId(), channel.getDeviceId());

        SSRCInfo ssrcInfo = receiveRtpServerService.openGbRTPServerForPlay(mediaServer, device, channel, ssrc,
                record != null ? record : userSetting.getRecordSip(),
                (code, msg, result) -> {

            if (code == InviteErrorCode.SUCCESS.getCode() && result != null && result.getHookData() != null) {
                // hook response
                StreamInfo streamInfo = onPublishHandlerForPlay(result.getHookData().getMediaServer(), result.getHookData().getMediaInfo(), device, channel);
                if (streamInfo == null){
                    if (callback != null) {
                        callback.run(InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getCode(),
                                InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getMsg(), null);
                    }
                    inviteStreamService.call(InviteSessionType.PLAY, channel.getId(), null,
                            InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getCode(),
                            InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getMsg(), null);
                    inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
                    return;
                }
                if (callback != null) {
                    callback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
                }
                inviteStreamService.call(InviteSessionType.PLAY, channel.getId(), null,
                        InviteErrorCode.SUCCESS.getCode(),
                        InviteErrorCode.SUCCESS.getMsg(),
                        streamInfo);

                log.info("[On-demand success] Device number: {}, Channel number:{}, Stream type：{}", device.getDeviceId(), channel.getDeviceId(),
                        channel.getStreamIdentification());
                snapOnPlay(result.getHookData().getMediaServer(), device.getDeviceId(), channel.getDeviceId(), streamId);
            }else {
                if (callback != null) {
                    callback.run(code, msg, null);
                }
                inviteStreamService.call(InviteSessionType.PLAY, channel.getId(), null, code, msg, null);
                inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
                SsrcTransaction ssrcTransaction = sessionManager.getSsrcTransactionByStream(MediaStreamUtil.RTP_APP, streamId);
                if (ssrcTransaction != null) {
                    try {
                        cmder.streamByeCmd(device, channel.getDeviceId(), MediaStreamUtil.RTP_APP, streamId, null, null);
                    } catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException e) {
                        log.error("[On-demand timeout]， Sending BYE failed {}", e.getMessage());
                    } finally {
                        sessionManager.removeByStream(MediaStreamUtil.RTP_APP, streamId);
                    }
                }
            }
        });
        if (ssrcInfo == null || ssrcInfo.getPort() <= 0) {
            log.info("[On-demand port/SSRC]Failed to obtain device number：{}, Channel number：{},ssrcInfo；{}", device.getDeviceId(), channel.getDeviceId(), ssrcInfo);
            callback.run(InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(), "Failed to obtain port or ssrc", null);
            inviteStreamService.call(InviteSessionType.PLAY, channel.getId(), null,
                    InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(),
                    InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getMsg(),
                    null);
            return null;
        }
        String sdpIp = !ObjectUtils.isEmpty(device.getSdpIp()) ? device.getSdpIp() : mediaServer.getSdpIp();
        log.info("[Start on demand] Device number: {}, Channel number: {}, Receiving address： {}:{}, flowID：{}, Flow collection mode：{}, SSRC: {}, SSRCVerification：{}",
                device.getDeviceId(), channel.getDeviceId(), sdpIp, ssrcInfo.getPort(), ssrcInfo.getStream(), device.getStreamMode(),
                ssrcInfo.getSsrc(), device.isSsrcCheck());

        // Initialize the invite message status in redis
        InviteInfo inviteInfo = InviteInfo.getInviteInfo(device.getDeviceId(), channel.getId(), ssrcInfo.getStream(), ssrcInfo, mediaServer.getId(),
                mediaServer.getSdpIp(), ssrcInfo.getPort(), device.getStreamMode(), InviteSessionType.PLAY,
                InviteSessionStatus.ready);
        inviteStreamService.updateInviteInfo(inviteInfo);

        try {
            cmder.playStreamCmd(mediaServer, ssrcInfo, device, channel, (eventResult) -> {
                // Handle the problem of TCP active connection and SSRC inconsistency after receiving 200ok
                InviteOKHandler(eventResult, ssrcInfo, mediaServer, device, channel, callback, inviteInfo, InviteSessionType.PLAY);
            }, (event) -> {
                log.info("[On-demand failed]{}:{} deviceId: {}, channelId:{}",event.statusCode, event.msg, device.getDeviceId(), channel.getDeviceId());
                receiveRtpServerService.closeRTPServer(mediaServer, ssrcInfo.getApp(), ssrcInfo.getStream());

                sessionManager.removeByStream(ssrcInfo.getApp(), ssrcInfo.getStream());
                if (callback != null) {
                    callback.run(event.statusCode, event.msg, null);
                }
                inviteStreamService.call(InviteSessionType.PLAY, channel.getId(), null,
                        event.statusCode, event.msg, null);

                inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
            }, userSetting.getPlayTimeout().longValue());
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[Command sending failed] on demand news: {}", e.getMessage());
            receiveRtpServerService.closeRTPServer(mediaServer, ssrcInfo.getApp(), ssrcInfo.getStream());
            sessionManager.removeByStream(ssrcInfo.getApp(), ssrcInfo.getStream());
            if (callback != null) {
                callback.run(InviteErrorCode.ERROR_FOR_SIP_SENDING_FAILED.getCode(),
                        InviteErrorCode.ERROR_FOR_SIP_SENDING_FAILED.getMsg(), null);
            }
            inviteStreamService.call(InviteSessionType.PLAY, channel.getId(), null,
                    InviteErrorCode.ERROR_FOR_SIP_SENDING_FAILED.getCode(),
                    InviteErrorCode.ERROR_FOR_SIP_SENDING_FAILED.getMsg(), null);

            inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
        }
        return ssrcInfo;
    }


    private void talk(MediaServer mediaServerItem, Device device, DeviceChannel channel, String stream,
                      SipSubscribe.Event errorEvent, Runnable timeoutCallback, AudioBroadcastEvent audioEvent) {

        String ySsrc = ssrcFactory.getPlaySsrc(mediaServerItem);

        if (ySsrc == null) {
            audioEvent.call("ssrcAlready exhausted");
            return;
        }
        String sendSsrc = sendSsrcFactory.getSendSsrc("0");
        SendRtpInfo sendRtpInfo;
        try {
            sendRtpInfo = sendRtpServerService.createSendRtpInfo(mediaServerItem, null, null, sendSsrc, device.getDeviceId(), MediaStreamUtil.GB28181_TALK, stream,
                    channel.getId(), true, false);
            if (sendRtpInfo == null) {
                audioEvent.call("Failed to obtain streaming port");
                return;
            }
            sendRtpInfo.setPlayType(InviteStreamType.TALK);
        }catch (PlayException e) {
            log.info("[Voice intercom]Start Failed to obtain streaming port deviceId: {}, channelId: {},", device.getDeviceId(), channel.getDeviceId());
            return;
        }

        sendRtpInfo.setOnlyAudio(true);
        sendRtpInfo.setPt(8);
        sendRtpInfo.setStatus(1);
        sendRtpInfo.setTcpActive(false);
        sendRtpInfo.setUsePs(false);
        sendRtpInfo.setReceiveStream(stream + "_talk");

        String callId = SipUtils.getNewCallId();
        log.info("[Voice intercom]start deviceId: {}, channelId: {},Flow collection port： {}, Flow collection mode：{}, SSRC: {}, SSRCVerification：{}", device.getDeviceId(), channel.getDeviceId(), sendRtpInfo.getLocalPort(), device.getStreamMode(), sendRtpInfo.getSsrc(), false);
        // Timeout processing
        String timeOutTaskKey = UUID.randomUUID().toString();
        dynamicTask.startDelay(timeOutTaskKey, () -> {

            log.info("[Voice intercom] Traffic collection timeout deviceId: {}, channelId: {}，port：{}, SSRC: {}", device.getDeviceId(), channel.getDeviceId(), sendRtpInfo.getPort(), sendRtpInfo.getSsrc());
            timeoutCallback.run();
            // On-demand timeout reply BYE and release ssrc and the resources of this on-demand broadcast.
            try {
                cmder.streamByeCmd(device, channel.getDeviceId(), null,  null, callId, null);
            } catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException e) {
                log.error("[Voice intercom]Timed out, failed to send BYE {}", e.getMessage());
            } finally {
                timeoutCallback.run();
                sessionManager.removeByStream(sendRtpInfo.getApp(), sendRtpInfo.getStream());
            }
        }, userSetting.getPlayTimeout());

        try {
            Integer localPort = mediaServerService.startSendRtpPassive(mediaServerItem, sendRtpInfo, userSetting.getPlayTimeout() * 1000);
            if (localPort == null || localPort <= 0) {
                timeoutCallback.run();
                sessionManager.removeByStream(sendRtpInfo.getApp(), sendRtpInfo.getStream());
                return;
            }
            sendRtpInfo.setPort(localPort);
            // Add authentication information
            receiveRtpServerService.addAuthenticateInfoForGb28181Talk(mediaServerItem, sendRtpInfo.getStream());

        }catch (ControllerException e) {
            log.info("[Voice intercom]failed deviceId: {}, channelId: {}", device.getDeviceId(), channel.getDeviceId());
            audioEvent.call("failed, " + e.getMessage());
            // Check whether the channel has been established and send it if it exists.bye
            stopTalk(device, channel);
        }


        // Check whether the device is already pushing streams
        try {
            cmder.talkStreamCmd(mediaServerItem, sendRtpInfo, ySsrc, device, channel, callId, (hookData) -> {
                log.info("[Voice intercom] The stream has been generated, start pushing the stream： " + hookData);
                dynamicTask.stop(timeOutTaskKey);
                // TODO No processing for now
            }, (hookData) -> {
                log.info("[Voice intercom] The device starts streaming： " + hookData);
                dynamicTask.stop(timeOutTaskKey);

            }, (event) -> {
                dynamicTask.stop(timeOutTaskKey);

                if (event.event instanceof ResponseEvent) {
                    ResponseEvent responseEvent = (ResponseEvent) event.event;
                    if (responseEvent.getResponse() instanceof SIPResponse) {
                        SIPResponse response = (SIPResponse) responseEvent.getResponse();
                        sendRtpInfo.setFromTag(response.getFromTag());
                        sendRtpInfo.setToTag(response.getToTag());
                        sendRtpInfo.setCallId(response.getCallIdHeader().getCallId());
                        sendRtpServerService.update(sendRtpInfo);

                        SsrcTransaction ssrcTransaction = SsrcTransaction.buildForDevice(device.getDeviceId(), sendRtpInfo.getChannelId(), response.getCallIdHeader().getCallId(), sendRtpInfo.getApp(),
                                sendRtpInfo.getStream(), sendRtpInfo.getSsrc(), sendRtpInfo.getMediaServerId(),
                                response, InviteSessionType.TALK);

                        sessionManager.put(ssrcTransaction);
                    } else {
                        log.error("[Voice intercom]The message received is wrong and the response is notSIPResponse");
                    }
                } else {
                    log.error("[Voice intercom]The message received is wrong and the event is notResponseEvent");
                }

            }, (event) -> {
                dynamicTask.stop(timeOutTaskKey);
                receiveRtpServerService.closeRTPServer(mediaServerItem, sendRtpInfo.getApp(), sendRtpInfo.getStream());
                sessionManager.removeByStream(sendRtpInfo.getApp(), sendRtpInfo.getStream());
                errorEvent.response(event);
            }, userSetting.getPlayTimeout().longValue());
        } catch (InvalidArgumentException | SipException | ParseException e) {

            log.error("[Command sending failed] intercom message: {}", e.getMessage());
            dynamicTask.stop(timeOutTaskKey);
            receiveRtpServerService.closeRTPServer(mediaServerItem, sendRtpInfo.getApp(), sendRtpInfo.getStream());
            sessionManager.removeByStream(sendRtpInfo.getApp(), sendRtpInfo.getStream());
            SipSubscribe.EventResult eventResult = new SipSubscribe.EventResult();
            eventResult.type = SipSubscribe.EventResultType.cmdSendFailEvent;
            eventResult.statusCode = -1;
            eventResult.msg = "Command sending failed";
            errorEvent.response(eventResult);
        }
//        }

    }

    private void tcpActiveHandler(Device device, DeviceChannel channel, String contentString,
                                  MediaServer mediaServerItem, SSRCInfo ssrcInfo, ErrorCallback<StreamInfo> callback,
                                  InviteSessionType inviteSessionType, InviteInfo inviteInfo){
        if (!device.getStreamMode().equalsIgnoreCase("TCP-ACTIVE")) {
            return;
        }

        String substring;
        if (contentString.indexOf("y=") > 0) {
            substring = contentString.substring(0, contentString.indexOf("y="));
        }else {
            substring = contentString;
        }
        try {
            SessionDescription sdp = SdpFactory.getInstance().createSessionDescription(substring);
            int port = -1;
            Vector mediaDescriptions = sdp.getMediaDescriptions(true);
            for (Object description : mediaDescriptions) {
                MediaDescription mediaDescription = (MediaDescription) description;
                Media media = mediaDescription.getMedia();

                Vector mediaFormats = media.getMediaFormats(false);
                if (mediaFormats.contains("96")) {
                    port = media.getMediaPort();
                    break;
                }
            }
            log.info("[TCPActively connect to the other party] deviceId: {}, channelId: {}, The address of the connecting party：{}:{}, Flow collection mode：{}, SSRC: {}, SSRCVerification：{}", device.getDeviceId(), channel.getDeviceId(), sdp.getConnection().getAddress(), port, device.getStreamMode(), ssrcInfo.getSsrc(), device.isSsrcCheck());
            Boolean result = mediaServerService.connectRtpServer(mediaServerItem, sdp.getConnection().getAddress(), port, ssrcInfo.getApp(), ssrcInfo.getStream());
            log.info("[TCPActively connect to the other party] result： {}" , result);
            if (!result) {
                // Active connection failed, ended the process, and cleared the data.
                receiveRtpServerService.closeRTPServer(mediaServerItem, ssrcInfo.getApp(), ssrcInfo.getStream());
                sessionManager.removeByStream(ssrcInfo.getApp(), ssrcInfo.getStream());
                callback.run(InviteErrorCode.ERROR_FOR_TCP_ACTIVE_CONNECTION_REFUSED_ERROR.getCode(),
                        InviteErrorCode.ERROR_FOR_TCP_ACTIVE_CONNECTION_REFUSED_ERROR.getMsg(), null);
                inviteStreamService.call(inviteSessionType, channel.getId(), null,
                        InviteErrorCode.ERROR_FOR_TCP_ACTIVE_CONNECTION_REFUSED_ERROR.getCode(),
                        InviteErrorCode.ERROR_FOR_TCP_ACTIVE_CONNECTION_REFUSED_ERROR.getMsg(), null);
                inviteStreamService.removeInviteInfo(inviteInfo);
            }
        } catch (SdpException e) {
            log.error("[TCPActively connect to the other party] deviceId: {}, channelId: {}, Failed to parse SDP information of 200OK", device.getDeviceId(), channel.getDeviceId(), e);
            receiveRtpServerService.closeRTPServer(mediaServerItem, ssrcInfo.getApp(), ssrcInfo.getStream());

            sessionManager.removeByStream(ssrcInfo.getApp(), ssrcInfo.getStream());

            callback.run(InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getCode(),
                    InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getMsg(), null);
            inviteStreamService.call(inviteSessionType, channel.getId(), null,
                    InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getCode(),
                    InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getMsg(), null);
            inviteStreamService.removeInviteInfo(inviteInfo);
        }
    }

    /**
     * Call screenshot when on-demand is successful.
     *
     * @param mediaServerItemInuse media
     * @param deviceId             Equipment ID
     * @param channelId            channel ID
     * @param stream               ssrc
     */
    private void snapOnPlay(MediaServer mediaServerItemInuse, String deviceId, String channelId, String stream) {
        String path = "snap";
        String fileName = deviceId + "_" + channelId + ".jpg";
        // Request a screenshot
        log.info("[Request a screenshot]: " + fileName);
        mediaServerService.getSnap(mediaServerItemInuse, MediaStreamUtil.RTP_APP, stream, 15, 1, path, fileName);
    }

    public StreamInfo onPublishHandlerForPlay(MediaServer mediaServerItem, MediaInfo mediaInfo, Device device, DeviceChannel channel) {
        StreamInfo streamInfo = null;
        streamInfo = onPublishHandler(mediaServerItem, mediaInfo, device, channel);
        if (streamInfo != null) {
            deviceChannelService.startPlay(channel.getId(), streamInfo.getStream());
            InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
            if (inviteInfo != null) {
                inviteInfo.setStatus(InviteSessionStatus.ok);
                inviteInfo.setStreamInfo(streamInfo);
                inviteStreamService.updateInviteInfo(inviteInfo);
            }
        }
        return streamInfo;

    }

    private StreamInfo onPublishHandlerForPlayback(MediaServer mediaServerItem, MediaInfo mediaInfo, Device device,
                                                   DeviceChannel channel, String startTime, String endTime) {
        StreamInfo streamInfo = onPublishHandler(mediaServerItem, mediaInfo, device, channel);
        if (streamInfo != null) {
            streamInfo.setStartTime(startTime);
            streamInfo.setEndTime(endTime);
            InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(InviteSessionType.PLAYBACK, mediaInfo.getStream());
            if (inviteInfo != null) {
                inviteInfo.setStatus(InviteSessionStatus.ok);
                inviteInfo.setStreamInfo(streamInfo);
                inviteStreamService.updateInviteInfo(inviteInfo);
            }
        }
        return streamInfo;
    }

    @Override
    public MediaServer getNewMediaServerItem(Device device) {
        if (device == null) {
            return null;
        }
        MediaServer mediaServerItem;
        if (ObjectUtils.isEmpty(device.getMediaServerId()) || "auto".equals(device.getMediaServerId())) {
            mediaServerItem = mediaServerService.getMediaServerForMinimumLoad(null);
        } else {
            mediaServerItem = mediaServerService.getOne(device.getMediaServerId());
        }
        if (mediaServerItem == null) {
            log.warn("No available video found during on-demand playbackZLM...");
        }
        return mediaServerItem;
    }

    @Override
    public void playBack(Device device, DeviceChannel channel, String startTime,
                         String endTime, ErrorCallback<StreamInfo> callback) {
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Device does not exist");
        }
        if (channel == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Channel does not exist");
        }
        if (!userSetting.getServerId().equals(device.getServerId())) {
            redisRpcPlayService.playback(device.getServerId(), channel.getId(), startTime, endTime, callback);
            return;
        }

        MediaServer newMediaServerItem = getNewMediaServerItem(device);
        if (newMediaServerItem == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "No available node found");
        }
        if (device.getStreamMode().equalsIgnoreCase("TCP-ACTIVE") && ! newMediaServerItem.isRtpEnable()) {
            log.warn("[Video playback] Single-port traffic collection does not support TCP active traffic collection. deviceId: {},channelId:{}", device.getDeviceId(), channel.getDeviceId());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Single-port traffic collection does not support TCP active traffic collection.");
        }

        playBack(newMediaServerItem, device, channel, startTime, endTime, callback);
    }

    private void playBack(MediaServer mediaServer,
                         Device device, DeviceChannel channel, String startTime,
                         String endTime, ErrorCallback<StreamInfo> callback) {

        String stream = receiveRtpServerService.getPlaybackStream(device, channel, startTime, endTime);

        SSRCInfo ssrcInfo = receiveRtpServerService.openGbRTPServerForPlayback(mediaServer, device, channel, startTime, endTime, (code, msg, result) -> {
            if (code == InviteErrorCode.SUCCESS.getCode() && result != null && result.getHookData() != null) {
                // hookresponse
                StreamInfo streamInfo = onPublishHandlerForPlayback(result.getHookData().getMediaServer(), result.getHookData().getMediaInfo(), device, channel, startTime, endTime);
                if (streamInfo == null) {
                    log.warn("Device playback API call failed！");
                    callback.run(InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getCode(),
                            InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getMsg(), null);
                    return;
                }
                callback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
                log.info("[Video playback] success deviceId: {}, channelId: {},  start time: {}, end time： {}", device.getDeviceId(), channel.getGbDeviceId(), startTime, endTime);
            }else {
                if (callback != null) {
                    callback.run(code, msg, null);
                }
                inviteStreamService.call(InviteSessionType.PLAYBACK, channel.getId(), null, code, msg, null);
                inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAYBACK, channel.getId());
                SsrcTransaction ssrcTransaction = sessionManager.getSsrcTransactionByStream(MediaStreamUtil.RTP_APP, stream);
                if (ssrcTransaction != null) {
                    try {
                        cmder.streamByeCmd(device, channel.getDeviceId(), MediaStreamUtil.RTP_APP,  stream, null, null);
                    } catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException e) {
                        log.error("[Video playback] Sending BYE failed {}", e.getMessage());
                    } finally {
                        sessionManager.removeByStream(MediaStreamUtil.RTP_APP, stream);
                    }
                }
            }
        });
        if (ssrcInfo == null || ssrcInfo.getPort() <= 0) {
            log.info("[playback port/SSRC]Failed to obtain，deviceId={},channelId={},ssrcInfo={}", device.getDeviceId(), channel.getDeviceId(), ssrcInfo);
            if (callback != null) {
                callback.run(InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(), "Failed to obtain port or ssrc", null);
            }
            inviteStreamService.call(InviteSessionType.PLAY, channel.getId(), null,
                    InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(),
                    InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getMsg(),
                    null);
            return;
        }

        log.info("[Video playback] deviceId: {}, channelId: {}, start time: {}, end time： {}, Flow collection port：{}, Flow collection mode：{}, SSRC: {}, SSRCVerification：{}",
                device.getDeviceId(), channel.getDeviceId(), startTime, endTime, ssrcInfo.getPort(), device.getStreamMode(),
                ssrcInfo.getSsrc(), device.isSsrcCheck());
        // Initialize the invite message status in redis
        InviteInfo inviteInfo = InviteInfo.getInviteInfo(device.getDeviceId(), channel.getId(), ssrcInfo.getStream(), ssrcInfo, mediaServer.getId(),
                mediaServer.getSdpIp(), ssrcInfo.getPort(), device.getStreamMode(), InviteSessionType.PLAYBACK,
                InviteSessionStatus.ready);
        inviteStreamService.updateInviteInfo(inviteInfo);

        try {
            cmder.playbackStreamCmd(mediaServer, ssrcInfo, device, channel, startTime, endTime,
                    eventResult -> {
                        // Handle the problem of TCP active connection and SSRC inconsistency after receiving 200ok
                        InviteOKHandler(eventResult, ssrcInfo, mediaServer, device, channel,
                                callback, inviteInfo, InviteSessionType.PLAYBACK);
                    }, eventResult -> {
                        log.info("[Video playback] failed，{} {}", eventResult.statusCode, eventResult.msg);
                        if (callback != null) {
                            callback.run(eventResult.statusCode, eventResult.msg, null);
                        }

                        receiveRtpServerService.closeRTPServer(mediaServer, ssrcInfo.getApp(), ssrcInfo.getStream());
                        sessionManager.removeByStream(ssrcInfo.getApp(), ssrcInfo.getStream());
                        inviteStreamService.removeInviteInfo(inviteInfo);
                    }, userSetting.getPlayTimeout().longValue());
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[Command sending failed] Video playback: {}", e.getMessage());
            if (callback != null) {
                callback.run(InviteErrorCode.FAIL.getCode(), e.getMessage(), null);
            }
            receiveRtpServerService.closeRTPServer(mediaServer, ssrcInfo.getApp(), ssrcInfo.getStream());
            sessionManager.removeByStream(ssrcInfo.getApp(), ssrcInfo.getStream());
            inviteStreamService.removeInviteInfo(inviteInfo);
        }
    }


    private void InviteOKHandler(SipSubscribe.EventResult eventResult, SSRCInfo ssrcInfo, MediaServer mediaServerItem,
                                 Device device, DeviceChannel channel, ErrorCallback<StreamInfo> callback,
                                 InviteInfo inviteInfo, InviteSessionType inviteSessionType){
        inviteInfo.setStatus(InviteSessionStatus.ok);
        ResponseEvent responseEvent = (ResponseEvent) eventResult.event;
        String contentString = new String(responseEvent.getResponse().getRawContent());
        String ssrcInResponse = SipUtils.getSsrcFromSdp(contentString);
        // Compatible reply messages are missingssrc(yField)situation
        if (ssrcInResponse == null) {
            ssrcInResponse = ssrcInfo.getSsrc();
        }
        if (ssrcInfo.getSsrc().equals(ssrcInResponse)) {
            // ssrc consistent
            if (mediaServerItem.isRtpEnable()) {
                // multi-port
                if (device.getStreamMode().equalsIgnoreCase("TCP-ACTIVE")) {
                    tcpActiveHandler(device, channel, contentString, mediaServerItem, ssrcInfo, callback, inviteSessionType, inviteInfo);
                }
            }else {
                // single port
                if (device.getStreamMode().equalsIgnoreCase("TCP-ACTIVE")) {
                    log.warn("[Invite 200OK] The single-port traffic collection mode does not support TCP active mode traffic collection.");
                }

            }
        }else {
            log.info("[Invite 200OK] Received invite 200 and found that the subordinate has customized itssrc: {}", ssrcInResponse);
            String oldStreamId = String.format("%08x", Long.parseLong(ssrcInfo.getSsrc())).toUpperCase();
            String newStreamId = String.format("%08x", Long.parseLong(ssrcInResponse)).toUpperCase();
            if (!mediaServerItem.isRtpEnable()) { // When using multiple ports, the stream is bound according to the port. Even if the stream is inconsistent with ssrc, it will not be affected.
                receiveRtpServerService.refreshAuthenticateInfo(oldStreamId, newStreamId);
            }
            // ssrc inconsistent
            if (mediaServerItem.isRtpEnable()) {
                // multi-port
                if (device.isSsrcCheck()) {
                    // ssrcInspect
                    // updatessrc
                    log.info("[Invite 200OK] SSRCCorrection {}->{}", ssrcInfo.getSsrc(), ssrcInResponse);
                    Boolean result = mediaServerService.updateRtpServerSSRC(mediaServerItem, ssrcInfo.getApp(), ssrcInfo.getStream(), ssrcInResponse);
                    if (!result) {
                        try {
                            log.warn("[Invite 200OK] Failed to update ssrc, stop on-demand playback {}/{}", device.getDeviceId(), channel.getDeviceId());
                            cmder.streamByeCmd(device, channel.getDeviceId(), ssrcInfo.getApp(), ssrcInfo.getStream(), null, null);
                        } catch (InvalidArgumentException | SipException | ParseException | SsrcTransactionNotFoundException e) {
                            log.error("[Command sending failed] Stop playing, sendBYE: {}", e.getMessage());
                        }

                        sessionManager.removeByStream(ssrcInfo.getApp(), ssrcInfo.getStream());

                        callback.run(InviteErrorCode.ERROR_FOR_RESET_SSRC.getCode(),
                                "The subordinate customized ssrc and failed to reset the traffic collection information.", null);
                        inviteStreamService.call(inviteSessionType, channel.getId(), null,
                                InviteErrorCode.ERROR_FOR_RESET_SSRC.getCode(),
                                "The subordinate customized ssrc and failed to reset the traffic collection information.", null);
                        inviteStreamService.removeInviteInfo(inviteInfo);

                    }else {
                        ssrcInfo.setSsrc(ssrcInResponse);
                        inviteInfo.setSsrcInfo(ssrcInfo);
                        inviteInfo.setStream(ssrcInfo.getStream());
                        if (device.getStreamMode().equalsIgnoreCase("TCP-ACTIVE")) {
                            if (mediaServerItem.isRtpEnable()) {
                                tcpActiveHandler(device, channel, contentString, mediaServerItem,  ssrcInfo, callback, inviteSessionType, inviteInfo);
                            }else {
                                log.warn("[Invite 200OK] The single-port traffic collection mode does not support TCP active mode traffic collection.");
                            }
                        }
                        inviteStreamService.updateInviteInfo(inviteInfo);
                    }
                } else {
                    ssrcInfo.setSsrc(ssrcInResponse);
                    inviteInfo.setSsrcInfo(ssrcInfo);
                    inviteInfo.setStream(ssrcInfo.getStream());
                    if (device.getStreamMode().equalsIgnoreCase("TCP-ACTIVE")) {
                        if (mediaServerItem.isRtpEnable()) {
                            tcpActiveHandler(device, channel, contentString, mediaServerItem,  ssrcInfo, callback, inviteSessionType, inviteInfo);
                        }else {
                            log.warn("[Invite 200OK] The single-port traffic collection mode does not support TCP active mode traffic collection.");
                        }
                    }
                    inviteStreamService.updateInviteInfo(inviteInfo);
                }
            }else {
                if (ssrcInResponse != null) {
                    // single port
                    // Resubscribe stream goes live
                    SsrcTransaction ssrcTransaction = sessionManager.getSsrcTransactionByStream(MediaStreamUtil.RTP_APP, inviteInfo.getStream());
                    if (ssrcTransaction == null) {
                        return;
                    }
                    sessionManager.removeByStream(MediaStreamUtil.RTP_APP, inviteInfo.getStream());
                    inviteStreamService.updateInviteInfoForSSRC(inviteInfo, ssrcInResponse);
                    ssrcTransaction.setDeviceId(device.getDeviceId());
                    ssrcTransaction.setChannelId(ssrcTransaction.getChannelId());
                    ssrcTransaction.setCallId(ssrcTransaction.getCallId());
                    ssrcTransaction.setSsrc(ssrcInResponse);
                    ssrcTransaction.setApp(MediaStreamUtil.RTP_APP);
                    ssrcTransaction.setStream(inviteInfo.getStream());
                    ssrcTransaction.setMediaServerId(mediaServerItem.getId());
                    ssrcTransaction.setSipTransactionInfo(new SipTransactionInfo((SIPResponse) responseEvent.getResponse()));
                    ssrcTransaction.setType(inviteSessionType);

                    sessionManager.put(ssrcTransaction);
                }
            }
        }
    }

    @Override
    public void download(Device device, DeviceChannel channel, String startTime, String endTime, int downloadSpeed, ErrorCallback<StreamInfo> callback) {

        if (!userSetting.getServerId().equals(device.getServerId())) {
            redisRpcPlayService.download(device.getServerId(), channel.getId(), startTime, endTime, downloadSpeed, callback);
            return;
        }

        MediaServer newMediaServerItem = this.getNewMediaServerItem(device);
        if (newMediaServerItem == null) {
            callback.run(InviteErrorCode.ERROR_FOR_ASSIST_NOT_READY.getCode(),
                    InviteErrorCode.ERROR_FOR_ASSIST_NOT_READY.getMsg(),
                    null);
            return;
        }

        download(newMediaServerItem, device, channel, startTime, endTime, downloadSpeed, callback);
    }


    private void download(MediaServer mediaServer, Device device, DeviceChannel channel, String startTime, String endTime, int downloadSpeed, ErrorCallback<StreamInfo> callback) {
        if (mediaServer == null ) {
            callback.run(InviteErrorCode.ERROR_FOR_PARAMETER_ERROR.getCode(),
                    InviteErrorCode.ERROR_FOR_PARAMETER_ERROR.getMsg(),
                    null);
            return;
        }

        SSRCInfo ssrcInfo = receiveRtpServerService.openGbRTPServerForDownload(mediaServer, device, channel, startTime, endTime, (code, msg, result) -> {
            if (code == InviteErrorCode.SUCCESS.getCode() && result != null && result.getHookData() != null) {
                // hookresponse
                StreamInfo streamInfo = onPublishHandlerForDownload(mediaServer, result.getHookData().getMediaInfo(), device, channel, startTime, endTime);
                if (streamInfo == null) {
                    log.warn("[Video download] Failed to obtain stream address information");
                    callback.run(InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getCode(),
                            InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getMsg(), null);
                    return;
                }
                callback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
                log.info("[Video download] Call successful deviceId: {}, channelId: {},  start time: {}, end time： {}", device.getDeviceId(), channel.getDeviceId(), startTime, endTime);
            }else {
                if (callback != null) {
                    callback.run(code, msg, null);
                }
                inviteStreamService.call(InviteSessionType.DOWNLOAD, channel.getId(), null, code, msg, null);
                inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.DOWNLOAD, channel.getId());
                if (result != null && result.getSsrcInfo() != null) {
                    SsrcTransaction ssrcTransaction = sessionManager.getSsrcTransactionByStream(result.getSsrcInfo().getApp(), result.getSsrcInfo().getStream());
                    if (ssrcTransaction != null) {
                        try {
                            cmder.streamByeCmd(device, channel.getDeviceId(), ssrcTransaction.getApp(), ssrcTransaction.getStream(), null, null);
                        } catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException e) {
                            log.error("[Video download] Sending BYE failed {}", e.getMessage());
                        } finally {
                            sessionManager.removeByStream(ssrcTransaction.getApp(), ssrcTransaction.getStream());
                        }
                    }
                }
            }
        });
        if (ssrcInfo == null || ssrcInfo.getPort() <= 0) {
            log.info("[Video download port/SSRC]Failed to obtain，deviceId={},channelId={},ssrcInfo={}", device.getDeviceId(), channel.getDeviceId(), ssrcInfo);
            if (callback != null) {
                callback.run(InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(), "Failed to obtain port or ssrc", null);
            }
            inviteStreamService.call(InviteSessionType.PLAY, channel.getId(), null,
                    InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(),
                    InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getMsg(),
                    null);
            return;
        }
        log.info("[Video download] deviceId: {}, channelId: {}, start time： {}, end time： {}， Download speed：{}, Flow collection port：{}, Flow collection mode：{}, SSRC: {}({}), SSRCVerification：{}",
                device.getDeviceId(), channel.getDeviceId(), startTime, endTime, downloadSpeed, ssrcInfo.getPort(), device.getStreamMode(),
                ssrcInfo.getSsrc(), String.format("%08x", Long.parseLong(ssrcInfo.getSsrc())).toUpperCase(),
                device.isSsrcCheck());

        // Initialize the invite message status in redis
        InviteInfo inviteInfo = InviteInfo.getInviteInfo(device.getDeviceId(), channel.getId(), ssrcInfo.getStream(), ssrcInfo, mediaServer.getId(),
                mediaServer.getSdpIp(), ssrcInfo.getPort(), device.getStreamMode(), InviteSessionType.DOWNLOAD,
                InviteSessionStatus.ready);
        inviteStreamService.updateInviteInfo(inviteInfo);
        try {
            cmder.downloadStreamCmd(mediaServer, ssrcInfo, device, channel, startTime, endTime, downloadSpeed,
                    eventResult -> {
                        // The other party returns an error
                        callback.run(InviteErrorCode.FAIL.getCode(), String.format("Video download failed, error code： %s, %s", eventResult.statusCode, eventResult.msg), null);
                        receiveRtpServerService.closeRTPServer(mediaServer, ssrcInfo.getApp(), ssrcInfo.getStream());
                        sessionManager.removeByStream(ssrcInfo.getApp(), ssrcInfo.getStream());
                        inviteStreamService.removeInviteInfo(inviteInfo);
                    }, eventResult ->{
                        // Handle the problem of TCP active connection and SSRC inconsistency after receiving 200ok
                        InviteOKHandler(eventResult, ssrcInfo, mediaServer, device, channel,
                                 callback, inviteInfo, InviteSessionType.DOWNLOAD);

                        // Register the video callback event and write the download address after the video download is completed.
                        HookSubscribe.Event hookEventForRecord = (hookData) -> {
                            log.info("[Video download] Received video recording written to disk message： ， {}/{}-{}",
                                    inviteInfo.getDeviceId(), inviteInfo.getChannelId(), ssrcInfo.getStream());
                            log.info("[Video download] Receive video recording content written to disk message： " + hookData);
                            RecordInfo recordInfo = hookData.getRecordInfo();
                            DownloadFileInfo downloadFileInfo = mediaServerService.getDownloadFilePath(mediaServer, recordInfo);
                            InviteInfo inviteInfoForNew = inviteStreamService.getInviteInfo(inviteInfo.getType()
                                    , inviteInfo.getChannelId(), inviteInfo.getStream());
                            if (inviteInfoForNew != null && inviteInfoForNew.getStreamInfo() != null) {
                                inviteInfoForNew.getStreamInfo().setDownLoadFilePath(downloadFileInfo);
                                // Failure to remove it immediately will result in subsequent interfaces not being able to obtain the download address.
                                inviteStreamService.updateInviteInfo(inviteInfoForNew, 60*15L);
                            }
                        };
                        Hook hook = Hook.getInstance(HookType.on_record_mp4, MediaStreamUtil.RTP_APP, ssrcInfo.getStream(), mediaServer.getId());
                        // Set expiration time and automatically process subscription data when download fails
                        hook.setExpireTime(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
                        subscribe.addSubscribe(hook, hookEventForRecord);
                    }, userSetting.getPlayTimeout().longValue());
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[Command sending failed] Video download: {}", e.getMessage());
            callback.run(InviteErrorCode.FAIL.getCode(),e.getMessage(), null);
            receiveRtpServerService.closeRTPServer(mediaServer, ssrcInfo.getApp(), ssrcInfo.getStream());
            sessionManager.removeByStream(ssrcInfo.getApp(), ssrcInfo.getStream());
            inviteStreamService.removeInviteInfo(inviteInfo);
        }
    }

    @Override
    public StreamInfo getDownLoadInfo(Device device, DeviceChannel channel, String stream) {


        InviteInfo inviteInfo = inviteStreamService.getInviteInfo(InviteSessionType.DOWNLOAD, channel.getId(), stream);
        if (inviteInfo == null) {
            String app = MediaStreamUtil.RTP_APP;
            StreamAuthorityInfo streamAuthorityInfo = redisCatchStorage.getStreamAuthorityInfo(app, stream);
            if (streamAuthorityInfo != null) {
                List<CloudRecordItem> allList = cloudRecordService.getAllList(null, app, stream, null, null, null, streamAuthorityInfo.getCallId(), null);
                if (allList.isEmpty()) {
                    log.warn("[Get download progress] No video download information found {}/{}-{}", device.getDeviceId(), channel.getDeviceId(), stream);
                    return null;
                }

                String mediaServerId = allList.get(0).getMediaServerId();
                MediaServer mediaServer = mediaServerService.getOne(mediaServerId);
                if (mediaServer == null) {
                    log.warn("[Get download progress] No node information for video download was found. {}/{}-{}", device.getDeviceId(), channel.getDeviceId(), stream);
                    return null;
                }
                log.warn("[Get download progress] It is found that the download has ended and the file is obtained directly from the database {}/{}-{}", device.getDeviceId(), channel.getDeviceId(), stream);
                DownloadFileInfo downloadFileInfo = mediaServerService.getDownloadFilePath(mediaServer, RecordInfo.getInstance(allList.get(0)));
                StreamInfo streamInfo = new StreamInfo();
                streamInfo.setDownLoadFilePath(downloadFileInfo);
                streamInfo.setApp(app);
                streamInfo.setStream(stream);
                streamInfo.setServerId(mediaServerId);
                streamInfo.setProgress(1.0);
                return streamInfo;
            }
        }

        if (inviteInfo == null || inviteInfo.getStreamInfo() == null) {
            log.warn("[Get download progress] No video download information found {}/{}-{}", device.getDeviceId(), channel.getDeviceId(), stream);
            return null;
        }

        if (inviteInfo.getStreamInfo().getProgress() == 1) {
            return inviteInfo.getStreamInfo();
        }

        // Get the current download time
        MediaServer mediaServerItem = inviteInfo.getStreamInfo().getMediaServer();
        if (mediaServerItem == null) {
            log.warn("[Get download progress] When querying recording information, it was found that the node does not exist.");
            return null;
        }
        String app = MediaStreamUtil.RTP_APP;
        Long duration  = mediaServerService.updateDownloadProcess(mediaServerItem, app, stream);
        if (duration == null || duration == 0) {
            inviteInfo.getStreamInfo().setProgress(0);
        } else {
            String startTime = inviteInfo.getStreamInfo().getStartTime();
            String endTime = inviteInfo.getStreamInfo().getEndTime();
            // At this time, the units of start and end are seconds.
            long start = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(startTime);
            long end = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(endTime);

            BigDecimal currentCount = new BigDecimal(duration);
            BigDecimal totalCount = new BigDecimal((end - start) * 1000);
            BigDecimal divide = currentCount.divide(totalCount, 2, RoundingMode.HALF_UP);
            double process = divide.doubleValue();
            if (process > 0.999) {
                process = 1.0;
            }
            inviteInfo.getStreamInfo().setProgress(process);
        }
        inviteStreamService.updateInviteInfo(inviteInfo);
        return inviteInfo.getStreamInfo();
    }

    private StreamInfo onPublishHandlerForDownload(MediaServer mediaServerItemInuse, MediaInfo mediaInfo, Device device, DeviceChannel channel, String startTime, String endTime) {
        StreamInfo streamInfo = onPublishHandler(mediaServerItemInuse, mediaInfo, device, channel);
        if (streamInfo != null) {
            streamInfo.setProgress(0);
            streamInfo.setStartTime(startTime);
            streamInfo.setEndTime(endTime);
            InviteInfo inviteInfo = inviteStreamService.getInviteInfo(InviteSessionType.DOWNLOAD, channel.getId(), streamInfo.getStream());
            if (inviteInfo != null) {
                log.info("[Video download] Update the stream information in the invite message");
                inviteInfo.setStatus(InviteSessionStatus.ok);
                inviteInfo.setStreamInfo(streamInfo);
                inviteStreamService.updateInviteInfo(inviteInfo);
            }
        }
        return streamInfo;
    }


    public StreamInfo onPublishHandler(MediaServer mediaServerItem, MediaInfo mediaInfo, Device device, DeviceChannel channel) {
        StreamInfo streamInfo = mediaServerService.getStreamInfoByAppAndStream(mediaServerItem, MediaStreamUtil.RTP_APP, mediaInfo.getStream(), mediaInfo, null);
        streamInfo.setDeviceId(device.getDeviceId());
        streamInfo.setChannelId(channel.getId());
        return streamInfo;
    }


    @Override
    public void zlmServerOffline(MediaServer mediaServer) {
        // Handle the upper-level platform that is pushing up the flow
        List<SendRtpInfo> sendRtpInfos = sendRtpServerService.queryAll();
        if (!sendRtpInfos.isEmpty()) {
            for (SendRtpInfo sendRtpInfo : sendRtpInfos) {
                if (sendRtpInfo.getMediaServerId().equals(mediaServer.getId()) && sendRtpInfo.isSendToPlatform()) {
                    Platform platform = platformService.queryPlatformByServerGBId(sendRtpInfo.getTargetId());
                    CommonGBChannel channel = channelService.getOne(sendRtpInfo.getChannelId());
                    try {
                        sipCommanderFroPlatform.streamByeCmd(platform, sendRtpInfo, channel);
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        log.error("[Command sending failed] National standard cascade sendBYE: {}", e.getMessage());
                    }
                }
            }
        }
        // Handle the GB device you are viewing
        List<SsrcTransaction> allSsrc = sessionManager.getAll();
        if (allSsrc.size() > 0) {
            for (SsrcTransaction ssrcTransaction : allSsrc) {
                if (ssrcTransaction.getMediaServerId().equals(mediaServer.getId())) {
                    Device device = deviceService.getDeviceByDeviceId(ssrcTransaction.getDeviceId());
                    if (device == null) {
                        continue;
                    }
                    DeviceChannel deviceChannel = deviceChannelService.getOneById(ssrcTransaction.getChannelId());
                    if (deviceChannel == null) {
                        continue;
                    }
                    try {
                        cmder.streamByeCmd(device, deviceChannel.getDeviceId(), ssrcTransaction.getApp(),
                                ssrcTransaction.getStream(), null, null);
                    } catch (InvalidArgumentException | ParseException | SipException |
                             SsrcTransactionNotFoundException e) {
                        log.error("[zlmOffline]Sending BYE failed for the device using this zlm {}", e.getMessage());
                    }
                }
            }
        }
    }

    @Override
    public AudioBroadcastResult audioBroadcast(String deviceId, String channelDeviceId, Boolean broadcastMode) {

        Device device = deviceService.getDeviceByDeviceId(deviceId);
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "Device not found： " + deviceId);
        }
        DeviceChannel deviceChannel = deviceChannelService.getOne(deviceId, channelDeviceId);
        if (deviceChannel == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "Channel not found： " + channelDeviceId);
        }

        if (!userSetting.getServerId().equals(device.getServerId())) {
            return redisRpcPlayService.audioBroadcast(device.getServerId(), deviceId, channelDeviceId, broadcastMode);
        }
        log.info("[Voice call] device： {}, channel: {}", device.getDeviceId(), deviceChannel.getDeviceId());
        MediaServer mediaServerItem = mediaServerService.getMediaServerForMinimumLoad(null);
        if (broadcastMode == null) {
            broadcastMode = true;
        }
        String app = broadcastMode ? MediaStreamUtil.GB28181_BROADCAST : MediaStreamUtil.GB28181_TALK;
        String stream = device.getDeviceId() + "_" + deviceChannel.getDeviceId();
        AudioBroadcastResult audioBroadcastResult = new AudioBroadcastResult();
        audioBroadcastResult.setApp(app);
        audioBroadcastResult.setStream(stream);
        audioBroadcastResult.setStreamInfo(new StreamContent(mediaServerService.getStreamInfoByAppAndStream(mediaServerItem, app, stream, null, null, null, false)));
        if (!broadcastMode) {
            audioBroadcastResult.setPlayStreamInfo(new StreamContent(
                    mediaServerService.getStreamInfoByAppAndStream(mediaServerItem,
                            MediaStreamUtil.GB28181_TALK, stream + "_talk",
                            null, null, null, true)));
        }
        audioBroadcastResult.setCodec("G.711");
        return audioBroadcastResult;
    }

    @Override
    public boolean audioBroadcastCmd(Device device, DeviceChannel deviceChannel, MediaServer mediaServerItem, String app, String stream, int timeout, boolean isFromPlatform, AudioBroadcastEvent event) throws InvalidArgumentException, ParseException, SipException {
        Assert.notNull(device, "Device does not exist");
        Assert.notNull(deviceChannel, "Channel does not exist");
        log.info("[Voice call] device： {}, channel: {}", device.getDeviceId(), deviceChannel.getDeviceId());
        // Query channel usage status
        if (audioBroadcastManager.exit(deviceChannel.getId())) {
            SendRtpInfo sendRtpInfo = sendRtpServerService.queryByChannelId(deviceChannel.getId(), device.getDeviceId());
            if (sendRtpInfo != null && sendRtpInfo.isOnlyAudio()) {
                // Query whether the stream exists. If it does not exist, it is considered an abnormal state.
                Boolean streamReady = mediaServerService.isStreamReady(mediaServerItem, sendRtpInfo.getApp(), sendRtpInfo.getStream());
                if (streamReady) {
                    log.warn("Voice broadcast has been turned on： {}", deviceChannel.getDeviceId());
                    event.call("Voice broadcast has been turned on");
                    return false;
                } else {
                    stopAudioBroadcast(device, deviceChannel);
                }
            }
        }

        // Send notification
        cmder.audioBroadcastCmd(device, deviceChannel.getDeviceId(), eventResultForOk -> {
            // Sent successfully
            AudioBroadcastCatch audioBroadcastCatch = new AudioBroadcastCatch(device.getDeviceId(), deviceChannel.getId(), mediaServerItem, app, stream, event, AudioBroadcastCatchStatus.Ready, isFromPlatform);
            audioBroadcastManager.update(audioBroadcastCatch);
            // Wait for the invite message and end when timeout occurs
            String key = VideoManagerConstants.BROADCAST_WAITE_INVITE +  device.getDeviceId();
            if (!SipUtils.isFrontEnd(device.getDeviceId())) {
                key += audioBroadcastCatch.getChannelId();
            }
            dynamicTask.startDelay(key, ()->{
                log.info("[voice broadcast]Timeout waiting for invite message：{}/{}", device.getDeviceId(), deviceChannel.getDeviceId());
                stopAudioBroadcast(device, deviceChannel);
            }, 10*1000);
        }, eventResultForError -> {
            // Sending failed
            log.error("Voice broadcast failed to send： {}:{}", deviceChannel.getDeviceId(), eventResultForError.msg);
            event.call("Voice broadcast failed to send");
            stopAudioBroadcast(device, deviceChannel);
        });
        return true;
    }

    @Override
    public boolean audioBroadcastInUse(Device device, DeviceChannel channel) {
        if (audioBroadcastManager.exit(channel.getId())) {
            SendRtpInfo sendRtpInfo = sendRtpServerService.queryByChannelId(channel.getId(), device.getDeviceId());
            if (sendRtpInfo != null && sendRtpInfo.isOnlyAudio()) {
                // Query whether the stream exists. If it does not exist, it is considered an abnormal state.
                MediaServer mediaServerServiceOne = mediaServerService.getOne(sendRtpInfo.getMediaServerId());
                Boolean streamReady = mediaServerService.isStreamReady(mediaServerServiceOne, sendRtpInfo.getApp(), sendRtpInfo.getStream());
                if (streamReady) {
                    log.warn("The voice broadcast channel is in use： {}", channel.getDeviceId());
                    return true;
                }
            }
        }
        return false;
    }


    @Override
    public void stopAudioBroadcast(Device device, DeviceChannel channel) {
        log.info("[Stop intercom] Equipment：{}, channel：{}", device.getDeviceId(), channel.getDeviceId());
        List<AudioBroadcastCatch> audioBroadcastCatchList = new ArrayList<>();
        if (channel == null) {
            audioBroadcastCatchList.addAll(audioBroadcastManager.getByDeviceId(device.getDeviceId()));
        } else {
            audioBroadcastCatchList.addAll(audioBroadcastManager.getByDeviceId(device.getDeviceId()));
        }
        if (!audioBroadcastCatchList.isEmpty()) {
            for (AudioBroadcastCatch audioBroadcastCatch : audioBroadcastCatchList) {
                if (audioBroadcastCatch == null) {
                    continue;
                }
                SendRtpInfo sendRtpInfo = sendRtpServerService.queryByChannelId(channel.getId(), device.getDeviceId());
                if (sendRtpInfo != null) {
                    sendRtpServerService.delete(sendRtpInfo);
                    MediaServer mediaServer = mediaServerService.getOne(sendRtpInfo.getMediaServerId());
                    mediaServerService.stopSendRtp(mediaServer, sendRtpInfo.getApp(), sendRtpInfo.getStream(), null);
                    try {
                        cmder.streamByeCmdForDeviceInvite(device, channel.getDeviceId(), audioBroadcastCatch.getSipTransactionInfo(), null);
                    } catch (InvalidArgumentException | ParseException | SipException |
                             SsrcTransactionNotFoundException e) {
                        log.error("[Message sending failed] Failed to send voice call BYE");
                    }
                }

                audioBroadcastManager.del(channel.getId());
            }
        }
    }

    @Override
    public void zlmServerOnline(MediaServer mediaServer) {
        // get
        List<InviteInfo> inviteInfoList = inviteStreamService.getAllInviteInfo();
        if (inviteInfoList.isEmpty()) {
            return;
        }

        List<String> rtpServerList = mediaServerService.listRtpServer(mediaServer);
        if (rtpServerList.isEmpty()) {
            return;
        }
        for (InviteInfo inviteInfo : inviteInfoList) {
            if (!rtpServerList.contains(inviteInfo.getStream())){
                inviteStreamService.removeInviteInfo(inviteInfo);
            }
        }
    }

    @Override
    public void playbackPause(String streamId) throws ServiceException, InvalidArgumentException, ParseException, SipException {

        InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(InviteSessionType.PLAYBACK, streamId);
        if (null == inviteInfo || inviteInfo.getStreamInfo() == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "streamIddoes not exist");
        }
        Device device = deviceService.getDeviceByDeviceId(inviteInfo.getDeviceId());
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Device does not exist");
        }
        if (!userSetting.getServerId().equals(device.getServerId())) {
            redisRpcPlayService.playbackPause(device.getServerId(), streamId);
            return;
        }

        inviteInfo.getStreamInfo().setPause(true);
        inviteStreamService.updateInviteInfo(inviteInfo);
        MediaServer mediaServerItem = inviteInfo.getStreamInfo().getMediaServer();
        if (null == mediaServerItem) {
            log.warn("mediaServer does not exist!");
            throw new ServiceException("mediaServerdoes not exist");
        }
        // zlm Pause RTP timeout check
        // Using streams in zlmID
        String streamKey = inviteInfo.getStream();
        if (!mediaServerItem.isRtpEnable()) {
            streamKey = Long.toHexString(Long.parseLong(inviteInfo.getSsrcInfo().getSsrc())).toUpperCase();
        }
        Boolean result = mediaServerService.pauseRtpCheck(mediaServerItem, streamKey);
        if (!result) {
            throw new ServiceException("Pause RTP reception failed");
        }

        DeviceChannel channel = deviceChannelService.getOneById(inviteInfo.getChannelId());
        cmder.playPauseCmd(device, channel, inviteInfo.getStreamInfo());
    }

    @Override
    public void playbackResume(String streamId) throws ServiceException, InvalidArgumentException, ParseException, SipException {
        InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(InviteSessionType.PLAYBACK, streamId);
        if (null == inviteInfo || inviteInfo.getStreamInfo() == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "streamIddoes not exist");
        }
        Device device = deviceService.getDeviceByDeviceId(inviteInfo.getDeviceId());
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Device does not exist");
        }
        if (!userSetting.getServerId().equals(device.getServerId())) {
            redisRpcPlayService.playbackResume(device.getServerId(), streamId);
            return;
        }

        inviteInfo.getStreamInfo().setPause(false);
        inviteStreamService.updateInviteInfo(inviteInfo);
        MediaServer mediaServerItem = inviteInfo.getStreamInfo().getMediaServer();
        if (null == mediaServerItem) {
            log.warn("mediaServer does not exist!");
            throw new ServiceException("mediaServerdoes not exist");
        }
        // Using streams in zlmID
        String streamKey = inviteInfo.getStream();
        if (!mediaServerItem.isRtpEnable()) {
            streamKey = Long.toHexString(Long.parseLong(inviteInfo.getSsrcInfo().getSsrc())).toUpperCase();
        }
        boolean result = mediaServerService.resumeRtpCheck(mediaServerItem, streamKey);
        if (!result) {
            throw new ServiceException("Continue RTP reception failed");
        }
        DeviceChannel channel = deviceChannelService.getOneById(inviteInfo.getChannelId());
        cmder.playResumeCmd(device, channel, inviteInfo.getStreamInfo());
    }

    @Override
    public void playbackSeek(String streamId, long seekTime) throws InvalidArgumentException, ParseException, SipException {
        InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(InviteSessionType.PLAYBACK, streamId);

        if (null == inviteInfo || inviteInfo.getStreamInfo() == null) {
            log.warn("streamIddoes not exist!");
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "streamIddoes not exist");
        }
        Device device = deviceService.getDeviceByDeviceId(inviteInfo.getDeviceId());
        DeviceChannel channel = deviceChannelService.getOneById(inviteInfo.getChannelId());
        cmder.playSeekCmd(device, channel, inviteInfo.getStreamInfo(), seekTime);
    }

    @Override
    public void playbackSpeed(String streamId, double speed) throws InvalidArgumentException, ParseException, SipException {
        InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(InviteSessionType.PLAYBACK, streamId);

        if (null == inviteInfo || inviteInfo.getStreamInfo() == null) {
            log.warn("streamIddoes not exist!");
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "streamIddoes not exist");
        }
        Device device = deviceService.getDeviceByDeviceId(inviteInfo.getDeviceId());
        DeviceChannel channel = deviceChannelService.getOneById(inviteInfo.getChannelId());
        cmder.playSpeedCmd(device, channel, inviteInfo.getStreamInfo(), speed);
    }

    @Override
    public void startPushStream(SendRtpInfo sendRtpInfo, DeviceChannel channel, SIPResponse sipResponse, Platform platform, CallIdHeader callIdHeader) {
        // Start streaming
        MediaServer mediaInfo = mediaServerService.getOne(sendRtpInfo.getMediaServerId());

        if (mediaInfo != null) {
            try {
                if (sendRtpInfo.isTcpActive()) {
                    mediaServerService.startSendRtpPassive(mediaInfo, sendRtpInfo, null);
                } else {
                    mediaServerService.startSendRtp(mediaInfo, sendRtpInfo);
                }
                redisCatchStorage.sendPlatformStartPlayMsg(sendRtpInfo, channel, platform);
            }catch (ControllerException e) {
                log.error("RTPPush failed: {}", e.getMessage());
                startSendRtpStreamFailHand(sendRtpInfo, platform, callIdHeader);
                return;
            }

            log.info("RTPSuccessful push[ {}/{} ]，{}, ", sendRtpInfo.getApp(), sendRtpInfo.getStream(),
                    sendRtpInfo.isTcpActive()?"Passive flow": sendRtpInfo.getIp() + ":" + sendRtpInfo.getPort());

        }
    }

    @Override
    public void startSendRtpStreamFailHand(SendRtpInfo sendRtpInfo, Platform platform, CallIdHeader callIdHeader) {
        if (sendRtpInfo.isOnlyAudio()) {
            Device device = deviceService.getDeviceByDeviceId(sendRtpInfo.getTargetId());
            DeviceChannel deviceChannel = deviceChannelService.getOneById(sendRtpInfo.getChannelId());
            AudioBroadcastCatch audioBroadcastCatch = audioBroadcastManager.get(sendRtpInfo.getChannelId());
            if (audioBroadcastCatch != null) {
                try {
                    cmder.streamByeCmd(device, deviceChannel.getDeviceId(), audioBroadcastCatch.getSipTransactionInfo(), null);
                } catch (SipException | ParseException | InvalidArgumentException |
                         SsrcTransactionNotFoundException exception) {
                    log.error("[Command sending failed] Stop voice intercom: {}", exception.getMessage());
                }
            }
        } else {
            if (platform != null) {
                // Upward platform
                CommonGBChannel channel = channelService.getOne(sendRtpInfo.getChannelId());
                try {
                    commanderForPlatform.streamByeCmd(platform, sendRtpInfo, channel);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[Command sending failed] National standard cascade sendBYE: {}", e.getMessage());
                }
            }

        }
    }

    @Override
    public void talkCmd(Device device, DeviceChannel channel, MediaServer mediaServerItem, String stream, AudioBroadcastEvent event) {
        if (device == null || channel == null) {
            return;
        }
        // TODO Multi-port mode is required to support voice call crane voice intercom
        log.info("[Voice intercom] device： {}, channel: {}", device.getDeviceId(), channel.getDeviceId());
        // Query channel usage status
        if (audioBroadcastManager.exit(channel.getId())) {
            SendRtpInfo sendRtpInfo = sendRtpServerService.queryByChannelId(channel.getId(), device.getDeviceId());
            if (sendRtpInfo != null && sendRtpInfo.isOnlyAudio()) {
                // Query whether the stream exists. If it does not exist, it is considered an abnormal state.
                MediaServer mediaServer = mediaServerService.getOne(sendRtpInfo.getMediaServerId());
                Boolean streamReady = mediaServerService.isStreamReady(mediaServer, sendRtpInfo.getApp(), sendRtpInfo.getStream());
                if (streamReady) {
                    log.warn("[Voice intercom] Voice broadcast in progress, voice call cannot be started： {}", channel.getDeviceId());
                    event.call("Voice broadcast in progress");
                    return;
                } else {
                    stopAudioBroadcast(device, channel);
                }
            }
        }

        SendRtpInfo sendRtpInfo = sendRtpServerService.queryByChannelId(channel.getId(), device.getDeviceId());
        if (sendRtpInfo != null) {
            MediaServer mediaServer = mediaServerService.getOne(sendRtpInfo.getMediaServerId());
            Boolean streamReady = mediaServerService.isStreamReady(mediaServer, MediaStreamUtil.GB28181_TALK, sendRtpInfo.getReceiveStream());
            if (streamReady) {
                log.warn("[Voice intercom] In progress： {}", channel.getDeviceId());
                event.call("Voice intercom in progress");
                return;
            } else {
                stopTalk(device, channel);
            }
        }

        talk(mediaServerItem, device, channel, stream, eventResult -> {
            log.warn("[Voice intercom] failed，{}/{}, error code {} {}", device.getDeviceId(), channel.getDeviceId(), eventResult.statusCode, eventResult.msg);
            event.call("Failure, error code " + eventResult.statusCode + ", " + eventResult.msg);
        }, () -> {
            log.warn("[Voice intercom] failed，{}/{} timeout", device.getDeviceId(), channel.getDeviceId());
            event.call("failed, timed out ");
            stopTalk(device, channel);
        }, errorMsg -> {
            log.warn("[Voice intercom] failed，{}/{} {}", device.getDeviceId(), channel.getDeviceId(), errorMsg);
            event.call(errorMsg);
            stopTalk(device, channel);
        });
    }

    private void stopTalk(Device device, DeviceChannel channel) {
        stopTalk(device, channel, null);
    }

    @Override
    public void stopTalk(Device device, DeviceChannel channel, Boolean streamIsReady) {
        log.info("[Voice intercom] stop， {}/{}", device.getDeviceId(), channel.getDeviceId());
        SendRtpInfo sendRtpInfo = sendRtpServerService.queryByChannelId(channel.getId(), device.getDeviceId());
        if (sendRtpInfo == null) {
            log.info("[Voice intercom] Stop failed, sending message not found, may have been stopped");
            return;
        }
        // Stop streaming to device
        String mediaServerId = sendRtpInfo.getMediaServerId();
        if (mediaServerId == null) {
            return;
        }

        MediaServer mediaServer = mediaServerService.getOne(mediaServerId);

        if (streamIsReady == null || streamIsReady) {
            mediaServerService.stopSendRtp(mediaServer, sendRtpInfo.getApp(), sendRtpInfo.getStream(), sendRtpInfo.getSsrc());
        }

        SsrcTransaction ssrcTransaction = sessionManager.getSsrcTransactionByStream(sendRtpInfo.getApp(), sendRtpInfo.getStream());
        if (ssrcTransaction != null) {
            try {
                cmder.streamByeCmd(device, channel.getDeviceId(), sendRtpInfo.getApp(), sendRtpInfo.getStream(), null, null);
            } catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException  e) {
                log.info("[Voice intercom] Stop message sending failed, may have stopped");
            }
        }
        sendRtpServerService.deleteByChannel(channel.getId(), device.getDeviceId());
    }

    @Override
    public void getSnap(String deviceId, String channelId, String fileName, ErrorCallback errorCallback) {
        Device device = deviceService.getDeviceByDeviceId(deviceId);
        Assert.notNull(device, "Device does not exist");
        DeviceChannel channel = deviceChannelService.getOne(deviceId, channelId);
        Assert.notNull(channel, "Channel does not exist");
        InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
        if (inviteInfo != null) {
            if (inviteInfo.getStreamInfo() != null) {
                // Take screenshots that already exist online
                MediaServer mediaServer = inviteInfo.getStreamInfo().getMediaServer();
                String path = "snap";
                // Request a screenshot
                log.info("[Request a screenshot]: " + fileName);
                mediaServerService.getSnap(mediaServer, MediaStreamUtil.RTP_APP,  inviteInfo.getStreamInfo().getStream(), 15, 1, path, fileName);
                File snapFile = new File(path + File.separator + fileName);
                if (snapFile.exists()) {
                    errorCallback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), snapFile.getAbsoluteFile());
                }else {
                    errorCallback.run(InviteErrorCode.FAIL.getCode(), InviteErrorCode.FAIL.getMsg(), null);
                }
                return;
            }
        }

        MediaServer newMediaServerItem = getNewMediaServerItem(device);
        play(newMediaServerItem, deviceId, channelId, null, (code, msg, data)->{
            if (code == InviteErrorCode.SUCCESS.getCode()) {
                InviteInfo inviteInfoForPlay = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
                if (inviteInfoForPlay != null && inviteInfoForPlay.getStreamInfo() != null) {
                    getSnap(deviceId, channelId, fileName, errorCallback);
                }else {
                    errorCallback.run(InviteErrorCode.FAIL.getCode(), InviteErrorCode.FAIL.getMsg(), null);
                }
            }else {
                errorCallback.run(InviteErrorCode.FAIL.getCode(), InviteErrorCode.FAIL.getMsg(), null);
            }
        });
    }

    @Override
    public void getSnap(CommonGBChannel channel, ErrorCallback<byte[]> errorCallback) {
        // 2016The protocol does not support directly obtaining screenshots of the national standard channel, and can only obtain them through on-demand.
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            log.warn("[Snapshot] Channel not found{}device information", channel);
            errorCallback.run(InviteErrorCode.FAIL.getCode(), "Device information not found", null);
            return;
        }
        DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(channel.getGbId());
        if (deviceChannel == null) {
            log.warn("[Snapshot] Channel not found{}device information", channel);
            errorCallback.run(InviteErrorCode.FAIL.getCode(), "Original channel not found", null);
            return;
        }

        InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getGbId());
        if (inviteInfo != null) {
            if (inviteInfo.getStreamInfo() != null) {
                // Take screenshots that already exist online
                MediaServer mediaServer = inviteInfo.getStreamInfo().getMediaServer();
                String path = "snap";
                // Request a screenshot
                log.info("[Request a screenshot]: Return byte array" );
                byte[] snapByteArray = mediaServerService.getSnap(mediaServer, MediaStreamUtil.RTP_APP,  inviteInfo.getStreamInfo().getStream(), 15, 1, path, null);
                if (snapByteArray != null) {
                    errorCallback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), snapByteArray);
                }else {
                    errorCallback.run(InviteErrorCode.FAIL.getCode(), InviteErrorCode.FAIL.getMsg(), null);
                }
                return;
            }
        }

        play(device, deviceChannel, (code, msg, data)->{
            if (code == InviteErrorCode.SUCCESS.getCode()) {
                InviteInfo inviteInfoForPlay = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getGbId());
                if (inviteInfoForPlay != null && inviteInfoForPlay.getStreamInfo() != null) {
                    byte[] snapByteArray = mediaServerService.getSnap(data.getMediaServer(), MediaStreamUtil.RTP_APP,  data.getStream(), 15, 1, null, null);
                    errorCallback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), snapByteArray);
                }else {
                    errorCallback.run(InviteErrorCode.FAIL.getCode(), InviteErrorCode.FAIL.getMsg(), null);
                }
            }else {
                errorCallback.run(InviteErrorCode.FAIL.getCode(), InviteErrorCode.FAIL.getMsg(), null);
            }
        });
    }



    @Override
    public void stop(InviteSessionType type, Device device, DeviceChannel channel, String stream) {
        if (!userSetting.getServerId().equals(device.getServerId())) {
            redisRpcPlayService.stop(device.getServerId(), type,  channel.getId(), stream);
        }else {
            log.info("[Stop on demand/Playback/Download] {}/{}", device.getDeviceId(), channel.getDeviceId());
            InviteInfo inviteInfo = inviteStreamService.getInviteInfo(type, channel.getId(), stream);
            if (inviteInfo == null) {
                if (type == InviteSessionType.PLAY) {
                    deviceChannelService.stopPlay(channel.getId());
                }
                return;
            }
            inviteStreamService.removeInviteInfo(inviteInfo);
            if (InviteSessionStatus.ok == inviteInfo.getStatus()) {
                try {
                    log.info("[Stop on demand/Playback/Download] success {}/{}", device.getDeviceId(), channel.getDeviceId());
                    cmder.streamByeCmd(device, channel.getDeviceId(), MediaStreamUtil.RTP_APP, inviteInfo.getStream(), null, null);
                } catch (InvalidArgumentException | SipException | ParseException | SsrcTransactionNotFoundException e) {
                    log.error("[Command sending failed] Stop on demand/Playback/download, sendBYE: {}", e.getMessage());
                    throw new ControllerException(ErrorCode.ERROR100.getCode(), "Command sending failed: " + e.getMessage());
                }
            }

            if (inviteInfo.getType() == InviteSessionType.PLAY) {
                deviceChannelService.stopPlay(channel.getId());
            }
            if (inviteInfo.getStreamInfo() != null) {
                receiveRtpServerService.closeRTPServer(inviteInfo.getStreamInfo().getMediaServer(), MediaStreamUtil.RTP_APP, stream);
            }
        }
    }

    @Override
    public void stop(InviteInfo inviteInfo) {
        Assert.notNull(inviteInfo, "Parameter exception");
        DeviceChannel channel = deviceChannelService.getOneForSourceById(inviteInfo.getChannelId());
        if (channel == null) {
            log.warn("[Stop on demand] Found that the channel does not exist");
            return;
        }
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            log.warn("[Stop on demand] Found the device does not exist");
            return;
        }
        inviteStreamService.removeInviteInfo(inviteInfo);
        if (InviteSessionStatus.ok == inviteInfo.getStatus()) {
            try {
                log.info("[Stop on demand/Playback/Download] {}/{}", device.getDeviceId(), channel.getDeviceId());
                cmder.streamByeCmd(device, channel.getDeviceId(), MediaStreamUtil.RTP_APP, inviteInfo.getStream(), null, null);
            } catch (InvalidArgumentException | SipException | ParseException | SsrcTransactionNotFoundException e) {
                log.warn("[Command sending failed] Stop on demand/Playback/download, sendBYE: {}", e.getMessage());
            }
        }

        if (inviteInfo.getType() == InviteSessionType.PLAY) {
            deviceChannelService.stopPlay(channel.getId());
        }
        if (inviteInfo.getStreamInfo() != null) {
            receiveRtpServerService.closeRTPServer(inviteInfo.getStreamInfo().getMediaServer(), MediaStreamUtil.RTP_APP, inviteInfo.getStream());
        }
    }

    @Override
    public void play(CommonGBChannel channel, Boolean record, ErrorCallback<StreamInfo> callback) {
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            log.warn("[on demand] Channel not found{}device information", channel);
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
        DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(channel.getGbId());

        MediaServer mediaServerItem = getNewMediaServerItem(device);
        if (mediaServerItem == null) {
            log.warn("[on demand] Not found availablezlm deviceId: {},channelId:{}", device.getDeviceId(), deviceChannel.getDeviceId());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Not found availablezlm");
        }
        play(mediaServerItem, device, deviceChannel, null, record, callback);

    }

    @Override
    public void stopPlay(InviteSessionType inviteSessionType, CommonGBChannel channel) {
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            log.warn("[Stop playing] Channel not found{}device information", channel);
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
        DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(channel.getGbId());
        String stream = String.format("%s_%s", device.getDeviceId(), deviceChannel.getDeviceId());
        stop(inviteSessionType, device, deviceChannel, stream);
    }

    @Override
    public void stop(InviteSessionType inviteSessionType, CommonGBChannel channel, String stream) {
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            log.warn("[Stop playing] Channel not found{}device information", channel);
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
        DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(channel.getGbId());
        stop(inviteSessionType, device, deviceChannel, stream);
    }

    @Override
    public void playBack(CommonGBChannel channel, Long startTime, Long stopTime, ErrorCallback<StreamInfo> callback) {
        if (startTime == null || stopTime == null) {
            throw new PlayException(Response.BAD_REQUEST, "bad request");
        }
        // National standard channel
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            log.warn("[on demand] Channel not found{}device information", channel);
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
        DeviceChannel deviceChannel = deviceChannelService.getOneById(channel.getGbId());
        if (deviceChannel == null) {
            log.warn("[on demand] Channel not found{}", channel.getGbDeviceId());
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
        String startTimeStr = DateUtil.timestampTo_yyyy_MM_dd_HH_mm_ss(startTime);
        String stopTimeStr = DateUtil.timestampTo_yyyy_MM_dd_HH_mm_ss(stopTime);
        playBack(device, deviceChannel, startTimeStr, stopTimeStr, callback);
    }

    @Override
    public void download(CommonGBChannel channel, Long startTime, Long stopTime, Integer downloadSpeed, ErrorCallback<StreamInfo> callback) {
        if (startTime == null || stopTime == null || downloadSpeed == null) {
            throw new PlayException(Response.BAD_REQUEST, "bad request");
        }
        // National standard channel
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            log.warn("[on demand] Channel not found{}device information", channel);
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
        DeviceChannel deviceChannel = deviceChannelService.getOneById(channel.getGbId());
        if (deviceChannel == null) {
            log.warn("[on demand] Channel not found{}", channel.getGbDeviceId());
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
        String startTimeStr = DateUtil.timestampTo_yyyy_MM_dd_HH_mm_ss(startTime);
        String stopTimeStr = DateUtil.timestampTo_yyyy_MM_dd_HH_mm_ss(stopTime);
        download(device, deviceChannel, startTimeStr, stopTimeStr, downloadSpeed, callback);

    }
}
