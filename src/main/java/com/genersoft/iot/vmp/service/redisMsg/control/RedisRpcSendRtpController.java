package com.genersoft.iot.vmp.service.redisMsg.control;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcRequest;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcResponse;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpInfo;
import com.genersoft.iot.vmp.gb28181.session.SendSsrcFactory;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.ISendRtpServerService;
import com.genersoft.iot.vmp.service.redisMsg.dto.RedisRpcController;
import com.genersoft.iot.vmp.service.redisMsg.dto.RedisRpcMapping;
import com.genersoft.iot.vmp.service.redisMsg.dto.RpcController;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RedisRpcController("sendRtp")
public class RedisRpcSendRtpController extends RpcController {

    @Autowired
    private SendSsrcFactory sendSsrcFactory;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private ISendRtpServerService sendRtpServerService;

    @Autowired
    private UserSetting userSetting;


    /**
     * Get streaming information
     */
    @RedisRpcMapping("getSendRtpItem")
    public RedisRpcResponse getSendRtpItem(RedisRpcRequest request) {
        String callId = request.getParam().toString();
        SendRtpInfo sendRtpItem = sendRtpServerService.queryByCallId(callId);
        if (sendRtpItem == null) {
            log.info("[redis-rpc] Obtain streaming information, the streaming information in redis was not found， callId：{}", callId);
            RedisRpcResponse response = request.getResponse();
            response.setStatusCode(ErrorCode.SUCCESS.getCode());
            return response;
        }
        log.info("[redis-rpc] Get streaming information： {}/{}, destination address： {}：{}", sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getIp(), sendRtpItem.getPort());
        // Query whether this stream exists at this level
        MediaServer mediaServerItem = mediaServerService.getMediaServerByAppAndStream(sendRtpItem.getApp(), sendRtpItem.getStream());
        if (mediaServerItem == null) {
            RedisRpcResponse response = request.getResponse();
            response.setStatusCode(ErrorCode.SUCCESS.getCode());
            return response;
        }
        // Self-platform content
        int localPort = sendRtpServerService.getNextPort(mediaServerItem);
        if (localPort <= 0) {
            log.info("[redis-rpc] getSendRtpItem->Insufficient server port resources" );
            RedisRpcResponse response = request.getResponse();
            response.setStatusCode(ErrorCode.SUCCESS.getCode());
            return response;
        }
        // Write to redis, reply when timeout
        sendRtpItem.setStatus(1);
        sendRtpItem.setServerId(userSetting.getServerId());
        sendRtpItem.setLocalIp(mediaServerItem.getSdpIp());
        if (sendRtpItem.getSsrc() == null) {
            sendRtpItem.setSsrc(sendSsrcFactory.getSendSsrc(
                    "Play".equalsIgnoreCase(sendRtpItem.getSessionName()) ? "0" : "1"));
        }
        sendRtpServerService.update(sendRtpItem);
        RedisRpcResponse response = request.getResponse();
        response.setStatusCode(ErrorCode.SUCCESS.getCode());
        response.setBody(callId);
        return response;
    }

