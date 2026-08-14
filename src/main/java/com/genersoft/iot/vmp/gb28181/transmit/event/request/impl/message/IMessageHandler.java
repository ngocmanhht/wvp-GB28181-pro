package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import org.dom4j.Element;

import javax.sip.RequestEvent;

public interface IMessageHandler {
    /**
     * Process information from devices
     * @param evt
     * @param device
     */
    void handForDevice(RequestEvent evt, Device device, Element element);

    /**
     * Process information from the platform
     * @param evt
     * @param parentPlatform
     */
    void handForPlatform(RequestEvent evt, Platform parentPlatform, Element element);
}
