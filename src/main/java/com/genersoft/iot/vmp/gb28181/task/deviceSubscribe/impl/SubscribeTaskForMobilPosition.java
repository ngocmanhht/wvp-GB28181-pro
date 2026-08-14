package com.genersoft.iot.vmp.gb28181.task.deviceSubscribe.impl;

import com.genersoft.iot.vmp.common.SubscribeCallback;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.SipTransactionInfo;
import com.genersoft.iot.vmp.gb28181.task.deviceSubscribe.SubscribeTask;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SubscribeTaskForMobilPosition extends SubscribeTask {

    public static final String name = "mobilPosition";

    public static SubscribeTask getInstance(Device device, SubscribeCallback callback, SipTransactionInfo transactionInfo) {
        if (device.getSubscribeCycleForMobilePosition() <= 0) {
            return null;
        }
        SubscribeTaskForMobilPosition subscribeTaskForMobilPosition = new SubscribeTaskForMobilPosition();
        subscribeTaskForMobilPosition.setDelayTime((device.getSubscribeCycleForMobilePosition() * 1000L - 500L)  + System.currentTimeMillis());
        subscribeTaskForMobilPosition.setDeviceId(device.getDeviceId());
        subscribeTaskForMobilPosition.setCallback(callback);
        subscribeTaskForMobilPosition.setTransactionInfo(transactionInfo);
        return subscribeTaskForMobilPosition;
    }

    @Override
    public void expired() {
        if (super.getCallback() == null) {
            log.info("[Device subscription expires] Mobile location subscription expiry processing callback not found, number： {}", getDeviceId());
            return;
        }
        getCallback().run(getDeviceId(), getTransactionInfo());
    }

    @Override
    public String getKey() {
        return String.format("%s_%s", name, getDeviceId());
    }

    @Override
    public String getName() {
        return name;
    }

    public static String getKey(Device device) {
        return String.format("%s_%s", SubscribeTaskForMobilPosition.name, device.getDeviceId());
    }
}
