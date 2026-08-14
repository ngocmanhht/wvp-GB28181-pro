package com.genersoft.iot.vmp.gb28181.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.dao.CommonGBChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.DeviceChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.DeviceMapper;
import com.genersoft.iot.vmp.gb28181.dao.PlatformChannelMapper;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.channel.ChannelEvent;
import com.genersoft.iot.vmp.gb28181.event.device.DeviceOfflineEvent;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.service.IInviteStreamService;
import com.genersoft.iot.vmp.gb28181.session.AudioBroadcastManager;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.gb28181.task.deviceStatus.DeviceStatusManager;
import com.genersoft.iot.vmp.gb28181.task.deviceSubscribe.SubscribeTask;
import com.genersoft.iot.vmp.gb28181.task.deviceSubscribe.SubscribeTaskInfo;
import com.genersoft.iot.vmp.gb28181.task.deviceSubscribe.SubscribeTaskRunner;
import com.genersoft.iot.vmp.gb28181.task.deviceSubscribe.impl.SubscribeTaskForAlarm;
import com.genersoft.iot.vmp.gb28181.task.deviceSubscribe.impl.SubscribeTaskForCatalog;
import com.genersoft.iot.vmp.gb28181.task.deviceSubscribe.impl.SubscribeTaskForMobilPosition;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.cmd.CatalogResponseMessageHandler;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IReceiveRtpServerService;
import com.genersoft.iot.vmp.service.ISendRtpServerService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.redisMsg.IRedisRpcService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import gov.nist.javax.sip.message.SIPResponse;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

/**
 * Equipment business (catalog subscription）
 */
@Slf4j
@Service
public class DeviceServiceImpl implements IDeviceService {

    @Autowired
    private ISIPCommander sipCommander;

    @Autowired
    private CatalogResponseMessageHandler catalogResponseMessageHandler;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IInviteStreamService inviteStreamService;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private PlatformChannelMapper platformChannelMapper;

    @Autowired
    private DeviceChannelMapper deviceChannelMapper;

    @Autowired
    private CommonGBChannelMapper commonGBChannelMapper;

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private ISendRtpServerService sendRtpServerService;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private ISIPCommander commander;

    @Autowired
    private SipInviteSessionManager sessionManager;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private IReceiveRtpServerService receiveRtpServerService;

    @Autowired
    private AudioBroadcastManager audioBroadcastManager;

    @Autowired
    private IRedisRpcService redisRpcService;

    @Autowired
    private SubscribeTaskRunner subscribeTaskRunner;

    @Autowired
    private DeviceStatusManager deviceStatusManager;

