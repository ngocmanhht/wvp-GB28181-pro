package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.cmd;

import com.genersoft.iot.vmp.common.RemoteAddressInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.task.deviceStatus.DeviceStatusManager;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.NotifyMessageHandler;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.utils.IpPortUtil;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * status information(heartbeat)Submit
 */
@Slf4j
@Component
public class KeepaliveNotifyMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {


    private final static String cmdType = "Keepalive";

    private final BlockingQueue<Device> taskQueue = new LinkedBlockingQueue<>();

    @Autowired
    private NotifyMessageHandler notifyMessageHandler;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private DeviceStatusManager deviceStatusManager;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Override
    public void afterPropertiesSet() throws Exception {
        notifyMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element rootElement) {
        // Reply200 OK
        try {
            responseAckAsync((SIPRequest) evt.getRequest(), Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[Command sending failed] heartbeat reply: {}", e.getMessage());
        }
        taskQueue.add(device);
        SIPRequest request = (SIPRequest) evt.getRequest();

        RemoteAddressInfo remoteAddressInfo = SipUtils.getRemoteAddressFromRequest(request, userSetting.getSipUseSourceIpAsRemoteAddress());
        if (device.getIp() == null || !device.getIp().equalsIgnoreCase(remoteAddressInfo.getIp()) || device.getPort() != remoteAddressInfo.getPort()) {
            log.info("[Heartbeat received] Address change, {}({}), {}:{}->{}:{}", device.getName(), device.getDeviceId(), device.getIp(), device.getPort(), remoteAddressInfo.getIp(), remoteAddressInfo.getPort());
            device.setPort(remoteAddressInfo.getPort());
            device.setHostAddress(IpPortUtil.concatenateIpAndPort(remoteAddressInfo.getIp(), String.valueOf(remoteAddressInfo.getPort())));
            device.setIp(remoteAddressInfo.getIp());
            device.setLocalIp(request.getLocalAddress().getHostAddress());
        }
        device.setKeepaliveTimeStamp(System.currentTimeMillis());

        if (device.isOnLine()) {
            long expiresTime = Math.min(device.getExpires(), device.getHeartBeatInterval() * device.getHeartBeatCount()) * 1000L;
            deviceStatusManager.add(device.getDeviceId(), expiresTime + System.currentTimeMillis());
        } else {
            if (userSetting.getGbDeviceOnline() == 1) {
                // Determine whether the registration of an offline device has expired
                deviceService.online(device);
            }
        }
    }
    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS)
    public void executeUpdateDeviceList() {
        log.debug("[scheduled tasks] Update heartbeat record, number of devices to be processed: {}", taskQueue.size());
        try {
            if (!taskQueue.isEmpty()) {
                redisCatchStorage.updateDeviceKeepaliveTimeStamp(taskQueue.stream().toList());
                taskQueue.clear();
            }
        } catch (Exception e) {
            log.error("[scheduled tasks] Update heartbeat record execution exception", e);
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, Platform parentPlatform, Element element) {
        // If some platforms are kept alive and do not reply with 200 OK, they will be judged as offline.
        // Reply200 OK
        try {
            responseAckAsync((SIPRequest) evt.getRequest(), Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[Command sending failed] Alarm notification reply: {}", e.getMessage());
        }
    }
}
