package com.genersoft.iot.vmp.gb28181.transmit.cmd;

import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.sip.header.WWWAuthenticateHeader;
import java.text.ParseException;
import java.util.List;

public interface ISIPCommanderForPlatform {

    /**
     * Register with the superior platform
     *
     * @param parentPlatform
     * @return
     */
    void register(Platform parentPlatform, SipSubscribe.Event errorEvent , SipSubscribe.Event okEvent) throws InvalidArgumentException, ParseException, SipException;

    void register(Platform parentPlatform, SipTransactionInfo sipTransactionInfo, SipSubscribe.Event errorEvent , SipSubscribe.Event okEvent) throws InvalidArgumentException, ParseException, SipException;


    void register(Platform parentPlatform, SipTransactionInfo sipTransactionInfo, WWWAuthenticateHeader www, SipSubscribe.Event errorEvent , SipSubscribe.Event okEvent, boolean isRegister) throws SipException, InvalidArgumentException, ParseException;

    /**
     * Log out from the superior platform
     *
     * @param parentPlatform
     * @return
     */
    void unregister(Platform parentPlatform, SipTransactionInfo sipTransactionInfo, SipSubscribe.Event errorEvent , SipSubscribe.Event okEvent) throws InvalidArgumentException, ParseException, SipException;


    /**
     * Send heartbeat information to superior level
     *
     * @param parentPlatform
     * @return callId(As a decision to accept the reply)
     */
    String keepalive(Platform parentPlatform, SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent)
            throws SipException, InvalidArgumentException, ParseException;


    /**
     * Reply channel information to superior
     *
     * @param channel        Channel information
     * @param parentPlatform Platform information
     * @param sn
     * @param fromTag
     * @param size
     * @return
     */
    void catalogQuery(CommonGBChannel channel, Platform parentPlatform, String sn, String fromTag, int size)
            throws SipException, InvalidArgumentException, ParseException;

    void catalogQuery(List<CommonGBChannel> channels, Platform parentPlatform, String sn, String fromTag)
            throws InvalidArgumentException, ParseException, SipException;

    /**
     * Reply to the superior for DeviceInfo query information
     *
     * @param parentPlatform Platform information
     * @param sn SN
     * @param fromTag FROMHeader tag information
     * @return
     */
    void deviceInfoResponse(Platform parentPlatform, Device device, String sn, String fromTag) throws SipException, InvalidArgumentException, ParseException;

    /**
     * Reply to the superior for DeviceStatus query information
     *
     * @param parentPlatform Platform information
     * @param sn
     * @param fromTag
     * @return
     */
    void deviceStatusResponse(Platform parentPlatform, String channelId, String sn, String fromTag, Boolean status) throws SipException, InvalidArgumentException, ParseException;

    /**
     * Reply mobile location subscription message to superior
     *
     * @param parentPlatform Platform information
     * @param gpsMsgInfo     GPSinformation
     * @param subscribeInfo  Subscribe to related information
     * @return
     */
    void sendNotifyMobilePosition(Platform parentPlatform, GPSMsgInfo gpsMsgInfo, CommonGBChannel channel, SubscribeInfo subscribeInfo)
            throws InvalidArgumentException, ParseException, NoSuchFieldException, SipException, IllegalAccessException;

    /**
     * Reply alarm message to superior
     *
     * @param parentPlatform Platform information
     * @param deviceAlarm    Alarm information
     * @return
     */
    void sendAlarmMessage(Platform parentPlatform, DeviceAlarmNotify deviceAlarm) throws SipException, InvalidArgumentException, ParseException;

    /**
     * Reply to catalog event-increase/update
     *
     * @param parentPlatform
     * @param deviceChannels
     */
    void sendNotifyForCatalogAddOrUpdate(String type, Platform parentPlatform, List<CommonGBChannel> deviceChannels, SubscribeInfo subscribeInfo, Integer index) throws InvalidArgumentException, ParseException, NoSuchFieldException, SipException, IllegalAccessException;

    /**
     * Reply to catalog event-Delete
     *
     * @param parentPlatform
     * @param deviceChannels
     */
    void sendNotifyForCatalogOther(String type, Platform parentPlatform, List<CommonGBChannel> deviceChannels,
                                   SubscribeInfo subscribeInfo, Integer index) throws InvalidArgumentException,
            ParseException, NoSuchFieldException, SipException, IllegalAccessException;

    /**
     * ReplyrecordInfo
     *
     * @param deviceChannel  Channel information
     * @param parentPlatform Platform information
     * @param fromTag        fromTag
     * @param recordInfo     Video information
     */
    void recordInfo(CommonGBChannel deviceChannel, Platform parentPlatform, String fromTag, RecordInfo recordInfo)
            throws SipException, InvalidArgumentException, ParseException;

    /**
     * Send MediaStatus message when video playback and push is completed
     *
     * @param platform
     * @param sendRtpItem
     * @return
     */
    void sendMediaStatusNotify(Platform platform, SendRtpInfo sendRtpItem, CommonGBChannel channel) throws SipException, InvalidArgumentException, ParseException;

    void streamByeCmd(Platform platform, SendRtpInfo sendRtpItem, CommonGBChannel channel) throws SipException, InvalidArgumentException, ParseException;

    void streamByeCmd(Platform platform, CommonGBChannel channel, String app, String stream, String callId, SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException, SsrcTransactionNotFoundException;

    void broadcastInviteCmd(Platform platform, CommonGBChannel channel, String sourceId,  MediaServer mediaServerItem,
                            SSRCInfo ssrcInfo, SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent) throws ParseException, SipException, InvalidArgumentException;

    void broadcastResultCmd(Platform platform, CommonGBChannel deviceChannel, String sn, boolean result, SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException;
}
