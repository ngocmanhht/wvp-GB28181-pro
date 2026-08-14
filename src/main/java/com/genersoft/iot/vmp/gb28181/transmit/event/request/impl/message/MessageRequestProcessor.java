package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceNotFoundEvent;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.bean.SsrcTransaction;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.event.sip.SipEvent;
import com.genersoft.iot.vmp.gb28181.service.IPlatformService;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class MessageRequestProcessor extends SIPRequestProcessorParent implements InitializingBean, ISIPRequestProcessor {

    private final String method = "MESSAGE";

    private static final Map<String, IMessageHandler> messageHandlerMap = new ConcurrentHashMap<>();

    @Autowired
    private SIPProcessorObserver sipProcessorObserver;

    @Autowired
    private IPlatformService platformService;

    @Autowired
    private SipSubscribe sipSubscribe;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private SipInviteSessionManager sessionManager;

    @Override
    public void afterPropertiesSet() throws Exception {
        // Add message processing subscription
        sipProcessorObserver.addRequestProcessor(method, this);
    }

    public void addHandler(String name, IMessageHandler handler) {
        messageHandlerMap.put(name, handler);
    }

    @Override
    public void process(RequestEvent evt) {
        SIPRequest sipRequest = (SIPRequest)evt.getRequest();
//        logger.info("message received：" + evt.getRequest());
        String deviceId = SipUtils.getUserIdFromFromHeader(evt.getRequest());
        CallIdHeader callIdHeader = sipRequest.getCallIdHeader();
        CSeqHeader cSeqHeader = sipRequest.getCSeqHeader();
        // Search within the session first
        SsrcTransaction ssrcTransaction = sessionManager.getSsrcTransactionByCallId(callIdHeader.getCallId());
        // Compatible with Hikvision media notification. The message from field is not the device ID.
        if (ssrcTransaction != null) {
            deviceId = ssrcTransaction.getDeviceId();
        }
        SIPRequest request = (SIPRequest) evt.getRequest();
        // Check if the device exists
        Device device = redisCatchStorage.getDevice(deviceId);
        // Check whether the upper-level platform exists
        Platform parentPlatform = platformService.queryPlatformByServerGBId(deviceId);
        try {
            if (device != null && parentPlatform != null) {
                String hostAddress = request.getRemoteAddress().getHostAddress();
                int remotePort = request.getRemotePort();
                if (device.getHostAddress().equals(hostAddress + ":" + remotePort)) {
                    parentPlatform = null;
                }else {
                    device = null;
                }
            }
            if (device == null && parentPlatform == null) {
                // Reply if it does not exist404
                responseAck(request, Response.NOT_FOUND, "device "+ deviceId +" not found");
                log.warn("[Device not found ]deviceId: {}, callId: {}", deviceId, callIdHeader.getCallId());
                SipEvent sipEvent = sipSubscribe.getSubscribe(callIdHeader.getCallId() + cSeqHeader.getSeqNumber());
                if (sipEvent != null && sipEvent.getErrorEvent() != null){
                    DeviceNotFoundEvent deviceNotFoundEvent = new DeviceNotFoundEvent(callIdHeader.getCallId());
                    SipSubscribe.EventResult eventResult = new SipSubscribe.EventResult(deviceNotFoundEvent);
                    sipEvent.getErrorEvent().response(eventResult);
                }
            }else {
                Element rootElement;
                try {
                    rootElement = getRootElement(evt);
                    if (rootElement == null) {
                        log.error("Processing MESSAGE request, message body not obtained{}", evt.getRequest());
                        responseAck(request, Response.BAD_REQUEST, "content is null");
                        return;
                    }
                    String name = rootElement.getName();
                    IMessageHandler messageHandler = messageHandlerMap.get(name);
                    if (messageHandler != null) {
                        if (device != null) {
                            messageHandler.handForDevice(evt, device, rootElement);
                        }else { // Since the above has been judged to be null, it will be returned directly, so one of the device and parentPlatform must be different.null
                            messageHandler.handForPlatform(evt, parentPlatform, rootElement);
                        }
                        // If there is a handler, it is up to the handler to decide what to reply.。
                    }else {
                        // Not supportedmessage
                        // Reply if it does not exist415
                        responseAck(request, Response.UNSUPPORTED_MEDIA_TYPE, "Unsupported message type, must Control/Notify/Query/Response");
                    }
                } catch (DocumentException e) {
                    log.warn("Exception in parsing XML message content", e);
                    // Reply if it does not exist404
                    responseAck(request, Response.BAD_REQUEST, e.getMessage());
                }
            }
        } catch (SipException e) {
            log.warn("SIP Reply to error", e);
        } catch (InvalidArgumentException e) {
            log.warn("Invalid parameter", e);
        } catch (ParseException e) {
            log.warn("SIPParse exception when replying", e);
        }
    }


}