    private Device getDeviceByDeviceIdFromDb(String deviceId) {
        return deviceMapper.getDeviceByDeviceId(deviceId);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(){

        // Clean data that does not exist in the database but exists in redis
        List<Device> devicesInDb = getAll();
        if (devicesInDb.isEmpty()) {
            redisCatchStorage.removeAllDevice();
            deviceStatusManager.clear();
        }else {
            List<Device> devicesInRedis = redisCatchStorage.getAllDevices();
            if (!devicesInRedis.isEmpty()) {
                Map<String, Device> deviceMapInDb = new HashMap<>();
                devicesInDb.parallelStream().forEach(device -> {
                    deviceMapInDb.put(device.getDeviceId(), device);
                });
                devicesInRedis.parallelStream().forEach(device -> {
                    if (deviceMapInDb.get(device.getDeviceId()) == null
                            && userSetting.getServerId().equals(device.getServerId())) {
                        redisCatchStorage.removeDevice(device.getDeviceId());
                    }
                });
            }
        }

        // Reset cseq count
        redisCatchStorage.resetAllCSEQ();
        // Handle device status
        dbStatusCheck();
    }

    /**
     *  Database status check, every 6 hours
     */
    @Scheduled(fixedDelay = 6, initialDelay = 6, timeUnit = TimeUnit.HOURS)
    public void dbStatusCheck(){
        // Handle device status
        Set<String> allDeviceIds = deviceStatusManager.getAll();
        if (allDeviceIds != null && !allDeviceIds.isEmpty()) {
            // Except for the recorded device, all other devices are offline
            List<Device> onlineDevice = getAllOnlineDevice(userSetting.getServerId());
            if (onlineDevice != null && !onlineDevice.isEmpty()) {
                List<Device> offlineDevices = new ArrayList<>();
                for (Device device : onlineDevice) {
                    if (!allDeviceIds.contains(device.getDeviceId())) {
                        // This device needs to be offline
                        device.setOnLine(false);
                        // Clear cache related to offline devices
                        cleanOfflineDevice(device);
                        // Update database
                        offlineDevices.add(device);
                    }
                }
                if (!offlineDevices.isEmpty()) {
                    offlineByIds(offlineDevices);
                }
            }
        }else {
            // All devices are offline
            List<Device> onlineDevice = getAllOnlineDevice(userSetting.getServerId());
            if (onlineDevice != null) {
                for (Device device : onlineDevice) {
                    // This device needs to be offline
                    device.setOnLine(false);
                    // Clear cache related to offline devices
                    cleanOfflineDevice(device);
                }
                offlineByIds(onlineDevice);
            }
        }

        // Handle subscription tasks
        List<SubscribeTaskInfo> taskInfoList = subscribeTaskRunner.getAllTaskInfo();
        if (!taskInfoList.isEmpty()) {
            for (SubscribeTaskInfo taskInfo : taskInfoList) {
                if (taskInfo == null) {
                    continue;
                }
                Device device = getDeviceByDeviceId(taskInfo.getDeviceId());
                if (device == null || !device.isOnLine() || !allDeviceIds.contains(taskInfo.getDeviceId())) {
                    subscribeTaskRunner.removeSubscribe(taskInfo.getKey());
                    continue;
                }
                if (SubscribeTaskForCatalog.name.equals(taskInfo.getName())) {
                    device.setSubscribeCycleForCatalog((int)taskInfo.getExpireTime());
                    SubscribeTask subscribeTask = SubscribeTaskForCatalog.getInstance(device, this::catalogSubscribeExpire, taskInfo.getTransactionInfo());
                    if (subscribeTask != null) {
                        subscribeTaskRunner.addSubscribe(subscribeTask);
                    }
                }else if (SubscribeTaskForMobilPosition.name.equals(taskInfo.getName())) {
                    device.setSubscribeCycleForMobilePosition((int)taskInfo.getExpireTime());
                    SubscribeTask subscribeTask = SubscribeTaskForMobilPosition.getInstance(device, this::mobilPositionSubscribeExpire, taskInfo.getTransactionInfo());
                    if (subscribeTask != null) {
                        subscribeTaskRunner.addSubscribe(subscribeTask);
                    }
                }else if (SubscribeTaskForAlarm.name.equals(taskInfo.getName())) {
                    device.setSubscribeCycleForAlarm((int)taskInfo.getExpireTime());
                    SubscribeTask subscribeTask = SubscribeTaskForAlarm.getInstance(device, this::mobilPositionSubscribeExpire, taskInfo.getTransactionInfo());
                    if (subscribeTask != null) {
                        subscribeTaskRunner.addSubscribe(subscribeTask);
                    }
                }
            }
        }

    }

    private void offlineByIds(List<Device> offlineDevices) {
        if (offlineDevices.isEmpty()) {
            log.info("[Update multiple offline device information] Parameter is empty");
            return;
        }
        int limitCount = 300;
        for (int i = 0; i < offlineDevices.size(); i += limitCount) {
            int endIndex = Math.min(i + limitCount, offlineDevices.size());
            List<Device> subList = offlineDevices.subList(i, endIndex);
            deviceMapper.offlineByList(subList);
        }

        for (Device device : offlineDevices) {
            device.setOnLine(false);
            redisCatchStorage.updateDevice(device);
            deviceStatusManager.remove(device.getDeviceId());
        }
    }

    private void cleanOfflineDevice(Device device) {
        if (subscribeTaskRunner.containsKey(SubscribeTaskForCatalog.getKey(device))) {
            subscribeTaskRunner.removeSubscribe(SubscribeTaskForCatalog.getKey(device));
        }
        if (subscribeTaskRunner.containsKey(SubscribeTaskForMobilPosition.getKey(device))) {
            subscribeTaskRunner.removeSubscribe(SubscribeTaskForMobilPosition.getKey(device));
        }
        // Release all offline ssrc
        if (subscribeTaskRunner.containsKey(SubscribeTaskForAlarm.getKey(device))) {
            subscribeTaskRunner.removeSubscribe(SubscribeTaskForAlarm.getKey(device));
        }
        deviceStatusManager.remove(device.getDeviceId());
        // Release all offlinessrc
        List<SsrcTransaction> ssrcTransactions = sessionManager.getSsrcTransactionByDeviceId(device.getDeviceId());
        if (ssrcTransactions != null && !ssrcTransactions.isEmpty()) {
            for (SsrcTransaction ssrcTransaction : ssrcTransactions) {
                receiveRtpServerService.closeRTPServerByMediaServerId(ssrcTransaction.getMediaServerId(), ssrcTransaction.getApp(), ssrcTransaction.getStream());
                sessionManager.removeByCallId(ssrcTransaction.getCallId());
            }
        }
        // Remove subscription
        removeCatalogSubscribe(device, null);
        removeMobilePositionSubscribe(device, null);

        List<AudioBroadcastCatch> audioBroadcastCatches = audioBroadcastManager.getByDeviceId(device.getDeviceId());
        if (!audioBroadcastCatches.isEmpty()) {
            for (AudioBroadcastCatch audioBroadcastCatch : audioBroadcastCatches) {

                SendRtpInfo sendRtpItem = sendRtpServerService.queryByChannelId(audioBroadcastCatch.getChannelId(), device.getDeviceId());
                if (sendRtpItem != null) {
                    sendRtpServerService.delete(sendRtpItem);
                    MediaServer mediaInfo = mediaServerService.getOne(sendRtpItem.getMediaServerId());
                    mediaServerService.stopSendRtp(mediaInfo, sendRtpItem.getApp(), sendRtpItem.getStream(), null);
                }

                audioBroadcastManager.del(audioBroadcastCatch.getChannelId());
            }
        }
    }

    // Listen for device expiration events
    @Async
    @EventListener
    public void onApplicationEvent(DeviceOfflineEvent event) {
        log.info("[Device status] Expiration, number： {}", event.getDeviceIds().toString());
        List<Device> deviceList = redisCatchStorage.getDeviceList(event.getDeviceIds());
        offline(deviceList);
    }

    @Override
    public void online(Device device) {
        log.info("[Device online] deviceId：{}->{}:{}", device.getDeviceId(), device.getIp(), device.getPort());
        Device deviceInRedis = redisCatchStorage.getDevice(device.getDeviceId());
        Device deviceInDb = getDeviceByDeviceIdFromDb(device.getDeviceId());

        String now = DateUtil.getNow();
        if (deviceInRedis != null && deviceInDb == null) {
            // redis Dirty data exists
            inviteStreamService.clearInviteInfo(device.getDeviceId());
        }
        device.setUpdateTime(now);
        if (device.getHeartBeatCount() == null) {
            // Read the device configuration, obtain the heartbeat interval and the number of heartbeat timeouts, and temporarily set them to default values before this time.
            device.setHeartBeatCount(3);
            device.setHeartBeatInterval(60);
            device.setPositionCapability(0);
        }

        // Going online for the first time or the device was offline before--Perform channel synchronization and device information query
        if (deviceInDb == null) {
            device.setOnLine(true);
            device.setCreateTime(now);
            device.setUpdateTime(now);
            log.info("[The device goes online and registers for the first time]: {}，Query device information and channel information", device.getDeviceId());
            if(device.getStreamMode() == null) {
                device.setStreamMode("TCP-PASSIVE");
            }
            deviceMapper.add(device);
            redisCatchStorage.updateDevice(device);
            try {
                commander.deviceInfoQuery(device, null);
                commander.deviceConfigQuery(device, null, BasicParam.class, null);
            } catch (InvalidArgumentException | SipException | ParseException e) {
                log.error("[Command sending failed] Query device information: {}", e.getMessage());
            }
            // Add subscription online
            if (userSetting.isSubscribeMobilePosition() && isDevice(device.getDeviceId())) {
                // Open subscription
                device.setSubscribeCycleForMobilePosition(60);
                device.setMobilePositionSubmissionInterval(5);
                addMobilePositionSubscribe(device, null);
            }

            sync(device);
        }else {
            device.setServerId(userSetting.getServerId());
            if(!deviceInDb.isOnLine()){
                device.setOnLine(true);
                device.setCreateTime(now);
                deviceMapper.update(device);
                redisCatchStorage.updateDevice(device);
                if (userSetting.getSyncChannelOnDeviceOnline()) {
                    log.info("[The device goes online and re-registers when offline.]: {}，Query device information and channel information", device.getDeviceId());
                    try {
                        commander.deviceInfoQuery(device, null);
                    } catch (InvalidArgumentException | SipException | ParseException e) {
                        log.error("[Command sending failed] Query device information: {}", e.getMessage());
                    }
                    sync(device);
                }else {
                    if (isDevice(device.getDeviceId())) {
                        sync(device);
                    }
                }
                // Add subscription online
                if (device.getSubscribeCycleForCatalog() > 0 && !subscribeTaskRunner.containsKey(SubscribeTaskForCatalog.getKey(device))) {
                    // Query which online devices have enabled subscriptions and enable scheduled directory subscriptions for the devices
                    addCatalogSubscribe(device, null);
                }
                if (device.getSubscribeCycleForMobilePosition() > 0 && !subscribeTaskRunner.containsKey(SubscribeTaskForMobilPosition.getKey(device))) {
                    addMobilePositionSubscribe(device, null);
                }else{
                    if (userSetting.isSubscribeMobilePosition() && isDevice(device.getDeviceId())) {
                        // Open subscription
                        device.setSubscribeCycleForMobilePosition(60);
                        device.setMobilePositionSubmissionInterval(5);
                        addMobilePositionSubscribe(device, null);
                    }
                }
                if (device.getSubscribeCycleForAlarm() > 0 && !subscribeTaskRunner.containsKey(SubscribeTaskForAlarm.getKey(device))) {
                    addAlarmSubscribe(device, null);
                }

                if (userSetting.getDeviceStatusNotify()) {
                    // Send redis message
                    redisCatchStorage.sendDeviceOrChannelStatus(device.getDeviceId(), null, true);
                }
            }else {
                deviceMapper.update(device);
                redisCatchStorage.updateDevice(device);
            }
            if (deviceChannelMapper.queryChannelsByDeviceDbId(device.getId()).isEmpty()) {
                log.info("[Device online]: {}，The number of channels is 0, query channel information", device.getDeviceId());
                sync(device);
            }
        }

        // Add device status task
        long expiresTime = Math.min(device.getExpires(), device.getHeartBeatInterval() * device.getHeartBeatCount()) * 1000L;
        deviceStatusManager.add(device.getDeviceId(), expiresTime + System.currentTimeMillis());
    }

    @Override
    public void offline(Device device) {
        if (device == null) {
            log.warn("[Device does not exist]");
            return;
        }
        String deviceId = device.getDeviceId();
        log.info("[Device offline] device：{}， heartbeat interval： {}，Number of heartbeat timeouts： {}", deviceId, device.getHeartBeatInterval(), device.getHeartBeatCount());
        device.setOnLine(false);
        cleanOfflineDevice(device);
        redisCatchStorage.updateDevice(device);
        deviceMapper.update(device);
        if (userSetting.getDeviceStatusNotify()) {
            // Send redis message
            redisCatchStorage.sendDeviceOrChannelStatus(device.getDeviceId(), null, false);
        }
        if (isDevice(deviceId)) {
            channelOfflineByDevice(List.of(device));
        }
    }

    public void offline(List<Device> deviceList) {
        if (deviceList == null  || deviceList.isEmpty()) {
            log.warn("[Device does not exist]");
            return;
        }
        List<Device> realDeviceList = new ArrayList<>();
        for (Device device : deviceList) {
            if (device == null) {
                continue;
            }
            log.info("[Device offline] device：{}， heartbeat interval： {}，Number of heartbeat timeouts： {}", device.getDeviceId(), device.getHeartBeatInterval(), device.getHeartBeatCount());
            device.setOnLine(false);
            cleanOfflineDevice(device);
            if (isDevice(device.getDeviceId())) {
                realDeviceList.add(device);
            }
            redisCatchStorage.updateDevice(device);
            if (userSetting.getDeviceStatusNotify()) {
                // Send redis message
                redisCatchStorage.sendDeviceOrChannelStatus(device.getDeviceId(), null, false);
            }
        }
        deviceMapper.offlineByList(deviceList);

        if (!realDeviceList.isEmpty()) {
            channelOfflineByDevice(realDeviceList);
        }
    }

    private void channelOfflineByDevice(List<Device> deviceList) {
        // Taking the channel offline
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryOnlineListsByGbDeviceIds(deviceList);
        if (channelList.isEmpty()) {
            return;
        }
        deviceChannelMapper.offlineByDeviceIds(deviceList);
        // Send channel offline notification
        eventPublisher.channelEventPublish(channelList, ChannelEvent.ChannelEventMessageType.OFF);
    }

    private boolean isDevice(String deviceId) {
        GbCode decode = GbCode.decode(deviceId);
        if (decode == null) {
            return true;
        }
        int code = Integer.parseInt(decode.getTypeCode());
        return code <= 199;
    }

    // Subscription lost check
    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS)
    public void lostCheckForSubscribe(){
        // Get all devices
        List<Device> deviceList = redisCatchStorage.getAllDevices();
        if (deviceList == null || deviceList.isEmpty()) {
            return;
        }
        for (Device device : deviceList) {
            if (device == null || !device.isOnLine() || !userSetting.getServerId().equals(device.getServerId())) {
                continue;
            }
            if (device.getSubscribeCycleForCatalog() > 0 && !subscribeTaskRunner.containsKey(SubscribeTaskForCatalog.getKey(device))) {
                log.debug("[Subscription lost] Catalog Subscription, No.： {}, Restart subscription", device.getDeviceId());
                addCatalogSubscribe(device, null);
            }
            if (device.getSubscribeCycleForMobilePosition() > 0 && !subscribeTaskRunner.containsKey(SubscribeTaskForMobilPosition.getKey(device))) {
                log.debug("[Subscription lost] Mobile location subscription, no.： {}, Restart subscription", device.getDeviceId());
                addMobilePositionSubscribe(device, null);
            }
            if (device.getSubscribeCycleForAlarm() > 0 && !subscribeTaskRunner.containsKey(SubscribeTaskForAlarm.getKey(device))) {
                log.debug("[Subscription lost] Alarm subscription, number： {}, Restart subscription", device.getDeviceId());
                addAlarmSubscribe(device, null);
            }
        }
    }

