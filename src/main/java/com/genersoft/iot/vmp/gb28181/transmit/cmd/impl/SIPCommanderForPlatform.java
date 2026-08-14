package com.genersoft.iot.vmp.gb28181.transmit.cmd.impl;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.SipLayer;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.SIPSender;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.SIPRequestHeaderPlarformProvider;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IReceiveRtpServerService;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.utils.GitUtil;
import gov.nist.javax.sip.message.MessageFactoryImpl;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.header.CallIdHeader;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Request;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@DependsOn("sipLayer")
public class SIPCommanderForPlatform implements ISIPCommanderForPlatform {

    @Autowired
    private SIPRequestHeaderPlarformProvider headerProviderPlatformProvider;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private IReceiveRtpServerService receiveRtpServerService;

    @Autowired
    private SipLayer sipLayer;

    @Autowired
    private SIPSender sipSender;

    @Autowired
    private HookSubscribe subscribe;

    @Autowired
    private UserSetting userSetting;


    @Autowired
    private SipInviteSessionManager sessionManager;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private GitUtil gitUtil;

    @Override
    public void register(Platform parentPlatform, SipSubscribe.Event errorEvent , SipSubscribe.Event okEvent) throws InvalidArgumentException, ParseException, SipException {
        register(parentPlatform, null, null, errorEvent, okEvent, true);
    }

    @Override
    public void register(Platform parentPlatform, SipTransactionInfo sipTransactionInfo, SipSubscribe.Event errorEvent , SipSubscribe.Event okEvent) throws InvalidArgumentException, ParseException, SipException {

        register(parentPlatform, sipTransactionInfo, null, errorEvent, okEvent, true);
    }

    @Override
    public void unregister(Platform parentPlatform, SipTransactionInfo sipTransactionInfo, SipSubscribe.Event errorEvent , SipSubscribe.Event okEvent) throws InvalidArgumentException, ParseException, SipException {
        register(parentPlatform, sipTransactionInfo, null, errorEvent, okEvent, false);
    }

    @Override
    public void register(Platform parentPlatform, @Nullable SipTransactionInfo sipTransactionInfo, @Nullable WWWAuthenticateHeader www,
                         SipSubscribe.Event errorEvent , SipSubscribe.Event okEvent, boolean isRegister) throws SipException, InvalidArgumentException, ParseException {
            Request request;

            CallIdHeader callIdHeader = sipSender.getNewCallIdHeader(parentPlatform.getDeviceIp(),parentPlatform.getTransport());
            String fromTag = SipUtils.getNewFromTag();
            String toTag = null;
            if (sipTransactionInfo != null ) {
                if (sipTransactionInfo.getCallId() != null) {
                    callIdHeader.setCallId(sipTransactionInfo.getCallId());
                }
                if (sipTransactionInfo.getFromTag() != null) {
                    fromTag = sipTransactionInfo.getFromTag();
                }
                if (sipTransactionInfo.getToTag() != null) {
                    toTag = sipTransactionInfo.getToTag();
                }
            }

            if (www == null ) {
                request = headerProviderPlatformProvider.createRegisterRequest(parentPlatform,
                        redisCatchStorage.getCSEQ(), fromTag,
                        toTag, callIdHeader, isRegister? parentPlatform.getExpires() : 0);
            }else {
                request = headerProviderPlatformProvider.createRegisterRequest(parentPlatform, fromTag, toTag, www, callIdHeader, isRegister? parentPlatform.getExpires() : 0);
            }

            sipSender.transmitRequest(parentPlatform.getDeviceIp(), request, (event)->{
                if (event != null) {
                    log.info("[National standard cascade]：{},  Registration failed: {} ", parentPlatform.getServerGBId(), event.msg);
                }
                if (errorEvent != null ) {
                    errorEvent.response(event);
                }
            }, okEvent, 2000L);
    }

    @Override
    public String keepalive(Platform parentPlatform, SipSubscribe.Event errorEvent , SipSubscribe.Event okEvent) throws SipException, InvalidArgumentException, ParseException {
        log.info("[National standard cascade] Send heartbeat to upper level platform： {}/{}", parentPlatform.getName(), parentPlatform.getServerGBId());
        String characterSet = parentPlatform.getCharacterSet();
        StringBuffer keepaliveXml = new StringBuffer(200);
        keepaliveXml.append("<?xml version=\"1.0\" encoding=\"")
                .append(characterSet).append("\"?>\r\n")
                .append("<Notify>\r\n")
                .append("<CmdType>Keepalive</CmdType>\r\n")
                .append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n")
                .append("<DeviceID>" + parentPlatform.getDeviceGBId() + "</DeviceID>\r\n")
                .append("<Status>OK</Status>\r\n")
                .append("</Notify>\r\n");

        CallIdHeader callIdHeader = sipSender.getNewCallIdHeader(parentPlatform.getDeviceIp(),parentPlatform.getTransport());

        Request request = headerProviderPlatformProvider.createMessageRequest(
                parentPlatform,
                keepaliveXml.toString(),
                SipUtils.getNewFromTag(),
                SipUtils.getNewViaTag(),
                callIdHeader);

        sipSender.transmitRequest(parentPlatform.getDeviceIp(), request, errorEvent, okEvent);
        return callIdHeader.getCallId();
    }

