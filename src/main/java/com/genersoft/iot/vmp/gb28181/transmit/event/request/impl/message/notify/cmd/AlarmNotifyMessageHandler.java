package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.cmd;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.NotifyMessageHandler;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
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
 * Handling of alarm events, refer to：9.4
 */
@Slf4j
@Component
public class AlarmNotifyMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private final String cmdType = "Alarm";

    @Autowired
    private NotifyMessageHandler notifyMessageHandler;

    @Autowired
    private EventPublisher publisher;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private SipConfig sipConfig;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IDeviceChannelService deviceChannelService;

    private final ConcurrentLinkedQueue<SipMsgInfo> taskQueue = new ConcurrentLinkedQueue<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        notifyMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element rootElement) {
        if (taskQueue.size() >= userSetting.getMaxNotifyCountQueue()) {
            log.error("[Alarm] The pending message queue is full {}，Returns 486 BUSY_HERE, the message is not processed", userSetting.getMaxNotifyCountQueue());
            return;
        }
        // Reply200 OK
        try {
            responseAckAsync((SIPRequest) evt.getRequest(), Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[Command sending failed] Alarm notification reply: {}", e.getMessage());
        }
        taskQueue.offer(new SipMsgInfo(evt, device, rootElement));
    }

    @Scheduled(fixedDelay = 200)
    public void executeTaskQueue() {
        if (taskQueue.isEmpty()) {
            return;
        }
        List<SipMsgInfo> handlerCatchDataList = new ArrayList<>();
        int size = taskQueue.size();
        for (int i = 0; i < size; i++) {
            SipMsgInfo poll = taskQueue.poll();
            if (poll != null) {
                handlerCatchDataList.add(poll);
            }
        }
        if (handlerCatchDataList.isEmpty()) {
            return;
        }
        List<DeviceAlarmNotify> deviceAlarmList = new ArrayList<>();
        for (SipMsgInfo sipMsgInfo : handlerCatchDataList) {
            if (sipMsgInfo == null || sipMsgInfo.getDevice() == null) {
                continue;
            }
            RequestEvent evt = sipMsgInfo.getEvt();

            try {
                DeviceAlarmNotify deviceAlarmNotify = DeviceAlarmNotify.fromXml(sipMsgInfo.getRootElement());
                Device device = sipMsgInfo.getDevice();
                if (log.isDebugEnabled()) {
                    log.debug("[Receive alarm notification]Equipment：{}， content：{}", device.getDeviceId(), JSON.toJSONString(deviceAlarmNotify));
                }
                deviceAlarmNotify.setDeviceId(device.getDeviceId());
                deviceAlarmNotify.setDeviceName(device.getName());
                if (deviceAlarmNotify.getAlarmMethod() != null && deviceAlarmNotify.getAlarmMethod() == DeviceAlarmMethod.GPS.getVal()) {
                    DeviceChannel deviceChannel = deviceChannelService.getOne(device.getDeviceId(), deviceAlarmNotify.getChannelId());
                    if (deviceChannel == null) {
                        log.warn("[Parse alarm messages] Channel not found：{}/{}", device.getDeviceId(), deviceAlarmNotify.getChannelId());
                    } else {
                        DeviceMobilePosition mobilePosition = new DeviceMobilePosition();
                        mobilePosition.setCreateTime(DateUtil.getNow());
                        mobilePosition.setChannelId(deviceChannel.getId());
                        mobilePosition.setChannelDeviceId(deviceChannel.getDeviceId());
                        mobilePosition.setTimestamp(DateUtil.yyyy_MM_dd_HH_mm_ssToTimestampMs(deviceAlarmNotify.getAlarmTime()));
                        mobilePosition.setLongitude(deviceAlarmNotify.getLongitude());
                        mobilePosition.setLatitude(deviceAlarmNotify.getLatitude());
                        mobilePosition.setDevice(device);
                        // Send mobile location events, which will be saved to the database and sent to the superior platform.
                        publisher.mobilePositionsEventPublish(List.of(mobilePosition));
                    }
                }

                // The author uses his own judgment. If other friends need this information, they can modify it themselves, but do not mention it in the PR.
                if (deviceAlarmNotify.getAlarmMethod() != null
                        && DeviceAlarmMethod.Other.getVal() == deviceAlarmNotify.getAlarmMethod()) {
                    // Alarm information sent to the platform. Send redis notification
                    log.info("[Alarm information sent to the platform]content：{}", JSONObject.toJSONString(deviceAlarmNotify));
                    AlarmChannelMessage alarmChannelMessage = new AlarmChannelMessage();
                    alarmChannelMessage.setAlarmSn(deviceAlarmNotify.getAlarmMethod());
                    alarmChannelMessage.setAlarmDescription(deviceAlarmNotify.getAlarmDescription());
                    alarmChannelMessage.setAlarmType(deviceAlarmNotify.getAlarmType());
                    alarmChannelMessage.setGbId(deviceAlarmNotify.getChannelId());
                    redisCatchStorage.sendAlarmMsg(alarmChannelMessage);
                    continue;
                }

                if (redisCatchStorage.deviceIsOnline(sipMsgInfo.getDevice().getDeviceId())) {
                    deviceAlarmList.add(deviceAlarmNotify);
                }
            } catch (Exception e) {
                log.error("unhandled exception ", e);
                log.warn("[Receive alarm notification] Unhandled exception found, {}\r\n{}", e.getMessage(), evt.getRequest());
            }
        }
        if (deviceAlarmList.isEmpty()) {
            return;
        }
        publisher.deviceAlarmEventPublish(deviceAlarmList);
    }

    @Override
    public void handForPlatform(RequestEvent evt, Platform parentPlatform, Element rootElement) {
        log.info("received from the platform[{}]alarm notification", parentPlatform.getServerGBId());
        // Reply200 OK
        try {
            responseAck((SIPRequest) evt.getRequest(), Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[Command sending failed] National standard cascade alarm notification reply: {}", e.getMessage());
        }
        Element deviceIdElement = rootElement.element("DeviceID");
        String channelId = deviceIdElement.getText();
        DeviceAlarmNotify deviceAlarmNotify = DeviceAlarmNotify.fromXml(rootElement);
        deviceAlarmNotify.setDeviceId(parentPlatform.getServerGBId());
        deviceAlarmNotify.setDeviceName(parentPlatform.getName());
        deviceAlarmNotify.setChannelId(channelId);

        if (channelId.equals(parentPlatform.getDeviceGBId())) {
            // Alarm information sent to the platform. Send redis notification
            AlarmChannelMessage alarmChannelMessage = new AlarmChannelMessage();
            alarmChannelMessage.setAlarmSn(deviceAlarmNotify.getAlarmMethod());
            alarmChannelMessage.setAlarmDescription(deviceAlarmNotify.getAlarmDescription());
            alarmChannelMessage.setGbId(channelId);
            alarmChannelMessage.setAlarmType(deviceAlarmNotify.getAlarmType());
            redisCatchStorage.sendAlarmMsg(alarmChannelMessage);
        }
    }
}
