package com.genersoft.iot.vmp.service.redisMsg.control;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.redis.RedisRpcConfig;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcMessage;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcRequest;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcResponse;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.bean.PlatformChannel;
import com.genersoft.iot.vmp.gb28181.controller.bean.ChannelListForRpcParam;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.service.IPlatformChannelService;
import com.genersoft.iot.vmp.gb28181.service.IPlatformService;
import com.genersoft.iot.vmp.service.redisMsg.dto.RedisRpcController;
import com.genersoft.iot.vmp.service.redisMsg.dto.RedisRpcMapping;
import com.genersoft.iot.vmp.service.redisMsg.dto.RpcController;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RedisRpcController("platform")
public class RedisRpcPlatformController extends RpcController {

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private IPlatformService platformService;

    @Autowired
    private IPlatformChannelService platformChannelService;

    @Autowired
    private EventPublisher eventPublisher;


    private void sendResponse(RedisRpcResponse response){
        log.info("[redis-rpc] >> {}", response);
        response.setToId(userSetting.getServerId());
        RedisRpcMessage message = new RedisRpcMessage();
        message.setResponse(response);
        redisTemplate.convertAndSend(RedisRpcConfig.REDIS_REQUEST_CHANNEL_KEY, message);
    }

    /**
     * update
     */
    @RedisRpcMapping("update")
    public RedisRpcResponse update(RedisRpcRequest request) {
        Platform platform = JSONObject.parseObject(request.getParam().toString(), Platform.class);
        RedisRpcResponse response = request.getResponse();
        boolean update = platformService.update(platform);
        response.setStatusCode(ErrorCode.SUCCESS.getCode());
        response.setBody(Boolean.toString(update));
        return response;
    }


    /**
     * Delete
     */
    @RedisRpcMapping("delete")
    public RedisRpcResponse delete(RedisRpcRequest request) {
        Integer platformId = Integer.parseInt(request.getParam().toString());
        RedisRpcResponse response = request.getResponse();
        try {
            boolean result = platformService.delete(platformId);
            response.setStatusCode(ErrorCode.SUCCESS.getCode());
            response.setBody(Boolean.toString(result));
        }catch (Exception e) {
            response.setStatusCode(ErrorCode.SUCCESS.getCode());
            response.setBody("false");
        }
        return response;
    }
    /**
     * Active push channel
     */
    @RedisRpcMapping("pushChannel")
    public RedisRpcResponse pushChannel(RedisRpcRequest request) {
        Integer platformId = Integer.parseInt(request.getParam().toString());
        RedisRpcResponse response = request.getResponse();
        try {
            platformChannelService.pushChannel(platformId);
            response.setStatusCode(ErrorCode.SUCCESS.getCode());
            response.setBody("true");
        }catch (Exception e) {
            response.setStatusCode(ErrorCode.SUCCESS.getCode());
            response.setBody("false");
        }
        return response;
    }

    /**
     * shared channel
     */
    @RedisRpcMapping("addChannelList")
    public RedisRpcResponse addChannelList(RedisRpcRequest request) {
        ChannelListForRpcParam param = JSONObject.parseObject(request.getParam().toString(), ChannelListForRpcParam.class);
        RedisRpcResponse response = request.getResponse();
        try {
            int result = platformChannelService.addChannels(param.getPlatformId(), param.getChannelIds());
            response.setStatusCode(ErrorCode.SUCCESS.getCode());
            response.setBody(result + "");
        }catch (Exception e) {
            response.setStatusCode(ErrorCode.SUCCESS.getCode());
            response.setBody("0");
        }
        return response;
    }

    /**
     * Remove all shared channels
     */
    @RedisRpcMapping("removeAllChannel")
    public RedisRpcResponse removeAllChannel(RedisRpcRequest request) {
        Integer platformId = Integer.parseInt(request.getParam().toString());
        RedisRpcResponse response = request.getResponse();
        try {
            int result = platformChannelService.removeAllChannel(platformId);
            response.setStatusCode(ErrorCode.SUCCESS.getCode());
            response.setBody(result + "");
        }catch (Exception e) {
            response.setStatusCode(ErrorCode.SUCCESS.getCode());
            response.setBody("0");
        }
        return response;
    }

    /**
     * Cancel shared channel
     */
    @RedisRpcMapping("removeChannelList")
    public RedisRpcResponse removeChannelList(RedisRpcRequest request) {
        ChannelListForRpcParam param = JSONObject.parseObject(request.getParam().toString(), ChannelListForRpcParam.class);
        RedisRpcResponse response = request.getResponse();
        try {
            int result = platformChannelService.removeChannels(param.getPlatformId(), param.getChannelIds());
            response.setStatusCode(ErrorCode.SUCCESS.getCode());
            response.setBody(result + "");
        }catch (Exception e) {
            response.setStatusCode(ErrorCode.SUCCESS.getCode());
            response.setBody("0");
        }
        return response;
    }

    /**
     * Custom channel
     */
    @RedisRpcMapping("updateCustomChannel")
    public RedisRpcResponse updateCustomChannel(RedisRpcRequest request) {
        PlatformChannel param = JSONObject.parseObject(request.getParam().toString(), PlatformChannel.class);
        RedisRpcResponse response = request.getResponse();
        try {
            platformChannelService.updateCustomChannel(param);
            response.setStatusCode(ErrorCode.SUCCESS.getCode());
            response.setBody("true");
        }catch (Exception e) {
            response.setStatusCode(ErrorCode.SUCCESS.getCode());
            response.setBody("false");
        }
        return response;
    }

    /**
     * Directory update push
     */
    @RedisRpcMapping("catalogEventPublish")
    public RedisRpcResponse catalogEventPublish(RedisRpcRequest request) {
        JSONObject jsonObject = JSONObject.parseObject(request.getParam().toString());
        Platform platform = jsonObject.getObject("platform", Platform.class);

        List<CommonGBChannel> channels = jsonObject.getJSONArray("channels").toJavaList(CommonGBChannel.class);
        String type = jsonObject.getString("type");
        eventPublisher.catalogEventPublish(platform, channels, type);
        RedisRpcResponse response = request.getResponse();
        response.setStatusCode(ErrorCode.SUCCESS.getCode());
        return response;
    }

}