    /**
     * Reply channel information to superior
     * @param channel Channel information
     * @param parentPlatform Platform information
     */
    @Override
    public void catalogQuery(CommonGBChannel channel, Platform parentPlatform, String sn, String fromTag, int size) throws SipException, InvalidArgumentException, ParseException {

        if ( parentPlatform ==null) {
            return ;
        }
        List<CommonGBChannel> channels = new ArrayList<>();
        if (channel != null) {
            channels.add(channel);
        }
        String catalogXml = getCatalogXml(channels, sn, parentPlatform, size);

        // callid
        CallIdHeader callIdHeader = sipSender.getNewCallIdHeader(parentPlatform.getDeviceIp(),parentPlatform.getTransport());

        Request request = headerProviderPlatformProvider.createMessageRequest(parentPlatform, catalogXml.toString(), fromTag, SipUtils.getNewViaTag(), callIdHeader);
        sipSender.transmitRequest(parentPlatform.getDeviceIp(), request);

    }

    @Override
    public void catalogQuery(List<CommonGBChannel> channels, Platform parentPlatform, String sn, String fromTag) throws InvalidArgumentException, ParseException, SipException {
        if ( parentPlatform ==null) {
            return ;
        }
        sendCatalogResponse(channels, parentPlatform, sn, fromTag, 0, true);
    }
    private String getCatalogXml(List<CommonGBChannel> channels, String sn, Platform platform, int size) {
        String characterSet = platform.getCharacterSet();
        StringBuffer catalogXml = new StringBuffer(600);
        catalogXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet +"\"?>\r\n")
                .append("<Response>\r\n")
                .append("<CmdType>Catalog</CmdType>\r\n")
                .append("<SN>" +sn + "</SN>\r\n")
                .append("<DeviceID>" + platform.getDeviceGBId() + "</DeviceID>\r\n")
                .append("<SumNum>" + size + "</SumNum>\r\n")
                .append("<DeviceList Num=\"" + channels.size() +"\">\r\n");
        if (!channels.isEmpty()) {
            for (CommonGBChannel channel : channels) {
                catalogXml.append(channel.encode(platform.getDeviceGBId()));
            }
        }

