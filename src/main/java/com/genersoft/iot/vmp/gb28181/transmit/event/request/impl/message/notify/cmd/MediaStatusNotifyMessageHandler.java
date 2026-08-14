package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.cmd;

import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.enums.MediaStreamUtil;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.service.*;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.NotifyMessageHandler;
import com.genersoft.iot.vmp.media.event.hook.Hook;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.event.hook.HookType;
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
import javax.sip.header.CallIdHeader;
import javax.sip.message.Response;
import java.text.ParseException;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.getText;

/**
 * media notification
 */
@Slf4j
@Component
public class MediaStatusNotifyMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private final String cmdType = "MediaStatus";

    @Autowired
    private NotifyMessageHandler notifyMessageHandler;

    @Autowired
    private SIPCommander cmder;

    @Autowired
    private SIPCommanderForPlatform sipCommanderFroPlatform;

    @Autowired
    private IPlatformChannelService platformChannelService;

    @Autowired
    private IPlatformService platformService;

    @Autowired
    private HookSubscribe subscribe;

    @Autowired
    private IInviteStreamService inviteStreamService;

    @Autowired
    private SipInviteSessionManager sessionManager;

    @Autowired
    private IDeviceChannelService deviceChannelService;

    @Autowired
    private IPlayService playService;

    @Autowired
    private ISendRtpServerService sendRtpServerService;

    @Override
    public void afterPropertiesSet() throws Exception {
        notifyMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element rootElement) {
        // Reply200 OK
        try {
             responseAck((SIPRequest) evt.getRequest(), Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[Command sending failed] National standard cascade video stream push completed, reply200OK: {}", e.getMessage());
        }
        CallIdHeader callIdHeader = (CallIdHeader)evt.getRequest().getHeader(CallIdHeader.NAME);
        String NotifyType =getText(rootElement, "NotifyType");
        if ("121".equals(NotifyType)){
            log.info("[video streaming]After the push is completed, you will receive the notification of closing the flow.");

            SsrcTransaction ssrcTransaction = sessionManager.getSsrcTransactionByCallId(callIdHeader.getCallId());
            if (ssrcTransaction != null) {
                log.info("[video streaming]After the push is completed, the notification will be turned off.， device: {}, channelId: {}", ssrcTransaction.getDeviceId(), ssrcTransaction.getChannelId());
                InviteInfo inviteInfo = inviteStreamService.getInviteInfo(InviteSessionType.DOWNLOAD, ssrcTransaction.getChannelId(), ssrcTransaction.getStream());
                if (inviteInfo != null) {
                    playService.stop(inviteInfo);
                }
                // Remove monitoring stream logout that automatically stops downloading
                Hook hook = Hook.getInstance(HookType.on_media_arrival, MediaStreamUtil.RTP_APP, ssrcTransaction.getStream(), ssrcTransaction.getMediaServerId());
                subscribe.removeSubscribe(hook);
                if (ssrcTransaction.getPlatformId() != null) {
                    // If you play in cascade, you need to send this notification to the superior. TODO Multiple superiors watch a subordinate at the same time. There may be a problem of wrong stopping. The on-demand CallId needs to be bound to the superior and subordinate.
                    SendRtpInfo sendRtpInfo =  sendRtpServerService.queryByChannelId(ssrcTransaction.getChannelId(), ssrcTransaction.getPlatformId());
                    if (sendRtpInfo != null) {
                        Platform parentPlatform = platformService.queryPlatformByServerGBId(sendRtpInfo.getTargetId());
                        if (parentPlatform == null) {
                            log.warn("[Cascading message sending]：Send MediaStatus to discover the superior platform{}does not exist", sendRtpInfo.getTargetId());
                            return;
                        }
                        CommonGBChannel channel = platformChannelService.queryChannelByPlatformIdAndChannelId(parentPlatform.getId(), sendRtpInfo.getChannelId());
                        if (channel == null) {
                            log.warn("[Cascading message sending]：Send MediaStatus discovery channel{}does not exist", sendRtpInfo.getChannelId());
                            return;
                        }
                        try {
                            sipCommanderFroPlatform.sendMediaStatusNotify(parentPlatform, sendRtpInfo, channel);
                        } catch (SipException | InvalidArgumentException | ParseException e) {
                            log.error("[Command sending failed] National standard cascade video playback completed: {}", e.getMessage());
                        }
                    }
                }
            }else {
                log.info("[video streaming]After the push is completed, the stream notification is turned off, but the corresponding download information is not found.");
            }
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, Platform parentPlatform, Element element) {

    }
}
