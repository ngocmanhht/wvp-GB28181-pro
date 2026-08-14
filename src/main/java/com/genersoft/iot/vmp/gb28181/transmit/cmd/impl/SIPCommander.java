package com.genersoft.iot.vmp.gb28181.transmit.cmd.impl;

import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.enums.MediaStreamUtil;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.SipLayer;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.MessageSubscribe;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.event.sip.MessageEvent;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.SIPSender;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.SIPRequestHeaderProvider;
import com.genersoft.iot.vmp.gb28181.utils.NumericUtil;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.hook.Hook;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.event.hook.HookType;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Request;
import java.text.ParseException;
import java.util.List;

/**
 * @description:Device capability interface, used to define the control and query capabilities of the device
 * @author: swwheihei
 * @date: 2020May 3rd, afternoon9:22:48
 */
@Component
@DependsOn("sipLayer")
@Slf4j
public class SIPCommander implements ISIPCommander {

    @Autowired
    private SipConfig sipConfig;

    @Autowired
    private SipLayer sipLayer;

    @Autowired
    private SIPSender sipSender;

    @Autowired
    private SIPRequestHeaderProvider headerProvider;

    @Autowired
    private SipInviteSessionManager sessionManager;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private HookSubscribe subscribe;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private MessageSubscribe messageSubscribe;

    /**
     * PTZ instruction code calculation
     *
     * @param cmdCode      Script code
     * @param parameter1   data1
     * @param parameter2   data2
     * @param combineCode2 Combination code2
     */
    public static String frontEndCmdString(int cmdCode, int parameter1, int parameter2, int combineCode2) {
        StringBuilder builder = new StringBuilder("A50F01");
        String strTmp;
        strTmp = String.format("%02X", cmdCode);
        builder.append(strTmp, 0, 2);
        strTmp = String.format("%02X", parameter1);
        builder.append(strTmp, 0, 2);
        strTmp = String.format("%02X", parameter2);
        builder.append(strTmp, 0, 2);
        strTmp = String.format("%02X", combineCode2 << 4);
        builder.append(strTmp, 0, 2);
        //Calculate check code
        int checkCode = (0XA5 + 0X0F + 0X01 + cmdCode + parameter1 + parameter2 + (combineCode2 << 4)) % 0X100;
        strTmp = String.format("%02X", checkCode);
        builder.append(strTmp, 0, 2);
        return builder.toString();
    }

    /**
     * PTZ control, supports direction and zoom control
     *
     * @param device    control equipment
     * @param channelId Preview channel
     * @param leftRight Camera moves left and right 0: Stop 1: Move left 2: Move right
     * @param upDown    Lens moves up and down 0: Stop 1: Move up 2: Move down
     * @param inOut     Lens zoom in and out 0: Stop 1: Zoom out 2: Zoom in
     * @param moveSpeed Lens movement speed
     * @param zoomSpeed Lens zoom speed
     */
    @Override
    public void ptzCmd(Device device, String channelId, int leftRight, int upDown, int inOut, int moveSpeed,
                       int zoomSpeed) throws InvalidArgumentException, SipException, ParseException {
        String cmdStr = SipUtils.cmdString(leftRight, upDown, inOut, moveSpeed, zoomSpeed);
        StringBuilder ptzXml = new StringBuilder(200);
        String charset = device.getCharset();
        ptzXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        ptzXml.append("<Control>\r\n");
        ptzXml.append("<CmdType>DeviceControl</CmdType>\r\n");
        ptzXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        ptzXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
        ptzXml.append("<PTZCmd>" + cmdStr + "</PTZCmd>\r\n");
        ptzXml.append("<Info>\r\n");
        ptzXml.append("<ControlPriority>5</ControlPriority>\r\n");
        ptzXml.append("</Info>\r\n");
        ptzXml.append("</Control>\r\n");

        Request request = headerProvider.createMessageRequest(device, ptzXml.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null, sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));

        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()),request);
    }

    /**
     * Front-end control, including PTZ command, FI command, preset position command, cruise command, scan command and auxiliary switch command
     *
     * @param device       control equipment
     * @param channelId    Preview channel
     * @param cmdCode      Script code
     * @param parameter1   data1
     * @param parameter2   data2
     * @param combineCode2 Combination code2
     */
    @Override
    public void frontEndCmd(Device device, String channelId, int cmdCode, int parameter1, int parameter2, int combineCode2) throws SipException, InvalidArgumentException, ParseException {

        String cmdStr = frontEndCmdString(cmdCode, parameter1, parameter2, combineCode2);
        StringBuffer ptzXml = new StringBuffer(200);
        String charset = device.getCharset();
        ptzXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        ptzXml.append("<Control>\r\n");
        ptzXml.append("<CmdType>DeviceControl</CmdType>\r\n");
        ptzXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        ptzXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
        ptzXml.append("<PTZCmd>" + cmdStr + "</PTZCmd>\r\n");
        ptzXml.append("<Info>\r\n");
        ptzXml.append("<ControlPriority>5</ControlPriority>\r\n");
        ptzXml.append("</Info>\r\n");
        ptzXml.append("</Control>\r\n");

        SIPRequest request = (SIPRequest) headerProvider.createMessageRequest(device, ptzXml.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()),request);

    }

    /**
     * Front-end control instructions (used to forward superior instructions）
     *
     * @param device    control equipment
     * @param channelId Preview channel
     * @param cmdString Front-end control command string
     */
    @Override
    public void fronEndCmd(Device device, String channelId, String cmdString, SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException {

        StringBuffer ptzXml = new StringBuffer(200);
        String charset = device.getCharset();
        ptzXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        ptzXml.append("<Control>\r\n");
        ptzXml.append("<CmdType>DeviceControl</CmdType>\r\n");
        ptzXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        ptzXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
        ptzXml.append("<PTZCmd>" + cmdString + "</PTZCmd>\r\n");
        ptzXml.append("<Info>\r\n");
        ptzXml.append("<ControlPriority>5</ControlPriority>\r\n");
        ptzXml.append("</Info>\r\n");
        ptzXml.append("</Control>\r\n");


        Request request = headerProvider.createMessageRequest(device, ptzXml.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()),request, errorEvent, okEvent);

    }

    /**
     * Request a preview of a video stream
     *
     * @param device     video equipment
     * @param channel  Preview channel
     * @param errorEvent sipWrong subscription
     */
    @Override
    public void playStreamCmd(MediaServer mediaServerItem, SSRCInfo ssrcInfo, Device device, DeviceChannel channel,
                              SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent, Long timeout) throws InvalidArgumentException, SipException, ParseException {
        String stream = ssrcInfo.getStream();

        if (device == null) {
            return;
        }
        String sdpIp = !ObjectUtils.isEmpty(device.getSdpIp()) ? device.getSdpIp() : mediaServerItem.getSdpIp();

        StringBuffer content = new StringBuffer(200);
        content.append("v=0\r\n");
        content.append("o=" + device.getDeviceId() + " 0 0 IN IP4 " + sdpIp + "\r\n");
        content.append("s=Play\r\n");
        content.append("c=IN IP4 " + sdpIp + "\r\n");
        content.append("t=0 0\r\n");

        if (userSetting.getSeniorSdp()) {
            if ("TCP-PASSIVE".equalsIgnoreCase(device.getStreamMode())) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 126 125 99 34 98 97\r\n");
            } else if ("TCP-ACTIVE".equalsIgnoreCase(device.getStreamMode())) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 126 125 99 34 98 97\r\n");
            } else if ("UDP".equalsIgnoreCase(device.getStreamMode())) {
                content.append("m=video " + ssrcInfo.getPort() + " RTP/AVP 96 126 125 99 34 98 97\r\n");
            }
            content.append("a=recvonly\r\n");
            content.append("a=rtpmap:96 PS/90000\r\n");
            content.append("a=fmtp:126 profile-level-id=42e01e\r\n");
            content.append("a=rtpmap:126 H264/90000\r\n");
            content.append("a=rtpmap:125 H264S/90000\r\n");
            content.append("a=fmtp:125 profile-level-id=42e01e\r\n");
            content.append("a=rtpmap:99 H265/90000\r\n");
            content.append("a=rtpmap:98 H264/90000\r\n");
            content.append("a=rtpmap:97 MPEG4/90000\r\n");
            if ("TCP-PASSIVE".equalsIgnoreCase(device.getStreamMode())) { // tcppassive mode
                content.append("a=setup:passive\r\n");
                content.append("a=connection:new\r\n");
            } else if ("TCP-ACTIVE".equalsIgnoreCase(device.getStreamMode())) { // tcpActive mode
                content.append("a=setup:active\r\n");
                content.append("a=connection:new\r\n");
            }
        } else {
            if ("TCP-PASSIVE".equalsIgnoreCase(device.getStreamMode())) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 97 98 99\r\n");
            } else if ("TCP-ACTIVE".equalsIgnoreCase(device.getStreamMode())) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 97 98 99\r\n");
            } else if ("UDP".equalsIgnoreCase(device.getStreamMode())) {
                content.append("m=video " + ssrcInfo.getPort() + " RTP/AVP 96 97 98 99\r\n");
            }
            content.append("a=recvonly\r\n");
            content.append("a=rtpmap:96 PS/90000\r\n");
            content.append("a=rtpmap:98 H264/90000\r\n");
            content.append("a=rtpmap:97 MPEG4/90000\r\n");
            content.append("a=rtpmap:99 H265/90000\r\n");
            if ("TCP-PASSIVE".equalsIgnoreCase(device.getStreamMode())) { // tcppassive mode
                content.append("a=setup:passive\r\n");
                content.append("a=connection:new\r\n");
            } else if ("TCP-ACTIVE".equalsIgnoreCase(device.getStreamMode())) { // tcpActive mode
                content.append("a=setup:active\r\n");
                content.append("a=connection:new\r\n");
            }
        }

        if (!ObjectUtils.isEmpty(channel.getStreamIdentification())) {
            content.append("a=" + channel.getStreamIdentification() + "\r\n");
        }

        content.append("y=" + ssrcInfo.getSsrc() + "\r\n");//ssrc
        // fField:f= v/encoding format/resolution/Frame rate/Code rate type/Code rate sizea/encoding format/Code rate size/Sampling rate