    private void catalogSubscribeExpire(String deviceId, SipTransactionInfo transactionInfo) {
        Device device = getDeviceByDeviceId(deviceId);
        if (device == null) {
            log.info("[directory subscription] Expiration, number： {}, Device does not exist, ignore", deviceId);
            return;
        }
        if (device.isOnLine() && device.getSubscribeCycleForCatalog() > 0) {
            log.info("[directory subscription] Expiration, number： {}", deviceId);
            addCatalogSubscribe(device, transactionInfo);
        }
    }

    private void mobilPositionSubscribeExpire(String deviceId, SipTransactionInfo transactionInfo) {
        Device device = getDeviceByDeviceId(deviceId);
        if (device == null) {
            log.info("[Mobile location subscription] Expiration, number： {}, Device does not exist, ignore", deviceId);
            return;
        }
        if (device.isOnLine() && device.getSubscribeCycleForMobilePosition() > 0) {
            log.info("[Mobile location subscription] Expiration, number： {}", deviceId);
            addMobilePositionSubscribe(device, transactionInfo);
        }
    }

    private void alarmSubscribeExpire(String deviceId, SipTransactionInfo transactionInfo) {
        Device device = getDeviceByDeviceId(deviceId);
        if (device == null) {
            log.info("[Mobile alert subscription] Expiration, number： {}, Device does not exist, ignore", deviceId);
            return;
        }
        if (device.isOnLine() && device.getSubscribeCycleForAlarm() > 0) {
            log.info("[Alarm subscription] Expiration, number： {}", deviceId);
            addAlarmSubscribe(device, transactionInfo);
        }
    }

