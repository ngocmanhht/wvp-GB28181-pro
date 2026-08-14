package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify;

import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.MessageHandlerAbstract;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.MessageRequestProcessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Command type: Notification command, see A.2.5 Notification command
 * Command type: status information(heartbeat)Reporting, alarm notifications, media notifications, mobile device location data, voice broadcast notifications(TODO), Equipment preset position(TODO)
 * @author lin
 */
@Component
public class NotifyMessageHandler extends MessageHandlerAbstract implements InitializingBean  {

    private final String messageType = "Notify";

    @Autowired
    private MessageRequestProcessor messageRequestProcessor;

    @Override
    public void afterPropertiesSet() throws Exception {
        messageRequestProcessor.addHandler(messageType, this);
    }
}
