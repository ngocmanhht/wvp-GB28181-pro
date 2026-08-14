package com.genersoft.iot.vmp.service.redisMsg.control;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.redis.RedisRpcConfig;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcMessage;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcRequest;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcResponse;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpInfo;
import com.genersoft.iot.vmp.gb28181.session.SendSsrcFactory;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.hook.Hook;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.event.hook.HookType;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.ISendRtpServerService;
import com.genersoft.iot.vmp.service.redisMsg.dto.RedisRpcController;
import com.genersoft.iot.vmp.service.redisMsg.dto.RedisRpcMapping;
import com.genersoft.iot.vmp.service.redisMsg.dto.RpcController;
import com.genersoft.iot.vmp.streamPush.service.IStreamPushPlayService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RedisRpcController("streamPush")
public class RedisRpcStreamPushController extends RpcController {

    @Autowired
    private SendSsrcFactory sendSsrcFactory;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private ISendRtpServerService sendRtpServerService;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private HookSubscribe hookSubscribe;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private IStreamPushPlayService streamPushPlayService;


    private void sendResponse(RedisRpcResponse response){
        log.info("[redis-rpc] >> {}", response);
        response.setToId(userSetting.getServerId());
        RedisRpcMessage message = new RedisRpcMessage();
        message.setResponse(response);
        redisTemplate.convertAndSend(RedisRpcConfig.REDIS_REQUEST_CHANNEL_KEY, message);
    }

    /**
     * Listening stream online
     */
    @RedisRpcMapping("waitePushStreamOnline")
    public RedisRpcResponse waitePushStreamOnline(RedisRpcRequest request) {
        SendRtpInfo sendRtpItem = JSONObject.parseObject(request.getParam().toString(), SendRtpInfo.class);
        log.info("[redis-rpc] Listening stream online： {}/{}, destination address： {}：{}", sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getIp(), sendRtpItem.getPort());
        // Query whether this stream exists at this level
        MediaServer mediaServer = mediaServerService.getMediaServerByAppAndStream(sendRtpItem.getApp(), sendRtpItem.getStream());
        if (mediaServer != null) {
            log.info("[redis-rpc] When the monitoring stream goes online, it is found that the stream already exists and returns directly.： {}/{}, destination address： {}：{}", sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getIp(), sendRtpItem.getPort() );
            // Read the superior on-demand information in redis, generate sendRtpItm and send it out
            if (sendRtpItem.getSsrc() == null) {
                sendRtpItem.setSsrc(sendSsrcFactory.getSendSsrc(
                        "Play".equalsIgnoreCase(sendRtpItem.getSessionName()) ? "0" : "1"));
            }
            sendRtpItem.setMediaServerId(mediaServer.getId());
            sendRtpItem.setLocalIp(mediaServer.getSdpIp());
            sendRtpItem.setServerId(userSetting.getServerId());

            sendRtpServerService.update(sendRtpItem);
            RedisRpcResponse response = request.getResponse();
            response.setBody(sendRtpItem.getChannelId());
            response.setStatusCode(ErrorCode.SUCCESS.getCode());
        }
        // The monitoring stream is online. When the stream goes online, it directly sends the sendRtpItem message to the actual signaling processor.
        Hook hook = Hook.getInstance(HookType.on_media_arrival, sendRtpItem.getApp(), sendRtpItem.getStream(), null);
        hookSubscribe.addSubscribe(hook, (hookData) -> {
            log.info("[redis-rpc] The listening stream is online. The stream is online.： {}/{}, destination address： {}：{}", sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getIp(), sendRtpItem.getPort());
            // Read the superior on-demand information in redis, generate sendRtpItm and send it out
            if (sendRtpItem.getSsrc() == null) {
                sendRtpItem.setSsrc(sendSsrcFactory.getSendSsrc(
                        "Play".equalsIgnoreCase(sendRtpItem.getSessionName()) ? "0" : "1"));
            }
            sendRtpItem.setMediaServerId(hookData.getMediaServer().getId());
            sendRtpItem.setLocalIp(hookData.getMediaServer().getSdpIp());
            sendRtpItem.setServerId(userSetting.getServerId());

            redisTemplate.opsForValue().set(sendRtpItem.getChannelId() + "", sendRtpItem);
            RedisRpcResponse response = request.getResponse();
            response.setBody(sendRtpItem.getChannelId());
            response.setStatusCode(ErrorCode.SUCCESS.getCode());
            // Send results manually
            sendResponse(response);
            hookSubscribe.removeSubscribe(hook);

        });
        return null;
    }