    /**
     * Start streaming
     */
    @RedisRpcMapping("startSendRtp")
    public RedisRpcResponse startSendRtp(RedisRpcRequest request) {
        String callId = request.getParam().toString();
        SendRtpInfo sendRtpItem = sendRtpServerService.queryByCallId(callId);
        RedisRpcResponse response = request.getResponse();
        response.setStatusCode(ErrorCode.SUCCESS.getCode());
        if (sendRtpItem == null) {
            log.info("[redis-rpc] Start streaming, no streaming information found in redis， callId：{}", callId);
            WVPResult wvpResult = WVPResult.fail(ErrorCode.ERROR100.getCode(), "The streaming information in redis was not found");
            response.setBody(wvpResult);
            return response;
        }
        log.info("[redis-rpc] Start streaming： {}/{}, destination address： {}：{}", sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getIp(), sendRtpItem.getPort());
        MediaServer mediaServer = mediaServerService.getOne(sendRtpItem.getMediaServerId());
        if (mediaServer == null) {
            log.info("[redis-rpc] startSendRtp->not foundMediaServer： {}", sendRtpItem.getMediaServerId() );
            clearSendRtpItem(sendRtpItem);
            WVPResult wvpResult = WVPResult.fail(ErrorCode.ERROR100.getCode(), "not foundMediaServer");
            response.setBody(wvpResult);
            return response;
        }
        MediaInfo mediaInfo = mediaServerService.getMediaInfo(mediaServer, sendRtpItem.getApp(), sendRtpItem.getStream());
        if (mediaInfo == null) {
            log.info("[redis-rpc] startSendRtp->Stream not online： {}/{}", sendRtpItem.getApp(), sendRtpItem.getStream() );
            clearSendRtpItem(sendRtpItem);
            WVPResult wvpResult = WVPResult.fail(ErrorCode.ERROR100.getCode(), "Stream not online");
            response.setBody(wvpResult);
            return response;
        }
        try {
            mediaServerService.startSendRtp(mediaServer, sendRtpItem);
        }catch (ControllerException exception) {
            log.info("[redis-rpc] Failed to send stream： {}/{}, destination address： {}：{}， {}", sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getIp(), sendRtpItem.getPort(), exception.getMsg());
            clearSendRtpItem(sendRtpItem);
            WVPResult wvpResult = WVPResult.fail(exception.getCode(), exception.getMsg());
            response.setBody(wvpResult);
            return response;
        }
        log.info("[redis-rpc] Flow successfully： {}/{}, destination address： {}：{}", sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getIp(), sendRtpItem.getPort());
        WVPResult wvpResult = WVPResult.success();
        response.setBody(wvpResult);
        return response;
    }

    /**
     * Stop streaming
     */
    @RedisRpcMapping("stopSendRtp")
    public RedisRpcResponse stopSendRtp(RedisRpcRequest request) {
        String callId = request.getParam().toString();
        SendRtpInfo sendRtpItem = sendRtpServerService.queryByCallId(callId);
        RedisRpcResponse response = request.getResponse();
        response.setStatusCode(ErrorCode.SUCCESS.getCode());
        if (sendRtpItem == null) {
            log.info("[redis-rpc] Stop pushing, no streaming information found in redis， key：{}", callId);
            WVPResult wvpResult = WVPResult.fail(ErrorCode.ERROR100.getCode(), "The streaming information in redis was not found");
            response.setBody(wvpResult);
            return response;
        }
        log.info("[redis-rpc] Stop pushing： {}/{}, destination address： {}：{}", sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getIp(), sendRtpItem.getPort() );
        MediaServer mediaServer = mediaServerService.getOne(sendRtpItem.getMediaServerId());
        if (mediaServer == null) {
            log.info("[redis-rpc] stopSendRtp->not foundMediaServer： {}", sendRtpItem.getMediaServerId() );
            clearSendRtpItem(sendRtpItem);
            WVPResult wvpResult = WVPResult.fail(ErrorCode.ERROR100.getCode(), "not foundMediaServer");
            response.setBody(wvpResult);
            return response;
        }
        try {
            mediaServerService.stopSendRtp(mediaServer, sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getSsrc());
        }catch (ControllerException exception) {
            log.info("[redis-rpc] Failed to stop streaming： {}/{}, destination address： {}：{}， code： {}, msg: {}", sendRtpItem.getApp(),
                    sendRtpItem.getStream(), sendRtpItem.getIp(), sendRtpItem.getPort(), exception.getCode(), exception.getMsg() );
            response.setBody(WVPResult.fail(exception.getCode(), exception.getMsg()));
            return response;
        }
        clearSendRtpItem(sendRtpItem);
        log.info("[redis-rpc] Stop pushing successfully： {}/{}, destination address： {}：{}", sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getIp(), sendRtpItem.getPort() );
        response.setBody(WVPResult.success());
        return response;
    }

    private void clearSendRtpItem(SendRtpInfo sendRtpItem) {
        if (sendRtpItem == null) {
            return;
        }
        sendRtpServerService.delete(sendRtpItem);

    }

}
