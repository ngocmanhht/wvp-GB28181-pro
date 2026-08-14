package com.genersoft.iot.vmp.storager.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.ServerInfo;
import com.genersoft.iot.vmp.common.SystemAllInfo;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.dao.DeviceMapper;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.zlm.dto.StreamAuthorityInfo;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.service.bean.MessageForPushChannel;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.utils.JsonUtil;
import com.genersoft.iot.vmp.utils.SystemInfoUtils;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCatchStorageImpl implements IRedisCatchStorage {

    @Autowired
    private final DeviceMapper deviceMapper;

    private final UserSetting userSetting;

    private final RedisTemplate<String, Object> redisTemplate;

    private final RedisTemplate<String, Long> longRedisTemplate;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public List<SendRtpInfo> queryAllSendRTPServer() {
        return Collections.emptyList();
    }

    @Override
    public Long getCSEQ() {
        String key = VideoManagerConstants.SIP_CSEQ_PREFIX  + userSetting.getServerId();

        Long result =  redisTemplate.opsForValue().increment(key, 1L);
        if (result != null && result > Integer.MAX_VALUE) {
            redisTemplate.opsForValue().set(key, 1);
            result = 1L;
        }
        return result;
    }

    @Override
    public void resetAllCSEQ() {
        String key = VideoManagerConstants.SIP_CSEQ_PREFIX  + userSetting.getServerId();
        redisTemplate.opsForValue().set(key, 1);
    }


    @Override
    public void updateWVPInfo(ServerInfo serverInfo, int time) {
        String key = VideoManagerConstants.WVP_SERVER_PREFIX + userSetting.getServerId();
        Duration duration = Duration.ofSeconds(time);
        redisTemplate.opsForValue().set(key, serverInfo, duration);
        // Set the score value of the platform
        String setKey = VideoManagerConstants.WVP_SERVER_LIST;
        // Set it to 0 for the first time. The smaller the subsequent value, the more recently started it is.
        redisTemplate.opsForZSet().add(setKey, userSetting.getServerId(), System.currentTimeMillis());
    }

    @Override
    public void removeOfflineWVPInfo(String serverId) {
        String setKey = VideoManagerConstants.WVP_SERVER_LIST;
        // Set it to 0 for the first time. The smaller the subsequent value, the more recently started it is.
        redisTemplate.opsForZSet().remove(setKey, serverId);
    }

    @Override
    public void sendStreamChangeMsg(String type, JSONObject jsonObject) {
        String key = VideoManagerConstants.WVP_MSG_STREAM_CHANGE_PREFIX + type;
        log.info("[redis flow change event] send {}: {}", key, jsonObject.toString());
        redisTemplate.convertAndSend(key, jsonObject);
    }

    @Override
    public void addStream(MediaServer mediaServerItem, String type, String app, String streamId, MediaInfo mediaInfo) {
        // Find if usedcallID
        StreamAuthorityInfo streamAuthorityInfo = getStreamAuthorityInfo(app, streamId);
        String key = VideoManagerConstants.WVP_SERVER_STREAM_PREFIX  + userSetting.getServerId() + "_" + type.toUpperCase() + "_" + app + "_" + streamId + "_" + mediaServerItem.getId();
        if (streamAuthorityInfo != null) {
            mediaInfo.setCallId(streamAuthorityInfo.getCallId());
        }
        redisTemplate.opsForValue().set(key, JSON.toJSONString(mediaInfo));
    }

    @Override
    public void removeStream(String mediaServerId, String type, String app, String streamId) {
        String key = VideoManagerConstants.WVP_SERVER_STREAM_PREFIX + userSetting.getServerId() + "_" + type.toUpperCase() + "_"  + app + "_" + streamId + "_" + mediaServerId;
        redisTemplate.delete(key);
    }

    @Override
    public void removeStream(String mediaServerId, String type) {
        String key = VideoManagerConstants.WVP_SERVER_STREAM_PREFIX + userSetting.getServerId() + "_" + type.toUpperCase() + "_*_*_" + mediaServerId;
        List<Object> streams = RedisUtil.scan(redisTemplate, key);
        for (Object stream : streams) {
            redisTemplate.delete((String) stream);
        }
    }

    @Override
    public List<MediaInfo> getStreams(String mediaServerId, String type) {
        List<MediaInfo> result = new ArrayList<>();
        String key = VideoManagerConstants.WVP_SERVER_STREAM_PREFIX + userSetting.getServerId() + "_" + type.toUpperCase() + "_*_*_" + mediaServerId;
        List<Object> streams = RedisUtil.scan(redisTemplate, key);
        for (Object stream : streams) {
            String mediaInfoJson = (String)redisTemplate.opsForValue().get(stream);
            MediaInfo mediaInfo = JSON.parseObject(mediaInfoJson, MediaInfo.class);
            result.add(mediaInfo);
        }
        return result;
    }

    @Override
    public void updateDevice(Device device) {
        String key = VideoManagerConstants.DEVICE_PREFIX;
        redisTemplate.opsForHash().put(key, device.getDeviceId(), device);
    }

    @Override
    public void removeDevice(String deviceId) {
        String key = VideoManagerConstants.DEVICE_PREFIX;
        redisTemplate.opsForHash().delete(key, deviceId);
        // Delete the registration time and heartbeat time cache lists at the same time
        longRedisTemplate.delete(VideoManagerConstants.DEVICE_REGISTER_PREFIX + deviceId);
        longRedisTemplate.delete(VideoManagerConstants.DEVICE_KEEPALIVE_PREFIX + deviceId);
    }

    @Override
    public void removeAllDevice() {
        String key = VideoManagerConstants.DEVICE_PREFIX;
        redisTemplate.delete(key);
        // Also delete all registration time and heartbeat time cache lists
        Set<String> registerKeys = stringRedisTemplate.keys(VideoManagerConstants.DEVICE_REGISTER_PREFIX + "*");
        if (registerKeys != null && !registerKeys.isEmpty()) {
            stringRedisTemplate.delete(registerKeys);
        }
        Set<String> keepaliveKeys = stringRedisTemplate.keys(VideoManagerConstants.DEVICE_KEEPALIVE_PREFIX + "*");
        if (keepaliveKeys != null && !keepaliveKeys.isEmpty()) {
            stringRedisTemplate.delete(keepaliveKeys);
        }
    }

    @Override
    public List<Device> getAllDevices() {
        String key = VideoManagerConstants.DEVICE_PREFIX;
        List<Device> result = new ArrayList<>();
        List<Object> values = redisTemplate.opsForHash().values(key);
        for (Object value : values) {
            if (Objects.nonNull(value)) {
                result.add((Device)value);
            }
        }
        return result;
    }

    @Override
    public Device getDevice(String deviceId) {
        String key = VideoManagerConstants.DEVICE_PREFIX;
        Device device;
        Object object = redisTemplate.opsForHash().get(key, deviceId);
        if (object == null){
            device = deviceMapper.getDeviceByDeviceId(deviceId);
            if (device != null) {
                updateDevice(device);
            }
        }else {
            device = (Device)object;
        }
        return device;
    }

    @Override
    public List<Device> getDeviceList(Set<String> deviceIds) {
        String key = VideoManagerConstants.DEVICE_PREFIX;
        List<Device> deviceList  = new ArrayList<>();
        List<Object> objectList = redisTemplate.opsForHash().multiGet(key, Arrays.asList(deviceIds.toArray()));
        for (Object object : objectList) {
            deviceList.add((Device)object);
        }
        return deviceList;
    }

    @Override
    public void updateGpsMsgInfo(GPSMsgInfo gpsMsgInfo) {
        String key = VideoManagerConstants.WVP_STREAM_GPS_MSG_PREFIX + userSetting.getServerId();
        Duration duration = Duration.ofSeconds(60L);
        gpsMsgInfo.setStored(false);
        redisTemplate.opsForHash().put(key, gpsMsgInfo.getId(),gpsMsgInfo);
        redisTemplate.expire(key, duration);
        // Default GPS messages are saved for 1 minute
    }

    @Override
    public GPSMsgInfo getGpsMsgInfo(String channelId) {
        String key = VideoManagerConstants.WVP_STREAM_GPS_MSG_PREFIX + userSetting.getServerId();
        return (GPSMsgInfo) redisTemplate.opsForHash().get(key, channelId);
    }

    @Override
    public List<GPSMsgInfo> getAllGpsMsgInfo() {
        String key = VideoManagerConstants.WVP_STREAM_GPS_MSG_PREFIX + userSetting.getServerId();
        List<GPSMsgInfo> result = new ArrayList<>();
        List<Object> values = redisTemplate.opsForHash().values(key);
        for (Object value : values) {
            result.add((GPSMsgInfo)value);
        }
        return result;
    }

    @Override
    public void updateStreamAuthorityInfo(String app, String stream, StreamAuthorityInfo streamAuthorityInfo) {
        String key = VideoManagerConstants.MEDIA_STREAM_AUTHORITY;
        String objectKey = app+ "_" + stream;
        redisTemplate.opsForHash().put(key, objectKey, streamAuthorityInfo);
    }

    @Override
    public void removeStreamAuthorityInfo(String app, String stream) {
        String key = VideoManagerConstants.MEDIA_STREAM_AUTHORITY;
        String objectKey = app+ "_" + stream;
        redisTemplate.opsForHash().delete(key, objectKey);
    }

    @Override
    public StreamAuthorityInfo getStreamAuthorityInfo(String app, String stream) {
        String key = VideoManagerConstants.MEDIA_STREAM_AUTHORITY;
        String objectKey = app+ "_" + stream;
        return (StreamAuthorityInfo)redisTemplate.opsForHash().get(key, objectKey);

    }

    @Override
    public List<StreamAuthorityInfo> getAllStreamAuthorityInfo() {
        String key = VideoManagerConstants.MEDIA_STREAM_AUTHORITY;
        List<StreamAuthorityInfo> result = new ArrayList<>();
        List<Object> values = redisTemplate.opsForHash().values(key);
        for (Object value : values) {
            result.add((StreamAuthorityInfo)value);
        }
        return result;
    }


    @Override
    public MediaInfo getStreamInfo(String app, String streamId, String mediaServerId) {
        String scanKey = VideoManagerConstants.WVP_SERVER_STREAM_PREFIX  + userSetting.getServerId() + "_*_" + app + "_" + streamId + "_" + mediaServerId;

        MediaInfo result = null;
        List<Object> keys = RedisUtil.scan(redisTemplate, scanKey);
        if (!keys.isEmpty()) {
            String key = (String) keys.get(0);
            String mediaInfoJson = (String)redisTemplate.opsForValue().get(key);
            result = JSON.parseObject(mediaInfoJson, MediaInfo.class);
        }

        return result;
    }

    @Override
    public MediaInfo getProxyStream(String app, String streamId) {
        String scanKey = VideoManagerConstants.WVP_SERVER_STREAM_PREFIX  + userSetting.getServerId() + "_PULL_" + app + "_" + streamId + "_*";

        MediaInfo result = null;
        List<Object> keys = RedisUtil.scan(redisTemplate, scanKey);
        if (!keys.isEmpty()) {
            String key = (String) keys.get(0);
            String mediaInfoJson = (String)redisTemplate.opsForValue().get(key);
            result = JSON.parseObject(mediaInfoJson, MediaInfo.class);
        }

        return result;
    }

    @Override
    public void addCpuInfo(double cpuInfo) {
        String key = VideoManagerConstants.SYSTEM_INFO_CPU_PREFIX + userSetting.getServerId();
        Map<String, String> infoMap = new HashMap<>();
        infoMap.put("time", DateUtil.getNow());
        infoMap.put("data", String.valueOf(cpuInfo));
        redisTemplate.opsForList().rightPush(key, infoMap);
        // One per second, only 30 can be stored at most
        Long size = redisTemplate.opsForList().size(key);
        if (size != null && size >= 30) {
            for (int i = 0; i < size - 30; i++) {
                redisTemplate.opsForList().leftPop(key);
            }
        }
    }

    @Override
    public void addMemInfo(double memInfo) {
        String key = VideoManagerConstants.SYSTEM_INFO_MEM_PREFIX + userSetting.getServerId();
        Map<String, String> infoMap = new HashMap<>();
        infoMap.put("time", DateUtil.getNow());
        infoMap.put("data", String.valueOf(memInfo));
        redisTemplate.opsForList().rightPush(key, infoMap);
        // One per second, only 30 can be stored at most
        Long size = redisTemplate.opsForList().size(key);
        if (size != null && size >= 30) {
            for (int i = 0; i < size - 30; i++) {
                redisTemplate.opsForList().leftPop(key);
            }
        }
    }

    @Override
    public void addNetInfo(Map<String, Double> networkInterfaces) {
        String key = VideoManagerConstants.SYSTEM_INFO_NET_PREFIX + userSetting.getServerId();
        Map<String, Object> infoMap = new HashMap<>();
        infoMap.put("time", DateUtil.getNow());
        for (String netKey : networkInterfaces.keySet()) {
            infoMap.put(netKey, networkInterfaces.get(netKey));
        }
        redisTemplate.opsForList().rightPush(key, infoMap);
        // One per second, only 30 can be stored at most
        Long size = redisTemplate.opsForList().size(key);
        if (size != null && size >= 30) {
            for (int i = 0; i < size - 30; i++) {
                redisTemplate.opsForList().leftPop(key);
            }
        }
    }

    @Override
    public void addDiskInfo(List<Map<String, Object>> diskInfo) {

        String key = VideoManagerConstants.SYSTEM_INFO_DISK_PREFIX + userSetting.getServerId();
        redisTemplate.opsForValue().set(key, diskInfo);
    }

    @Override
    public SystemAllInfo getSystemInfo() {
        String cpuKey = VideoManagerConstants.SYSTEM_INFO_CPU_PREFIX + userSetting.getServerId();
        String memKey = VideoManagerConstants.SYSTEM_INFO_MEM_PREFIX + userSetting.getServerId();
        String netKey = VideoManagerConstants.SYSTEM_INFO_NET_PREFIX + userSetting.getServerId();
        String diskKey = VideoManagerConstants.SYSTEM_INFO_DISK_PREFIX + userSetting.getServerId();
        SystemAllInfo systemAllInfo = new SystemAllInfo();
        systemAllInfo.setCpu(redisTemplate.opsForList().range(cpuKey, 0, -1));
        systemAllInfo.setMem(redisTemplate.opsForList().range(memKey, 0, -1));
        systemAllInfo.setNet(redisTemplate.opsForList().range(netKey, 0, -1));

        systemAllInfo.setDisk(redisTemplate.opsForValue().get(diskKey));
        systemAllInfo.setNetTotal(SystemInfoUtils.getNetworkTotal());
        return systemAllInfo;
    }

    @Override
    public void sendStreamPushRequestedMsg(MessageForPushChannel msg) {
        String key = VideoManagerConstants.VM_MSG_STREAM_PUSH_REQUESTED;
        log.info("[redisSend notification] Send push stream requested {}: {}/{}", key, msg.getApp(), msg.getStream());
        redisTemplate.convertAndSend(key, JSON.toJSON(msg));
    }

    @Override
    public void sendAlarmMsg(AlarmChannelMessage msg) {
        // This message is used to connect the message content from the third-party service.
        String key = VideoManagerConstants.VM_MSG_SUBSCRIBE_ALARM;
        log.info("[redisSend notification] Send alarm{}: {}", key, JSON.toJSON(msg));
        redisTemplate.convertAndSend(key, JSON.toJSON(msg));
    }

    @Override
    public boolean deviceIsOnline(String deviceId) {
        return getDevice(deviceId).isOnLine();
    }


    @Override
    public void sendStreamPushRequestedMsgForStatus() {
        String key = VideoManagerConstants.VM_MSG_GET_ALL_ONLINE_REQUESTED;
        log.info("[redisNotification] Send Get the status of all streaming devices");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(key, key);
        redisTemplate.convertAndSend(key, jsonObject);
    }

    @Override
    public int getPushStreamCount(String id) {
        String key = VideoManagerConstants.WVP_SERVER_STREAM_PREFIX + userSetting.getServerId() + "_PUSH_*_*_" + id;
        return RedisUtil.scan(redisTemplate, key).size();
    }

    @Override
    public int getProxyStreamCount(String id) {
        String key = VideoManagerConstants.WVP_SERVER_STREAM_PREFIX + userSetting.getServerId() + "_PULL_*_*_" + id;
        return RedisUtil.scan(redisTemplate, key).size();
    }

    @Override
    public int getGbSendCount(String id) {
        String key = VideoManagerConstants.SEND_RTP_INFO_CALLID;
        return redisTemplate.opsForHash().size(key).intValue();
    }

    @Override
    public void sendDeviceOrChannelStatus(String deviceId, String channelId, boolean online) {
        String key = VideoManagerConstants.VM_MSG_SUBSCRIBE_DEVICE_STATUS;
        StringBuilder msg = new StringBuilder();
        msg.append(deviceId);
        if (channelId != null) {
            msg.append(":").append(channelId);
        }
        msg.append(" ").append(online? "ON":"OFF");
        log.info("[redisNotification] push device/Channel status-> {} ", msg);
        // Use RedisTemplate<String, Object> Sending a string message will cause the sent message to have extra double quotes.
        stringRedisTemplate.convertAndSend(key, msg.toString());
    }

    @Override
    public void sendChannelAddOrDelete(String deviceId, String channelId, boolean add) {
        String key = VideoManagerConstants.VM_MSG_SUBSCRIBE_DEVICE_STATUS;


        StringBuilder msg = new StringBuilder();
        msg.append(deviceId);
        if (channelId != null) {
            msg.append(":").append(channelId);
        }
        msg.append(" ").append(add? "ADD":"DELETE");
        log.info("[redisNotification] push channel-> {}", msg);
        // Use RedisTemplate<String, Object> Sending a string message will cause the sent message to have extra double quotes.
        stringRedisTemplate.convertAndSend(key, msg.toString());
    }

    @Override
    public void sendPlatformStartPlayMsg(SendRtpInfo sendRtpItem, DeviceChannel channel, Platform platform) {
        if (platform == null) {
            log.info("[redisSend notification] Failure, the platform information isNULL");
            return;
        }
        if (sendRtpItem.getPlayType() != InviteStreamType.PUSH) {
            log.info("[redisSend notification] Cancel, the stream source channel is not a push device");
            return;
        }
        MessageForPushChannel messageForPushChannel = MessageForPushChannel.getInstance(0, sendRtpItem.getApp(), sendRtpItem.getStream(),
                channel.getDeviceId(), platform.getServerGBId(), platform.getName(), userSetting.getServerId(),
                sendRtpItem.getMediaServerId());
        messageForPushChannel.setPlatFormIndex(platform.getId());
        String key = VideoManagerConstants.VM_MSG_STREAM_START_PLAY_NOTIFY;
        log.info("[redisSend notification] Send the push stream to be viewed by the superior platform {}: {}/{}->{}", key, sendRtpItem.getApp(), sendRtpItem.getStream(), platform.getServerGBId());
        redisTemplate.convertAndSend(key, JSON.toJSON(messageForPushChannel));
    }

    @Override
    public void sendPlatformStopPlayMsg(SendRtpInfo sendRtpItem, Platform platform, CommonGBChannel channel) {

        MessageForPushChannel msg = MessageForPushChannel.getInstance(0,
                sendRtpItem.getApp(), sendRtpItem.getStream(), channel.getGbDeviceId(),
                sendRtpItem.getTargetId(), platform.getName(), userSetting.getServerId(), sendRtpItem.getMediaServerId());
        msg.setPlatFormIndex(platform.getId());

        String key = VideoManagerConstants.VM_MSG_STREAM_STOP_PLAY_NOTIFY;
        log.info("[redisSend notification] Send the superior platform to stop watching {}: {}/{}->{}", key, sendRtpItem.getApp(), sendRtpItem.getStream(), platform.getServerGBId());
        redisTemplate.convertAndSend(key, JSON.toJSON(msg));
    }

    @Override
    public void addPushListItem(String app, String stream, MediaInfo mediaInfo) {
        String key = VideoManagerConstants.PUSH_STREAM_LIST + app + "_" + stream;
        redisTemplate.opsForValue().set(key, mediaInfo);
    }

    @Override
    public MediaInfo getPushListItem(String app, String stream) {
        String key = VideoManagerConstants.PUSH_STREAM_LIST + app + "_" + stream;
        return (MediaInfo)redisTemplate.opsForValue().get(key);
    }

    @Override
    public void removePushListItem(String app, String stream, String mediaServerId) {
        String key = VideoManagerConstants.PUSH_STREAM_LIST + app + "_" + stream;
        MediaInfo param = (MediaInfo)redisTemplate.opsForValue().get(key);
        if (param != null && userSetting.getServerId().equals(param.getServerId())) {
            redisTemplate.delete(key);
        }
    }

    @Override
    public void sendPushStreamClose(MessageForPushChannel msg) {
        String key = VideoManagerConstants.VM_MSG_STREAM_PUSH_CLOSE_REQUESTED;
        log.info("[redisSend notification] Send Stop pushing to superior {}: {}/{}->{}", key, msg.getApp(), msg.getStream(), msg.getPlatFormId());
        redisTemplate.convertAndSend(key, JSON.toJSON(msg));
    }

    @Override
    public void addWaiteSendRtpItem(SendRtpInfo sendRtpItem, int platformPlayTimeout) {
        String key = VideoManagerConstants.WAITE_SEND_PUSH_STREAM + sendRtpItem.getApp() + "_" + sendRtpItem.getStream();
        redisTemplate.opsForValue().set(key, sendRtpItem);
    }

    @Override
    public SendRtpInfo getWaiteSendRtpItem(String app, String stream) {
        String key = VideoManagerConstants.WAITE_SEND_PUSH_STREAM + app + "_" + stream;
        return JsonUtil.redisJsonToObject(redisTemplate, key, SendRtpInfo.class);
    }

    @Override
    public void sendStartSendRtp(SendRtpInfo sendRtpItem) {
        String key = VideoManagerConstants.START_SEND_PUSH_STREAM + sendRtpItem.getApp() + "_" + sendRtpItem.getStream();
        log.info("[redisSend notification] Notify other WVP push streams {}: {}/{}->{}", key, sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getTargetId());
        redisTemplate.convertAndSend(key, JSON.toJSON(sendRtpItem));
    }

    @Override
    public void sendPushStreamOnline(SendRtpInfo sendRtpItem) {
        String key = VideoManagerConstants.VM_MSG_STREAM_PUSH_CLOSE_REQUESTED;
        log.info("[redisSend notification] stream online {}: {}/{}->{}", key, sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getTargetId());
        redisTemplate.convertAndSend(key, JSON.toJSON(sendRtpItem));
    }

    @Override
    public ServerInfo queryServerInfo(String serverId) {
        String key = VideoManagerConstants.WVP_SERVER_PREFIX + serverId;
        return (ServerInfo)redisTemplate.opsForValue().get(key);
    }

    @Override
    public String chooseOneServer(String serverId) {
        String key = VideoManagerConstants.WVP_SERVER_LIST;
        if (serverId != null) {
            redisTemplate.opsForZSet().remove(key, serverId);
        }
        // Get the wvp with the highest score and the last updated time to redis. This can avoid reading offline wvp. At the same time, the latest time also represents the healthiest one to a certain extent.
        Set<Object> range = redisTemplate.opsForZSet().reverseRange(key, 0, 0);
        if (range == null || range.isEmpty()) {
            return null;
        }
        return (String) range.iterator().next();
    }

    @Override
    public void updateDeviceKeepaliveTimeStamp(List<Device> deviceList) {
        if (deviceList == null || deviceList.isEmpty()) {
            return;
        }
        // Use SessionCallback Ensure that batch operations are executed on the same connection
        SessionCallback<Boolean> sessionCallback = new SessionCallback<>() {
            @Override
            // Note: write it down directly here String, String Covering the interface K, V
            public Boolean execute(@NonNull RedisOperations operations) {
                // 1. Add heartbeat data to the end of the list in batches
                for (Device device : deviceList) {
                    Long timestamp = device.getKeepaliveTimeStamp();
                    if (timestamp == null) {
                        continue;
                    }
                    String key = VideoManagerConstants.DEVICE_KEEPALIVE_PREFIX + device.getDeviceId();
                    operations.opsForList().rightPush(key, timestamp);
                    // 2. Intercept the list and keep only the latest N items
                    if (userSetting.getDeviceKeepaliveTimeMaxCount() > 0) {
                        operations.opsForList().trim(key, -userSetting.getDeviceKeepaliveTimeMaxCount(), -1);
                    }
                    // 3. Set expiration time，ttlHours <= 0 then skip
                    if (userSetting.getDeviceKeepaliveTimeTtlHours() > 0) {
                        operations.expire(key, Duration.ofHours(userSetting.getDeviceKeepaliveTimeTtlHours()));
                    }
                }
                return true;
            }
        };

        longRedisTemplate.execute(sessionCallback);
    }

    @Override
    public List<Long> getDeviceKeepaliveTimeStamp(String deviceId, Integer count) {
        if (deviceId == null ) {
            return List.of();
        }
        if (count == null) {
            count = 20;
        }
        return longRedisTemplate.opsForList().range(VideoManagerConstants.DEVICE_KEEPALIVE_PREFIX + deviceId, -count - 1, -1);
    }



    @Override
    public void updateDeviceRegisterTimeStamp(List<Device> deviceList) {
        if (deviceList == null || deviceList.isEmpty()) {
            return;
        }
        // Use SessionCallback Ensure that batch operations are executed on the same connection
        SessionCallback<Boolean> sessionCallback = new SessionCallback<>() {
            @Override
            // Note: write it down directly here String, String Covering the interface K, V
            public Boolean execute(@NonNull RedisOperations operations) {
                // 1. Add registration data to the end of the list in batches
                for (Device device : deviceList) {
                    Long timestamp = device.getRegisterTimeStamp();
                    if (timestamp == null) {
                        continue;
                    }
                    String key = VideoManagerConstants.DEVICE_REGISTER_PREFIX + device.getDeviceId();
                    operations.opsForList().rightPush(key, timestamp);
                    // 2. Intercept the list and keep only the latest N items
                    if (userSetting.getDeviceRegisterTimeMaxCount() > 0) {
                        operations.opsForList().trim(key, -userSetting.getDeviceRegisterTimeMaxCount(), -1);
                    }
                    // 3. Set expiration time，ttlHours <= 0 then skip
                    if (userSetting.getDeviceRegisterTimeTtlHours() > 0) {
                        operations.expire(key, Duration.ofHours(userSetting.getDeviceRegisterTimeTtlHours()));
                    }
                }
                return true;
            }
        };
        longRedisTemplate.execute(sessionCallback);
    }

    @Override
    public List<Long> getDeviceRegisterTimeStamp(String deviceId, Integer count) {
        if (deviceId == null ) {
            return List.of();
        }
        if (count == null) {
            count = 20;
        }
        return longRedisTemplate.opsForList().range(VideoManagerConstants.DEVICE_REGISTER_PREFIX + deviceId, -count - 1, -1);
    }
}
