package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.cmd;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.NotifyMessageHandler;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Mobile device location data notification is initiated by the device and does not require superior subscription.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MobilePositionNotifyMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private final NotifyMessageHandler notifyMessageHandler;

    private final ConcurrentLinkedQueue<HandlerCatchData> taskQueue = new ConcurrentLinkedQueue<>();

    private final EventPublisher eventPublisher;

    private final UserSetting userSetting;


    @Override
    public void afterPropertiesSet() throws Exception {
        String cmdType = "MobilePosition";
        notifyMessageHandler.addHandler(cmdType, this);
    }


    @Override
    public void handForDevice(RequestEvent evt, Device device, Element rootElement) {
        if (taskQueue.size() >= userSetting.getMaxNotifyCountQueue()) {
            log.error("[message-notify-Move location] The pending message queue is full {}，Return486 BUSY_HERE", userSetting.getMaxNotifyCountQueue());
            return;
        }
        taskQueue.offer(new HandlerCatchData(evt, device, rootElement));
        // Reply200 OK
        try {
            responseAckAsync((SIPRequest) evt.getRequest(), Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[Command sending failed] Mobile location notification reply: {}", e.getMessage());
        }

    }
    @Scheduled(fixedDelay = 400)   //Executed every 400 milliseconds
    @Async
    public void executeTaskQueue(){
        if (taskQueue.isEmpty()) {
            return;
        }
        List<HandlerCatchData> handlerCatchDataList = new ArrayList<>();
        int size = taskQueue.size();
        for (int i = 0; i < size; i++) {
            HandlerCatchData poll = taskQueue.poll();
            if (poll != null) {
                handlerCatchDataList.add(poll);
            }
        }
        if (handlerCatchDataList.isEmpty()) {
            return;
        }
        List<DeviceMobilePosition> mobilePositionList = new ArrayList<>();
        for (HandlerCatchData take : handlerCatchDataList) {
            if (take == null) {
                continue;
            }
            Device device = take.getDevice();
            try {
                Element rootElementAfterCharset = getRootElement(take.getEvt(), device.getCharset());
                if (rootElementAfterCharset == null) {
                    log.warn("[Mobile location notifications] {}Processing failed, the information body was not recognized", device.getDeviceId());
                    continue;
                }
                List<DeviceMobilePosition> mobilePositions = DeviceMobilePosition.decode(device, rootElementAfterCharset);
                for (DeviceMobilePosition mobilePosition : mobilePositions) {
                    try {
                        log.info("[Receive mobile location subscription notification]：{}/{}->{}.{}, time： {}", device.getDeviceId(), mobilePosition.getChannelDeviceId(),
                                mobilePosition.getLongitude(), mobilePosition.getLatitude(), mobilePosition.getTimestamp());
                        mobilePositionList.add(mobilePosition);
                    }catch (Exception e) {
                        log.error("unhandled exception ", e);
                    }
                }
            }catch (Exception e) {
                log.warn("[Mobile location notifications] Unhandled exception found, \r\n{}", take.getEvt().getRequest());
                log.error("[Mobile location notifications] Unusual content： ", e);
            }
        }
        // Send a mobile location subscription message to the upper-level platform that is associated with the channel and has enabled mobile location subscription.
        if (!mobilePositionList.isEmpty()) {
            try {
                eventPublisher.mobilePositionsEventPublish(mobilePositionList);
            }catch (Exception e) {
                log.error("[MobilePositionEvent] Sending failed：  ", e);
            }
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, Platform parentPlatform, Element element) {

    }
}
