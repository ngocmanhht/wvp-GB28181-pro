package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.*;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.dao.PlatformChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.PlatformMapper;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.gb28181.service.IInviteStreamService;
import com.genersoft.iot.vmp.gb28181.service.IPlatformService;
import com.genersoft.iot.vmp.gb28181.session.SSRCFactory;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.gb28181.task.platformStatus.PlatformKeepaliveTask;
import com.genersoft.iot.vmp.gb28181.task.platformStatus.PlatformRegisterTask;
import com.genersoft.iot.vmp.gb28181.task.platformStatus.PlatformRegisterTaskInfo;
import com.genersoft.iot.vmp.gb28181.task.platformStatus.PlatformStatusTaskRunner;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.hook.HookData;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.event.media.MediaDepartureEvent;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaSendRtpStoppedEvent;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IReceiveRtpServerService;
import com.genersoft.iot.vmp.service.ISendRtpServerService;
import com.genersoft.iot.vmp.service.bean.*;
import com.genersoft.iot.vmp.service.redisMsg.IRedisRpcService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.sdp.*;
import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

/**
 * @author lin
 */
@Slf4j
@Service
public class PlatformServiceImpl implements IPlatformService {

    @Autowired
    private PlatformMapper platformMapper;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private SSRCFactory ssrcFactory;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private ISIPCommanderForPlatform commanderForPlatform;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private SubscribeHolder subscribeHolder;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private IRedisRpcService redisRpcService;

    @Autowired
    private SipConfig sipConfig;

    @Autowired
    private SipInviteSessionManager sessionManager;

    @Autowired
    private IInviteStreamService inviteStreamService;

    @Autowired
    private PlatformChannelMapper platformChannelMapper;

    @Autowired
    private IGbChannelService channelService;

    @Autowired
    private ISendRtpServerService sendRtpServerService;

    @Autowired
    private IReceiveRtpServerService receiveRtpServerService;

    @Autowired
    private PlatformStatusTaskRunner statusTaskRunner;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(){

        // Find national standard push streaming
        List<SendRtpInfo> sendRtpItems = redisCatchStorage.queryAllSendRTPServer();
        if (!sendRtpItems.isEmpty()) {
            for (SendRtpInfo sendRtpItem : sendRtpItems) {
                MediaServer mediaServerItem = mediaServerService.getOne(sendRtpItem.getMediaServerId());
                CommonGBChannel channel = channelService.getOne(sendRtpItem.getChannelId());
                if (channel == null){
                    continue;
                }
                sendRtpServerService.delete(sendRtpItem);
                if (mediaServerItem != null) {
                    boolean stopResult = mediaServerService.initStopSendRtp(mediaServerItem, sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getSsrc());
                    if (stopResult) {
                        Platform platform = queryPlatformByServerGBId(sendRtpItem.getTargetId());

                        if (platform != null && userSetting.getServerId().equals(platform.getServerId())) {
                            try {
                                commanderForPlatform.streamByeCmd(platform, sendRtpItem, channel);
                            } catch (InvalidArgumentException | ParseException | SipException e) {
                                log.error("[Command sending failed] National standard cascade sendBYE: {}", e.getMessage());
                            }
                        }
                    }
                }
            }
        }

        // At startup, if there is an unexpired registration platform, a logout is sent.
        List<PlatformRegisterTaskInfo> registerTaskInfoList = statusTaskRunner.getAllRegisterTaskInfo();
        if (registerTaskInfoList.isEmpty()) {
            return;
        }
        for (PlatformRegisterTaskInfo taskInfo : registerTaskInfoList) {
            log.info("[National standard cascade] When starting the service, it is found that the platform registration is still valid and logs out.： {}", taskInfo.getPlatformServerId());
            Platform platform = queryPlatformByServerGBId(taskInfo.getPlatformServerId());
            if (platform == null) {
                statusTaskRunner.removeRegisterTask(taskInfo.getPlatformServerId());
                continue;
            }
            if (userSetting.getServerId().equals(platform.getServerId())) {
                sendUnRegister(platform, taskInfo.getSipTransactionInfo());
            }
        }
        // All platforms are offline by default at startup
        platformMapper.offlineAll(userSetting.getServerId());
    }
    @Scheduled(fixedDelay = 20, timeUnit = TimeUnit.SECONDS)   //Executed every 3 seconds
    public void statusLostCheck(){
        // Check every 20 seconds to see if there is an enabled but unregistered platform. If it exists, initiate registration.
        // Get all online and enabled platforms
        List<Platform> platformList = platformMapper.queryServerIdsWithEnableAndServer(userSetting.getServerId());
        if (platformList.isEmpty()) {
            return;
        }
        for (Platform platform : platformList) {
             if (statusTaskRunner.containsRegister(platform.getServerGBId()) && statusTaskRunner.containsKeepAlive(platform.getServerGBId())) {
                 continue;
             }
             if (statusTaskRunner.containsRegister(platform.getServerGBId())) {
                 SipTransactionInfo transactionInfo = statusTaskRunner.getRegisterTransactionInfo(platform.getServerGBId());
                 // After logging out, the starting platform is offline. If it is an enabled platform, the next loss detection will be detected and re-registered online.
                 sendUnRegister(platform, transactionInfo);
             }else {
                 statusTaskRunner.removeKeepAliveTask(platform.getServerGBId());
                 sendRegister(platform, null);
             }
        }
    }

