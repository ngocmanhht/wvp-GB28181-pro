package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.control;

import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.MessageHandlerAbstract;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.MessageRequestProcessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Command type: control command
 * Command type: Device control: remote start, video control (TODO), alarm arming/Disarm command (TODO), alarm reset command（TODO）,
 *                   Forced keyframe command (TODO), zoom in by pulling the box/Reduce control command (TODO), guard position control (TODO), alarm reset（TODO）
 * Command type: Device configuration: SVAC encoding configuration (TODO), audio parameters (TODO), SVAC decoding configuration（TODO）
 */
@Component
public class ControlMessageHandler extends MessageHandlerAbstract implements InitializingBean  {

    private final String messageType = "Control";

    @Autowired
    private MessageRequestProcessor messageRequestProcessor;

    @Override
    public void afterPropertiesSet() throws Exception {
        messageRequestProcessor.addHandler(messageType, this);
    }
}
