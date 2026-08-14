package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.cmd;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.ResponseMessageHandler;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.getText;

/**
 * @author lin
 */
@Slf4j
@Component
public class DeviceInfoResponseMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private final String cmdType = "DeviceInfo";

    @Autowired
    private ResponseMessageHandler responseMessageHandler;


    @Autowired
    private IDeviceService deviceService;

    @Override
    public void afterPropertiesSet() throws Exception {
        responseMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element rootElement) {
        log.debug("DeviceInfo response message received");
        // Check whether the device exists. If it does not exist, do not reply.
        if (device == null || !device.isOnLine()) {
            log.warn("[DeviceInfo response message received, but the device is offline]：" + (device != null ? device.getDeviceId():"" ));
            return;
        }
        SIPRequest request = (SIPRequest) evt.getRequest();
        try {
            rootElement = getRootElement(evt, device.getCharset());

            if (rootElement == null) {
                log.warn("[ DeviceInfo response message received ] content cannot be null, {}", evt.getRequest());
                try {
                    responseAck((SIPRequest) evt.getRequest(), Response.BAD_REQUEST);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[Command sending failed] DeviceInforeply message BAD_REQUEST: {}", e.getMessage());
                }
                return;
            }
            device.setName(getText(rootElement, "DeviceName"));

            device.setManufacturer(getText(rootElement, "Manufacturer"));
            device.setModel(getText(rootElement, "Model"));
            device.setFirmware(getText(rootElement, "Firmware"));
            if (ObjectUtils.isEmpty(device.getStreamMode())) {
                device.setStreamMode("TCP-PASSIVE");
            }
            deviceService.updateDevice(device);
            responseMessageHandler.handMessageEvent(rootElement, device);

        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }
        try {
            // Reply200 OK
            responseAckAsync(request, Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[Command sending failed] DeviceInforeply message 200: {}", e.getMessage());
        }

    }

    @Override
    public void handForPlatform(RequestEvent evt, Platform parentPlatform, Element rootElement) {

    }
}
