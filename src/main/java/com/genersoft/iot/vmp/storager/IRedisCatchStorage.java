package com.genersoft.iot.vmp.storager;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.ServerInfo;
import com.genersoft.iot.vmp.common.SystemAllInfo;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.zlm.dto.StreamAuthorityInfo;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.service.bean.MessageForPushChannel;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IRedisCatchStorage {

    /**
     * counter. count for cseq
     *
     * @return
     */
    Long getCSEQ();

    /**
     * Add wvp information in redis
     */
    void updateWVPInfo(ServerInfo serverInfo, int time);

    void removeOfflineWVPInfo(String serverId);

    /**
     * Send push flow generation and push flow disappearance messages
     * @param jsonObject Message content
     */
    void sendStreamChangeMsg(String type, JSONObject jsonObject);

    /**
     * Send alarm message
     * @param msg Message content
     */
    void sendAlarmMsg(AlarmChannelMessage msg);

    /**
     * Add flow information toredis
     * @param mediaServerItem
     * @param app
     * @param streamId
     */
    void addStream(MediaServer mediaServerItem, String type, String app, String streamId, MediaInfo item);

    /**
     * Remove flow information fromredis
     * @param mediaServerId
     * @param app
     * @param streamId
     */
    void removeStream(String mediaServerId, String type, String app, String streamId);


    /**
     * Remove flow information fromredis
     * @param mediaServerId
     */
    void removeStream(String mediaServerId, String type);

    List<MediaInfo> getStreams(String mediaServerId, String pull);

    /**
     * Write device informationredis
     * @param device
     */
    void updateDevice(Device device);

    void removeDevice(String deviceId);

    /**
     * getDevice
     */
    Device getDevice(String deviceId);

    /**
     * getDevice
     */
    List<Device> getDeviceList(Set<String> deviceIds);

    void resetAllCSEQ();

    void updateGpsMsgInfo(GPSMsgInfo gpsMsgInfo);

    GPSMsgInfo getGpsMsgInfo(String gbId);

    List<GPSMsgInfo> getAllGpsMsgInfo();

    MediaInfo getStreamInfo(String app, String streamId, String mediaServerId);

    MediaInfo getProxyStream(String app, String streamId);

    void addCpuInfo(double cpuInfo);

    void addMemInfo(double memInfo);

    void addNetInfo(Map<String, Double> networkInterfaces);

    void sendStreamPushRequestedMsg(MessageForPushChannel messageForPushChannel);

    /**
     * Determine device status
     */
    boolean deviceIsOnline(String deviceId);

    /**
     * Store authentication information for push streaming
     * @param app Application name
     * @param stream flow
     * @param streamAuthorityInfo Authentication information
     */
    void updateStreamAuthorityInfo(String app, String stream, StreamAuthorityInfo streamAuthorityInfo);

    /**
     * Remove authentication information for push streaming
     * @param app Application name
     * @param streamId flow
     */
    void removeStreamAuthorityInfo(String app, String streamId);

    /**
     * Obtain authentication information for push streaming
     * @param app Application name
     * @param stream flow
     * @return
     */
    StreamAuthorityInfo getStreamAuthorityInfo(String app, String stream);

    List<StreamAuthorityInfo> getAllStreamAuthorityInfo();

    /**
     * Send a redis message to query the status of all streaming devices
     */
    void sendStreamPushRequestedMsgForStatus();

    SystemAllInfo getSystemInfo();

    int getPushStreamCount(String id);

    int getProxyStreamCount(String id);

    int getGbSendCount(String id);

    void addDiskInfo(List<Map<String, Object>> diskInfo);

    List<SendRtpInfo> queryAllSendRTPServer();

    List<Device> getAllDevices();

    void removeAllDevice();

    void sendDeviceOrChannelStatus(String deviceId, String channelId, boolean online);

    void sendChannelAddOrDelete(String deviceId, String channelId, boolean add);

    void sendPlatformStartPlayMsg(SendRtpInfo sendRtpItem, DeviceChannel channel, Platform platform);

    void sendPlatformStopPlayMsg(SendRtpInfo sendRtpItem, Platform platform, CommonGBChannel channel);

    void addPushListItem(String app, String stream, MediaInfo param);

    MediaInfo getPushListItem(String app, String stream);

    void removePushListItem(String app, String stream, String mediaServerId);

    void sendPushStreamClose(MessageForPushChannel messageForPushChannel);

    void addWaiteSendRtpItem(SendRtpInfo sendRtpItem, int platformPlayTimeout);

    SendRtpInfo getWaiteSendRtpItem(String app, String stream);

    void sendStartSendRtp(SendRtpInfo sendRtpItem);

    void sendPushStreamOnline(SendRtpInfo sendRtpItem);

    ServerInfo queryServerInfo(String serverId);

    String chooseOneServer(String serverId);

    void updateDeviceKeepaliveTimeStamp(List<Device> deviceList);

    List<Long> getDeviceKeepaliveTimeStamp(String deviceId, Integer count);

    void updateDeviceRegisterTimeStamp(List<Device> deviceList);

    List<Long> getDeviceRegisterTimeStamp(String deviceId, Integer count);
}
