package com.genersoft.iot.vmp.streamProxy.service.impl;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.hook.Hook;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.event.hook.HookType;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.service.redisMsg.IRedisRpcPlayService;
import com.genersoft.iot.vmp.streamProxy.bean.StreamProxy;
import com.genersoft.iot.vmp.streamProxy.dao.StreamProxyMapper;
import com.genersoft.iot.vmp.streamProxy.service.IStreamProxyPlayService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.util.UUID;

/**
 * Video agency business
 */
@Slf4j
@Service
public class StreamProxyPlayServiceImpl implements IStreamProxyPlayService {

    @Autowired
    private StreamProxyMapper streamProxyMapper;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private HookSubscribe subscribe;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private IRedisRpcPlayService redisRpcPlayService;

    @Override
    public void start(int id, Boolean record, ErrorCallback<StreamInfo> callback) {
        log.info("[Streaming agent]， Start streaming，ID：{}", id);
        StreamProxy streamProxy = streamProxyMapper.select(id);
        if (streamProxy == null) {
            throw new ControllerException(ErrorCode.ERROR404.getCode(), "Agent information not found");
        }
        log.info("[Streaming agent] Type： {}， app：{}, stream: {}, stream address： {}", streamProxy.getType(), streamProxy.getApp(), streamProxy.getStream(), streamProxy.getSrcUrl());
        if (record != null) {
            streamProxy.setEnableMp4(record);
        }

        startProxy(streamProxy, callback);
    }

    @Override
    public void startProxy(@NotNull StreamProxy streamProxy, ErrorCallback<StreamInfo> callback){
        if (!streamProxy.isEnable()) {
            callback.run(ErrorCode.ERROR100.getCode(), "Proxy not enabled", null);
            return;
        }
        if (streamProxy.getServerId() == null) {
            streamProxy.setServerId(userSetting.getServerId());
        }
        if (!userSetting.getServerId().equals(streamProxy.getServerId())) {
            log.info("[Streaming agent] by other services{}management", streamProxy.getServerId());
            redisRpcPlayService.playProxy(streamProxy.getServerId(), streamProxy.getId(), callback);
            return;
        }

        if (streamProxy.getMediaServerId() != null) {
            StreamInfo streamInfo = mediaServerService.getStreamInfoByAppAndStreamWithCheck(streamProxy.getApp(), streamProxy.getStream(), streamProxy.getMediaServerId(), null, false);
            if (streamInfo != null) {
                callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), streamInfo);
                return;
            }
        }

        MediaServer mediaServer;
        String mediaServerId = streamProxy.getRelatesMediaServerId();
        if (mediaServerId == null) {
            mediaServer = mediaServerService.getMediaServerForMinimumLoad(null);
        }else {
            mediaServer = mediaServerService.getOne(mediaServerId);
        }
        if (mediaServer == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), mediaServerId == null?"No available media node found":"Node not found" + mediaServerId);
        }

        // Set a scheduled task for stream timeout
        String timeOutTaskKey = UUID.randomUUID().toString();
        Hook rtpHook = Hook.getInstance(HookType.on_media_arrival, streamProxy.getApp(), streamProxy.getStream(), mediaServer.getId());
        dynamicTask.startDelay(timeOutTaskKey, () -> {
            log.info("[Streaming agent] Traffic collection timeout，app：{}，stream: {}", streamProxy.getApp(), streamProxy.getStream());
            // Traffic collection timeout
            subscribe.removeSubscribe(rtpHook);
            callback.run(InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getCode(), InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getMsg(), null);
        }, userSetting.getPlayTimeout());

        // Enable monitoring of incoming streams
        subscribe.addSubscribe(rtpHook, (hookData) -> {
            log.info("[Streaming agent] Successfully collected traffic，app：{}，stream: {}", hookData.getApp(), hookData.getStream());
            dynamicTask.stop(timeOutTaskKey);
            StreamInfo streamInfo = mediaServerService.getStreamInfoByAppAndStream(mediaServer, hookData.getApp(), hookData.getStream(), hookData.getMediaInfo(), null);
            // hookresponse
            callback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
            subscribe.removeSubscribe(rtpHook);
            streamProxy.setPulling(true);
            streamProxyMapper.updateStream(streamProxy);
        });

        String key = mediaServerService.startProxy(mediaServer, streamProxy);
        streamProxy.setStreamKey(key);
        streamProxy.setMediaServerId(mediaServer.getId());
        streamProxyMapper.updateStream(streamProxy);
    }

    @Override
    public void stop(int id) {
        StreamProxy streamProxy = streamProxyMapper.select(id);
        if (streamProxy == null) {
            throw new ControllerException(ErrorCode.ERROR404.getCode(), "Agent information not found");
        }
        if (!userSetting.getServerId().equals(streamProxy.getServerId())) {
            redisRpcPlayService.stopProxy(streamProxy.getServerId(), streamProxy.getId());
            return;
        }
        stopProxy(streamProxy);
    }

    @Override
    public void stopProxy(StreamProxy streamProxy){

        String mediaServerId = streamProxy.getMediaServerId();
        Assert.notNull(mediaServerId, "Agent node does not exist");

        MediaServer mediaServer = mediaServerService.getOne(mediaServerId);
        if (mediaServer == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Media node does not exist");
        }
        if (ObjectUtils.isEmpty(streamProxy.getStreamKey())) {
            mediaServerService.closeStreams(mediaServer, streamProxy.getApp(), streamProxy.getStream());
        }else {
            mediaServerService.stopProxy(mediaServer, streamProxy.getStreamKey(), streamProxy.getType());
        }
        streamProxyMapper.removeStream(streamProxy.getId());
    }

}