        catalogXml.append("</DeviceList>\r\n");
        catalogXml.append("</Response>\r\n");
        return catalogXml.toString();
    }

    private void sendCatalogResponse(List<CommonGBChannel> channels, Platform parentPlatform, String sn, String fromTag, int index, boolean sendAfterResponse) throws SipException, InvalidArgumentException, ParseException {
        if (index > channels.size()) {
            return;
        }
        String catalogXml;
        if (channels.isEmpty()) {
            catalogXml = getCatalogXml(Collections.emptyList(), sn, parentPlatform, 0);
        }else {
            List<CommonGBChannel> subChannelList;
            if (index + parentPlatform.getCatalogGroup() < channels.size()) {
                subChannelList = channels.subList(index, index + parentPlatform.getCatalogGroup());
            }else {
                subChannelList = channels.subList(index, channels.size());
            }
            catalogXml = getCatalogXml(subChannelList, sn, parentPlatform, channels.size());
        }
        // callid
        CallIdHeader callIdHeader = sipSender.getNewCallIdHeader(parentPlatform.getDeviceIp(),parentPlatform.getTransport());

        SIPRequest request = (SIPRequest)headerProviderPlatformProvider.createMessageRequest(parentPlatform, catalogXml, fromTag, SipUtils.getNewViaTag(), callIdHeader);

        String timeoutTaskKey = "catalog_task_" + parentPlatform.getServerGBId() + sn;

        String callId = request.getCallIdHeader().getCallId();

        log.info("[command sent] National standard cascade{} Catalog query replies: Total{}Article, sent{}Article", parentPlatform.getServerGBId(),
                channels.size(), Math.min(index + parentPlatform.getCatalogGroup(), channels.size()));
        log.debug(catalogXml);
        if (sendAfterResponse) {
            // By default, the next one will be sent after receiving 200 replies. If no reply is received after a timeout, it will be sent directly at an interval of 30 milliseconds.。
            sipSender.transmitRequest(parentPlatform.getDeviceIp(), request, eventResult -> {
                if (eventResult.statusCode == -1024) {
                    // Message sending timeout, sent directly at 30 millisecond intervals
                    int indexNext = index + parentPlatform.getCatalogGroup();
                    try {
                        sendCatalogResponse(channels, parentPlatform, sn, fromTag, indexNext, false);
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        log.error("[Command sending failed] National Standard Cascade Catalog Query Reply: {}", e.getMessage());
                    }
                    return;
                }
                log.error("[Directory push failed] National standard cascade platform : {}, code: {}, msg: {}, Stop sending", parentPlatform.getServerGBId(), eventResult.statusCode, eventResult.msg);
                dynamicTask.stop(timeoutTaskKey);
            }, eventResult -> {
                dynamicTask.stop(timeoutTaskKey);
                int indexNext = index + parentPlatform.getCatalogGroup();
                try {
                    sendCatalogResponse(channels, parentPlatform, sn, fromTag, indexNext, true);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[Command sending failed] National Standard Cascade Catalog Query Reply: {}", e.getMessage());
                }
            });
        }else {
            sipSender.transmitRequest(parentPlatform.getDeviceIp(), request, eventResult -> {
                log.error("[Directory push failed] National standard cascade platform : {}, code: {}, msg: {}", parentPlatform.getServerGBId(), eventResult.statusCode, eventResult.msg);
            }, null);
            dynamicTask.startDelay(timeoutTaskKey, ()->{
                int indexNext = index + parentPlatform.getCatalogGroup();
                try {
                    sendCatalogResponse(channels, parentPlatform, sn, fromTag, indexNext, false);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[Command sending failed] National Standard Cascade Catalog Query Reply: {}", e.getMessage());
                }
            }, 100);
        }
    }

    /**
     * Reply to the superior for DeviceInfo query information
     * @param parentPlatform Platform information
     * @param sn
     * @param fromTag
     * @return
     */
    @Override
    public void deviceInfoResponse(Platform parentPlatform, Device device, String sn, String fromTag) throws SipException, InvalidArgumentException, ParseException {
        if (parentPlatform == null) {
            return;
        }
        String deviceId = device == null ? parentPlatform.getDeviceGBId() : device.getDeviceId();
        String deviceName = device == null ? parentPlatform.getName() : device.getName();
        String manufacturer = device == null ? "WVP-28181-PRO" : device.getManufacturer();
        String model = device == null ? "platform" : device.getModel();
        String firmware = device == null ? gitUtil.getBuildVersion() : device.getFirmware();
        String characterSet = parentPlatform.getCharacterSet();
        StringBuffer deviceInfoXml = new StringBuffer(600);
        deviceInfoXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n");
        deviceInfoXml.append("<Response>\r\n");
        deviceInfoXml.append("<CmdType>DeviceInfo</CmdType>\r\n");
        deviceInfoXml.append("<SN>" +sn + "</SN>\r\n");
        deviceInfoXml.append("<DeviceID>" + deviceId + "</DeviceID>\r\n");
        deviceInfoXml.append("<DeviceName>" + deviceName + "</DeviceName>\r\n");
        deviceInfoXml.append("<Manufacturer>" + manufacturer + "</Manufacturer>\r\n");
        deviceInfoXml.append("<Model>" + model + "</Model>\r\n");
        deviceInfoXml.append("<Firmware>" + firmware + "</Firmware>\r\n");
        deviceInfoXml.append("<Result>OK</Result>\r\n");
        deviceInfoXml.append("</Response>\r\n");

        CallIdHeader callIdHeader = sipSender.getNewCallIdHeader(parentPlatform.getDeviceIp(),parentPlatform.getTransport());

        Request request = headerProviderPlatformProvider.createMessageRequest(parentPlatform, deviceInfoXml.toString(), fromTag, SipUtils.getNewViaTag(), callIdHeader);
        sipSender.transmitRequest(parentPlatform.getDeviceIp(), request);
    }


    /**
     * Reply to the superior for DeviceStatus query information
     * @param parentPlatform Platform information
     * @param sn
     * @param fromTag
     * @return
     */
    @Override
    public void deviceStatusResponse(Platform parentPlatform, String channelId, String sn, String fromTag, Boolean status) throws SipException, InvalidArgumentException, ParseException {
        if (parentPlatform == null) {
            return ;
        }
        String statusStr = null;
        if (status != null) {
            statusStr = (status)?"ONLINE":"OFFLINE";
        }
        String characterSet = parentPlatform.getCharacterSet();
        StringBuffer deviceStatusXml = new StringBuffer(600);
        deviceStatusXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n")
                .append("<Response>\r\n")
                .append("<CmdType>DeviceStatus</CmdType>\r\n")
                .append("<SN>" +sn + "</SN>\r\n")
                .append("<DeviceID>" + channelId + "</DeviceID>\r\n");
        if (statusStr == null) {
            deviceStatusXml.append("<Result>ERROR</Result>\r\n");
        }else {
            deviceStatusXml.append("<Result>OK</Result>\r\n")
                    .append("<Online>"+statusStr+"</Online>\r\n")
                    .append("<Status>OK</Status>\r\n");
        }
        deviceStatusXml.append("</Response>\r\n");

        CallIdHeader callIdHeader = sipSender.getNewCallIdHeader(parentPlatform.getDeviceIp(),parentPlatform.getTransport());

        Request request = headerProviderPlatformProvider.createMessageRequest(parentPlatform, deviceStatusXml.toString(), fromTag, SipUtils.getNewViaTag(), callIdHeader);
        sipSender.transmitRequest(parentPlatform.getDeviceIp(), request);
    }

    @Override
    public void sendNotifyMobilePosition(Platform parentPlatform, GPSMsgInfo gpsMsgInfo, CommonGBChannel channel, SubscribeInfo subscribeInfo) throws InvalidArgumentException, ParseException, NoSuchFieldException, SipException, IllegalAccessException {
        if (parentPlatform == null) {
            return;
        }
        log.info("[Send Mobile Location Subscription] {}/{}->{},{}", parentPlatform.getServerGBId(), gpsMsgInfo.getId(), gpsMsgInfo.getLng(), gpsMsgInfo.getLat());
        if (log.isDebugEnabled()) {
            log.debug("[Send Mobile Location Subscription] {}/{}->{},{}", parentPlatform.getServerGBId(), gpsMsgInfo.getId(), gpsMsgInfo.getLng(), gpsMsgInfo.getLat());
        }

        String characterSet = parentPlatform.getCharacterSet();
        StringBuffer deviceStatusXml = new StringBuffer(600);
        deviceStatusXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n")
                .append("<Notify>\r\n")
                .append("<CmdType>MobilePosition</CmdType>\r\n")
                .append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n")
                .append("<DeviceID>" + channel.getGbDeviceId() + "</DeviceID>\r\n")
                .append("<Time>" + DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(gpsMsgInfo.getTime()) + "</Time>\r\n")
                .append("<Longitude>" + gpsMsgInfo.getLng() + "</Longitude>\r\n")
                .append("<Latitude>" + gpsMsgInfo.getLat() + "</Latitude>\r\n")
                .append("<Speed>" + gpsMsgInfo.getSpeed() + "</Speed>\r\n")
                .append("<Direction>" + gpsMsgInfo.getDirection() + "</Direction>\r\n")
                .append("<Altitude>" + gpsMsgInfo.getAltitude() + "</Altitude>\r\n")
                .append("</Notify>\r\n");

       sendNotify(parentPlatform, deviceStatusXml.toString(), subscribeInfo, eventResult -> {
            log.error("Failed to send NOTIFY notification message. Error：{} {}", eventResult.statusCode, eventResult.msg);
        }, null);

    }

    @Override
    public void sendAlarmMessage(Platform parentPlatform, DeviceAlarmNotify deviceAlarm) throws SipException, InvalidArgumentException, ParseException {
        if (parentPlatform == null) {
            return;
        }
        log.info("[Send alarm notification]platform： {}/{}->{},{}: {}", parentPlatform.getServerGBId(), deviceAlarm.getChannelId(),
                deviceAlarm.getLongitude(), deviceAlarm.getLatitude(), JSON.toJSONString(deviceAlarm));
        String characterSet = parentPlatform.getCharacterSet();
        StringBuffer deviceStatusXml = new StringBuffer(600);
        deviceStatusXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n")
                .append("<Notify>\r\n")
                .append("<CmdType>Alarm</CmdType>\r\n")
                .append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n")
                .append("<DeviceID>" + deviceAlarm.getChannelId() + "</DeviceID>\r\n")
                .append("<AlarmPriority>" + deviceAlarm.getAlarmPriority() + "</AlarmPriority>\r\n")
                .append("<AlarmMethod>" + deviceAlarm.getAlarmMethod() + "</AlarmMethod>\r\n")
                .append("<AlarmTime>" + DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(deviceAlarm.getAlarmTime()) + "</AlarmTime>\r\n")
                .append("<AlarmDescription>" + deviceAlarm.getAlarmDescription() + "</AlarmDescription>\r\n")
                .append("<Longitude>" + deviceAlarm.getLongitude() + "</Longitude>\r\n")
                .append("<Latitude>" + deviceAlarm.getLatitude() + "</Latitude>\r\n")
                .append("<info>\r\n")
                .append("<AlarmType>" + deviceAlarm.getAlarmType() + "</AlarmType>\r\n")
                .append("</info>\r\n")
                .append("</Notify>\r\n");

        CallIdHeader callIdHeader = sipSender.getNewCallIdHeader(parentPlatform.getDeviceIp(),parentPlatform.getTransport());

        Request request = headerProviderPlatformProvider.createMessageRequest(parentPlatform, deviceStatusXml.toString(), SipUtils.getNewFromTag(), SipUtils.getNewViaTag(), callIdHeader);
        sipSender.transmitRequest(parentPlatform.getDeviceIp(), request);

    }

    @Override
    public void sendNotifyForCatalogAddOrUpdate(String type, Platform parentPlatform, List<CommonGBChannel> deviceChannels, SubscribeInfo subscribeInfo, Integer index) throws InvalidArgumentException, ParseException, NoSuchFieldException, SipException, IllegalAccessException {
        if (parentPlatform == null || deviceChannels == null || deviceChannels.isEmpty() || subscribeInfo == null) {
            return;
        }
        if (index == null) {
            index = 0;
        }
        if (index >= deviceChannels.size()) {
            return;
        }
        List<CommonGBChannel> channels;
        if (index + parentPlatform.getCatalogGroup() < deviceChannels.size()) {
            channels = deviceChannels.subList(index, index + parentPlatform.getCatalogGroup());
        }else {
            channels = deviceChannels.subList(index, deviceChannels.size());
        }
        Integer finalIndex = index;
        String catalogXmlContent = getCatalogXmlContentForCatalogAddOrUpdate(parentPlatform, channels,
                deviceChannels.size(), type, subscribeInfo);
        String channelDeviceIds = channels.stream().map(CommonGBChannel::getGbDeviceId).collect(Collectors.joining(","));
        log.info("[Send NOTIFY notification]Type： {}，channel： {}", type, channelDeviceIds);
        sendNotify(parentPlatform, catalogXmlContent, subscribeInfo, eventResult -> {
            log.error("Failed to send NOTIFY notification message. Error：{} {}", eventResult.statusCode, eventResult.msg);
            log.error(catalogXmlContent);
        }, (eventResult -> {
            try {
                sendNotifyForCatalogAddOrUpdate(type, parentPlatform, deviceChannels, subscribeInfo,
                        finalIndex + parentPlatform.getCatalogGroup());
            } catch (InvalidArgumentException | ParseException | NoSuchFieldException | SipException |
                     IllegalAccessException e) {
                log.error("[Command sending failed] National standard cascade NOTIFY notice: {}", e.getMessage());
            }
        }));
    }

    private void sendNotify(Platform parentPlatform, String catalogXmlContent,
                            SubscribeInfo subscribeInfo, SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent )
            throws SipException, ParseException, InvalidArgumentException {
        MessageFactoryImpl messageFactory = (MessageFactoryImpl) SipFactory.getInstance().createMessageFactory();
        String characterSet = parentPlatform.getCharacterSet();
        // Set encoding to prevent Chinese garbled characters
        messageFactory.setDefaultContentEncodingCharset(characterSet);

        SIPRequest notifyRequest = headerProviderPlatformProvider.createNotifyRequest(parentPlatform, catalogXmlContent, subscribeInfo);

        sipSender.transmitRequest(parentPlatform.getDeviceIp(), notifyRequest, errorEvent, okEvent);
    }

    private  String getCatalogXmlContentForCatalogAddOrUpdate(Platform platform, List<CommonGBChannel> channels, int sumNum, String type, SubscribeInfo subscribeInfo) {
        StringBuffer catalogXml = new StringBuffer(600);
        String characterSet = platform.getCharacterSet();
        catalogXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n")
                .append("<Notify>\r\n")
                .append("<CmdType>Catalog</CmdType>\r\n")
                .append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n")
                .append("<DeviceID>" + platform.getDeviceGBId() + "</DeviceID>\r\n")
                .append("<SumNum>"+ sumNum +"</SumNum>\r\n")
                .append("<DeviceList Num=\"" + channels.size() + "\">\r\n");
        if (!channels.isEmpty()) {
            for (CommonGBChannel channel : channels) {
                catalogXml.append(channel.encode(type, platform.getDeviceGBId()));
            }
        }
        catalogXml.append("</DeviceList>\r\n")
                .append("</Notify>\r\n");
        return catalogXml.toString();
    }

    @Override
    public void sendNotifyForCatalogOther(String type, Platform parentPlatform, List<CommonGBChannel> deviceChannels,
                                          SubscribeInfo subscribeInfo, Integer index) throws InvalidArgumentException, ParseException, NoSuchFieldException, SipException, IllegalAccessException {
        if (parentPlatform == null
                || deviceChannels == null
                || deviceChannels.size() == 0
                || subscribeInfo == null) {
            log.warn("[Missing required parameters]");
            return;
        }

        if (index == null) {
            index = 0;
        }
        if (index >= deviceChannels.size()) {
            return;
        }
        List<CommonGBChannel> channels;
        if (index + parentPlatform.getCatalogGroup() < deviceChannels.size()) {
            channels = deviceChannels.subList(index, index + parentPlatform.getCatalogGroup());
        }else {
            channels = deviceChannels.subList(index, deviceChannels.size());
        }
        log.info("[Send NOTIFY notification]Type： {}，Send quantity： {}", type, channels.size());
        Integer finalIndex = index;
        String catalogXmlContent = getCatalogXmlContentForCatalogOther(parentPlatform, channels, type);
        sendNotify(parentPlatform, catalogXmlContent, subscribeInfo, eventResult -> {
            log.error("Failed to send NOTIFY notification message. Error：{} {}", eventResult.statusCode, eventResult.msg);
        }, eventResult -> {
            try {
                sendNotifyForCatalogOther(type, parentPlatform, deviceChannels, subscribeInfo,
                        finalIndex + parentPlatform.getCatalogGroup());
            } catch (InvalidArgumentException | ParseException | NoSuchFieldException | SipException |
                     IllegalAccessException e) {
                log.error("[Command sending failed] National standard cascade NOTIFY notice: {}", e.getMessage());
            }
        });
    }

    private String getCatalogXmlContentForCatalogOther(Platform platform, List<CommonGBChannel> channels, String type) {

        String characterSet = platform.getCharacterSet();
        StringBuffer catalogXml = new StringBuffer(600);
        catalogXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n")
                .append("<Notify>\r\n")
                .append("<CmdType>Catalog</CmdType>\r\n")
                .append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n")
                .append("<DeviceID>" + platform.getDeviceGBId() + "</DeviceID>\r\n")
                .append("<SumNum>1</SumNum>\r\n")
                .append("<DeviceList Num=\" " + channels.size() + " \">\r\n");
        if (!channels.isEmpty()) {
            for (CommonGBChannel channel : channels) {
               catalogXml.append(channel.encode(type, platform.getDeviceGBId()));
            }
        }
        catalogXml.append("</DeviceList>\r\n")
                .append("</Notify>\r\n");
        return catalogXml.toString();
    }
    @Override
    public void recordInfo(CommonGBChannel deviceChannel, Platform parentPlatform, String fromTag, RecordInfo recordInfo) throws SipException, InvalidArgumentException, ParseException {
        if ( parentPlatform ==null) {
            return ;
        }
        log.info("[National standard cascade] Send video data channel： {}", recordInfo.getChannelId());
        String characterSet = parentPlatform.getCharacterSet();
        StringBuffer recordXml = new StringBuffer(600);
        recordXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n")
                .append("<Response>\r\n")
                .append("<CmdType>RecordInfo</CmdType>\r\n")
                .append("<SN>" +recordInfo.getSn() + "</SN>\r\n")
                .append("<DeviceID>" + deviceChannel.getGbDeviceId() + "</DeviceID>\r\n")
                .append("<SumNum>" + recordInfo.getSumNum() + "</SumNum>\r\n");
        if (recordInfo.getRecordList() == null ) {
            recordXml.append("<RecordList Num=\"0\">\r\n");
        }else {
            recordXml.append("<RecordList Num=\"" + recordInfo.getRecordList().size()+"\">\r\n");
            if (recordInfo.getRecordList().size() > 0) {
                for (RecordItem recordItem : recordInfo.getRecordList()) {
                    recordXml.append("<Item>\r\n");
                    if (deviceChannel != null) {
                        recordXml.append("<DeviceID>" + deviceChannel.getGbDeviceId() + "</DeviceID>\r\n")
                                .append("<Name>" + recordItem.getName() + "</Name>\r\n")
                                .append("<StartTime>" + DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(recordItem.getStartTime()) + "</StartTime>\r\n")
                                .append("<EndTime>" + DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(recordItem.getEndTime()) + "</EndTime>\r\n")
                                .append("<Secrecy>" + recordItem.getSecrecy() + "</Secrecy>\r\n")
                                .append("<Type>" + recordItem.getType() + "</Type>\r\n");
                        if (!ObjectUtils.isEmpty(recordItem.getFileSize())) {
                            recordXml.append("<FileSize>" + recordItem.getFileSize() + "</FileSize>\r\n");
                        }
                        if (!ObjectUtils.isEmpty(recordItem.getFilePath())) {
                            recordXml.append("<FilePath>" + recordItem.getFilePath() + "</FilePath>\r\n");
                        }
                    }
                    recordXml.append("</Item>\r\n");
                }
            }
        }

        recordXml.append("</RecordList>\r\n")
                .append("</Response>\r\n");
        log.debug("[National standard cascade] Send video data channel：{}, content： {}", recordInfo.getChannelId(), recordXml);
        // callid
        CallIdHeader callIdHeader = sipSender.getNewCallIdHeader(parentPlatform.getDeviceIp(),parentPlatform.getTransport());

        Request request = headerProviderPlatformProvider.createMessageRequest(parentPlatform, recordXml.toString(), fromTag, SipUtils.getNewViaTag(), callIdHeader);
        sipSender.transmitRequest(parentPlatform.getDeviceIp(), request, null, eventResult -> {
            log.info("[National standard cascade] Send video data channel：{}, Sent successfully", recordInfo.getChannelId());
        });

    }

    @Override
    public void sendMediaStatusNotify(Platform parentPlatform, SendRtpInfo sendRtpInfo, CommonGBChannel channel) throws SipException, InvalidArgumentException, ParseException {
        if (channel == null || parentPlatform == null) {
            return;
        }

        String characterSet = parentPlatform.getCharacterSet();
        StringBuffer mediaStatusXml = new StringBuffer(200);
        mediaStatusXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n")
                .append("<Notify>\r\n")
                .append("<CmdType>MediaStatus</CmdType>\r\n")
                .append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n")
                .append("<DeviceID>" + channel.getGbDeviceId() + "</DeviceID>\r\n")
                .append("<NotifyType>121</NotifyType>\r\n")
                .append("</Notify>\r\n");

        SIPRequest messageRequest = (SIPRequest)headerProviderPlatformProvider.createMessageRequest(parentPlatform, mediaStatusXml.toString(),
                sendRtpInfo);

        sipSender.transmitRequest(parentPlatform.getDeviceIp(),messageRequest);

    }

    @Override
    public synchronized void streamByeCmd(Platform platform, SendRtpInfo sendRtpItem, CommonGBChannel channel) throws SipException, InvalidArgumentException, ParseException {
        if (sendRtpItem == null ) {
            log.info("[Send to superiorBYE]， sendRtpItem forNULL");
            return;
        }
        if (platform == null) {
            log.info("[Send to superiorBYE]， platform forNULL");
            return;
        }
        log.info("[Send to superiorBYE]， {}/{}", platform.getServerGBId(), sendRtpItem.getChannelId());
        String mediaServerId = sendRtpItem.getMediaServerId();
        MediaServer mediaServerItem = mediaServerService.getOne(mediaServerId);
        if (mediaServerItem != null) {
            receiveRtpServerService.closeRTPServer(mediaServerItem, sendRtpItem.getApp(), sendRtpItem.getStream());
        }
        SIPRequest byeRequest = headerProviderPlatformProvider.createByeRequest(platform, sendRtpItem, channel);
        if (byeRequest == null) {
            log.warn("[Send to superiorbye]：Unable to create byeRequest");
        }
        sipSender.transmitRequest(platform.getDeviceIp(),byeRequest);
    }

    @Override
    public void streamByeCmd(Platform platform, CommonGBChannel channel, String app, String stream, String callId, SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException, SsrcTransactionNotFoundException {

        SsrcTransaction ssrcTransaction = null;
        if (callId != null) {
            ssrcTransaction = sessionManager.getSsrcTransactionByCallId(callId);
        }else if (stream != null) {
            ssrcTransaction = sessionManager.getSsrcTransactionByStream(app, stream);
        }
        if (ssrcTransaction == null) {
            throw new SsrcTransactionNotFoundException(platform.getServerGBId(), channel.getGbDeviceId(), callId, stream);
        }

        sessionManager.removeByStream(ssrcTransaction.getApp(), ssrcTransaction.getStream());

        Request byteRequest = headerProviderPlatformProvider.createByteRequest(platform, channel.getGbDeviceId(), ssrcTransaction.getSipTransactionInfo());
        sipSender.transmitRequest(sipLayer.getLocalIp(platform.getDeviceIp()), byteRequest, null, okEvent);
    }

    @Override
    public void broadcastResultCmd(Platform platform, CommonGBChannel deviceChannel, String sn, boolean result, SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException {
        if (platform == null || deviceChannel == null) {
            return;
        }
        String characterSet = platform.getCharacterSet();
        StringBuffer mediaStatusXml = new StringBuffer(200);
        mediaStatusXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n")
                      .append("<Response>\r\n")
                      .append("<CmdType>Broadcast</CmdType>\r\n")
                      .append("<SN>" + sn + "</SN>\r\n")
                      .append("<DeviceID>" + deviceChannel.getGbDeviceId() + "</DeviceID>\r\n")
                      .append("<Result>" + (result?"OK":"ERROR") + "</Result>\r\n")
                      .append("</Response>\r\n");

        CallIdHeader callIdHeader = sipSender.getNewCallIdHeader(platform.getDeviceIp(), platform.getTransport());

        SIPRequest messageRequest = (SIPRequest)headerProviderPlatformProvider.createMessageRequest(platform, mediaStatusXml.toString(),
               SipUtils.getNewFromTag(), SipUtils.getNewViaTag(), callIdHeader);

        sipSender.transmitRequest(platform.getDeviceIp(),messageRequest, errorEvent, okEvent);
    }

    @Override
    public void broadcastInviteCmd(Platform platform, CommonGBChannel channel,String sourceId, MediaServer mediaServer,
                                   SSRCInfo ssrcInfo, SipSubscribe.Event okEvent,
                                   SipSubscribe.Event errorEvent) throws ParseException, SipException, InvalidArgumentException {
        String stream = ssrcInfo.getStream();

        if (platform == null) {
            return;
        }

        log.info("{} The allocated ZLM is: {} [{}:{}]", stream, mediaServer.getId(), mediaServer.getIp(), ssrcInfo.getPort());
        String sdpIp = mediaServer.getSdpIp();

        StringBuffer content = new StringBuffer(200);
        content.append("v=0\r\n");
        content.append("o=" + platform.getDeviceGBId() + " 0 0 IN IP4 " + sdpIp + "\r\n");
        content.append("s=Play\r\n");
        content.append("u=" + channel.getGbDeviceId() + ":0\r\n");
        content.append("c=IN IP4 " + sdpIp + "\r\n");
        content.append("t=0 0\r\n");

        if ("TCP-PASSIVE".equalsIgnoreCase(userSetting.getBroadcastForPlatform())) {
            content.append("m=audio " + ssrcInfo.getPort() + " TCP/RTP/AVP 8 96\r\n");
        } else if ("TCP-ACTIVE".equalsIgnoreCase(userSetting.getBroadcastForPlatform())) {
            content.append("m=audio " + ssrcInfo.getPort() + " TCP/RTP/AVP 8 96\r\n");
        } else if ("UDP".equalsIgnoreCase(userSetting.getBroadcastForPlatform())) {
            content.append("m=audio " + ssrcInfo.getPort() + " RTP/AVP 8 96\r\n");
        }

        content.append("a=recvonly\r\n");
        content.append("a=rtpmap:8 PCMA/8000\r\n");
        content.append("a=rtpmap:96 PS/90000\r\n");
        if ("TCP-PASSIVE".equalsIgnoreCase(userSetting.getBroadcastForPlatform())) {
            content.append("a=setup:passive\r\n");
            content.append("a=connection:new\r\n");
        }else if ("TCP-ACTIVE".equalsIgnoreCase(userSetting.getBroadcastForPlatform())) {
            content.append("a=setup:active\r\n");
            content.append("a=connection:new\r\n");
        }

        content.append("y=" + ssrcInfo.getSsrc() + "\r\n");//ssrc
        // fField:f= v/encoding format/resolution/Frame rate/Code rate type/Code rate sizea/encoding format/Code rate size/Sampling rate
        content.append("f=v/2/5/25/1/4096a/1/8/1\r\n");
        CallIdHeader callIdHeader = sipSender.getNewCallIdHeader(sipLayer.getLocalIp(platform.getDeviceIp()), platform.getTransport());

        Request request = headerProviderPlatformProvider.createInviteRequest(platform, sourceId, channel.getGbDeviceId(),
                content.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(),  ssrcInfo.getSsrc(),
                callIdHeader);
        sipSender.transmitRequest(sipLayer.getLocalIp(platform.getDeviceIp()), request, (e -> {
            sessionManager.removeByStream(ssrcInfo.getApp(), ssrcInfo.getStream());
            errorEvent.response(e);
        }), e -> {
            ResponseEvent responseEvent = (ResponseEvent) e.event;
            SIPResponse response = (SIPResponse) responseEvent.getResponse();
            SsrcTransaction ssrcTransaction = SsrcTransaction.buildForPlatform(platform.getServerGBId(), channel.getGbId(),
                    callIdHeader.getCallId(), ssrcInfo.getApp(),  stream, ssrcInfo.getSsrc(), mediaServer.getId(), response, InviteSessionType.BROADCAST);
            sessionManager.put(ssrcTransaction);
            okEvent.response(e);
        });
    }
}