    @Override
    public boolean addCatalogSubscribe(@NotNull Device device, SipTransactionInfo transactionInfo) {
        if (device == null || device.getSubscribeCycleForCatalog() < 0) {
            return false;
        }
        if (transactionInfo == null) {
            log.info("[Add directory subscription] Equipment {}", device.getDeviceId());
        }else {
            log.info("[Directory Subscription Renewal] Equipment {}", device.getDeviceId());
        }
        try {
            sipCommander.catalogSubscribe(device, transactionInfo, eventResult -> {
                ResponseEvent event = (ResponseEvent) eventResult.event;
                // success
                log.info("[directory subscription]success： {}", device.getDeviceId());
                if (!subscribeTaskRunner.containsKey(SubscribeTaskForCatalog.getKey(device))) {
                    SIPResponse response = (SIPResponse) event.getResponse();
                    SipTransactionInfo transactionInfoForResponse = new SipTransactionInfo(response);
                    SubscribeTask subscribeTask = SubscribeTaskForCatalog.getInstance(device, this::catalogSubscribeExpire, transactionInfoForResponse);
                    if (subscribeTask != null) {
                        subscribeTaskRunner.addSubscribe(subscribeTask);
                    }
                }else {
                    subscribeTaskRunner.updateDelay(SubscribeTaskForCatalog.getKey(device), (device.getSubscribeCycleForCatalog() * 1000L - 500L) + System.currentTimeMillis());
                }

            },eventResult -> {
                // failed
                log.warn("[directory subscription]Failed, signaling failed to send： {}-{} ", device.getDeviceId(), eventResult.msg);
            });
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[Command sending failed] directory subscription: {}", e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public boolean removeCatalogSubscribe(@NotNull Device device, CommonCallback<Boolean> callback) {
        String key = SubscribeTaskForCatalog.getKey(device);
        if (subscribeTaskRunner.containsKey(key)) {
            log.info("[Remove directory subscription]: {}", device.getDeviceId());
            SipTransactionInfo transactionInfo = subscribeTaskRunner.getTransactionInfo(key);
            if (transactionInfo == null) {
                log.warn("[Remove directory subscription] Transaction information not found，{}", device.getDeviceId());
            }
            try {
                device.setSubscribeCycleForCatalog(0);
                sipCommander.catalogSubscribe(device, transactionInfo, eventResult -> {
                    // success
                    log.info("[Cancel directory subscription]success： {}", device.getDeviceId());
                    subscribeTaskRunner.removeSubscribe(SubscribeTaskForCatalog.getKey(device));
                    if (callback != null) {
                        callback.run(true);
                    }
                },eventResult -> {
                    // failed
                    log.warn("[Cancel directory subscription]Failed, signaling failed to send： {}-{} ", device.getDeviceId(), eventResult.msg);
                });
            }catch (Exception e) {
                // failed
                log.warn("[Cancel directory subscription]failed： {}-{} ", device.getDeviceId(), e.getMessage());
            }
        }
        return true;
    }

    @Override
    public boolean addMobilePositionSubscribe(@NotNull Device device, SipTransactionInfo transactionInfo) {
        if (transactionInfo == null) {
            log.info("[Add mobile location subscription] Equipment {}", device.getDeviceId());
        }else {
            log.info("[Mobile location subscription renewal] Equipment {}", device.getDeviceId());
        }
        try {
            sipCommander.mobilePositionSubscribe(device, transactionInfo, eventResult -> {
                ResponseEvent event = (ResponseEvent) eventResult.event;
                // success
                log.info("[Mobile location subscription]success： {}", device.getDeviceId());
                if (!subscribeTaskRunner.containsKey(SubscribeTaskForMobilPosition.getKey(device))) {
                    SIPResponse response = (SIPResponse) event.getResponse();
                    SipTransactionInfo transactionInfoForResponse = new SipTransactionInfo(response);
                    SubscribeTask subscribeTask = SubscribeTaskForMobilPosition.getInstance(device, this::mobilPositionSubscribeExpire, transactionInfoForResponse);
                    if (subscribeTask != null) {
                        subscribeTaskRunner.addSubscribe(subscribeTask);
                    }
                }else {
                    subscribeTaskRunner.updateDelay(SubscribeTaskForMobilPosition.getKey(device), (device.getSubscribeCycleForMobilePosition() * 1000L - 500L) + System.currentTimeMillis());
                }

            },eventResult -> {
                // failed
                log.warn("[Mobile location subscription]Failed, signaling failed to send： {}-{} ", device.getDeviceId(), eventResult.msg);
            });
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[Command sending failed] Mobile location subscription: {}", e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public boolean removeMobilePositionSubscribe(Device device, CommonCallback<Boolean> callback) {
        String key = SubscribeTaskForMobilPosition.getKey(device);
        if (subscribeTaskRunner.containsKey(key)) {
            log.info("[Remove mobile location subscription]: {}", device.getDeviceId());
            SipTransactionInfo transactionInfo = subscribeTaskRunner.getTransactionInfo(key);
            if (transactionInfo == null) {
                log.warn("[Remove mobile location subscription] Transaction information not found，{}", device.getDeviceId());
            }
            try {
                device.setSubscribeCycleForMobilePosition(0);
                sipCommander.mobilePositionSubscribe(device, transactionInfo, eventResult -> {
                    // success
                    log.info("[Cancel mobile location subscription]success： {}", device.getDeviceId());
                    subscribeTaskRunner.removeSubscribe(SubscribeTaskForMobilPosition.getKey(device));
                    if (callback != null) {
                        callback.run(true);
                    }
                },eventResult -> {
                    // failed
                    log.warn("[Cancel mobile location subscription]Failed, signaling failed to send： {}-{} ", device.getDeviceId(), eventResult.msg);
                });
            }catch (Exception e) {
                // failed
                log.warn("[Cancel mobile location subscription]failed： {}-{} ", device.getDeviceId(), e.getMessage());
            }
        }
        return true;
    }

    @Override
    public boolean addAlarmSubscribe(@NotNull Device device, SipTransactionInfo transactionInfo) {
        if (transactionInfo == null) {
            log.info("[Add alarm subscription] Equipment {}", device.getDeviceId());
        }else {
            log.info("[Alarm subscription renewal] Equipment {}", device.getDeviceId());
        }

        try {
            sipCommander.alarmSubscribe(device, transactionInfo, eventResult -> {
                ResponseEvent event = (ResponseEvent) eventResult.event;
                // success
                log.info("[Alarm subscription]success： {}", device.getDeviceId());
                if (!subscribeTaskRunner.containsKey(SubscribeTaskForAlarm.getKey(device))) {
                    SIPResponse response = (SIPResponse) event.getResponse();
                    SipTransactionInfo transactionInfoForResponse = new SipTransactionInfo(response);
                    SubscribeTask subscribeTask = SubscribeTaskForAlarm.getInstance(device, this::alarmSubscribeExpire, transactionInfoForResponse);
                    if (subscribeTask != null) {
                        subscribeTaskRunner.addSubscribe(subscribeTask);
                    }
                }else {
                    subscribeTaskRunner.updateDelay(SubscribeTaskForAlarm.getKey(device), (device.getSubscribeCycleForAlarm() * 1000L - 500L) + System.currentTimeMillis());
                }

            },eventResult -> {
                // failed
                log.warn("[Alarm subscription]Failed, signaling failed to send： {}-{} ", device.getDeviceId(), eventResult.msg);
            });
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[Command sending failed] Alarm subscription: {}", e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public boolean removeAlarmSubscribe(Device device, CommonCallback<Boolean> callback) {
        String key = SubscribeTaskForAlarm.getKey(device);
        if (subscribeTaskRunner.containsKey(key)) {
            log.info("[Remove alert subscription]: {}", device.getDeviceId());
            SipTransactionInfo transactionInfo = subscribeTaskRunner.getTransactionInfo(key);
            if (transactionInfo == null) {
                log.warn("[Remove alert subscription] Transaction information not found，{}", device.getDeviceId());
            }
            try {
                device.setSubscribeCycleForAlarm(0);
                sipCommander.alarmSubscribe(device, transactionInfo, eventResult -> {
                    // success
                    log.info("[Cancel alarm subscription]success： {}", device.getDeviceId());
                    subscribeTaskRunner.removeSubscribe(SubscribeTaskForAlarm.getKey(device));
                    if (callback != null) {
                        callback.run(true);
                    }
                },eventResult -> {
                    // failed
                    log.warn("[Cancel alarm subscription]Failed, signaling failed to send： {}-{} ", device.getDeviceId(), eventResult.msg);
                });
            }catch (Exception e) {
                // failed
                log.warn("[Cancel alarm subscription]failed： {}-{} ", device.getDeviceId(), e.getMessage());
            }
        }
        return true;
    }

    @Override
    public SyncStatus getChannelSyncStatus(String deviceId) {
        Device device = deviceMapper.getDeviceByDeviceId(deviceId);
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR404.getCode(), "Device does not exist");
        }
        if (!userSetting.getServerId().equals(device.getServerId())) {
            return redisRpcService.getChannelSyncStatus(device.getServerId(), deviceId);
        }
        return catalogResponseMessageHandler.getChannelSyncProgress(deviceId);
    }

    @Override
    public Boolean isSyncRunning(String deviceId) {
        return catalogResponseMessageHandler.isSyncRunning(deviceId);
    }

    @Override
    public void sync(Device device) {
        int sn;
        synchronized (device.getDeviceId().intern()) {
            if (catalogResponseMessageHandler.isSyncRunning(device.getDeviceId())) {
                SyncStatus syncStatus = catalogResponseMessageHandler.getChannelSyncProgress(device.getDeviceId());
                log.info("[sync channel] Sync already exists, device: {}, Sync information: {}", device.getDeviceId(), JSON.toJSON(syncStatus));
                return;
            }
            sn = (int)((Math.random()*9+1)*100000);
            catalogResponseMessageHandler.setChannelSyncReady(device, sn);
        }
        try {
            sipCommander.catalogQuery(device, sn, event -> {
                String errorMsg = String.format("Synchronization channel failed, error code： %s, %s", event.statusCode, event.msg);
                log.info("[sync channel]failed,number: {}, error code： {}, {}", device.getDeviceId(), event.statusCode, event.msg);
                catalogResponseMessageHandler.setChannelSyncEnd(device.getDeviceId(), sn, errorMsg);
            });
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[sync channel], Signaling failed：{}", e.getMessage() );
            String errorMsg = String.format("The synchronization channel failed and the signaling transmission failed.： %s", e.getMessage());
            catalogResponseMessageHandler.setChannelSyncEnd(device.getDeviceId(), sn, errorMsg);
        }
    }

    @Override
    public Device getDeviceByDeviceId(String deviceId) {
        Device device = redisCatchStorage.getDevice(deviceId);
        if (device == null) {
            device = getDeviceByDeviceIdFromDb(deviceId);
            if (device != null) {
                redisCatchStorage.updateDevice(device);
            }
        }
        return device;
    }

    @Override
    public List<Device> getAllOnlineDevice(String serverId) {
        return deviceMapper.getOnlineDevicesByServerId(serverId);
    }

    @Override
    public List<Device> getAllByStatus(Boolean status) {
        return deviceMapper.getDevices(ChannelDataType.GB28181, status);
    }

    @Override
    public Boolean getDeviceStatus(@NotNull Device device) {
        SynchronousQueue<String> queue = new SynchronousQueue<>();
        try {
            sipCommander.deviceStatusQuery(device, ((code, msg, data) -> {
                queue.offer(msg);
            }));
            String data = queue.poll(10, TimeUnit.SECONDS);
            if (data != null && "ONLINE".equalsIgnoreCase(data.trim())) {
                return Boolean.TRUE;
            }else {
                return Boolean.FALSE;
            }

        } catch (InvalidArgumentException | SipException | ParseException | InterruptedException e) {
            log.error("[Command sending failed] Equipment status query: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public Device getDeviceByHostAndPort(String host, int port) {
        return deviceMapper.getDeviceByHostAndPort(host, port);
    }

    @Override
    public void updateDevice(Device device) {

        device.setCharset(device.getCharset() == null ? "" : device.getCharset().toUpperCase());
        device.setUpdateTime(DateUtil.getNow());
        if (deviceMapper.update(device) > 0) {
            redisCatchStorage.updateDevice(device);
        }
    }

    @Transactional
    @Override
    public void updateDeviceList(List<Device> deviceList) {
        if (deviceList.isEmpty()){
            log.info("[Update devices in batches] The list is empty and the details failed.");
            return;
        }
        if (deviceList.size() == 1) {
            updateDevice(deviceList.get(0));
        }else {
            for (Device device : deviceList) {
                device.setCharset(device.getCharset() == null ? "" : device.getCharset().toUpperCase());
                device.setUpdateTime(DateUtil.getNow());
            }
            int limitCount = 300;
            if (!deviceList.isEmpty()) {
                for (int i = 0; i < deviceList.size(); i += limitCount) {
                    int endIndex = Math.min(i + limitCount, deviceList.size());
                    List<Device> subList = deviceList.subList(i, endIndex);
                    deviceMapper.batchUpdate(subList);
                }
                for (Device device : deviceList) {
                    redisCatchStorage.updateDevice(device);
                }
            }
        }
    }

    @Override
    public boolean isExist(String deviceId) {
        return getDeviceByDeviceIdFromDb(deviceId) != null;
    }

    @Override
    public void addCustomDevice(Device device) {
        device.setOnLine(false);
        device.setCreateTime(DateUtil.getNow());
        device.setUpdateTime(DateUtil.getNow());
        if(device.getStreamMode() == null) {
            device.setStreamMode("TCP-PASSIVE");
        }
        deviceMapper.addCustomDevice(device);
    }

    @Override
    public void updateCustomDevice(Device device) {
        // The modification of the subscription status is controlled by a separate method, and no status modification is performed here.
        Device deviceInStore = deviceMapper.query(device.getId());
        if (deviceInStore == null) {
            log.warn("Device information not found when updating device");
            return;
        }
        if (deviceInStore.getGeoCoordSys() != null) {
            // The coordinate system changes and the GCJ02 coordinates and WGS84 coordinates need to be recalculated.
            if (!deviceInStore.getGeoCoordSys().equals(device.getGeoCoordSys())) {
                deviceInStore.setGeoCoordSys(device.getGeoCoordSys());
            }
        }else {
            deviceInStore.setGeoCoordSys("WGS84");
        }
        if (device.getCharset() == null) {
            deviceInStore.setCharset("GB2312");
        }

        deviceMapper.updateCustom(device);
        redisCatchStorage.updateDevice(device);
    }

    @Override
    @Transactional
    public boolean delete(String deviceId) {
        Device device = getDeviceByDeviceIdFromDb(deviceId);
        Assert.notNull(device, "Device not found");
        if (subscribeTaskRunner.containsKey(SubscribeTaskForCatalog.getKey(device))) {
            removeCatalogSubscribe(device, null);
        }
        if (subscribeTaskRunner.containsKey(SubscribeTaskForMobilPosition.getKey(device))) {
            removeMobilePositionSubscribe(device, null);
        }
        if (subscribeTaskRunner.containsKey(SubscribeTaskForAlarm.getKey(device))) {
            removeAlarmSubscribe(device, null);
        }
        if (deviceStatusManager.contains(deviceId)) {
            deviceStatusManager.remove(deviceId);
        }
        List<CommonGBChannel> commonGBChannels = commonGBChannelMapper.queryByDataTypeAndDeviceIds(1, List.of(device.getId()));

        try {
            // sendcatalog
            eventPublisher.channelEventPublish(commonGBChannels, ChannelEvent.ChannelEventMessageType.DEL);
        } catch (Exception e) {
            log.warn("[Delete multiple channels] Failed to send, quantity：{}", commonGBChannels.size(), e);
        }

        platformChannelMapper.delChannelForDeviceId(deviceId);
        deviceChannelMapper.cleanChannelsByDeviceId(device.getId());
        deviceMapper.del(deviceId);
        redisCatchStorage.removeDevice(deviceId);
        inviteStreamService.clearInviteInfo(deviceId);
        return true;
    }

    @Override
    public ResourceBaseInfo getOverview() {
        List<Device> onlineDevices = deviceMapper.getOnlineDevices();
        List<Device> all = deviceMapper.getAll();
        return new ResourceBaseInfo(all.size(), onlineDevices.size());
    }

    @Override
    public List<Device> getAll() {
        return deviceMapper.getAll();
    }

    @Override
    public PageInfo<Device> getAll(int page, int count, String query, Boolean status) {
        PageHelper.startPage(page, count);
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }
        List<Device> all = deviceMapper.getDeviceList(ChannelDataType.GB28181, query, status);
        return new PageInfo<>(all);
    }

    @Override
    public Device getDevice(Integer id) {
        return deviceMapper.query(id);
    }

    @Override
    public Device getDeviceByChannelId(Integer channelId) {
        return deviceMapper.queryByChannelId(ChannelDataType.GB28181,channelId);
    }

    @Override
    public Device getDeviceBySourceChannelDeviceId(String channelId) {
        return deviceMapper.getDeviceBySourceChannelDeviceId(ChannelDataType.GB28181,channelId);
    }

    @Override
    public void subscribeCatalog(int id, int cycle) {
        Device device = deviceMapper.query(id);
        Assert.notNull(device, "Device not found");
        Assert.isTrue(device.isOnLine(), "Device is offline");
        if (device.getSubscribeCycleForCatalog() == cycle) {
            return;
        }
        if (!userSetting.getServerId().equals(device.getServerId())) {
            redisRpcService.subscribeCatalog(id, cycle);
            return;
        }
        if (cycle > 0) {
            //  Directory subscription related information
            if (device.getSubscribeCycleForCatalog() > 0) {
                // If the subscription periods are different, cancel first
                removeCatalogSubscribe(device, result->{
                    device.setSubscribeCycleForCatalog(cycle);
                    updateDevice(device);
                    // Open subscription
                    addCatalogSubscribe(device, null);
                });
            }else {
                // Open subscription
                device.setSubscribeCycleForCatalog(cycle);
                updateDevice(device);
                addCatalogSubscribe(device, null);
            }
        }else {
            // Unsubscribe
            removeCatalogSubscribe(device, null);
            device.setSubscribeCycleForCatalog(0);
            updateDevice(device);
        }
    }

    @Override
    public void subscribeMobilePosition(int id, int cycle, int interval) {
        Device device = deviceMapper.query(id);
        Assert.notNull(device, "Device not found");
        if (!device.isOnLine()) {
            // Open subscription
            device.setSubscribeCycleForMobilePosition(cycle);
            device.setMobilePositionSubmissionInterval(interval);
            updateDevice(device);
            if (subscribeTaskRunner.containsKey(SubscribeTaskForMobilPosition.getKey(device))) {
                subscribeTaskRunner.removeSubscribe(SubscribeTaskForMobilPosition.getKey(device));
            }
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Device is offline");
        }

        if (device.getSubscribeCycleForMobilePosition() == cycle) {
            return;
        }
        if (!userSetting.getServerId().equals(device.getServerId())) {
            redisRpcService.subscribeMobilePosition(id, cycle, interval);
            return;
        }
        if (cycle > 0) {
            //  Directory subscription related information
            if (device.getSubscribeCycleForMobilePosition() > 0) {
                // The subscription cycle has already started, please cancel first
                removeMobilePositionSubscribe(device, result->{
                    // Open subscription
                    device.setSubscribeCycleForMobilePosition(cycle);
                    device.setMobilePositionSubmissionInterval(interval);
                    updateDevice(device);
                    addMobilePositionSubscribe(device, null);
                });
            }else {
                // Subscription is not enabled
                device.setSubscribeCycleForMobilePosition(cycle);
                device.setMobilePositionSubmissionInterval(interval);
                updateDevice(device);
                // Open subscription
                addMobilePositionSubscribe(device, null);
            }
        }else {
            // Unsubscribe
            removeMobilePositionSubscribe(device, null);
            device.setSubscribeCycleForMobilePosition(0);
            updateDevice(device);
        }
    }

    @Override
    public void subscribeAlarm(int id, int cycle) {
        Device device = deviceMapper.query(id);
        Assert.notNull(device, "Device not found");
        Assert.isTrue(device.isOnLine(), "Device is offline");
        if (device.getSubscribeCycleForAlarm() == cycle) {
            return;
        }
        if (!userSetting.getServerId().equals(device.getServerId())) {
            redisRpcService.subscribeAlarm(id, cycle);
            return;
        }
        if (cycle  > 0) {
            //  Alarm subscription related information
            if (device.getSubscribeCycleForAlarm() > 0) {
                // If the subscription periods are different, cancel first
                removeAlarmSubscribe(device, result->{
                    device.setSubscribeCycleForAlarm(cycle);
                    updateDevice(device);
                    // Open subscription
                    addAlarmSubscribe(device, null);
                });
            }else {
                // Open subscription
                device.setSubscribeCycleForAlarm(cycle);
                updateDevice(device);
                addAlarmSubscribe(device, null);
            }
        }else {
            // Unsubscribe
            removeAlarmSubscribe(device, null);
            device.setSubscribeCycleForAlarm(0);
            updateDevice(device);
        }
    }

    @Override
    public void updateDeviceHeartInfo(Device device) {
        Device deviceInDb = deviceMapper.query(device.getId());
        if (deviceInDb == null) {
            return;
        }
        if (!Objects.equals(deviceInDb.getHeartBeatCount(), device.getHeartBeatCount())
                || !Objects.equals(deviceInDb.getHeartBeatInterval(), device.getHeartBeatInterval())) {

            deviceInDb.setHeartBeatCount(device.getHeartBeatCount());
            deviceInDb.setHeartBeatInterval(device.getHeartBeatInterval());
            deviceInDb.setPositionCapability(device.getPositionCapability());
            updateDevice(deviceInDb);

            long expiresTime = Math.min(device.getExpires(), device.getHeartBeatInterval() * device.getHeartBeatCount()) * 1000L;
            deviceStatusManager.add(device.getDeviceId(), expiresTime + System.currentTimeMillis());
        }
    }

    @Override
    public WVPResult<SyncStatus> devicesSync(Device device) {
        if (device.getServerId() != null && !userSetting.getServerId().equals(device.getServerId())) {
            return redisRpcService.devicesSync(device.getServerId(), device.getDeviceId());
        }
        // If it already exists, return the progress
        if (isSyncRunning(device.getDeviceId())) {
            SyncStatus channelSyncStatus = getChannelSyncStatus(device.getDeviceId());
            WVPResult<SyncStatus> wvpResult = new WVPResult();
            if (channelSyncStatus.getErrorMsg() != null) {
                wvpResult.setCode(ErrorCode.ERROR100.getCode());
                wvpResult.setMsg(channelSyncStatus.getErrorMsg());
            }else if (channelSyncStatus.getTotal() == null || channelSyncStatus.getTotal() == 0){
                wvpResult.setCode(ErrorCode.SUCCESS.getCode());
                wvpResult.setMsg("Waiting for channel information...");
            }else {
                wvpResult.setCode(ErrorCode.SUCCESS.getCode());
                wvpResult.setMsg(ErrorCode.SUCCESS.getMsg());
                wvpResult.setData(channelSyncStatus);
            }
            return wvpResult;
        }
        sync(device);
        WVPResult<SyncStatus> wvpResult = new WVPResult<>();
        wvpResult.setCode(0);
        wvpResult.setMsg("Start syncing");
        return wvpResult;
    }

    @Override
    public void deviceBasicConfig(Device device, BasicParam basicParam, ErrorCallback<String> callback) {
        if (!userSetting.getServerId().equals(device.getServerId())) {
            WVPResult<String> result = redisRpcService.deviceBasicConfig(device.getServerId(), device, basicParam);
            if (result.getCode() == ErrorCode.SUCCESS.getCode()) {
                callback.run(result.getCode(), result.getMsg(), result.getData());
            }
            return;
        }

        try {
            sipCommander.deviceBasicConfigCmd(device, basicParam, callback);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[Command sending failed] Device configuration: {}", e.getMessage());
            callback.run(ErrorCode.ERROR100.getCode(), "command sent: " + e.getMessage(), null);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "command sent: " + e.getMessage());
        }
    }

    @Override
    public void deviceVideoParamConfig(Device device, VideoParamOpt videoParamOpt, ErrorCallback<String> callback) {
        if (!userSetting.getServerId().equals(device.getServerId())) {
            WVPResult<String> result = redisRpcService.deviceVideoParamConfig(device.getServerId(), device, videoParamOpt);
            if (result.getCode() == ErrorCode.SUCCESS.getCode()) {
                callback.run(result.getCode(), result.getMsg(), result.getData());
            }
            return;
        }

        try {
            sipCommander.deviceVideoParamConfigCmd(device, videoParamOpt, callback);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[Command sending failed] Device configuration: {}", e.getMessage());
            callback.run(ErrorCode.ERROR100.getCode(), "command sent: " + e.getMessage(), null);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "command sent: " + e.getMessage());
        }
    }