    private void sendRegister(Platform platform, SipTransactionInfo sipTransactionInfo) {
        try {
            commanderForPlatform.register(platform, sipTransactionInfo, eventResult -> {
                log.info("[National standard cascade] {}（{}）,Registration failed", platform.getName(), platform.getServerGBId());
                offline(platform);
            }, null);
        } catch (InvalidArgumentException | ParseException | SipException e) {
            log.error("[Command sending failed] National standard cascade: {}", e.getMessage());
        }
    }

    private void sendUnRegister(Platform platform, SipTransactionInfo sipTransactionInfo) {
        statusTaskRunner.removeRegisterTask(platform.getServerGBId());
        statusTaskRunner.removeKeepAliveTask(platform.getServerGBId());
        try {
            commanderForPlatform.unregister(platform, sipTransactionInfo, null, eventResult -> {
                log.info("[National standard cascade] Logout successful, platform：{}", platform.getServerGBId());
            });
        } catch (InvalidArgumentException | ParseException | SipException e) {
            log.error("[Command sending failed] National standard cascade: {}", e.getMessage());
        }
    }

    // Regularly monitor whether the WVP service performed by the national standard cascade is normal. If it is abnormal, select a new wvp to execute.
    @Scheduled(fixedDelay = 2, timeUnit = TimeUnit.SECONDS)   //Executed every 3 seconds
    public void execute(){
        if (!userSetting.isAutoRegisterPlatform()) {
            return;
        }
        // Find non-platform national standard cascade execution servicesId
        List<String> serverIds = platformMapper.queryServerIdsWithEnableAndNotInServer(userSetting.getServerId());
        if (serverIds == null || serverIds.isEmpty()) {
            return;
        }
        serverIds.forEach(serverId -> {
           // Check if each is alive
            ServerInfo serverInfo = redisCatchStorage.queryServerInfo(serverId);
            if (serverInfo != null) {
                return;
            }
            log.info("[cluster] detected {} Offline", serverId);
            redisCatchStorage.removeOfflineWVPInfo(serverId);
            String chooseServerId = redisCatchStorage.chooseOneServer(serverId);
            if (!userSetting.getServerId().equals(chooseServerId)){
                return;
            }
            // This platform needs to select a new platform for processing. Make sure the current platform starts processing.
            List<Platform> platformList = platformMapper.queryByServerId(serverId);
            platformList.forEach(platform -> {
                log.info("[cluster] Open the upper level platform from this platform{}({})registration", platform.getName(), platform.getServerGBId());
                // Set the platform to use the current platformIP
                platform.setAddress(getIpWithSameNetwork(platform.getAddress()));
                platform.setServerId(userSetting.getServerId());
                platformMapper.update(platform);
                // Check whether the platform registration has expired. If not, it will be canceled and re-registered by this platform.
                List<PlatformRegisterTaskInfo> taskInfoList = statusTaskRunner.getRegisterTransactionInfoByServerId(serverId);
                boolean needUnregister = false;
                SipTransactionInfo sipTransactionInfo = null;
                if (!taskInfoList.isEmpty()) {
                    for (PlatformRegisterTaskInfo taskInfo : taskInfoList) {
                        if (taskInfo.getPlatformServerId().equals(platform.getServerGBId())
                                && taskInfo.getSipTransactionInfo() != null) {
                            needUnregister = true;
                            sipTransactionInfo = taskInfo.getSipTransactionInfo();
                            break;
                        }
                    }
                }
                if (needUnregister) {
                    sendUnRegister(platform, sipTransactionInfo);
                }else {
                    // Start registration
                    // When the registration is successful, the online method is directly called by the program.
                    sendRegister(platform, null);
                }
            });
        });
    }

