package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.cmd;

import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.service.*;
import com.genersoft.iot.vmp.gb28181.session.AudioBroadcastManager;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.NotifyMessageHandler;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.ISendRtpServerService;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;

/**
 * Voice call request
 */
@Slf4j
@Component
public class BroadcastNotifyMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private final static String cmdType = "Broadcast";

    @Autowired
    private NotifyMessageHandler notifyMessageHandler;

    @Autowired
    private IGbChannelService channelService;

    @Autowired
    private ISIPCommanderForPlatform commanderForPlatform;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private IPlayService playService;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private IDeviceChannelService deviceChannelService;

    @Autowired
    private IPlatformService platformService;

    @Autowired
    private AudioBroadcastManager audioBroadcastManager;

    @Autowired
    private ISendRtpServerService sendRtpServerService;

    @Override
    public void afterPropertiesSet() throws Exception {
        notifyMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element element) {
        try {
            responseAck((SIPRequest) evt.getRequest(), Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[Command sending failed] Reply200 OK: {}", e.getMessage());
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, Platform platform, Element rootElement) {
        // Voice call request from superior platform
        SIPRequest request = (SIPRequest) evt.getRequest();
        try {
            Element snElement = rootElement.element("SN");
            if (snElement == null) {
                responseAck(request, Response.BAD_REQUEST, "sn must not null");
                return;
            }
            String sn = snElement.getText();
            Element targetIDElement = rootElement.element("TargetID");
            if (targetIDElement == null) {
                responseAck(request, Response.BAD_REQUEST, "TargetID must not null");
                return;
            }
            String targetId = targetIDElement.getText();

            Element sourceIdElement = rootElement.element("SourceID");
            String sourceId;
            if (sourceIdElement != null) {
                sourceId = sourceIdElement.getText();
            }else {
                sourceId = targetId;
            }
            log.info("[National standard cascade voice announcement] platform: {}, channel: {}", platform.getServerGBId(), targetId);

            CommonGBChannel channel = channelService.queryOneWithPlatform(platform.getId(), targetId);
            if (channel == null) {
                log.warn("[National standard cascade voice announcement] Channel not found platform: {}, channel: {}", platform.getServerGBId(), targetId);
                responseAck(request, Response.NOT_FOUND, "TargetID not found");
                return;
            }
            if (channel.getDataType() != ChannelDataType.GB28181) {
                // Only supports national standard voice calls
                log.warn("[INFO news] Only supports national standard voice commands, channelID： {}", channel.getGbId());
                try {
                    responseAck(request, Response.FORBIDDEN, "");
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[Command sending failed] error message: {}", e.getMessage());
                }
                return;
            }
            // Send voice call request to subordinates
            Device device = deviceService.getDevice(channel.getDataDeviceId());
            if (device == null) {
                responseAck(request, Response.NOT_FOUND, "device not found");
                return;
            }
            DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(channel.getGbId());
            if (deviceChannel == null) {
                responseAck(request, Response.NOT_FOUND, "channel not found");
                return;
            }
            responseAck(request, Response.OK);

            // Check whether the voice channel has been established and is in use
            if (playService.audioBroadcastInUse(device, deviceChannel)) {
                commanderForPlatform.broadcastResultCmd(platform, channel, sn, false,null, null);
                return;
            }

            MediaServer mediaServerForMinimumLoad = mediaServerService.getMediaServerForMinimumLoad(null);
            commanderForPlatform.broadcastResultCmd(platform, channel, sn, true,  eventResult->{
                log.info("[National standard cascade] Voice call reply failed platform： {}， Error：{}/{}", platform.getServerGBId(), eventResult.statusCode, eventResult.msg);
            }, eventResult->{
                // The message is sent successfully. Send an invite to the superior to get the push stream.
                try {
                    platformService.broadcastInvite(platform, channel, sourceId, mediaServerForMinimumLoad,  (hookData)->{
                        // The upper-level platform pushed the stream successfully
                        AudioBroadcastCatch broadcastCatch = audioBroadcastManager.get(channel.getGbId());
                        if (broadcastCatch != null ) {

                            if (playService.audioBroadcastInUse(device, deviceChannel)) {
                                log.info("[National standard cascade] Voice call The device is in use platform： {}， channel: {}",
                                        platform.getServerGBId(), channel.getGbDeviceId());
                                //  Check that the voice channel has been established and occupied ReplyBYE
                                platformService.stopBroadcast(platform, channel, hookData.getApp(), hookData.getStream(), true, hookData.getMediaServer());
                            }else {
                                // Check that the voice channel has been established but is not occupied
                                broadcastCatch.setApp(hookData.getApp());
                                broadcastCatch.setStream(hookData.getStream());
                                broadcastCatch.setMediaServerItem(hookData.getMediaServer());
                                audioBroadcastManager.update(broadcastCatch);
                                // Push to device
                                SendRtpInfo sendRtpItem = sendRtpServerService.queryByStream(hookData.getStream(), targetId);
                                if (sendRtpItem == null) {
                                    log.warn("[National standard cascade] Voice call abnormal, no streaming information found， channelId: {}, stream: {}", targetId, hookData.getStream());
                                    log.info("[National standard cascade] Voice call Start again，channelId: {}, stream: {}", targetId, hookData.getStream());
                                    try {
                                        playService.audioBroadcastCmd(device, deviceChannel, hookData.getMediaServer(), hookData.getApp(), hookData.getStream(), 60, true, msg -> {
                                            log.info("[Voice call] Channel established successfully, device: {}, channel: {}", device.getDeviceId(), targetId);
                                        });
                                    } catch (SipException | InvalidArgumentException | ParseException e) {
                                        log.info("[Message sending failed] National standard cascade voice announcement platform： {}", platform.getServerGBId());
                                    }
                                }else {
                                    // Flow
                                    try {
                                        mediaServerService.startSendRtp(hookData.getMediaServer(), sendRtpItem);
                                    }catch (ControllerException e) {
                                        log.info("[Voice call] Push failed, result： {}", e.getMessage());
                                        return;
                                    }
                                    log.info("[Voice call] Automatic streaming successful, device: {}, channel: {}", device.getDeviceId(), targetId);
                                }
                            }
                        }else {
                            try {
                                playService.audioBroadcastCmd(device, deviceChannel, hookData.getMediaServer(), hookData.getApp(), hookData.getStream(), 60, true, msg -> {
                                    log.info("[Voice call] Channel established successfully, device: {}, channel: {}", device.getDeviceId(), targetId);
                                });
                            } catch (SipException | InvalidArgumentException | ParseException e) {
                                log.info("[Message sending failed] National standard cascade voice announcement platform： {}", platform.getServerGBId());
                            }
                        }

                    }, eventResultForBroadcastInvite -> {
                        // Received an error
                        log.info("[National standard cascade-Voice call] Failed to establish channel with lower level device: {}, channel: {}， Error：{}/{}", device.getDeviceId(),
                                targetId, eventResultForBroadcastInvite.statusCode, eventResultForBroadcastInvite.msg);
                    }, (code, msg)->{
                        // timeout
                        log.info("[National standard cascade-Voice call] Establish timeout with lower level channel device: {}, channel: {}， Error：{}/{}", device.getDeviceId(),
                                targetId, code, msg);
                    });
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.info("[Message sending failed] National standard cascade voice call invite message platform： {}", platform.getServerGBId());
                }
            });
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.info("[Message sending failed] National standard cascade voice announcement platform： {}", platform.getServerGBId());
        }

    }
}