    @Override
    public <T extends DeviceConfigAware> void deviceConfigQuery(Device device, String channelId, Class<T> configClass, ErrorCallback<T> callback) {

        if (!userSetting.getServerId().equals(device.getServerId())) {
            WVPResult<Object> result = redisRpcService.deviceConfigQuery(device.getServerId(), device, channelId, configClass.getName());
            if (result.getData() instanceof JSONObject) {
                T obj = ((JSONObject) result.getData()).toJavaObject(configClass);
                callback.run(result.getCode(), result.getMsg(), obj);
            } else {
                callback.run(result.getCode(), result.getMsg(), null);
            }
            return;
        }

        try {
            sipCommander.deviceConfigQuery(device, channelId, configClass, callback);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[Command sending failed] Get device configuration: {}", e.getMessage());
            callback.run(ErrorCode.ERROR100.getCode(), "command sent: " + e.getMessage(), null);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "command sent: " + e.getMessage());
        }
    }

    @Override
    public void teleboot(Device device) {

        if (!userSetting.getServerId().equals(device.getServerId())) {
            redisRpcService.teleboot(device.getServerId(), device);
        }
        try {
            sipCommander.teleBootCmd(device);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[Command sending failed] remote start: {}", e.getMessage());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Command sending failed: " + e.getMessage());
        }
    }

    @Override
    public void record(Device device, String channelId, String recordCmdStr, ErrorCallback<String> callback) {

        if (!userSetting.getServerId().equals(device.getServerId())) {
            WVPResult<String> result = redisRpcService.recordControl(device.getServerId(), device, channelId, recordCmdStr);
            callback.run(result.getCode(), result.getMsg(), result.getData());
            return;
        }

        try {
            sipCommander.recordCmd(device, channelId, recordCmdStr, callback);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[Command sending failed] start/Stop recording: {}", e.getMessage());
            callback.run(ErrorCode.ERROR100.getCode(), "command sent: " + e.getMessage(), null);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "command sent: " + e.getMessage());
        }
    }

    @Override
    public void guard(Device device, String guardCmdStr, ErrorCallback<String> callback) {
        if (!userSetting.getServerId().equals(device.getServerId())) {
            WVPResult<String> result = redisRpcService.guard(device.getServerId(), device, guardCmdStr);
            callback.run(result.getCode(), result.getMsg(), result.getData());
            return;
        }

        try {
            sipCommander.guardCmd(device, guardCmdStr, callback);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[Command sending failed] arm/Disarm operation: {}", e.getMessage());
            callback.run(ErrorCode.ERROR100.getCode(), "command sent: " + e.getMessage(), null);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "command sent: " + e.getMessage());
        }
    }

    @Override
    public void resetAlarm(Device device, String channelId, String alarmMethod, String alarmType, ErrorCallback<String> callback) {
        if (!userSetting.getServerId().equals(device.getServerId())) {
            WVPResult<String> result = redisRpcService.resetAlarm(device.getServerId(), device, channelId, alarmMethod, alarmType);
            callback.run(result.getCode(), result.getMsg(), result.getData());
            return;
        }
        try {
            sipCommander.alarmResetCmd(device, alarmMethod, alarmType, callback);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[Command sending failed] arm/Disarm operation: {}", e.getMessage());
            callback.run(ErrorCode.ERROR100.getCode(), "command sent: " + e.getMessage(), null);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "command sent: " + e.getMessage());
        }

    }

    @Override
    public void iFrame(Device device, String channelId) {
        if (!userSetting.getServerId().equals(device.getServerId())) {
            redisRpcService.iFrame(device.getServerId(), device, channelId);
            return;
        }

        try {
            sipCommander.iFrameCmd(device, channelId);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[Command sending failed] Force keyframe operation: {}", e.getMessage());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "command sent: " + e.getMessage());
        }
    }

    @Override
    public void homePosition(Device device, String channelId, Boolean enabled, Integer resetTime, Integer presetIndex, ErrorCallback<String> callback) {
        if (!userSetting.getServerId().equals(device.getServerId())) {
            WVPResult<String> result = redisRpcService.homePosition(device.getServerId(), device, channelId, enabled, resetTime, presetIndex);
            callback.run(result.getCode(), result.getMsg(), result.getData());
            return;
        }

        try {
            sipCommander.homePositionCmd(device, channelId, enabled, resetTime, presetIndex, callback);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[Command sending failed] Watch bit setting: {}", e.getMessage());
            callback.run(ErrorCode.ERROR100.getCode(), "command sent: " + e.getMessage(), null);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Command sending failed: " + e.getMessage());
        }
    }

    @Override
    public void dragZoomIn(Device device, String channelId, int length, int width, int midPointX, int midPointY, int lengthX, int lengthY) {
        if (!userSetting.getServerId().equals(device.getServerId())) {
            redisRpcService.dragZoomIn(device.getServerId(), device, channelId, length, width, midPointX, midPointY, lengthX, lengthY);
            return;
        }

        StringBuffer cmdXml = new StringBuffer(200);
        cmdXml.append("<DragZoomIn>\r\n");
        cmdXml.append("<Length>" + length+ "</Length>\r\n");
        cmdXml.append("<Width>" + width+ "</Width>\r\n");
        cmdXml.append("<MidPointX>" + midPointX+ "</MidPointX>\r\n");
        cmdXml.append("<MidPointY>" + midPointY+ "</MidPointY>\r\n");
        cmdXml.append("<LengthX>" + lengthX+ "</LengthX>\r\n");
        cmdXml.append("<LengthY>" + lengthY+ "</LengthY>\r\n");
        cmdXml.append("</DragZoomIn>\r\n");
        try {
            sipCommander.dragZoomCmd(device, channelId, cmdXml.toString());
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[Command sending failed] Scroll down to enlarge: {}", e.getMessage());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Command sending failed: " +  e.getMessage());
        }
    }

    @Override
    public void dragZoomOut(Device device, String channelId, int length, int width, int midPointX, int midPointY, int lengthX, int lengthY) {
        if (!userSetting.getServerId().equals(device.getServerId())) {
            redisRpcService.dragZoomOut(device.getServerId(), device, channelId, length, width, midPointX, midPointY, lengthX, lengthY);
            return;
        }

        StringBuffer cmdXml = new StringBuffer(200);
        cmdXml.append("<DragZoomOut>\r\n");
        cmdXml.append("<Length>" + length+ "</Length>\r\n");
        cmdXml.append("<Width>" + width+ "</Width>\r\n");
        cmdXml.append("<MidPointX>" + midPointX+ "</MidPointX>\r\n");
        cmdXml.append("<MidPointY>" + midPointY+ "</MidPointY>\r\n");
        cmdXml.append("<LengthX>" + lengthX+ "</LengthX>\r\n");
        cmdXml.append("<LengthY>" + lengthY+ "</LengthY>\r\n");
        cmdXml.append("</DragZoomOut>\r\n");
        try {
            sipCommander.dragZoomCmd(device, channelId, cmdXml.toString());
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[Command sending failed] Scroll down to enlarge: {}", e.getMessage());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Command sending failed: " +  e.getMessage());
        }
    }

    @Override
    public void deviceStatus(Device device, ErrorCallback<String> callback) {

        if (!userSetting.getServerId().equals(device.getServerId())) {
            WVPResult<String> result = redisRpcService.deviceStatus(device.getServerId(), device);
            callback.run(result.getCode(), result.getMsg(), result.getData());
            return;
        }
        try {
            sipCommander.deviceStatusQuery(device, (code, msg, data) -> {
                if ("ONLINE".equalsIgnoreCase(data.trim())) {
                    online(device);
                }else {
                    offline(device);
                }
                if (callback != null) {
                    callback.run(code, msg, data);
                }
            });
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[Command sending failed] Get device status: {}", e.getMessage());
            callback.run(ErrorCode.ERROR100.getCode(), "command sent: " + e.getMessage(), null);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Command sending failed: " + e.getMessage());
        }
    }


    @Override
    public void alarm(Device device, String startPriority, String endPriority, String alarmMethod, String alarmType, String startTime, String endTime, ErrorCallback<Object> callback) {
        if (!userSetting.getServerId().equals(device.getServerId())) {
            WVPResult<String> result = redisRpcService.alarm(device.getServerId(), device, startPriority, endPriority, alarmMethod, alarmType, startTime, endTime);
            callback.run(result.getCode(), result.getMsg(), result.getData());
            return;
        }

        String startAlarmTime = "";
        if (startTime != null) {
            startAlarmTime = DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(startTime);
        }
        String endAlarmTime = "";
        if (startTime != null) {
            endAlarmTime = DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(endTime);
        }

        try {
            sipCommander.alarmInfoQuery(device, startPriority, endPriority, alarmMethod, alarmType, startAlarmTime, endAlarmTime, callback);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[Command sending failed] Get device status: {}", e.getMessage());
            callback.run(ErrorCode.ERROR100.getCode(), "command sent: " + e.getMessage(), null);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Command sending failed: " + e.getMessage());
        }
    }

    @Override
    public void deviceInfo(Device device, ErrorCallback<Object> callback) {
        if (!userSetting.getServerId().equals(device.getServerId())) {
            WVPResult<Object> result = redisRpcService.deviceInfo(device.getServerId(), device);
            callback.run(result.getCode(), result.getMsg(), result.getData());
            return;
        }

        try {
            sipCommander.deviceInfoQuery(device, callback);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[Command sending failed] Get device information: {}", e.getMessage());
            callback.run(ErrorCode.ERROR100.getCode(), "command sent: " + e.getMessage(), null);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Command sending failed: " + e.getMessage());
        }
    }

    @Override
    public void queryPreset(Device device, String channelId, ErrorCallback<List<Preset>> callback) {
        if (!userSetting.getServerId().equals(device.getServerId())) {
            WVPResult<List<Preset>> result = redisRpcService.queryPreset(device.getServerId(), device, channelId);
            callback.run(result.getCode(), result.getMsg(), result.getData());
            return;
        }

        try {
            sipCommander.presetQuery(device, channelId, callback);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[Command sending failed] Prefabricated position query: {}", e.getMessage());
            callback.run(ErrorCode.ERROR100.getCode(), "command sent: " + e.getMessage(), null);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Command sending failed: " + e.getMessage());
        }
    }

    @Override
    public List<TimeStatistics> getKeepaliveTimeStatistics(String deviceId, Integer count) {
        List<Long> timeStampList = redisCatchStorage.getDeviceKeepaliveTimeStamp(deviceId, count);
        return formateTimeStatistics(timeStampList, count);
    }

    @Override
    public List<TimeStatistics> getRegisterTimeStatistics(String deviceId, Integer count) {
        List<Long> timeStampList = redisCatchStorage.getDeviceRegisterTimeStamp(deviceId, count);
        return formateTimeStatistics(timeStampList, count);
    }

    private List<TimeStatistics> formateTimeStatistics(List<Long> timeStampList, Integer count) {
        if (timeStampList.isEmpty()) {
            return List.of();
        }
        List<TimeStatistics> timeStatisticsList = new ArrayList<>();
        for (int i = 0; i < timeStampList.size(); i++) {
            Long timeStamp = timeStampList.get(i);
            TimeStatistics timeStatistics = new TimeStatistics();
            timeStatistics.setTime(DateUtil.timestampMsTo_yyyy_MM_dd_HH_mm_ss(timeStamp));
            if (i > 0) {
                Long lastTimeStamp = timeStampList.get(i - 1);
                timeStatistics.setTimeDiff((timeStamp - lastTimeStamp) / 1000);
            }
            timeStatisticsList.add(timeStatistics);
        }
        // Since the first data does not have a previous timestamp, the time difference cannot be calculated and is removed.
        timeStatisticsList.removeFirst();
        if (timeStatisticsList.size() - 1 > count) {
            timeStatisticsList = timeStatisticsList.subList(timeStatisticsList.size() - count, timeStatisticsList.size());
        }
        return timeStatisticsList;
    }
}
