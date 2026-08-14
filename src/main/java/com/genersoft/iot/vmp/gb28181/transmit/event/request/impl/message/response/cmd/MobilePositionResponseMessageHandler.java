package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.cmd;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceMobilePosition;
import com.genersoft.iot.vmp.gb28181.bean.HandlerCatchData;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.ResponseMessageHandler;
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
 * Mobile device location data query responses
 * @author lin
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MobilePositionResponseMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private final String cmdType = "MobilePosition";

    private final ResponseMessageHandler responseMessageHandler;

    private final EventPublisher eventPublisher;

    private final UserSetting userSetting;

    private final DeferredResultHolder resultHolder;

    private final ConcurrentLinkedQueue<HandlerCatchData> taskQueue = new ConcurrentLinkedQueue<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        responseMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element rootElement) {
        if (taskQueue.size() >= userSetting.getMaxNotifyCountQueue()) {
            log.error("[Mobile device location query responses] The pending message queue is full {}，discard message", userSetting.getMaxNotifyCountQueue());
            return;
        }
        taskQueue.offer(new HandlerCatchData(evt, device, rootElement));
        try {
            responseAckAsync((SIPRequest) evt.getRequest(), Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[Command sending failed] Mobile device location data query 200: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 400)
    @Async
    public void executeTaskQueue() {
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
            Device device = take.getDevice();
            try {
                Element rootElementAfterCharset = getRootElement(take.getEvt(), device.getCharset());
                if (rootElementAfterCharset == null) {
                    log.warn("[Mobile device location query responses] {}Processing failed, the information body was not recognized", device.getDeviceId());
                    continue;
                }
                List<DeviceMobilePosition> mobilePositions = DeviceMobilePosition.decode(device, rootElementAfterCharset);
                for (DeviceMobilePosition mobilePosition : mobilePositions) {
                    log.info("[Receive mobile location query reply]：{}/{}->{}.{}, time： {}", device.getDeviceId(), mobilePosition.getChannelDeviceId(),
                            mobilePosition.getLongitude(), mobilePosition.getLatitude(), mobilePosition.getTimestamp());
                    mobilePositionList.add(mobilePosition);
                    String key = DeferredResultHolder.CALLBACK_CMD_MOBILE_POSITION + device.getDeviceId();
                    RequestMessage msg = new RequestMessage();
                    msg.setKey(key);
                    msg.setData(mobilePosition);
                    resultHolder.invokeAllResult(msg);
                }
            } catch (Exception e) {
                log.warn("[Mobile device location query responses] Unhandled exception found, \r\n{}", take.getEvt().getRequest());
                log.error("[Mobile device location query responses] Unusual content： ", e);
            }
        }
        if (!mobilePositionList.isEmpty()) {
            try {
                eventPublisher.mobilePositionsEventPublish(mobilePositionList);
            } catch (Exception e) {
                log.error("[MobilePositionEvent] Sending failed：  ", e);
            }
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, Platform parentPlatform, Element element) {

    }
}
