package com.genersoft.iot.vmp.gb28181.service;

import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.bean.SipTransactionInfo;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.service.bean.InviteTimeOutCallback;
import com.github.pagehelper.PageInfo;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.List;

/**
 * Business categories of the national standard platform
 * @author lin
 */
public interface IPlatformService {

    Platform queryPlatformByServerGBId(String platformGbId);

    /**
     * Get the upper-level platform by page
     * @param page
     * @param count
     * @return
     */
    PageInfo<Platform> queryPlatformList(int page, int count, String query);

    /**
     * Add cascading platform
     * @param parentPlatform Cascade platform
     */
    boolean add(Platform parentPlatform);

    /**
     * Add cascading platform
     * @param parentPlatform Cascade platform
     */
    boolean update(Platform parentPlatform);

    /**
     * Platform is online
     * @param parentPlatform Platform information
     */
    void online(Platform parentPlatform, SipTransactionInfo sipTransactionInfo);

    /**
     * Platform offline
     * @param parentPlatform Platform information
     */
    void offline(Platform parentPlatform);

    /**
     * Send location subscriptions to upper-level platforms
     * @param platformId platform
     */
    void sendNotifyMobilePosition(String platformId);

    /**
     * Send a voice message to superiors
     */
    void broadcastInvite(Platform platform, CommonGBChannel channel, String sourceId, MediaServer mediaServerItem, HookSubscribe.Event hookEvent,
                         SipSubscribe.Event errorEvent, InviteTimeOutCallback timeoutCallback) throws InvalidArgumentException, ParseException, SipException;

    /**
     * Voice call replyBYE
     */
    void stopBroadcast(Platform platform, CommonGBChannel channel, String app, String stream, boolean sendBye, MediaServer mediaServerItem);

    void addSimulatedSubscribeInfo(Platform parentPlatform);

    Platform queryOne(Integer platformId);

    List<Platform> queryEnablePlatformList(String serverId);

    boolean delete(Integer platformId);

    List<Platform> queryAll(String serverId);

}