//			content.append("f=v/2/6/25/1/4000a/6/8/1" + "\r\n"); // No device found supporting this feature

        Request request = headerProvider.createInviteRequest(device, channel.getDeviceId(), content.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null, ssrcInfo.getSsrc(),sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, (e -> {
            sessionManager.removeByStream(ssrcInfo.getApp(), ssrcInfo.getStream());
            errorEvent.response(e);
        }), e -> {
            ResponseEvent responseEvent = (ResponseEvent) e.event;
            SIPResponse response = (SIPResponse) responseEvent.getResponse();
            String callId = response.getCallIdHeader().getCallId();
            SsrcTransaction ssrcTransaction = SsrcTransaction.buildForDevice(device.getDeviceId(), channel.getId(),
                    callId,ssrcInfo.getApp(), ssrcInfo.getStream(), ssrcInfo.getSsrc(), mediaServerItem.getId(), response,
                    InviteSessionType.PLAY);
            sessionManager.put(ssrcTransaction);
            okEvent.response(e);
        }, timeout);
    }

    /**
     * Request playback of a video stream
     *
     * @param device    video equipment
     * @param channel Preview channel
     * @param startTime Start time, format requirements：yyyy-MM-dd HH:mm:ss
     * @param endTime   End time, format requirements：yyyy-MM-dd HH:mm:ss
     */
    @Override
    public void playbackStreamCmd(MediaServer mediaServerItem, SSRCInfo ssrcInfo, Device device, DeviceChannel channel,
                                  String startTime, String endTime,
                                  SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent, Long timeout) throws InvalidArgumentException, SipException, ParseException {


        log.info("{} The allocated ZLM is: {} [{}:{}]", ssrcInfo.getStream(), mediaServerItem.getId(), mediaServerItem.getSdpIp(), ssrcInfo.getPort());
        String sdpIp;
        if (!ObjectUtils.isEmpty(device.getSdpIp())) {
            sdpIp = device.getSdpIp();
        }else {
            sdpIp = mediaServerItem.getSdpIp();
        }
        StringBuffer content = new StringBuffer(200);
        content.append("v=0\r\n");
        content.append("o=" + device.getDeviceId() + " 0 0 IN IP4 " + sdpIp + "\r\n");
        content.append("s=Playback\r\n");
        content.append("u=" + channel.getDeviceId() + ":0\r\n");
        content.append("c=IN IP4 " + sdpIp + "\r\n");
        content.append("t=" + DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(startTime) + " "
                + DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(endTime) + "\r\n");

        String streamMode = device.getStreamMode();

        if (userSetting.getSeniorSdp()) {
            if ("TCP-PASSIVE".equalsIgnoreCase(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 126 125 99 34 98 97\r\n");
            } else if ("TCP-ACTIVE".equalsIgnoreCase(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 126 125 99 34 98 97\r\n");
            } else if ("UDP".equalsIgnoreCase(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " RTP/AVP 96 126 125 99 34 98 97\r\n");
            }
            content.append("a=recvonly\r\n");
            content.append("a=rtpmap:96 PS/90000\r\n");
            content.append("a=fmtp:126 profile-level-id=42e01e\r\n");
            content.append("a=rtpmap:126 H264/90000\r\n");
            content.append("a=rtpmap:125 H264S/90000\r\n");
            content.append("a=fmtp:125 profile-level-id=42e01e\r\n");
            content.append("a=rtpmap:99 H265/90000\r\n");
            content.append("a=rtpmap:98 H264/90000\r\n");
            content.append("a=rtpmap:97 MPEG4/90000\r\n");
            if ("TCP-PASSIVE".equalsIgnoreCase(streamMode)) { // tcppassive mode
                content.append("a=setup:passive\r\n");
                content.append("a=connection:new\r\n");
            } else if ("TCP-ACTIVE".equalsIgnoreCase(streamMode)) { // tcpActive mode
                content.append("a=setup:active\r\n");
                content.append("a=connection:new\r\n");
            }
        } else {
            if ("TCP-PASSIVE".equalsIgnoreCase(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 97 98 99\r\n");
            } else if ("TCP-ACTIVE".equalsIgnoreCase(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 97 98 99\r\n");
            } else if ("UDP".equalsIgnoreCase(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " RTP/AVP 96 97 98 99\r\n");
            }
            content.append("a=recvonly\r\n");
            content.append("a=rtpmap:96 PS/90000\r\n");
            content.append("a=rtpmap:97 MPEG4/90000\r\n");
            content.append("a=rtpmap:98 H264/90000\r\n");
            content.append("a=rtpmap:99 H265/90000\r\n");
            if ("TCP-PASSIVE".equalsIgnoreCase(streamMode)) {
                // tcppassive mode
                content.append("a=setup:passive\r\n");
                content.append("a=connection:new\r\n");
            } else if ("TCP-ACTIVE".equalsIgnoreCase(streamMode)) {
                // tcpActive mode
                content.append("a=setup:active\r\n");
                content.append("a=connection:new\r\n");
            }
        }

        //ssrc
        content.append("y=" + ssrcInfo.getSsrc() + "\r\n");

        Request request = headerProvider.createPlaybackInviteRequest(device, channel.getDeviceId(), content.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()), ssrcInfo.getSsrc());

        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent, event -> {
            ResponseEvent responseEvent = (ResponseEvent) event.event;
            SIPResponse response = (SIPResponse) responseEvent.getResponse();
            SsrcTransaction ssrcTransaction = SsrcTransaction.buildForDevice(device.getDeviceId(),
                    channel.getId(), sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),
                            device.getTransport()).getCallId(), ssrcInfo.getApp(), ssrcInfo.getStream(), ssrcInfo.getSsrc(),
                    mediaServerItem.getId(), response, InviteSessionType.PLAYBACK);
            sessionManager.put(ssrcTransaction);
            okEvent.response(event);
        }, timeout);
    }

    /**
     * Request historical media downloads
     */
    @Override
    public void downloadStreamCmd(MediaServer mediaServerItem, SSRCInfo ssrcInfo, Device device, DeviceChannel channel,
                                  String startTime, String endTime, int downloadSpeed,
                                  SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent, Long timeout) throws InvalidArgumentException, SipException, ParseException {

        log.info("[send-Request historical media downloads-command] flowID： {}，The node is: {} [{}:{}]", ssrcInfo.getStream(), mediaServerItem.getId(), mediaServerItem.getSdpIp(), ssrcInfo.getPort());
        String sdpIp;
        if (!ObjectUtils.isEmpty(device.getSdpIp())) {
            sdpIp = device.getSdpIp();
        }else {
            sdpIp = mediaServerItem.getSdpIp();
        }
        StringBuffer content = new StringBuffer(200);
        content.append("v=0\r\n");
        content.append("o=" + device.getDeviceId() + " 0 0 IN IP4 " + sdpIp + "\r\n");
        content.append("s=Download\r\n");
        content.append("u=" + channel.getDeviceId() + ":0\r\n");
        content.append("c=IN IP4 " + sdpIp + "\r\n");
        content.append("t=" + DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(startTime) + " "
                + DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(endTime) + "\r\n");

        String streamMode = device.getStreamMode().toUpperCase();

        if (userSetting.getSeniorSdp()) {
            if ("TCP-PASSIVE".equals(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 126 125 99 34 98 97\r\n");
            } else if ("TCP-ACTIVE".equals(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 126 125 99 34 98 97\r\n");
            } else if ("UDP".equals(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " RTP/AVP 96 126 125 99 34 98 97\r\n");
            }
            content.append("a=recvonly\r\n");
            content.append("a=rtpmap:96 PS/90000\r\n");
            content.append("a=fmtp:126 profile-level-id=42e01e\r\n");
            content.append("a=rtpmap:126 H264/90000\r\n");
            content.append("a=rtpmap:125 H264S/90000\r\n");
            content.append("a=fmtp:125 profile-level-id=42e01e\r\n");
            content.append("a=rtpmap:99 MP4V-ES/90000\r\n");
            content.append("a=fmtp:99 profile-level-id=3\r\n");
            content.append("a=rtpmap:98 H264/90000\r\n");
            content.append("a=rtpmap:97 MPEG4/90000\r\n");
            if ("TCP-PASSIVE".equals(streamMode)) { // tcppassive mode
                content.append("a=setup:passive\r\n");
                content.append("a=connection:new\r\n");
            } else if ("TCP-ACTIVE".equals(streamMode)) { // tcpActive mode
                content.append("a=setup:active\r\n");
                content.append("a=connection:new\r\n");
            }
        } else {
            if ("TCP-PASSIVE".equals(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 97 98 99\r\n");
            } else if ("TCP-ACTIVE".equals(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 97 98 99\r\n");
            } else if ("UDP".equals(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " RTP/AVP 96 97 98 99\r\n");
            }
            content.append("a=recvonly\r\n");
            content.append("a=rtpmap:96 PS/90000\r\n");
            content.append("a=rtpmap:97 MPEG4/90000\r\n");
            content.append("a=rtpmap:98 H264/90000\r\n");
            content.append("a=rtpmap:99 H265/90000\r\n");
            if ("TCP-PASSIVE".equals(streamMode)) { // tcppassive mode
                content.append("a=setup:passive\r\n");
                content.append("a=connection:new\r\n");
            } else if ("TCP-ACTIVE".equals(streamMode)) { // tcpActive mode
                content.append("a=setup:active\r\n");
                content.append("a=connection:new\r\n");
            }
        }
        content.append("a=downloadspeed:" + downloadSpeed + "\r\n");

        content.append("y=" + ssrcInfo.getSsrc() + "\r\n");//ssrc
        log.debug("Requesting to download signaling at this timessrc===>{}",ssrcInfo.getSsrc());
        // Add subscription
        CallIdHeader newCallIdHeader = sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()), device.getTransport());
        Request request = headerProvider.createPlaybackInviteRequest(device, channel.getDeviceId(), content.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null,newCallIdHeader, ssrcInfo.getSsrc());

        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent, event -> {
            ResponseEvent responseEvent = (ResponseEvent) event.event;
            SIPResponse response = (SIPResponse) responseEvent.getResponse();
            String contentString =new String(response.getRawContent());
            String ssrc = SipUtils.getSsrcFromSdp(contentString);
            SsrcTransaction ssrcTransaction = SsrcTransaction.buildForDevice(device.getDeviceId(), channel.getId(),
                    response.getCallIdHeader().getCallId(), ssrcInfo.getApp(), ssrcInfo.getStream(), ssrc,
                    mediaServerItem.getId(), response, InviteSessionType.DOWNLOAD);
            sessionManager.put(ssrcTransaction);
            okEvent.response(event);
        }, timeout);
    }

    @Override
    public void talkStreamCmd(MediaServer mediaServerItem, SendRtpInfo sendRtpItem, String ySsrc, Device device, DeviceChannel channel,
                              String callId, HookSubscribe.Event event, HookSubscribe.Event eventForPush, SipSubscribe.Event okEvent,
                              SipSubscribe.Event errorEvent, Long timeout) throws InvalidArgumentException, SipException, ParseException {

        String stream = sendRtpItem.getStream();

        if (device == null) {
            return;
        }
        if (!mediaServerItem.isRtpEnable()) {
            // Single port does not currently support voice calls
            log.warn("[Voice call] Single port does not currently support this operation");
            return;
        }

        log.info("[Voice call] {} The allocated ZLM is: {} [{}:{}]", stream, mediaServerItem.getId(), mediaServerItem.getIp(), sendRtpItem.getPort());
        Hook hook = Hook.getInstance(HookType.on_media_arrival, MediaStreamUtil.RTP_APP, stream, mediaServerItem.getId());
        subscribe.addSubscribe(hook, (hookData) -> {
            if (event != null) {
                event.response(hookData);
                subscribe.removeSubscribe(hook);
            }
        });

        CallIdHeader callIdHeader = sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()), device.getTransport());
        callIdHeader.setCallId(callId);
        Hook publishHook = Hook.getInstance(HookType.on_publish, MediaStreamUtil.RTP_APP, stream, mediaServerItem.getId());
        subscribe.addSubscribe(publishHook, (hookData) -> {
            if (eventForPush != null) {
                eventForPush.response(hookData);
            }
        });
        //
        StringBuffer content = new StringBuffer(200);
        content.append("v=0\r\n");
        content.append("o=" + device.getDeviceId() + " 0 0 IN IP4 " + mediaServerItem.getSdpIp() + "\r\n");
        content.append("s=Talk\r\n");
        content.append("c=IN IP4 " + mediaServerItem.getSdpIp() + "\r\n");
        content.append("t=0 0\r\n");

        content.append("m=audio " + sendRtpItem.getPort() + " TCP/RTP/AVP 8\r\n");
        content.append("a=setup:passive\r\n");
        content.append("a=connection:new\r\n");
        content.append("a=sendrecv\r\n");
        content.append("a=rtpmap:8 PCMA/8000\r\n");

        content.append("y=" + ySsrc + "\r\n");//ssrc
        // fField:f= v/encoding format/resolution/Frame rate/Code rate type/Code rate sizea/encoding format/Code rate size/Sampling rate
        content.append("f=v/////a/1/8/1" + "\r\n");

        Request request = headerProvider.createInviteRequest(device, channel.getDeviceId(), content.toString(),
                SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null, ySsrc, callIdHeader);
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, (e -> {
            sessionManager.removeByStream(sendRtpItem.getApp(), sendRtpItem.getStream());
            errorEvent.response(e);
        }), e -> {
            // Here is an example to prevent a channel from having only one callID parameter. Use a fixed value.
            ResponseEvent responseEvent = (ResponseEvent) e.event;
            SIPResponse response = (SIPResponse) responseEvent.getResponse();
            SsrcTransaction ssrcTransaction = SsrcTransaction.buildForDevice(device.getDeviceId(), channel.getId(), MediaStreamUtil.GB28181_TALK,sendRtpItem.getApp(), stream, sendRtpItem.getSsrc(), mediaServerItem.getId(), response, InviteSessionType.TALK);
            sessionManager.put(ssrcTransaction);
            okEvent.response(e);
        }, timeout);
    }

    /**
     * Video streaming stopped
     */
    @Override
    public void streamByeCmd(Device device, String channelId, String app, String stream, String callId, SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException, SsrcTransactionNotFoundException {
        if (device == null) {
            log.warn("[sendBYE] devicefornull");
            return;
        }
        SsrcTransaction ssrcTransaction = null;
        if (callId != null) {
            ssrcTransaction = sessionManager.getSsrcTransactionByCallId(callId);
        }else if (stream != null) {
            ssrcTransaction = sessionManager.getSsrcTransactionByStream(app, stream);
        }

        if (ssrcTransaction == null) {
            log.info("[sendBYE] Transaction information not found, device： device: {}, channel: {}", device.getDeviceId(), channelId);
            throw new SsrcTransactionNotFoundException(device.getDeviceId(), channelId, callId, stream);
        }

        log.info("[sendBYE] Equipment： device: {}, channel: {}, callId: {}", device.getDeviceId(), channelId, ssrcTransaction.getCallId());
        sessionManager.removeByCallId(ssrcTransaction.getCallId());
        Request byteRequest = headerProvider.createByteRequest(device, channelId, ssrcTransaction.getSipTransactionInfo());
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), byteRequest, null, okEvent);
    }

    @Override
    public void streamByeCmd(Device device, String channelId, SipTransactionInfo sipTransactionInfo, SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException, SsrcTransactionNotFoundException {
        Request byteRequest = headerProvider.createByteRequest(device, channelId, sipTransactionInfo);
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), byteRequest, null, okEvent);
    }

    @Override
    public void streamByeCmdForDeviceInvite(Device device, String channelId, SipTransactionInfo sipTransactionInfo, SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException, SsrcTransactionNotFoundException {
        Request byteRequest = headerProvider.createByteRequestForDeviceInvite(device, channelId, sipTransactionInfo);
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), byteRequest, null, okEvent);
    }

    /**
     * voice broadcast
     *
     * @param device video equipment
     */
	@Override
	public void audioBroadcastCmd(Device device, String channelId, SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent) throws InvalidArgumentException, SipException, ParseException {
        StringBuffer broadcastXml = new StringBuffer(200);
        String charset = device.getCharset();
        broadcastXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        broadcastXml.append("<Notify>\r\n");
        broadcastXml.append("<CmdType>Broadcast</CmdType>\r\n");
        broadcastXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
        broadcastXml.append("<SourceID>" + sipConfig.getId() + "</SourceID>\r\n");
        broadcastXml.append("<TargetID>" + channelId + "</TargetID>\r\n");
        broadcastXml.append("</Notify>\r\n");

        Request request = headerProvider.createMessageRequest(device, broadcastXml.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent, okEvent);

    }


    /**
     * Audio and video recording control
     *
     * @param device       video equipment
     * @param channelId    Preview channel
     * @param recordCmdStr Recording command：Record / StopRecord
     */
    @Override
    public void recordCmd(Device device, String channelId, String recordCmdStr, ErrorCallback<String> callback) throws InvalidArgumentException, SipException, ParseException {
        final String cmdType = "DeviceControl";
        final int sn = (int) ((Math.random() * 9 + 1) * 100000);

        StringBuffer cmdXml = new StringBuffer(200);
        String charset = device.getCharset();
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Control>\r\n");
        cmdXml.append("<CmdType>" + cmdType + "</CmdType>\r\n");
        cmdXml.append("<SN>" + sn + "</SN>\r\n");
        if (ObjectUtils.isEmpty(channelId)) {
            cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        } else {
            cmdXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
        }
        cmdXml.append("<RecordCmd>" + recordCmdStr + "</RecordCmd>\r\n");
        cmdXml.append("</Control>\r\n");

        MessageEvent<String> messageEvent = MessageEvent.getInstance(cmdType, sn + "", channelId, 1000L, callback);
        messageSubscribe.addSubscribe(messageEvent);

        Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, eventResult -> {
            messageSubscribe.removeSubscribe(messageEvent.getKey());
            callback.run(ErrorCode.ERROR100.getCode(), "failed，" + eventResult.msg, null);
        },null);
    }

    /**
     * Remote start control command
     *
     * @param device video equipment
     */
    @Override
    public void teleBootCmd(Device device) throws InvalidArgumentException, SipException, ParseException {

        StringBuffer cmdXml = new StringBuffer(200);
        String charset = device.getCharset();
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Control>\r\n");
        cmdXml.append("<CmdType>DeviceControl</CmdType>\r\n");
        cmdXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        cmdXml.append("<TeleBoot>Boot</TeleBoot>\r\n");
        cmdXml.append("</Control>\r\n");



        Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request);
    }

    /**
     * Alarm arming/disarm order
     *
     * @param device      video equipment
     * @param guardCmdStr "SetGuard"/"ResetGuard"
     */
    @Override
    public void guardCmd(Device device, String guardCmdStr, ErrorCallback<String> callback) throws InvalidArgumentException, SipException, ParseException {

        String cmdType = "DeviceControl";
        int sn = (int) ((Math.random() * 9 + 1) * 100000);

        StringBuffer cmdXml = new StringBuffer(200);
        String charset = device.getCharset();
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Control>\r\n");
        cmdXml.append("<CmdType>" + cmdType + "</CmdType>\r\n");
        cmdXml.append("<SN>" + sn + "</SN>\r\n");
        cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        cmdXml.append("<GuardCmd>" + guardCmdStr + "</GuardCmd>\r\n");
        cmdXml.append("</Control>\r\n");

        MessageEvent<String> messageEvent = MessageEvent.getInstance(cmdType, sn + "", device.getDeviceId(), 1000L, callback);
        messageSubscribe.addSubscribe(messageEvent);

        Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, eventResult -> {
            messageSubscribe.removeSubscribe(messageEvent.getKey());
            callback.run(ErrorCode.ERROR100.getCode(), "failed，" + eventResult.msg, null);
        });
    }

    /**
     * Alarm reset command
     *
     * @param device video equipment
     */
    @Override
    public void alarmResetCmd(Device device, String alarmMethod, String alarmType, ErrorCallback<String> callback) throws InvalidArgumentException, SipException, ParseException {

        String cmdType = "DeviceControl";
        int sn = (int) ((Math.random() * 9 + 1) * 100000);

        StringBuffer cmdXml = new StringBuffer(200);
        String charset = device.getCharset();
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Control>\r\n");
        cmdXml.append("<CmdType>" + cmdType + "</CmdType>\r\n");
        cmdXml.append("<SN>" + sn + "</SN>\r\n");
        cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        cmdXml.append("<AlarmCmd>ResetAlarm</AlarmCmd>\r\n");
        if (!ObjectUtils.isEmpty(alarmMethod) || !ObjectUtils.isEmpty(alarmType)) {
            cmdXml.append("<Info>\r\n");
        }
        if (!ObjectUtils.isEmpty(alarmMethod)) {
            cmdXml.append("<AlarmMethod>" + alarmMethod + "</AlarmMethod>\r\n");
        }
        if (!ObjectUtils.isEmpty(alarmType)) {
            cmdXml.append("<AlarmType>" + alarmType + "</AlarmType>\r\n");
        }
        if (!ObjectUtils.isEmpty(alarmMethod) || !ObjectUtils.isEmpty(alarmType)) {
            cmdXml.append("</Info>\r\n");
        }
        cmdXml.append("</Control>\r\n");

        MessageEvent<String> messageEvent = MessageEvent.getInstance(cmdType, sn + "", device.getDeviceId(), 1000L, callback);
        messageSubscribe.addSubscribe(messageEvent);

        Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, eventResult -> {
            messageSubscribe.removeSubscribe(messageEvent.getKey());
            callback.run(ErrorCode.ERROR100.getCode(), "failed，" + eventResult.msg, null);
        });
    }

    /**
     * Force key frame command, the device should immediately send an IDR frame after receiving this command
     *
     * @param device    video equipment
     * @param channelId Preview channel
     */
    @Override
    public void iFrameCmd(Device device, String channelId) throws InvalidArgumentException, SipException, ParseException {

        StringBuffer cmdXml = new StringBuffer(200);
        String charset = device.getCharset();
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Control>\r\n");
        cmdXml.append("<CmdType>DeviceControl</CmdType>\r\n");
        cmdXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        if (ObjectUtils.isEmpty(channelId)) {
            cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        } else {
            cmdXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
        }
        cmdXml.append("<IFameCmd>Send</IFameCmd>\r\n");
        cmdXml.append("</Control>\r\n");



        Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request);
    }

    /**
     * Watch bit setting
     *
     * @param device      video equipment
     * @param channelId      Channel id, non-channel is the device itself
     * @param enabled     Watchdog bit enabled：1 = turn on，0 = Close
     * @param resetTime   Automatic homing time interval, used when the guard position is turned on, unit: seconds(s)
     * @param presetIndex Call the preset position number, used when turning on the guard position, value range0~255
     */
    @Override
    public void homePositionCmd(Device device, String channelId, Boolean enabled, Integer resetTime, Integer presetIndex, ErrorCallback<String> callback) throws InvalidArgumentException, SipException, ParseException {

        String cmdType = "DeviceControl";
        int sn = (int) ((Math.random() * 9 + 1) * 100000);

        StringBuffer cmdXml = new StringBuffer(200);
        String charset = device.getCharset();
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Control>\r\n");
        cmdXml.append("<CmdType>" + cmdType + "</CmdType>\r\n");
        cmdXml.append("<SN>" + sn + "</SN>\r\n");
        if (ObjectUtils.isEmpty(channelId)) {
            channelId = device.getDeviceId();
        }
        cmdXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
        cmdXml.append("<HomePosition>\r\n");
        if (enabled) {
            cmdXml.append("<Enabled>1</Enabled>\r\n");
            cmdXml.append("<ResetTime>" + resetTime + "</ResetTime>\r\n");
            cmdXml.append("<PresetIndex>" + presetIndex + "</PresetIndex>\r\n");
        } else {
            cmdXml.append("<Enabled>0</Enabled>\r\n");
        }
        cmdXml.append("</HomePosition>\r\n");
        cmdXml.append("</Control>\r\n");

        MessageEvent<String> messageEvent = MessageEvent.getInstance(cmdType, sn + "", channelId, 1000L, callback);
        messageSubscribe.addSubscribe(messageEvent);

        Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, eventResult -> {
            messageSubscribe.removeSubscribe(messageEvent.getKey());
            callback.run(ErrorCode.ERROR100.getCode(), "failed，" + eventResult.msg, null);
        });
    }

    /**
     * Device configuration commands
     *
     * @param device video equipment
     */
    @Override
    public void deviceConfigCmd(Device device) {
        // TODO Auto-generated method stub
    }

    /**
     * Device configuration commands：basicParam
     */
    @Override
    public void deviceBasicConfigCmd(Device device, BasicParam basicParam, ErrorCallback<String> callback) throws InvalidArgumentException, SipException, ParseException {

        int sn = (int) ((Math.random() * 9 + 1) * 100000);
        String cmdType = "DeviceConfig";
        StringBuffer cmdXml = new StringBuffer(200);
        String charset = device.getCharset();
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Control>\r\n");
        cmdXml.append("<CmdType>" + cmdType + "</CmdType>\r\n");
        cmdXml.append("<SN>" + sn + "</SN>\r\n");
//        String channelId = basicParam.getChannelId();
//        if (ObjectUtils.isEmpty(channelId)) {
//            channelId = device.getDeviceId();
//        }
        // Detailed testing has been done here. Regarding whether channel ID or device ID is used here, Hikvision supports both. Dahua must use device ID. Uniview supports both, but it will log out and restart without replying to the message.，
        // So here is the value device.getDeviceId()
        cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        cmdXml.append("<BasicParam>\r\n");
        if (!ObjectUtils.isEmpty(basicParam.getName())) {
            cmdXml.append("<Name>" + basicParam.getName() + "</Name>\r\n");
        }
        if (NumericUtil.isInteger(basicParam.getExpiration())) {
            if (Integer.parseInt(basicParam.getExpiration()) > 0) {
                cmdXml.append("<Expiration>" + basicParam.getExpiration() + "</Expiration>\r\n");
            }
        }
        if (basicParam.getHeartBeatInterval() != null && basicParam.getHeartBeatInterval() > 0) {
            cmdXml.append("<HeartBeatInterval>" + basicParam.getHeartBeatInterval() + "</HeartBeatInterval>\r\n");
        }
        if (basicParam.getHeartBeatCount() != null && basicParam.getHeartBeatCount() > 0) {
            cmdXml.append("<HeartBeatCount>" + basicParam.getHeartBeatCount() + "</HeartBeatCount>\r\n");
        }
        cmdXml.append("</BasicParam>\r\n");
        cmdXml.append("</Control>\r\n");


        MessageEvent<String> messageEvent = MessageEvent.getInstance(cmdType, sn + "", device.getDeviceId(), 2000L, callback);
        messageSubscribe.addSubscribe(messageEvent);

        Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, eventResult -> {
            messageSubscribe.removeSubscribe(messageEvent.getKey());
            callback.run(ErrorCode.ERROR100.getCode(), "failed，" + eventResult.msg, null);
        });
    }

    @Override
    public void deviceVideoParamConfigCmd(Device device, VideoParamOpt videoParamOpt, ErrorCallback<String> callback) throws InvalidArgumentException, SipException, ParseException {

        int sn = (int) ((Math.random() * 9 + 1) * 100000);
        String cmdType = "DeviceConfig";
        StringBuffer cmdXml = new StringBuffer(200);
        String charset = device.getCharset();
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Control>\r\n");
        cmdXml.append("<CmdType>" + cmdType + "</CmdType>\r\n");
        cmdXml.append("<SN>" + sn + "</SN>\r\n");
        cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        cmdXml.append("<VideoParamOpt>\r\n");
        if (!ObjectUtils.isEmpty(videoParamOpt.getResolution())) {
            cmdXml.append("<Resolution>" + videoParamOpt.getResolution() + "</Resolution>\r\n");
        }
        if (!ObjectUtils.isEmpty(videoParamOpt.getDownloadSpeed())) {
            cmdXml.append("<DownloadSpeed>" + videoParamOpt.getDownloadSpeed() + "</DownloadSpeed>\r\n");
        }
        cmdXml.append("</VideoParamOpt>\r\n");
        cmdXml.append("</Control>\r\n");

        MessageEvent<String> messageEvent = MessageEvent.getInstance(cmdType, sn + "", device.getDeviceId(), 2000L, callback);
        messageSubscribe.addSubscribe(messageEvent);

        Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, eventResult -> {
            messageSubscribe.removeSubscribe(messageEvent.getKey());
            callback.run(ErrorCode.ERROR100.getCode(), "failed，" + eventResult.msg, null);
        });
    }

    /**
     * Query device status
     *
     * @param device video equipment
     */
    @Override
    public void deviceStatusQuery(Device device, ErrorCallback<String> callback) throws InvalidArgumentException, SipException, ParseException {

        String cmdType = "DeviceStatus";
        int sn = (int) ((Math.random() * 9 + 1) * 100000);

        String charset = device.getCharset();
        StringBuffer catalogXml = new StringBuffer(200);
        catalogXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        catalogXml.append("<Query>\r\n");
        catalogXml.append("<CmdType>" + cmdType + "</CmdType>\r\n");
        catalogXml.append("<SN>" + sn + "</SN>\r\n");
        catalogXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        catalogXml.append("</Query>\r\n");

        MessageEvent<String> messageEvent = MessageEvent.getInstance(cmdType, sn + "", device.getDeviceId(), 1000L, callback);
        messageSubscribe.addSubscribe(messageEvent);

        Request request = headerProvider.createMessageRequest(device, catalogXml.toString(), null, SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));

        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, eventResult -> {
            messageSubscribe.removeSubscribe(messageEvent.getKey());
            callback.run(ErrorCode.ERROR100.getCode(), "failed，" + eventResult.msg, null);
        });
    }

    /**
     * Query device information
     *
     * @param device   video equipment
     * @param callback
     */
    @Override
    public void deviceInfoQuery(Device device, ErrorCallback<Object> callback) throws InvalidArgumentException, SipException, ParseException {

        String cmdType = "DeviceInfo";
        String sn = (int) ((Math.random() * 9 + 1) * 100000) + "";

        StringBuffer catalogXml = new StringBuffer(200);
        String charset = device.getCharset();
        catalogXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        catalogXml.append("<Query>\r\n");
        catalogXml.append("<CmdType>" + cmdType +"</CmdType>\r\n");
        catalogXml.append("<SN>" + sn + "</SN>\r\n");
        catalogXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        catalogXml.append("</Query>\r\n");

        MessageEvent<Object> messageEvent = MessageEvent.getInstance(cmdType, sn, device.getDeviceId(), 1000L, callback);
        messageSubscribe.addSubscribe(messageEvent);

        Request request = headerProvider.createMessageRequest(device, catalogXml.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));

        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, eventResult -> {
            messageSubscribe.removeSubscribe(messageEvent.getKey());
            if (callback != null) {
                callback.run(ErrorCode.ERROR100.getCode(), "failed，" + eventResult.msg, null);
            }
        });

    }

    /**
     * Query directory listing
     *
     * @param device video equipment
     */
    @Override
    public void catalogQuery(Device device, int sn, SipSubscribe.Event errorEvent) throws SipException, InvalidArgumentException, ParseException {

        StringBuffer catalogXml = new StringBuffer(200);
        String charset = device.getCharset();
        catalogXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        catalogXml.append("<Query>\r\n");
        catalogXml.append("  <CmdType>Catalog</CmdType>\r\n");
        catalogXml.append("  <SN>" + sn + "</SN>\r\n");
        catalogXml.append("  <DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        catalogXml.append("</Query>\r\n");

        Request request = headerProvider.createMessageRequest(device, catalogXml.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));

        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent);
    }

    /**
     * Query video information
     *
     * @param device    video equipment
     * @param startTime Start time, format requirements：yyyy-MM-dd HH:mm:ss
     * @param endTime   End time, format requirements：yyyy-MM-dd HH:mm:ss
     */
    @Override
    public void recordInfoQuery(Device device, String channelId, String startTime, String endTime, int sn, Integer secrecy, String type, SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent) throws InvalidArgumentException, SipException, ParseException {
        if (secrecy == null) {
            secrecy = 0;
        }
        if (type == null) {
            type = "all";
        }

        StringBuffer recordInfoXml = new StringBuffer(200);
        String charset = device.getCharset();
        recordInfoXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        recordInfoXml.append("<Query>\r\n");
        recordInfoXml.append("<CmdType>RecordInfo</CmdType>\r\n");
        recordInfoXml.append("<SN>" + sn + "</SN>\r\n");
        recordInfoXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
        if (startTime != null) {
            recordInfoXml.append("<StartTime>" + DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(startTime) + "</StartTime>\r\n");
        }
        if (endTime != null) {
            recordInfoXml.append("<EndTime>" + DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(endTime) + "</EndTime>\r\n");
        }
        if (secrecy != null) {
            recordInfoXml.append("<Secrecy> " + secrecy + " </Secrecy>\r\n");
        }
        if (type != null) {
            // Dahua NVR requires that a text element node with a value of all must be added.Type
            recordInfoXml.append("<Type>" + type + "</Type>\r\n");
        }
        recordInfoXml.append("</Query>\r\n");



        Request request = headerProvider.createMessageRequest(device, recordInfoXml.toString(),
                SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));

        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent, okEvent);
    }

    /**
     * Query alarm information
     *
     * @param device        video equipment
     * @param startPriority Alarm starting level (optional）
     * @param endPriority   Alarm termination level (optional）
     * @param alarmMethod   Alarm mode conditions (optional）
     * @param alarmType     Alarm type
     * @param startTime     Start time of alarm occurrence (optional）
     * @param endTime       Alarm occurrence end time (optional）
     * @return true = Command sent successfully
     */
    @Override
    public void alarmInfoQuery(Device device, String startPriority, String endPriority, String alarmMethod, String alarmType,
                               String startTime, String endTime, ErrorCallback<Object> callback) throws InvalidArgumentException, SipException, ParseException {

        String cmdType = "Alarm";
        String sn = (int) ((Math.random() * 9 + 1) * 100000) + "";

        StringBuffer cmdXml = new StringBuffer(200);
        String charset = device.getCharset();
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Query>\r\n");
        cmdXml.append("<CmdType>" + cmdType + "</CmdType>\r\n");
        cmdXml.append("<SN>" + sn + "</SN>\r\n");
        cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        if (!ObjectUtils.isEmpty(startPriority)) {
            cmdXml.append("<StartAlarmPriority>" + startPriority + "</StartAlarmPriority>\r\n");
        }
        if (!ObjectUtils.isEmpty(endPriority)) {
            cmdXml.append("<EndAlarmPriority>" + endPriority + "</EndAlarmPriority>\r\n");
        }
        if (!ObjectUtils.isEmpty(alarmMethod)) {
            cmdXml.append("<AlarmMethod>" + alarmMethod + "</AlarmMethod>\r\n");
        }
        if (!ObjectUtils.isEmpty(alarmType)) {
            cmdXml.append("<AlarmType>" + alarmType + "</AlarmType>\r\n");
        }
        if (!ObjectUtils.isEmpty(startTime)) {
            cmdXml.append("<StartAlarmTime>" + startTime + "</StartAlarmTime>\r\n");
        }
        if (!ObjectUtils.isEmpty(endTime)) {
            cmdXml.append("<EndAlarmTime>" + endTime + "</EndAlarmTime>\r\n");
        }
        cmdXml.append("</Query>\r\n");

        MessageEvent<Object> messageEvent = MessageEvent.getInstance(cmdType, sn, device.getDeviceId(), 1000L, callback);
        messageSubscribe.addSubscribe(messageEvent);

        Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, eventResult -> {
            messageSubscribe.removeSubscribe(messageEvent.getKey());
            callback.run(ErrorCode.ERROR100.getCode(), "failed，" + eventResult.msg, null);
        });
    }

    /**
     * Query device configuration
     *
     * @param device      video equipment
     * @param channelId   Channel encoding (optional）
     * @param configClass Configuration type
     */
    @Override
    public <T extends DeviceConfigAware> void deviceConfigQuery(Device device, String channelId, Class<T> configClass, ErrorCallback<T> callback) throws InvalidArgumentException, SipException, ParseException {

        String cmdType = "ConfigDownload";
        int sn = (int) ((Math.random() * 9 + 1) * 100000);
        StringBuffer cmdXml = new StringBuffer(200);
        String charset = device.getCharset();
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Query>\r\n");
        cmdXml.append("<CmdType>" + cmdType + "</CmdType>\r\n");
        cmdXml.append("<SN>" + sn + "</SN>\r\n");
        if (ObjectUtils.isEmpty(channelId)) {
            cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        } else {
            cmdXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
        }

        T sample;
        try {
            sample = configClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            log.error("[Command sending failed] Instantiate configuration class: {}", e.getMessage());
            if (callback != null) {
                callback.run(ErrorCode.ERROR100.getCode(), "Instantiation failed: " + e.getMessage(), null);
            }
            return;
        }
        String configType = sample.configType();
        cmdXml.append("<ConfigType>" + configType + "</ConfigType>\r\n");
        cmdXml.append("</Query>\r\n");

        ErrorCallback<Object> wrappedCallback = (code, msg, data) -> {
            if (callback != null) {
                if (code == ErrorCode.SUCCESS.getCode() && data instanceof Element) {
                    Element responseElement = (Element) data;
                    Element configElement = responseElement.element(configType);
                    if (configElement == null) {
                        // Compatible with some Uniview devices where ElementName and configType are inconsistent
                        for (Element child : responseElement.elements()) {
                            if (child.element("Item") != null) {
                                configElement = child;
                                break;
                            }
                        }
                    }
                    if (configElement != null) {
                        try {
                            T result = configClass.getDeclaredConstructor().newInstance();
                            result.fromXml(configElement);
                            callback.run(code, msg, result);
                            return;
                        } catch (Exception e) {
                            log.error("[Device configuration query] Failed to create instance", e);
                        }
                    }
                }
                callback.run(code, msg, null);
            }
        };

        MessageEvent<Object> messageEvent = MessageEvent.getInstance(cmdType, sn + "", channelId, 1000L, wrappedCallback);
        messageSubscribe.addSubscribe(messageEvent);

        Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, eventResult -> {
            messageSubscribe.removeSubscribe(messageEvent.getKey());
            if (callback !=  null) {
                callback.run(ErrorCode.ERROR100.getCode(), "failed，" + eventResult.msg, null);
            }
        });
    }

    /**
     * Query device preset location
     *
     * @param device video equipment
     */
    @Override
    public void presetQuery(Device device, String channelId, ErrorCallback<List<Preset>> callback) throws InvalidArgumentException, SipException, ParseException {

        String cmdType = "PresetQuery";
        int sn = (int) ((Math.random() * 9 + 1) * 100000);

        StringBuffer cmdXml = new StringBuffer(200);
        String charset = device.getCharset();
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Query>\r\n");
        cmdXml.append("<CmdType>" + cmdType + "</CmdType>\r\n");
        cmdXml.append("<SN>" + sn + "</SN>\r\n");
        if (ObjectUtils.isEmpty(channelId)) {
            cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        } else {
            cmdXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
        }
        cmdXml.append("</Query>\r\n");

        MessageEvent<List<Preset>> messageEvent = MessageEvent.getInstance(cmdType, sn + "", channelId, 4000L, callback);
        messageSubscribe.addSubscribe(messageEvent);
        log.info("[Preset position query] Device number： {}， Channel number： {}， SN： {}", device.getDeviceId(), channelId, sn);
        Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, eventResult -> {
            messageSubscribe.removeSubscribe(messageEvent.getKey());
            callback.run(ErrorCode.ERROR100.getCode(), "failed，" + eventResult.msg, null);
        });
    }

    /**
     * Query mobile device location data
     *
     * @param device video equipment
     */
    @Override
    public void mobilePositionQuery(Device device, SipSubscribe.Event errorEvent) throws InvalidArgumentException, SipException, ParseException {

        StringBuffer mobilePositionXml = new StringBuffer(200);
        String charset = device.getCharset();
        mobilePositionXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        mobilePositionXml.append("<Query>\r\n");
        mobilePositionXml.append("<CmdType>MobilePosition</CmdType>\r\n");
        mobilePositionXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        mobilePositionXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        mobilePositionXml.append("<Interval>60</Interval>\r\n");
        mobilePositionXml.append("</Query>\r\n");



        Request request = headerProvider.createMessageRequest(device, mobilePositionXml.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));

        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent);

    }

    /**
     * Subscribe, unsubscribe mobile location
     *
     * @param device video equipment
     * @return true = Command sent successfully
     */
    @Override
    public SIPRequest mobilePositionSubscribe(Device device, SipTransactionInfo sipTransactionInfo, SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent) throws InvalidArgumentException, SipException, ParseException {

        StringBuffer subscribePositionXml = new StringBuffer(200);
        String charset = device.getCharset();
        subscribePositionXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        subscribePositionXml.append("<Query>\r\n");
        subscribePositionXml.append("<CmdType>MobilePosition</CmdType>\r\n");
        subscribePositionXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        subscribePositionXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        if (device.getSubscribeCycleForMobilePosition() > 0) {
            subscribePositionXml.append("<Interval>" + device.getMobilePositionSubmissionInterval() + "</Interval>\r\n");
        }else {
            subscribePositionXml.append("<Interval>5</Interval>\r\n");
        }
        subscribePositionXml.append("</Query>\r\n");

        CallIdHeader callIdHeader;

        if (sipTransactionInfo != null) {
            callIdHeader = SipFactory.getInstance().createHeaderFactory().createCallIdHeader(sipTransactionInfo.getCallId());
        } else {
            callIdHeader = sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport());
        }

        int subscribeCycleForMobilePosition = device.getSubscribeCycleForMobilePosition();
        if (subscribeCycleForMobilePosition > 0) {
            // Mobile location subscription validity period is no less than 30 seconds
            subscribeCycleForMobilePosition = Math.max(subscribeCycleForMobilePosition, 30);
        }
        SIPRequest request = (SIPRequest) headerProvider.createSubscribeRequest(device, subscribePositionXml.toString(), sipTransactionInfo, subscribeCycleForMobilePosition, "presence",callIdHeader); //Position;id=" + tm.substring(tm.length() - 4));

        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent, okEvent);
        return request;
    }

    /**
     * Subscribe and unsubscribe alarm information
     *
     */
    @Override
    public SIPRequest alarmSubscribe(Device device, SipTransactionInfo sipTransactionInfo, SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent) throws InvalidArgumentException, SipException, ParseException {

        StringBuffer cmdXml = new StringBuffer(200);
        String charset = device.getCharset();
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Query>\r\n");
        cmdXml.append("<CmdType>Alarm</CmdType>\r\n");
        cmdXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
//        cmdXml.append("<StartAlarmPriority>1</StartAlarmPriority>\r\n");
//        cmdXml.append("<EndAlarmPriority>4/EndAlarmPriority>\r\n");
//        cmdXml.append("<AlarmMethod>0</AlarmMethod>\r\n");
//
//        LocalDateTime nowDateTime = LocalDateTime.now();
//        String startTime = DateUtil.formatterISO8601.format(nowDateTime);
//        // Go back one month as the end time
//        String endTime = DateUtil.formatterISO8601.format(nowDateTime.plusMonths(1));
//
//        cmdXml.append("<StartTime>" + startTime + "</StartTime>\r\n");
//        cmdXml.append("<EndTime>" + endTime + "</EndTime>\r\n");

        cmdXml.append("</Query>\r\n");

        CallIdHeader callIdHeader;

        if (sipTransactionInfo != null) {
            callIdHeader = SipFactory.getInstance().createHeaderFactory().createCallIdHeader(sipTransactionInfo.getCallId());
        } else {
            callIdHeader = sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport());
        }

        int subscribeCycleForAlarm = device.getSubscribeCycleForAlarm();
        if (subscribeCycleForAlarm > 0) {
            // Directory subscription validity period is no less than 30 seconds
            subscribeCycleForAlarm = Math.max(subscribeCycleForAlarm, 30);
        }

        // The effective time defaults to more than 60 seconds
        SIPRequest request = (SIPRequest) headerProvider.createSubscribeRequest(device, cmdXml.toString(), sipTransactionInfo, subscribeCycleForAlarm, "presence",
                callIdHeader);
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent, okEvent);
        return request;
    }

    @Override
    public SIPRequest catalogSubscribe(Device device, SipTransactionInfo sipTransactionInfo, SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent) throws InvalidArgumentException, SipException, ParseException {

        StringBuffer cmdXml = new StringBuffer(200);
        String charset = device.getCharset();
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Query>\r\n");
        cmdXml.append("<CmdType>Catalog</CmdType>\r\n");
        cmdXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        cmdXml.append("</Query>\r\n");

        CallIdHeader callIdHeader;

        if (sipTransactionInfo != null) {
            callIdHeader = SipFactory.getInstance().createHeaderFactory().createCallIdHeader(sipTransactionInfo.getCallId());
        } else {
            callIdHeader = sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport());
        }

        int subscribeCycleForCatalog = device.getSubscribeCycleForCatalog();
        if (subscribeCycleForCatalog > 0) {
            // Directory subscription validity period is no less than 30 seconds
            subscribeCycleForCatalog = Math.max(subscribeCycleForCatalog, 30);
        }
        // The effective time defaults to more than 60 seconds
        SIPRequest request = (SIPRequest) headerProvider.createSubscribeRequest(device, cmdXml.toString(), sipTransactionInfo, subscribeCycleForCatalog, "Catalog",
                callIdHeader);
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent, okEvent);
        return request;
    }

    @Override
    public void dragZoomCmd(Device device, String channelId, String cmdString) throws InvalidArgumentException, SipException, ParseException {

        String cmdType = "DeviceControl";
        int sn = (int) ((Math.random() * 9 + 1) * 100000);

        StringBuffer dragXml = new StringBuffer(200);
        String charset = device.getCharset();
        dragXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        dragXml.append("<Control>\r\n");
        dragXml.append("<CmdType>" + cmdType + "</CmdType>\r\n");
        dragXml.append("<SN>" + sn + "</SN>\r\n");
        if (ObjectUtils.isEmpty(channelId)) {
            dragXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        } else {
            dragXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
        }
        dragXml.append(cmdString);
        dragXml.append("</Control>\r\n");

        Request request = headerProvider.createMessageRequest(device, dragXml.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()),request);
    }



    /**
     * Playback paused
     */
    @Override
    public void playPauseCmd(Device device, DeviceChannel channel, StreamInfo streamInfo) throws InvalidArgumentException, ParseException, SipException {
        StringBuffer content = new StringBuffer(200);
        content.append("PAUSE RTSP/1.0\r\n");
        content.append("CSeq: " + getInfoCseq() + "\r\n");
        content.append("PauseTime: now\r\n");

        playbackControlCmd(device, channel, streamInfo, content.toString(), null, null);
    }


    /**
     * Playback resume
     */
    @Override
    public void playResumeCmd(Device device, DeviceChannel channel, StreamInfo streamInfo) throws InvalidArgumentException, ParseException, SipException {
        StringBuffer content = new StringBuffer(200);
        content.append("PLAY RTSP/1.0\r\n");
        content.append("CSeq: " + getInfoCseq() + "\r\n");
        content.append("Range: npt=now-\r\n");

        playbackControlCmd(device, channel, streamInfo, content.toString(), null, null);
    }

    /**
     * Playback drag and play
     */
    @Override
    public void playSeekCmd(Device device, DeviceChannel channel, StreamInfo streamInfo, long seekTime) throws InvalidArgumentException, ParseException, SipException {
        StringBuffer content = new StringBuffer(200);
        content.append("PLAY RTSP/1.0\r\n");
        content.append("CSeq: " + getInfoCseq() + "\r\n");
        content.append("Range: npt=" + Math.abs(seekTime) + "-\r\n");

        playbackControlCmd(device, channel, streamInfo, content.toString(), null, null);
    }

    /**
     * Playback at double speed
     */
    @Override
    public void playSpeedCmd(Device device, DeviceChannel channel, StreamInfo streamInfo, Double speed) throws InvalidArgumentException, ParseException, SipException {
        StringBuffer content = new StringBuffer(200);
        content.append("PLAY RTSP/1.0\r\n");
        content.append("CSeq: " + getInfoCseq() + "\r\n");
        content.append("Scale: " + String.format("%.6f", speed) + "\r\n");

        playbackControlCmd(device, channel, streamInfo, content.toString(), null, null);
    }

    private int getInfoCseq() {
        return (int) ((Math.random() * 9 + 1) * Math.pow(10, 8));
    }

    @Override
    public void playbackControlCmd(Device device, DeviceChannel channel, StreamInfo streamInfo, String content, SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent) throws SipException, InvalidArgumentException, ParseException {

        playbackControlCmd(device, channel, streamInfo.getStream(), content, errorEvent, okEvent);
    }

    @Override
    public void playbackControlCmd(Device device, DeviceChannel channel, String stream, String content, SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent) throws SipException, InvalidArgumentException, ParseException {

        SsrcTransaction ssrcTransaction = sessionManager.getSsrcTransactionByStream(MediaStreamUtil.RTP_APP, stream);
        if (ssrcTransaction == null) {
            log.info("[Playback control]Video stream information not found, device：{}, flowID: {}", device.getDeviceId(), stream);
            return;
        }

        SIPRequest request = headerProvider.createInfoRequest(device, channel.getDeviceId(), content, ssrcTransaction.getSipTransactionInfo());
        if (request == null) {
            log.info("[Playback control]Failed to build Request information, device：{}, flowID: {}", device.getDeviceId(), stream);
            return;
        }

        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent, okEvent);
    }

    @Override
    public void sendAlarmMessage(Device device, DeviceAlarmNotify deviceAlarm) throws InvalidArgumentException, SipException, ParseException {
        if (device == null) {
            return;
        }
        log.info("[Send alarm notification]Equipment： {}/{}->{},{}", device.getDeviceId(), deviceAlarm.getChannelId(),
                deviceAlarm.getLongitude(), deviceAlarm.getLatitude());

        String characterSet = device.getCharset();
        StringBuffer deviceStatusXml = new StringBuffer(600);
        deviceStatusXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n");
        deviceStatusXml.append("<Notify>\r\n");
        deviceStatusXml.append("<CmdType>Alarm</CmdType>\r\n");
        deviceStatusXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        deviceStatusXml.append("<DeviceID>" + deviceAlarm.getChannelId() + "</DeviceID>\r\n");
        deviceStatusXml.append("<AlarmPriority>" + deviceAlarm.getAlarmPriority() + "</AlarmPriority>\r\n");
        deviceStatusXml.append("<AlarmMethod>" + deviceAlarm.getAlarmMethod() + "</AlarmMethod>\r\n");
        deviceStatusXml.append("<AlarmTime>" + DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(deviceAlarm.getAlarmTime()) + "</AlarmTime>\r\n");
        deviceStatusXml.append("<AlarmDescription>" + deviceAlarm.getAlarmDescription() + "</AlarmDescription>\r\n");
        deviceStatusXml.append("<Longitude>" + deviceAlarm.getLongitude() + "</Longitude>\r\n");
        deviceStatusXml.append("<Latitude>" + deviceAlarm.getLatitude() + "</Latitude>\r\n");
        deviceStatusXml.append("<info>\r\n");
        deviceStatusXml.append("<AlarmType>" + deviceAlarm.getAlarmType() + "</AlarmType>\r\n");
        deviceStatusXml.append("</info>\r\n");
        deviceStatusXml.append("</Notify>\r\n");


        Request request = headerProvider.createMessageRequest(device, deviceStatusXml.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()),request);


    }
}
