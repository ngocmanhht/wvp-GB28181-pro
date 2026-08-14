package com.genersoft.iot.vmp.streamPush.service.impl;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.media.zlm.dto.StreamAuthorityInfo;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.MessageForPushChannel;
import com.genersoft.iot.vmp.service.redisMsg.IRedisRpcPlayService;
import com.genersoft.iot.vmp.service.redisMsg.IRedisRpcService;
import com.genersoft.iot.vmp.service.redisMsg.RedisPushStreamResponseListener;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.streamPush.bean.StreamPush;
import com.genersoft.iot.vmp.streamPush.dao.StreamPushMapper;
import com.genersoft.iot.vmp.streamPush.service.IStreamPushPlayService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.UUID;

@Service
@Slf4j
public class StreamPushPlayServiceImpl implements IStreamPushPlayService {

    @Autowired
    private StreamPushMapper streamPushMapper;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private IRedisRpcService redisRpcService;

    @Autowired
    private IRedisRpcPlayService redisRpcPlayService;

    @Autowired
    private RedisPushStreamResponseListener redisPushStreamResponseListener;

    @Override
    public void start(Integer id, ErrorCallback<StreamInfo> callback, String platformDeviceId, String platformName ) {
        StreamPush streamPush = streamPushMapper.queryOne(id);
        Assert.notNull(streamPush, "Push information not found");

        if (streamPush.isPushing() && !userSetting.getServerId().equals(streamPush.getServerId())) {
            redisRpcPlayService.playPush(streamPush.getServerId(), id, callback);
            return;
        }

        MediaServer mediaServer = mediaServerService.getOne(streamPush.getMediaServerId());
        if (mediaServer != null) {
            MediaInfo mediaInfo = mediaServerService.getMediaInfo(mediaServer, streamPush.getApp(), streamPush.getStream());
            if (mediaInfo != null) {
                String callId = null;
                StreamAuthorityInfo streamAuthorityInfo = redisCatchStorage.getStreamAuthorityInfo(streamPush.getApp(), streamPush.getStream());
                if (streamAuthorityInfo != null) {
                    callId = streamAuthorityInfo.getCallId();
                }
                callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), mediaServerService.getStreamInfoByAppAndStream(mediaServer,
                        streamPush.getApp(), streamPush.getStream(), mediaInfo, callId));
                if (!streamPush.isPushing()) {
                    streamPush.setPushing(true);
                    streamPushMapper.update(streamPush);
                }
                return;
            }
        }
        Assert.isTrue(streamPush.isStartOfflinePush(), "Channel is not pushed");
        // Send a redis message to bring the device online, and the stream will be
        log.info("[ app={}, stream={} ]The channel is not pushing. Send redis information to control the device to start pushing.", streamPush.getApp(), streamPush.getStream());
        MessageForPushChannel messageForPushChannel = MessageForPushChannel.getInstance(1,
                streamPush.getApp(), streamPush.getStream(), streamPush.getGbDeviceId(), platformDeviceId,
                platformName, userSetting.getServerId(), null);
        redisCatchStorage.sendStreamPushRequestedMsg(messageForPushChannel);
        // Set timeout
        String timeOutTaskKey = UUID.randomUUID().toString();
        dynamicTask.startDelay(timeOutTaskKey, () -> {
            redisRpcService.unPushStreamOnlineEvent(streamPush.getApp(), streamPush.getStream());
            log.info("[ app={}, stream={} ] Timeout waiting for the device to start streaming", streamPush.getApp(), streamPush.getStream());
            callback.run(ErrorCode.ERROR100.getCode(), "timeout", null);

        }, userSetting.getPlatformPlayTimeout());
        //
        long key = redisRpcService.onStreamOnlineEvent(streamPush.getApp(), streamPush.getStream(), (streamInfo) -> {
            dynamicTask.stop(timeOutTaskKey);
            if (streamInfo == null) {
                log.warn("Waiting for the push flow to get the result but it is not empty.： {}/{}", streamPush.getApp(), streamPush.getStream());
                callback.run(ErrorCode.ERROR100.getCode(), "fail", null);
            }else {
                callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), streamInfo);
            }
        });
        // Add reply rejection or error notifications
        // redisMessages such as： PUBLISH VM_MSG_STREAM_PUSH_RESPONSE  '{"code":1,"msg":"failed","app":"1","stream":"2"}'
        redisPushStreamResponseListener.addEvent(streamPush.getApp(), streamPush.getStream(), response -> {
            if (response.getCode() != 0) {
                dynamicTask.stop(timeOutTaskKey);
                redisRpcService.unPushStreamOnlineEvent(streamPush.getApp(), streamPush.getStream());
                redisRpcService.removeCallback(key);
                callback.run(response.getCode(), response.getMsg(), null);
            }
        });
    }

    @Override
    public void stop(String app, String stream) {
        StreamPush streamPush = streamPushMapper.selectByAppAndStream(app, stream);
        if (streamPush == null || !streamPush.isPushing()) {
            return;
        }
        String mediaServerId = streamPush.getMediaServerId();
        MediaServer mediaServer = mediaServerService.getOne(mediaServerId);
        Assert.notNull(mediaServer, "Used node not found");
        mediaServerService.closeStreams(mediaServer, app, stream);
    }

    @Override
    public void stop(Integer id) {
        StreamPush streamPush = streamPushMapper.queryOne(id);
        if (streamPush == null || !streamPush.isPushing()) {
            return;
        }
        String mediaServerId = streamPush.getMediaServerId();
        MediaServer mediaServer = mediaServerService.getOne(mediaServerId);
        Assert.notNull(mediaServer, "Used node not found");
        mediaServerService.closeStreams(mediaServer, streamPush.getApp(), streamPush.getStream());
    }
}