    /**
     * Get the same network segmentIP
     */
    private String getIpWithSameNetwork(String ip){
        if (ip == null || sipConfig.getMonitorIps().size() == 1) {
            return sipConfig.getMonitorIps().get(0);
        }
        String[] ipSplit = ip.split("\\.");
        String ip1 = null, ip2 = null, ip3 = null;
        for (String monitorIp : sipConfig.getMonitorIps()) {
            String[] monitorIpSplit = monitorIp.split("\\.");
            if (monitorIpSplit[0].equals(ipSplit[0]) && monitorIpSplit[1].equals(ipSplit[1]) && monitorIpSplit[2].equals(ipSplit[2])) {
                ip3 = monitorIp;
            }else if (monitorIpSplit[0].equals(ipSplit[0]) && monitorIpSplit[1].equals(ipSplit[1])) {
                ip2 = monitorIp;
            }else if (monitorIpSplit[0].equals(ipSplit[0])) {
                ip1 = monitorIp;
            }
        }
        if (ip3 != null) {
            return ip3;
        }else if (ip2 != null) {
            return ip2;
        }else if (ip1 != null) {
            return ip1;
        }else {
            return sipConfig.getMonitorIps().get(0);
        }
    }

    /**
     * Stream departure processing
     */
    @Async
    @EventListener
    public void onApplicationEvent(MediaDepartureEvent event) {
        List<SendRtpInfo> sendRtpItems = sendRtpServerService.queryByStream(event.getStream());
        if (!sendRtpItems.isEmpty()) {
            for (SendRtpInfo sendRtpItem : sendRtpItems) {
                if (sendRtpItem != null && sendRtpItem.getApp().equals(event.getApp()) && sendRtpItem.isSendToPlatform()) {
                    String platformId = sendRtpItem.getTargetId();
                    Platform platform = platformMapper.getParentPlatByServerGBId(platformId);
                    CommonGBChannel channel = channelService.getOne(sendRtpItem.getChannelId());
                    try {
                        if (platform != null && channel != null) {
                            commanderForPlatform.streamByeCmd(platform, sendRtpItem, channel);
                            sendRtpServerService.delete(sendRtpItem);
                        }
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        log.error("[Command sending failed] sendBYE: {}", e.getMessage());
                    }
                }
            }
        }
    }


    /**
     * Stop streaming
     */
    @Async
    @EventListener
    public void onApplicationEvent(MediaSendRtpStoppedEvent event) {
        List<SendRtpInfo> sendRtpItems = sendRtpServerService.queryByStream(event.getStream());
        if (sendRtpItems != null && !sendRtpItems.isEmpty()) {
            for (SendRtpInfo sendRtpItem : sendRtpItems) {
                if (sendRtpItem != null && sendRtpItem.getApp().equals(event.getApp()) && sendRtpItem.isSendToPlatform()) {
                    Platform platform = platformMapper.getParentPlatByServerGBId(sendRtpItem.getTargetId());
                    CommonGBChannel channel = channelService.getOne(sendRtpItem.getChannelId());
                    try {
                        commanderForPlatform.streamByeCmd(platform, sendRtpItem, channel);
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        log.error("[Command sending failed] National standard cascade sendBYE: {}", e.getMessage());
                    }
                    sendRtpServerService.delete(sendRtpItem);
                }
            }
        }
    }

    @Override
    public Platform queryPlatformByServerGBId(String platformGbId) {
        return platformMapper.getParentPlatByServerGBId(platformGbId);
    }

