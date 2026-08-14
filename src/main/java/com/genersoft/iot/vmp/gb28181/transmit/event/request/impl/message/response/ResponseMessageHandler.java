package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.MessageHandlerAbstract;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.MessageRequestProcessor;
import org.dom4j.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;

/**
 * Command type: Response to request action
 * Command type: device control, alarm notification, device directory information query, directory information query, directory received, device information query, device status information query ......
 */
@Component
public class ResponseMessageHandler extends MessageHandlerAbstract implements InitializingBean  {

    private final String messageType = "Response";

    @Autowired
    private MessageRequestProcessor messageRequestProcessor;



    @Override
    public void afterPropertiesSet() throws Exception {
        messageRequestProcessor.addHandler(messageType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element element) {
        super.handForDevice(evt, device, element);
    }
}
