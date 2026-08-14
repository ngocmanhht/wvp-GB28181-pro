package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.info;

import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.service.*;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.service.ISendRtpServerService;
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
import javax.sip.header.ContentTypeHeader;
import javax.sip.message.Response;
import java.text.ParseException;

/**
 * INFO Generally used for playback control during national standard cascading
 */
@Slf4j
@Component
public class InfoRequestProcessor extends SIPRequestProcessorParent implements InitializingBean, ISIPRequestProcessor {

    private final String method = "INFO";

    @Autowired
    private SIPProcessorObserver sipProcessorObserver;

    @Autowired
    private IPlatformService platformService;

    @Autowired
    private SipSubscribe sipSubscribe;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IInviteStreamService inviteStreamService;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private IGbChannelService channelService;

    @Autowired
    private IDeviceChannelService deviceChannelService;

    @Autowired
    private SIPCommander cmder;

    @Autowired
    private SipInviteSessionManager sessionManager;

    @Autowired
    private ISendRtpServerService sendRtpServerService;

    @Override
    public void afterPropertiesSet() throws Exception {
        // Add message processing subscription
        sipProcessorObserver.addRequestProcessor(method, this);
    }

    @Override
    public void process(RequestEvent evt) {
        SIPRequest request = (SIPRequest) evt.getRequest();
        CallIdHeader callIdHeader = request.getCallIdHeader();
        // Search within the session first
        try {
            SendRtpInfo sendRtpInfo = sendRtpServerService.queryByCallId(callIdHeader.getCallId());
            if (sendRtpInfo == null || !sendRtpInfo.isSendToPlatform()) {
                // Reply if it does not exist404
                log.warn("[INFO news] Transaction not found， callID： {}", callIdHeader.getCallId());
                responseAck(request, Response.NOT_FOUND, "transaction not found");
                return;
            }
            // Check whether the upper-level platform exists
            Platform platform = platformService.queryPlatformByServerGBId(sendRtpInfo.getTargetId());
            if (platform == null || !platform.isStatus()) {
                // Reply if it does not exist404
                log.warn("[INFO news] Platform not found or offline: Platform： {}", sendRtpInfo.getTargetId());
                responseAck(request, Response.NOT_FOUND, "platform "+ sendRtpInfo.getTargetId() +" not found or offline");
                return;
            }
            CommonGBChannel channel = channelService.getOne(sendRtpInfo.getChannelId());
            if (channel == null) {
                // Reply if it does not exist404
                log.warn("[INFO news] Channel does not exist: channelID： {}", sendRtpInfo.getChannelId());
                responseAck(request, Response.NOT_FOUND, "channel not found or offline");
                return;
            }
            // Determine channel type
            if (channel.getDataType() != ChannelDataType.GB28181) {
                // Non-national standard channels do not support video playback control
                log.warn("[INFO news] Non-national standard channels do not support video playback control: ChannelID： {}", sendRtpInfo.getChannelId());
                responseAck(request, Response.FORBIDDEN, "");
                return;
            }

            // Get the device according to the channel ID
            Device device = deviceService.getDevice(channel.getDataDeviceId());
            if (device == null) {
                // Reply if it does not exist404
                log.warn("[INFO news] The device to which the channel belongs does not exist, channelID： {}", sendRtpInfo.getChannelId());
                responseAck(request, Response.NOT_FOUND, "platform "+ sendRtpInfo.getChannelId() +" not found or offline");
                return;
            }
            // Get the original information of the channel
            DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(sendRtpInfo.getChannelId());
            // Forward control messages to the original channel
            ContentTypeHeader header = (ContentTypeHeader)evt.getRequest().getHeader(ContentTypeHeader.NAME);
            String contentType = header.getContentType();
            String contentSubType = header.getContentSubType();
            if ("Application".equalsIgnoreCase(contentType) && "MANSRTSP".equalsIgnoreCase(contentSubType)) {
                log.info("[INFO news] platform： {}->{}({})/{}", platform.getServerGBId(), device.getName(),
                        device.getDeviceId(), deviceChannel.getId());
                // The protocol is not parsed and forwarded directly to the corresponding device.
                cmder.playbackControlCmd(device, deviceChannel, sendRtpInfo.getStream(), new String(evt.getRequest().getRawContent()), eventResult -> {
                    // failed reply
                    try {
                        responseAck(request, eventResult.statusCode, eventResult.msg);
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        log.error("[Command sending failed] National standard cascade video control: {}", e.getMessage());
                    }
                }, eventResult -> {
                    // successful reply
                    try {
                        responseAck(request, eventResult.statusCode);
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        log.error("[Command sending failed] National standard cascade video control: {}", e.getMessage());
                    }
                });
            }
        } catch (SipException e) {
            log.warn("SIP Reply to error", e);
        } catch (InvalidArgumentException e) {
            log.warn("Invalid parameter", e);
        } catch (ParseException e) {
            log.warn("SIPParse exception when replying", e);
        }
    }
}