    @Override
    public PageInfo<Platform> queryPlatformList(int page, int count, String query) {
        PageHelper.startPage(page, count);
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }
        List<Platform> all = platformMapper.queryList(query);
        return new PageInfo<>(all);
    }

    @Override
    public boolean add(Platform platform) {
        log.info("[National standard cascade]Add platform {}", platform.getDeviceGBId());
        if (platform.getCatalogGroup() == 0) {
            // The default number of directories sent each time is1
            platform.setCatalogGroup(1);
        }
        platform.setServerId(userSetting.getServerId());
        int result = platformMapper.add(platform);

        if (platform.isEnable()) {
            // Enable to send registration when saving
            // When the registration is successful, the online method is directly called by the program.
            sendRegister(platform, null);
        }
        return result > 0;
    }



    @Override
    public boolean update(Platform platform) {
        Assert.isTrue(platform.getId() > 0, "IDmust exist");
        log.info("[National standard cascade] Update platform {}({})", platform.getName(), platform.getDeviceGBId());
        platform.setCharacterSet(platform.getCharacterSet().toUpperCase());
        Platform platformInDb = platformMapper.query(platform.getId());
        Assert.notNull(platformInDb, "Platform does not exist");
        if (!userSetting.getServerId().equals(platformInDb.getServerId())) {
            return redisRpcService.updatePlatform(platformInDb.getServerId(), platform);
        }
        // Update database
        if (platform.getCatalogGroup() == 0) {
            platform.setCatalogGroup(1);
        }
        platformMapper.update(platform);
        if (statusTaskRunner.containsRegister(platformInDb.getServerGBId())) {
            SipTransactionInfo transactionInfo = statusTaskRunner.getRegisterTransactionInfo(platformInDb.getServerGBId());
            // After logging out, the starting platform is offline. If it is an enabled platform, the next loss detection will be detected and re-registered online.
            sendUnRegister(platformInDb, transactionInfo);
        }else if (platform.isEnable()) {
            sendRegister(platform, null);
        }

        return false;
    }

    @Override
    public void online(Platform platform, SipTransactionInfo sipTransactionInfo) {
        log.info("[National standard cascade]：{}, Platform is online", platform.getServerGBId());
        PlatformRegisterTask registerTask = new PlatformRegisterTask(platform.getServerGBId(), platform.getExpires() * 1000L - 500L,
                sipTransactionInfo, (platformServerGbId) -> {
            this.registerExpire(platformServerGbId, sipTransactionInfo);
        });
        statusTaskRunner.addRegisterTask(registerTask);

        PlatformKeepaliveTask keepaliveTask = new PlatformKeepaliveTask(platform.getServerGBId(), platform.getKeepTimeout() * 1000L,
                this::keepaliveExpire);
        statusTaskRunner.addKeepAliveTask(keepaliveTask);
        platformMapper.updateStatus(platform.getId(), true, userSetting.getServerId());

        if (platform.getAutoPushChannel() != null && platform.getAutoPushChannel()) {
            if (subscribeHolder.getCatalogSubscribe(platform.getServerGBId()) == null) {
                log.info("[National standard cascade]：{}, Add automatic channel push simulation subscription information", platform.getServerGBId());
                addSimulatedSubscribeInfo(platform);
            }
        }else {
            SubscribeInfo catalogSubscribe = subscribeHolder.getCatalogSubscribe(platform.getServerGBId());
            if (catalogSubscribe != null && catalogSubscribe.getExpires() == -1) {
                subscribeHolder.removeCatalogSubscribe(platform.getServerGBId());
            }
        }
    }

    /**
     * Registration expiration processing
     */
    private void registerExpire(String platformServerId, SipTransactionInfo transactionInfo) {
        log.info("[National standard cascade] Registration expires, superior platform number： {}", platformServerId);
        Platform platform = queryPlatformByServerGBId(platformServerId);
        if (platform == null || !platform.isEnable()) {
            log.info("[National standard cascade] Registration expires, superior platform number： {}, Platform does not exist or is not enabled, ignore", platformServerId);
            return;
        }
        sendRegister(platform, transactionInfo);
    }

    private void keepaliveExpire(String platformServerId, int failCount) {
        Platform platform = queryPlatformByServerGBId(platformServerId);
        if (platform == null || !platform.isEnable()) {
            log.info("[National standard cascade] Heartbeat expiration, upper level platform number： {}, Platform does not exist or is not enabled, ignore", platformServerId);
            return;
        }
        try {
            commanderForPlatform.keepalive(platform, eventResult -> {
                // Heartbeat failed
                if (eventResult.type != SipSubscribe.EventResultType.timeout) {
                    log.warn("[National standard cascade] Sending heartbeat received error，code： {}, msg: {}", eventResult.statusCode, eventResult.msg);
                }

                // Heartbeat timeout failed
                if (failCount < 2) {
                    log.info("[National standard cascade] Heartbeat sending timeout, platform service number： {}", platformServerId);
                    PlatformKeepaliveTask keepaliveTask = new PlatformKeepaliveTask(platform.getServerGBId(), platform.getKeepTimeout() * 1000L,
                            this::keepaliveExpire);
                    keepaliveTask.setFailCount(failCount + 1);
                    statusTaskRunner.addKeepAliveTask(keepaliveTask);
                }else {
                    // The heartbeat timed out three times, no more heartbeats were sent, and the platform was offline.
                    log.info("[National standard cascade] Heartbeat sending timed out three times, the platform is offline, platform service number： {}", platformServerId);
                    offline(platform);
                }
            }, eventResult -> {
                PlatformKeepaliveTask keepaliveTask = new PlatformKeepaliveTask(platform.getServerGBId(), platform.getKeepTimeout() * 1000L,
                        this::keepaliveExpire);
                statusTaskRunner.addKeepAliveTask(keepaliveTask);
            });
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[Command sending failed] National standard cascade sending heartbeat: {}", e.getMessage());
            if (failCount < 2) {
                PlatformKeepaliveTask keepaliveTask = new PlatformKeepaliveTask(platform.getServerGBId(), platform.getKeepTimeout() * 1000L,
                        this::keepaliveExpire);
                keepaliveTask.setFailCount(failCount + 1);
                statusTaskRunner.addKeepAliveTask(keepaliveTask);
            }else {
                // The heartbeat timed out three times, no more heartbeats were sent, and the platform was offline.
                log.info("[National standard cascade] Heartbeat sending failed three times, the platform is offline, platform service number： {}", platformServerId);
                offline(platform);
            }
        }
    }

    @Override
    public void addSimulatedSubscribeInfo(Platform platform) {
        // Automatically add a simulated subscription message
        subscribeHolder.putCatalogSubscribe(platform.getServerGBId(),
                SubscribeInfo.buildSimulated(platform.getServerGBId(), platform.getServerIp()));
    }

    @Override
    public void offline(Platform platform) {
        log.info("[Platform offline]：{}({})", platform.getName(), platform.getServerGBId());
        statusTaskRunner.removeRegisterTask(platform.getServerGBId());
        statusTaskRunner.removeKeepAliveTask(platform.getServerGBId());

        subscribeHolder.removeCatalogSubscribe(platform.getServerGBId());
        subscribeHolder.removeMobilePositionSubscribe(platform.getServerGBId());

        platformMapper.updateStatus(platform.getId(), false, userSetting.getServerId());

        // Stop all streams
        log.info("[Platform offline] {}({}), Stop all streams", platform.getName(),  platform.getServerGBId());
        stopAllPush(platform.getServerGBId());
    }

    private void stopAllPush(String platformId) {
        List<SendRtpInfo> sendRtpItems = sendRtpServerService.queryForPlatform(platformId);
        if (sendRtpItems != null && !sendRtpItems.isEmpty()) {
            for (SendRtpInfo sendRtpItem : sendRtpItems) {
                sendRtpServerService.delete(sendRtpItem);
                MediaServer mediaInfo = mediaServerService.getOne(sendRtpItem.getMediaServerId());
                mediaServerService.stopSendRtp(mediaInfo, sendRtpItem.getApp(), sendRtpItem.getStream(), null);
            }
        }
    }

    @Override
    public void sendNotifyMobilePosition(String platformId) {
        Platform platform = platformMapper.getParentPlatByServerGBId(platformId);
        if (platform == null) {
            return;
        }
        SubscribeInfo subscribe = subscribeHolder.getMobilePositionSubscribe(platform.getServerGBId());
        if (subscribe != null) {

            List<CommonGBChannel> channelList = platformChannelMapper.queryShare(platform.getId(), null);
            if (channelList.isEmpty()) {
                return;
            }
            for (CommonGBChannel channel : channelList) {
                GPSMsgInfo gpsMsgInfo = redisCatchStorage.getGpsMsgInfo(channel.getGbDeviceId());
                // If there is no latest location, the current location will be sent.
                if (gpsMsgInfo != null && (gpsMsgInfo.getLng() == 0 && gpsMsgInfo.getLat() == 0)) {
                    gpsMsgInfo = null;
                }

                if (gpsMsgInfo == null && !userSetting.isSendPositionOnDemand()){
                    gpsMsgInfo = new GPSMsgInfo();
                    gpsMsgInfo.setId(channel.getGbDeviceId());
                    gpsMsgInfo.setLng(channel.getGbLongitude());
                    gpsMsgInfo.setLat(channel.getGbLatitude());
                    gpsMsgInfo.setAltitude(channel.getGpsAltitude());
                    gpsMsgInfo.setSpeed(channel.getGpsSpeed());
                    gpsMsgInfo.setDirection(channel.getGpsDirection());
                    gpsMsgInfo.setTime(channel.getGpsTime());
                }

                // Do not send if there is no latest location
                if (gpsMsgInfo != null) {
                    // Send GPS message
                    try {
                        commanderForPlatform.sendNotifyMobilePosition(platform, gpsMsgInfo, channel, subscribe);
                    } catch (InvalidArgumentException | ParseException | NoSuchFieldException | SipException |
                             IllegalAccessException e) {
                        log.error("[Command sending failed] National standard cascade mobile location notification: {}", e.getMessage());
                    }
                }
            }
        }
    }

    @Override
    public void broadcastInvite(Platform platform, CommonGBChannel channel, String sourceId, MediaServer mediaServerItem, HookSubscribe.Event hookEvent,
                                SipSubscribe.Event errorEvent, InviteTimeOutCallback timeoutCallback) throws InvalidArgumentException, ParseException, SipException {

        if (mediaServerItem == null) {
            log.info("[National standard cascade] Voice call not found availablezlm. platform: {}", platform.getServerGBId());
            return;
        }
        InviteInfo inviteInfoForOld = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.BROADCAST, channel.getGbId());

        if (inviteInfoForOld != null && inviteInfoForOld.getStreamInfo() != null) {
            // If this stream does not exist in zlm, just delete the data
            MediaServer mediaServerItemForStreamInfo = mediaServerService.getOne(inviteInfoForOld.getStreamInfo().getMediaServer().getId());
            if (mediaServerItemForStreamInfo != null) {
                Boolean ready = mediaServerService.isStreamReady(mediaServerItemForStreamInfo, inviteInfoForOld.getStreamInfo().getApp(), inviteInfoForOld.getStreamInfo().getStream());
                if (!ready) {
                    // Error exists in data in redis
                    inviteStreamService.removeInviteInfo(inviteInfoForOld);
                }else {
                    // The stream is indeed still being pushed, and the result is directly called back.
                    HookData hookData = new HookData();
                    hookData.setApp(inviteInfoForOld.getStreamInfo().getApp());
                    hookData.setStream(inviteInfoForOld.getStreamInfo().getStream());
                    hookData.setMediaServer(mediaServerItemForStreamInfo);
                    hookEvent.response(hookData);
                    return;
                }
            }
        }

        SSRCInfo ssrcInfo = receiveRtpServerService.openGbRTPServerForBroadcast(mediaServerItem, platform, channel, ((code, msg, data) -> {
                    if (code == InviteErrorCode.SUCCESS.getCode() && data != null && data.getHookData() != null) {
                        log.info("[National standard cascade] Initiate a voice call and receive a push from superiors deviceId: {}, channelId: {}", platform.getServerGBId(), channel.getGbDeviceId());
                        HookData hookData = data.getHookData();
                        // hookresponse
                        onPublishHandlerForBroadcast(hookData.getMediaServer(), hookData.getMediaInfo(), platform, channel);
                        // receive stream
                        if (hookEvent != null) {
                            hookEvent.response(hookData);
                        }
                    }else {
                        InviteInfo inviteInfoForBroadcast = inviteStreamService.getInviteInfo(InviteSessionType.BROADCAST, channel.getGbId(), null);
                        if (inviteInfoForBroadcast == null) {
                            log.info("[National standard cascade] Initiating a voice call. Streaming timeout. deviceId: {}, channelId: {}", platform.getServerGBId(), channel.getGbDeviceId());
                            // On-demand timeout reply BYE and release ssrc and the resources of this on-demand broadcast.
                            try {
                                commanderForPlatform.streamByeCmd(platform, channel, data.getSsrcInfo().getApp(), data.getSsrcInfo().getStream(), null, null);
                            } catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException e) {
                                log.error("[On-demand timeout]， Sending BYE failed {}", e.getMessage());
                            } finally {
                                timeoutCallback.run(1, "Traffic collection timeout");
                                receiveRtpServerService.closeRTPServer(mediaServerItem, data.getSsrcInfo().getApp(), data.getSsrcInfo().getStream());
                                sessionManager.removeByStream(data.getSsrcInfo().getApp(), data.getSsrcInfo().getStream());
                            }
                        }
                    }
                }));
        if (ssrcInfo == null || ssrcInfo.getPort() < 0) {
            log.info("[National standard cascade] Initiating a voice call. Failed to enable port monitoring.， platform: {}, channel： {}", platform.getServerGBId(), channel.getGbDeviceId());
            SipSubscribe.EventResult<Object> eventResult = new SipSubscribe.EventResult<>();
            eventResult.statusCode = -1;
            eventResult.msg = "Port listening failed";
            eventResult.type = SipSubscribe.EventResultType.failedToGetPort;
            errorEvent.response(eventResult);
            return;
        }
        log.info("[National standard cascade] Make voice calls and initiate Invite messages deviceId: {}, channelId: {},Flow collection port： {}, Flow collection mode：{}, SSRC: {}, SSRCVerification：{}",
                platform.getServerGBId(), channel.getGbDeviceId(), ssrcInfo.getPort(), userSetting.getBroadcastForPlatform(), ssrcInfo.getSsrc(), false);

        // Initialize the invite message status in redis
        InviteInfo inviteInfo = InviteInfo.getInviteInfo(platform.getServerGBId(), channel.getGbId(), ssrcInfo.getStream(), ssrcInfo, mediaServerItem.getId(),
                mediaServerItem.getSdpIp(), ssrcInfo.getPort(), userSetting.getBroadcastForPlatform(), InviteSessionType.BROADCAST,
                InviteSessionStatus.ready);
        inviteStreamService.updateInviteInfo(inviteInfo);
        commanderForPlatform.broadcastInviteCmd(platform, channel,sourceId, mediaServerItem, ssrcInfo, event -> {
            inviteOKHandler(event, ssrcInfo, false, mediaServerItem, platform, channel,
                    null, inviteInfo, InviteSessionType.BROADCAST);
        }, eventResult -> {
            // Received error reply
            if (errorEvent != null) {
                errorEvent.response(eventResult);
            }
            inviteStreamService.removeInviteInfo(inviteInfo);
        });
    }

    public void onPublishHandlerForBroadcast(MediaServer mediaServerItem, MediaInfo mediaInfo, Platform platform, CommonGBChannel channel) {
        StreamInfo streamInfo = mediaServerService.getStreamInfoByAppAndStream(mediaServerItem, mediaInfo.getApp(), mediaInfo.getStream(), mediaInfo, null);
        streamInfo.setChannelId(channel.getGbId());

        InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.BROADCAST, channel.getGbId());
        if (inviteInfo != null) {
            inviteInfo.setStatus(InviteSessionStatus.ok);
            inviteInfo.setStreamInfo(streamInfo);
            inviteStreamService.updateInviteInfo(inviteInfo);
        }
    }

    private void inviteOKHandler(SipSubscribe.EventResult eventResult, SSRCInfo ssrcInfo, boolean ssrcCheck, MediaServer mediaServerItem,
                                 Platform platform, CommonGBChannel channel, ErrorCallback<Object> callback,
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
            if (userSetting.getBroadcastForPlatform().equalsIgnoreCase("TCP-ACTIVE")) {
                if (mediaServerItem.isRtpEnable()) {
                    tcpActiveHandler(platform, channel, contentString, mediaServerItem, ssrcCheck,
                            ssrcInfo, callback);
                }else {
                    log.warn("[Invite 200OK] The single-port traffic collection mode does not support TCP active mode traffic collection.");
                }
            }
        }else {
            log.info("[Invite 200OK] Received invite 200 and found that the subordinate has customized itssrc: {}", ssrcInResponse);
            // ssrc inconsistent
            if (mediaServerItem.isRtpEnable()) {
                // multi-port
                if (ssrcCheck) {
                    // ssrcInspect
                    // updatessrc
                    log.info("[Invite 200OK] SSRCCorrection {}->{}", ssrcInfo.getSsrc(), ssrcInResponse);
                    Boolean result = mediaServerService.updateRtpServerSSRC(mediaServerItem, ssrcInfo.getApp(), ssrcInfo.getStream(), ssrcInResponse);
                    if (!result) {
                        try {
                            log.warn("[Invite 200OK] Failed to update ssrc, stop shouting {}/{}", platform.getServerGBId(), channel.getGbDeviceId());
                            commanderForPlatform.streamByeCmd(platform, channel, ssrcInfo.getApp(), ssrcInfo.getStream(), null, null);
                        } catch (InvalidArgumentException | SipException | ParseException | SsrcTransactionNotFoundException e) {
                            log.error("[Command sending failed] Stop playing, sendBYE: {}", e.getMessage());
                        } finally {
                            receiveRtpServerService.closeRTPServer(mediaServerItem, ssrcInfo.getApp(), ssrcInfo.getStream());
                            sessionManager.removeByStream(ssrcInfo.getApp(), ssrcInfo.getStream());

                            callback.run(InviteErrorCode.ERROR_FOR_RESET_SSRC.getCode(),
                                    "The subordinate customized ssrc and failed to reset the traffic collection information.", null);
                            inviteStreamService.call(inviteSessionType, channel.getGbId(), null,
                                    InviteErrorCode.ERROR_FOR_RESET_SSRC.getCode(),
                                    "The subordinate customized ssrc and failed to reset the traffic collection information.", null);
                            inviteStreamService.removeInviteInfo(inviteInfo);
                        }
                    }else {
                        ssrcInfo.setSsrc(ssrcInResponse);
                        inviteInfo.setSsrcInfo(ssrcInfo);
                        inviteInfo.setStream(ssrcInfo.getStream());
                        if (userSetting.getBroadcastForPlatform().equalsIgnoreCase("TCP-ACTIVE")) {
                            if (mediaServerItem.isRtpEnable()) {
                                tcpActiveHandler(platform, channel, contentString, mediaServerItem, ssrcCheck,
                                        ssrcInfo, callback);
                            }else {
                                log.warn("[Invite 200OK] The single-port traffic collection mode does not support TCP active mode traffic collection.");
                            }
                        }
                        inviteStreamService.updateInviteInfo(inviteInfo);
                    }
                }else {
                    ssrcInfo.setSsrc(ssrcInResponse);
                    inviteInfo.setSsrcInfo(ssrcInfo);
                    inviteInfo.setStream(ssrcInfo.getStream());
                    if (userSetting.getBroadcastForPlatform().equalsIgnoreCase("TCP-ACTIVE")) {
                        if (mediaServerItem.isRtpEnable()) {
                            tcpActiveHandler(platform, channel, contentString, mediaServerItem, ssrcCheck,
                                    ssrcInfo, callback);
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
                    SsrcTransaction ssrcTransaction = sessionManager.getSsrcTransactionByStream(ssrcInfo.getApp(), inviteInfo.getStream());
                    if (ssrcTransaction == null) {
                        return;
                    }
                    sessionManager.removeByStream(ssrcInfo.getApp(), inviteInfo.getStream());
                    inviteStreamService.updateInviteInfoForSSRC(inviteInfo, ssrcInResponse);

                    ssrcTransaction.setPlatformId(platform.getServerGBId());
                    ssrcTransaction.setChannelId(channel.getGbId());
                    ssrcTransaction.setApp(ssrcInfo.getApp());
                    ssrcTransaction.setStream(inviteInfo.getStream());
                    ssrcTransaction.setSsrc(ssrcInResponse);
                    ssrcTransaction.setMediaServerId(mediaServerItem.getId());
                    ssrcTransaction.setSipTransactionInfo(new SipTransactionInfo((SIPResponse) responseEvent.getResponse()));
                    ssrcTransaction.setType(inviteSessionType);

                    sessionManager.put(ssrcTransaction);
                }
            }
        }
    }

    private void tcpActiveHandler(Platform platform, CommonGBChannel channel, String contentString,
                                  MediaServer mediaServerItem, boolean ssrcCheck,
                                  SSRCInfo ssrcInfo, ErrorCallback<Object> callback){
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
                if (mediaFormats.contains("8") || mediaFormats.contains("0")) {
                    port = media.getMediaPort();
                    break;
                }
            }
            log.info("[TCPActively connect to the other party] serverGbId: {}, channelId: {}, The address of the connecting party：{}:{}, SSRC: {}, SSRCVerification：{}",
                    platform.getServerGBId(), channel.getGbDeviceId(), sdp.getConnection().getAddress(), port, ssrcInfo.getSsrc(), ssrcCheck);
            Boolean result = mediaServerService.connectRtpServer(mediaServerItem, sdp.getConnection().getAddress(), port, ssrcInfo.getApp(), ssrcInfo.getStream());
            log.info("[TCPActively connect to the other party] result： {}", result);
        } catch (SdpException e) {
            log.error("[TCPActively connect to the other party] serverGbId: {}, channelId: {}, Failed to parse SDP information of 200OK", platform.getServerGBId(), channel.getGbDeviceId(), e);
            receiveRtpServerService.closeRTPServer(mediaServerItem, ssrcInfo.getApp(), ssrcInfo.getStream());
            sessionManager.removeByStream(ssrcInfo.getApp(), ssrcInfo.getStream());

            callback.run(InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getCode(),
                    InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getMsg(), null);
            inviteStreamService.call(InviteSessionType.PLAY, channel.getGbId(), null,
                    InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getCode(),
                    InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getMsg(), null);
        }
    }

    @Override
    public void stopBroadcast(Platform platform, CommonGBChannel channel, String app, String stream, boolean sendBye, MediaServer mediaServerItem) {

        try {
            if (sendBye) {
                commanderForPlatform.streamByeCmd(platform, channel, app, stream, null, null);
            }
        } catch (InvalidArgumentException | SipException | ParseException | SsrcTransactionNotFoundException e) {
            log.warn("[Message sending failed] Stop voice intercom, platform：{}，channel：{}", platform.getId(), channel.getGbDeviceId() );
        } finally {
            receiveRtpServerService.closeRTPServer(mediaServerItem, app, stream);
            InviteInfo inviteInfo = inviteStreamService.getInviteInfo(null, channel.getGbId(), stream);
            if (inviteInfo != null) {
                inviteStreamService.removeInviteInfo(inviteInfo);
            }
            sessionManager.removeByStream(app, stream);
        }
    }

    @Override
    public Platform queryOne(Integer platformId) {
        return platformMapper.query(platformId);
    }

    @Override
    public List<Platform> queryEnablePlatformList(String serverId) {
        return platformMapper.queryEnableParentPlatformListByServerId(serverId,true);
    }

    @Override
    @Transactional
    public boolean delete(Integer platformId) {
        Platform platform = platformMapper.query(platformId);
        Assert.notNull(platform, "Platform does not exist");
        log.info("[Delete platform] {}/{} {}:{}", platform.getName(), platform.getServerGBId(), platform.getServerIp(), platform.getServerPort());
        if (!userSetting.getServerId().equals(platform.getServerId())) {
            boolean result = redisRpcService.deletePlatform(platform.getServerId(), platformId);
            if (result) {
                log.info("[Delete platform] Cross-platform deletion successful {}/{}", platform.getName(), platform.getServerGBId());
            }else {
                log.info("[Delete platform] Cross-platform deletion failed {}/{}", platform.getName(), platform.getServerGBId());
            }
            return result;
        }
        try {
            if (statusTaskRunner.containsRegister(platform.getServerGBId())) {
                try {
                    SipTransactionInfo transactionInfo = statusTaskRunner.getRegisterTransactionInfo(platform.getServerGBId());
                    sendUnRegister(platform, transactionInfo);
                }catch (Exception ignored) {}
            }
            platformMapper.delete(platform.getId());

            statusTaskRunner.removeRegisterTask(platform.getServerGBId());
            statusTaskRunner.removeKeepAliveTask(platform.getServerGBId());

            subscribeHolder.removeCatalogSubscribe(platform.getServerGBId());
            subscribeHolder.removeMobilePositionSubscribe(platform.getServerGBId());
        }catch (Exception e) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), e.getMessage());
        }

        return true;
    }

    @Override
    public List<Platform> queryAll(String serverId) {
        return platformMapper.queryByServerId(serverId);
    }
}
