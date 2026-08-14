package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.query.cmd;

import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.gb28181.service.IPlatformChannelService;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderForPlatform;
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
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class CatalogQueryMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private final String cmdType = "Catalog";

    @Autowired
    private QueryMessageHandler queryMessageHandler;

    @Autowired
    private IGbChannelService channelService;

    @Autowired
    private IPlatformChannelService platformChannelService;

    @Autowired
    private SIPCommanderForPlatform cmderFroPlatform;


    @Override
    public void afterPropertiesSet() throws Exception {
        queryMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element element) {
        try {
            // Reply200 OK
            responseAck((SIPRequest) evt.getRequest(), Response.FORBIDDEN);
        } catch (SipException | InvalidArgumentException | ParseException ignored) {}
    }

    @Override
    public void handForPlatform(RequestEvent evt, Platform platform, Element rootElement) {

        FromHeader fromHeader = (FromHeader) evt.getRequest().getHeader(FromHeader.NAME);
        try {
            // Reply200 OK
             responseAck((SIPRequest) evt.getRequest(), Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[Command sending failed] National Standard Cascade Catalog Query Reply200OK: {}", e.getMessage());
        }
        Element snElement = rootElement.element("SN");
        String sn = snElement.getText();
        List<CommonGBChannel> channelList = platformChannelService.queryByPlatform(platform);

        try {
            if (!channelList.isEmpty()) {
                cmderFroPlatform.catalogQuery(channelList, platform, sn, fromHeader.getTag());
            }else {
                // Reply no channel
                cmderFroPlatform.catalogQuery(Collections.emptyList(), platform, sn, fromHeader.getTag());
            }
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[Command sending failed] National Standard Cascade Catalog Query Reply: {}", e.getMessage());
        }
    }
}
