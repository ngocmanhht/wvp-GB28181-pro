package com.genersoft.iot.vmp.gb28181.event.record;

import com.genersoft.iot.vmp.gb28181.bean.RecordInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description: Video query end event
 * @author: pan
 * @data: 2022-02-23
 */
@Slf4j
@Component
public class RecordInfoEventListener implements ApplicationListener<RecordInfoEvent> {

    private final Map<String, RecordEndEventHandler> handlerMap = new ConcurrentHashMap<>();
    public interface RecordEndEventHandler{
        void  handler(RecordInfo recordInfo);
    }

    @Override
    public void onApplicationEvent(RecordInfoEvent event) {
        String deviceId = event.getRecordInfo().getDeviceId();
        String channelId = event.getRecordInfo().getChannelId();
        int count = event.getRecordInfo().getCount();
        int sumNum = event.getRecordInfo().getSumNum();
        log.info("Recording query event trigger，deviceId：{}, channelId: {}, Number of videos{}/{}Article", event.getRecordInfo().getDeviceId(),
                event.getRecordInfo().getChannelId(), count,sumNum);
        if (!handlerMap.isEmpty()) {
            RecordEndEventHandler handler = handlerMap.get(deviceId + channelId);
            log.info("Recording query event triggers, sends subscription，deviceId：{}, channelId: {}",
                    event.getRecordInfo().getDeviceId(), event.getRecordInfo().getChannelId());
            if (handler !=null){
                handler.handler(event.getRecordInfo());
                if (count ==sumNum){
                    handlerMap.remove(deviceId + channelId);
                }
            }
        }
    }

    /**
     * add
     */
    public void addEndEventHandler(String device, String channelId, RecordEndEventHandler recordEndEventHandler) {
        log.info("Add monitoring for video query events，deviceId：{}, channelId: {}", device, channelId);
        handlerMap.put(device + channelId, recordEndEventHandler);
    }
    /**
     * add
     */
    public void delEndEventHandler(String device, String channelId) {
        log.info("Recording query event removal monitoring，deviceId：{}, channelId: {}", device, channelId);
        handlerMap.remove(device + channelId);
    }

}
