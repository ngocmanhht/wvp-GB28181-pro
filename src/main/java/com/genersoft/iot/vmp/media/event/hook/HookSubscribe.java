package com.genersoft.iot.vmp.media.event.hook;

import com.genersoft.iot.vmp.media.event.media.*;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * zlm hookevent parameters
 * @author lin
 */
@Component
public class HookSubscribe {

    /**
     * Subscription data expiration time
     */
    private final long subscribeExpire = 5 * 60 * 1000;


    @FunctionalInterface
    public interface Event{
        void response(HookData data);
    }

    /**
     * Processing of incoming streams
     */
    @Async
    @EventListener
    public void onApplicationEvent(MediaArrivalEvent event) {
        if (event.getSchema() == null || "rtsp".equals(event.getSchema())) {
            sendNotify(HookType.on_media_arrival, event);
        }

    }

    /**
     * stream end event
     */
    @Async
    @EventListener
    public void onApplicationEvent(MediaDepartureEvent event) {
        if (event.getSchema() == null || "rtsp".equals(event.getSchema())) {
            sendNotify(HookType.on_media_departure, event);
        }

    }
    /**
     * Push authentication event
     */
    @Async
    @EventListener
    public void onApplicationEvent(MediaPublishEvent event) {
        sendNotify(HookType.on_publish, event);
    }
    /**
     * Generate video file event
     */
    @Async
    @EventListener
    public void onApplicationEvent(MediaRecordMp4Event event) {
        sendNotify(HookType.on_record_mp4, event);
    }

    private final Map<String, Event> allSubscribes = new ConcurrentHashMap<>();
    private final Map<String, Hook> allHook = new ConcurrentHashMap<>();

    private void sendNotify(HookType hookType, MediaEvent event) {
        Hook paramHook = Hook.getInstance(hookType, event.getApp(), event.getStream());
        Event hookSubscribeEvent = allSubscribes.get(paramHook.toString());
        if (hookSubscribeEvent != null) {
            HookData data = HookData.getInstance(event);
            hookSubscribeEvent.response(data);
        }
    }

    public void addSubscribe(Hook hook, HookSubscribe.Event event) {
        if (hook.getExpireTime() == null) {
            hook.setExpireTime(System.currentTimeMillis() + subscribeExpire);
        }
        allSubscribes.put(hook.toString(), event);
        allHook.put(hook.toString(), hook);
    }

    public void removeSubscribe(Hook hook) {
        allSubscribes.remove(hook.toString());
        allHook.remove(hook.toString());
    }

    /**
     * Clean up expiration of subscription data
     */
    @Scheduled(fixedRate=subscribeExpire)   //Execute every 5 minutes
    public void execute(){
        long expireTime = System.currentTimeMillis();
        for (Hook hook : allHook.values()) {
            if (hook.getExpireTime() < expireTime) {
                allSubscribes.remove(hook.toString());
                allHook.remove(hook.toString());
            }
        }
    }

    public List<Hook> getAll() {
        return new ArrayList<>(allHook.values());
    }
}
