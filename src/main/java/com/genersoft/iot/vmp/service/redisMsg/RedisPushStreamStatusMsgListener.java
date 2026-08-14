package com.genersoft.iot.vmp.service.redisMsg;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.service.bean.PushStreamStatusChangeFromRedisDto;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.streamPush.service.IStreamPushService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * Receive online and offline notifications of streaming devices sent by redis
 *
 * @author lin
 * send PUBLISH VM_MSG_PUSH_STREAM_STATUS_CHANGE '{"setAllOffline":false,"offlineStreams":[{"app":"1000","stream":"10000022","timeStamp":1726729716551}]}'
 * Subscribe SUBSCRIBE VM_MSG_PUSH_STREAM_STATUS_CHANGE
 */
@Slf4j
@Component
public class RedisPushStreamStatusMsgListener implements MessageListener, ApplicationRunner {

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IStreamPushService streamPushService;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private UserSetting userSetting;

    private final ConcurrentLinkedQueue<Message> taskQueue = new ConcurrentLinkedQueue<>();

    @Override
    public void onMessage(Message message, byte[] bytes) {
        log.info("[REDIS: Push device status changes]： {}", new String(message.getBody()));
        taskQueue.offer(message);
    }

    @Scheduled(fixedDelay = 100)
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
                PushStreamStatusChangeFromRedisDto streamStatusMessage = JSON.parseObject(msg.getBody(), PushStreamStatusChangeFromRedisDto.class);
                if (streamStatusMessage == null) {
                    log.warn("[REDISnews]Failed to parse push device status change message");
                    continue;
                }
                // Cancel scheduled tasks
                dynamicTask.stop(VideoManagerConstants.VM_MSG_GET_ALL_ONLINE_REQUESTED);
                if (streamStatusMessage.isSetAllOffline()) {
                    // All devices offline
                    streamPushService.allOfflineForRedisMsg();
                }
                if (streamStatusMessage.getOfflineStreams() != null
                        && !streamStatusMessage.getOfflineStreams().isEmpty()) {
                    // Update some devices offline
                    log.info("[REDIS: Push device status changes] Update some devices offline： {}a", streamStatusMessage.getOfflineStreams().size());
                    streamPushService.offlineforRedisMsg(streamStatusMessage.getOfflineStreams());
                }
                if (streamStatusMessage.getOnlineStreams() != null &&
                        !streamStatusMessage.getOnlineStreams().isEmpty()) {
                    // Update some devices online
                    log.info("[REDIS: Push device status changes] Update some devices online： {}a", streamStatusMessage.getOnlineStreams().size());
                    streamPushService.onlineForRedisMsg(streamStatusMessage.getOnlineStreams());
                }
            } catch (Exception e) {
                log.warn("[REDISnews-Push device status changes] Unhandled exception found, \r\n{}", JSON.parseObject(msg.getBody()));
                log.error("[REDISnews-Push device status changes] Unusual content： ", e);
            }
        }
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (userSetting.getUsePushingAsStatus()) {
            return;
        }
        // Query whether there is a streaming device, if not, it will not be sent.
        List<String> allAppAndStream = streamPushService.getAllAppAndStream();
        if (allAppAndStream == null || allAppAndStream.isEmpty()) {
            return;
        }
        //  Set all push channels offline at startup and initiate a query request
        redisCatchStorage.sendStreamPushRequestedMsgForStatus();
        dynamicTask.startDelay(VideoManagerConstants.VM_MSG_GET_ALL_ONLINE_REQUESTED, () -> {
            log.info("[REDISnews]The push device status was not received from redis, and the push device was offline.");
            // If no request is received within five seconds, set the channel offline and then notify the superior to go offline.
            streamPushService.allOfflineForRedisMsg();
        }, 5000);
    }

}