    /**
     * Listening stream online
     */
    @RedisRpcMapping("onStreamOnlineEvent")
    public RedisRpcResponse onStreamOnlineEvent(RedisRpcRequest request) {
        StreamInfo streamInfo = JSONObject.parseObject(request.getParam().toString(), StreamInfo.class);
        log.info("[redis-rpc] Monitor the flow information and wait for the flow to come online： {}/{}", streamInfo.getApp(), streamInfo.getStream());
        // Query whether this stream exists at this level
        StreamInfo streamInfoInServer = mediaServerService.getMediaByAppAndStream(streamInfo.getApp(), streamInfo.getStream());
        if (streamInfoInServer != null) {
            log.info("[redis-rpc] When the monitoring stream goes online, it is found that the stream already exists and returns directly.： {}/{}", streamInfo.getApp(), streamInfo.getStream());
            RedisRpcResponse response = request.getResponse();
            response.setBody(JSONObject.toJSONString(streamInfoInServer));
            response.setStatusCode(ErrorCode.SUCCESS.getCode());
            return response;
        }
        // The monitoring stream is online. When the stream goes online, it directly sends the sendRtpItem message to the actual signaling processor.
        Hook hook = Hook.getInstance(HookType.on_media_arrival, streamInfo.getApp(), streamInfo.getStream());
        hookSubscribe.addSubscribe(hook, (hookData) -> {
            log.info("[redis-rpc] The listening stream is online. The stream is online.： {}/{}", streamInfo.getApp(), streamInfo.getStream());
            // Read the superior on-demand information in redis, generate sendRtpItm and send it out
            RedisRpcResponse response = request.getResponse();
            StreamInfo streamInfoByAppAndStream = mediaServerService.getStreamInfoByAppAndStream(hookData.getMediaServer(),
                    streamInfo.getApp(), streamInfo.getStream(), hookData.getMediaInfo(),
                    hookData.getMediaInfo() != null ? hookData.getMediaInfo().getCallId() : null);
            response.setBody(JSONObject.toJSONString(streamInfoByAppAndStream));
            response.setStatusCode(ErrorCode.SUCCESS.getCode());
            // Send results manually
            sendResponse(response);
            hookSubscribe.removeSubscribe(hook);
        });
        return null;
    }

    /**
     * Stop monitoring the stream online
     */
    @RedisRpcMapping("stopWaitePushStreamOnline")
    public RedisRpcResponse stopWaitePushStreamOnline(RedisRpcRequest request) {
        SendRtpInfo sendRtpItem = JSONObject.parseObject(request.getParam().toString(), SendRtpInfo.class);
        log.info("[redis-rpc] Stop monitoring the stream online： {}/{}, destination address： {}：{}", sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getIp(), sendRtpItem.getPort() );
        // The monitoring stream is online. When the stream goes online, it directly sends the sendRtpItem message to the actual signaling processor.
        Hook hook = Hook.getInstance(HookType.on_media_arrival, sendRtpItem.getApp(), sendRtpItem.getStream(), null);
        hookSubscribe.removeSubscribe(hook);
        RedisRpcResponse response = request.getResponse();
        response.setStatusCode(ErrorCode.SUCCESS.getCode());
        return response;
    }

    /**
     * Stop monitoring the stream online
     */
    @RedisRpcMapping("unPushStreamOnlineEvent")
    public RedisRpcResponse unPushStreamOnlineEvent(RedisRpcRequest request) {
        StreamInfo streamInfo = JSONObject.parseObject(request.getParam().toString(), StreamInfo.class);
        log.info("[redis-rpc] Stop monitoring the stream online： {}/{}", streamInfo.getApp(), streamInfo.getStream());
        // The monitoring stream is online. When the stream goes online, it directly sends the sendRtpItem message to the actual signaling processor.
        Hook hook = Hook.getInstance(HookType.on_media_arrival, streamInfo.getApp(), streamInfo.getStream(), null);
        hookSubscribe.removeSubscribe(hook);
        RedisRpcResponse response = request.getResponse();
        response.setStatusCode(ErrorCode.SUCCESS.getCode());
        return response;
    }

    /**
     * Stop monitoring the stream online
     */
    @RedisRpcMapping("play")
    public RedisRpcResponse play(RedisRpcRequest request) {
        JSONObject paramJson = JSONObject.parseObject(request.getParam().toString());
        int id = paramJson.getInteger("id");
        RedisRpcResponse response = request.getResponse();
        if (id <= 0) {
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }
        try {
            streamPushPlayService.start(id, (code, msg, data) -> {
                if (code == ErrorCode.SUCCESS.getCode()) {
                    response.setStatusCode(ErrorCode.SUCCESS.getCode());
                    response.setBody(JSONObject.toJSONString(data));
                    sendResponse(response);
                }
            }, null, null);
        }catch (IllegalArgumentException e) {
            response.setStatusCode(ErrorCode.ERROR100.getCode());
            response.setBody(e.getMessage());
            sendResponse(response);
        }
        return null;
    }

}
