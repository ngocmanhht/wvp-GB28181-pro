package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.query.cmd;

import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.query.QueryMessageHandler;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.header.FromHeader;
import javax.sip.message.Response;
import java.text.ParseException;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.getText;

@Slf4j
@Component
public class DeviceStatusQueryMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private final String cmdType = "DeviceStatus";

    @Autowired
    private QueryMessageHandler queryMessageHandler;

    @Autowired
    private IGbChannelService channelService;

    @Autowired
    private ISIPCommanderForPlatform cmderFroPlatform;

    @Override
    public void afterPropertiesSet() throws Exception {
        queryMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element element) {
        try {
            responseAck((SIPRequest) evt.getRequest(), Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[Command sending failed] Reply200 OK: {}", e.getMessage());
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, Platform platform, Element rootElement) {

        log.info("[DeviceStatus Query] \n {}", rootElement.asXML());
        FromHeader fromHeader = (FromHeader) evt.getRequest().getHeader(FromHeader.NAME);
        // Reply200 OK
        try {
            responseAck((SIPRequest) evt.getRequest(), Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[Command sending failed] National standard cascade DeviceStatus query reply200OK: {}", e.getMessage());
        }
        String sn = rootElement.element("SN").getText();
        String channelId = getText(rootElement, "DeviceID");
        if (platform.getDeviceGBId().equals(channelId)) {
            // Check the status of this platform on the superior platform
            try {
                cmderFroPlatform.deviceStatusResponse(platform, channelId, sn, fromHeader.getTag(), true);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[Command sending failed] National standard cascade DeviceStatus query reply: {}", e.getMessage());
            }
            return;
        }
        CommonGBChannel channel= channelService.queryOneWithPlatform(platform.getId(), channelId);
        if (channel ==null){
            log.error("[The platform does not have permission to use this channel]:platformId: {}  deviceID:{}", platform.getServerGBId(), channelId);
            // Check the status of this platform on the superior platform
            try {
                cmderFroPlatform.deviceStatusResponse(platform, channelId, sn, fromHeader.getTag(), null);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[Command sending failed] National standard cascade DeviceStatus query reply: {}", e.getMessage());
            }
            return;
        }
        try {
            cmderFroPlatform.deviceStatusResponse(platform, channelId, sn, fromHeader.getTag(), "ON".equalsIgnoreCase(channel.getGbStatus()));
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[Command sending failed] National standard cascade DeviceStatus query reply: {}", e.getMessage());
        }
    }
}
