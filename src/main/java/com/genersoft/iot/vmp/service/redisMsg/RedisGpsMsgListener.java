package com.genersoft.iot.vmp.service.redisMsg;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.streamPush.service.IStreamPushService;
import com.genersoft.iot.vmp.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Receive GPS update notifications from redis
 *
 * @author lin
 * monitor：  SUBSCRIBE VM_MSG_GPS
 * publish   PUBLISH VM_MSG_GPS '{"messageId":"1727228507555","id":"24212345671381000047","lng":116.30307666666667,"lat":40.03295833333333,"time":"2024-09-25T09:41:47","direction":"56.0","speed":0.0,"altitude":60.0,"unitNo":"100000000","memberNo":"10000047"}'
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisGpsMsgListener implements MessageListener {

    private final IRedisCatchStorage redisCatchStorage;

    private final IGbChannelService channelService;

    private final ConcurrentLinkedQueue<Message> taskQueue = new ConcurrentLinkedQueue<>();


    @Override
    public void onMessage(@NotNull Message message, byte[] bytes) {
        log.debug("[REDIS: GPS]： {}", new String(message.getBody()));
        taskQueue.offer(message);
    }

    @Scheduled(fixedDelay = 200, timeUnit = TimeUnit.MILLISECONDS)   //Executed every 400 milliseconds
    public void executeTaskQueue() {
        if (taskQueue.isEmpty()) {
            return;
        }
        List<Message> messageDataList = new ArrayList<>();
        int size = taskQueue.size();
        for (int i = 0; i < size; i++) {
            Message msg = taskQueue.poll();
            if (msg != null) {
                messageDataList.add(msg);
            }
        }
        if (messageDataList.isEmpty()) {
            return;
        }
        for (Message msg : messageDataList) {
            try {
                GPSMsgInfo gpsMsgInfo = JSON.parseObject(msg.getBody(), GPSMsgInfo.class);
                gpsMsgInfo.setStored(false);
                gpsMsgInfo.setTime(DateUtil.ISO8601Toyyyy_MM_dd_HH_mm_ss(gpsMsgInfo.getTime()));
                log.debug("[REDISlocation change notifications], {}", JSON.toJSONString(gpsMsgInfo));
                // Just put it into redis and cache it
                redisCatchStorage.updateGpsMsgInfo(gpsMsgInfo);
            } catch (Exception e) {
                log.warn("[REDISlocation change notifications] Unhandled exception found, \r\n{}", JSON.toJSONString(msg));
                log.error("[REDISlocation change notifications] Unusual content： ", e);
            }
        }
    }

    /**
     * Update the latitude and longitude to the database regularly
     */
    @Scheduled(fixedDelay = 2, timeUnit = TimeUnit.SECONDS)   //Executed every 2 seconds
    public void execute() {
        // Need to find out
        List<GPSMsgInfo> gpsMsgInfoList = redisCatchStorage.getAllGpsMsgInfo();
        if (!gpsMsgInfoList.isEmpty()) {
            gpsMsgInfoList = gpsMsgInfoList.stream().filter(gpsMsgInfo -> !gpsMsgInfo.isStored()).collect(Collectors.toList());;
            if (!gpsMsgInfoList.isEmpty()) {
                channelService.updateGPSFromGPSMsgInfo(gpsMsgInfoList);
                for (GPSMsgInfo msgInfo : gpsMsgInfoList) {
                    msgInfo.setStored(true);
                    redisCatchStorage.updateGpsMsgInfo(msgInfo);
                }
            }
        }
    }
}
