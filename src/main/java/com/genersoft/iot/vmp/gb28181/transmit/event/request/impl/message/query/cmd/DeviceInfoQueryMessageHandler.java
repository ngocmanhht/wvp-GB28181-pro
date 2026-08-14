package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.query.cmd;

import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
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

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.getText;

@Slf4j
@Component
public class DeviceInfoQueryMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private final String cmdType = "DeviceInfo";

    @Autowired
    private QueryMessageHandler queryMessageHandler;

    @Autowired
    private SIPCommanderForPlatform cmderFroPlatform;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private IGbChannelService channelService;

    @Autowired
    private IDeviceChannelService deviceChannelService;

    @Override
    public void afterPropertiesSet() throws Exception {
        queryMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element rootElement) {
        try {
            responseAck((SIPRequest) evt.getRequest(), Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[Command sending failed] Reply200 OK: {}", e.getMessage());
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, Platform platform, Element rootElement) {
        log.info("[DeviceInfoQuery]news");
        SIPRequest request = (SIPRequest) evt.getRequest();
        FromHeader fromHeader = (FromHeader) evt.getRequest().getHeader(FromHeader.NAME);

        String sn = rootElement.element("SN").getText();

        /*According to the original data structure of WVP, devices and channels are placed separately, and device information is stored in the device table. The device information in the channel table cannot be processed as real information.
        most ofNVR/IPCThe device returns the default value for its channel information implementation, which has no reference value.。NVR/IPCFor channels, we uniformly use the device information in the device table as return。
        Here we use the method of querying the database to realize the function of querying device information, and update the device information in other places to achieve the correct purpose.。*/

        String channelId = getText(rootElement, "DeviceID");
        // Query whether this is a channel id or a deviceid
        if (platform.getDeviceGBId().equals(channelId)) {
            // idPoint to the national standard number of the platform, then query the platform information
            try {
                cmderFroPlatform.deviceInfoResponse(platform, null, sn, fromHeader.getTag());
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[Command sending failed] National standard cascade DeviceInfo query reply: {}", e.getMessage());
            }
            return;
        }
        CommonGBChannel channel = channelService.queryOneWithPlatform(platform.getId(), channelId);
        if (channel == null) {
            // Reply if it does not exist404
            log.warn("[DeviceInfo] Channel does not exist: channel number： {}", channelId);
            try {
                responseAck(request, Response.NOT_FOUND, "channel not found or offline");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[Command sending failed] DeviceInfoInquiry reply: {}", e.getMessage());
                return;
            }
            return;
        }
        // Determine channel type
        if (channel.getDataType() != ChannelDataType.GB28181) {
            // Non-national standard channels do not support video playback control
            log.warn("[DeviceInfo] Non-national standard channels do not support video playback control: ChannelID： {}", channel.getGbId());
            try {
                responseAck(request, Response.FORBIDDEN, "");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[Command sending failed] DeviceInfoInquiry reply: {}", e.getMessage());
                return;
            }
            return;
        }

        // Get the device according to the channel ID
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            // Reply if it does not exist404
            log.warn("[DeviceInfo] The device to which the channel belongs does not exist, channelID： {}", channel.getDataDeviceId());

            try {
                responseAck(request, Response.NOT_FOUND, "device not found ");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[Command sending failed] DeviceInfoInquiry reply: {}", e.getMessage());
                return;
            }
            return;
        }
        try {
            // Reply200 OK
            responseAck((SIPRequest) evt.getRequest(), Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[Command sending failed] DeviceInfoInquiry reply: {}", e.getMessage());
            return;
        }
        try {
            cmderFroPlatform.deviceInfoResponse(platform, device, sn, fromHeader.getTag());
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[Command sending failed] National standard cascade DeviceInfo query reply: {}", e.getMessage());
        }
    }
}
