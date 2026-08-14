package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.control.cmd;

import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.common.enums.DeviceControlType;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelControlService;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.control.ControlMessageHandler;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.address.SipURI;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.List;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.getText;
import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.loadElement;

@Slf4j
@Component
public class DeviceControlQueryMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private final String cmdType = "DeviceControl";

    @Autowired
    private ControlMessageHandler controlMessageHandler;

    @Autowired
    private IGbChannelService channelService;

    @Autowired
    private IGbChannelControlService channelControlService;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private IDeviceChannelService deviceChannelService;

    @Autowired
    private SIPCommander cmder;

    @Override
    public void afterPropertiesSet() throws Exception {
        controlMessageHandler.addHandler(cmdType, this);
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

        SIPRequest request = (SIPRequest) evt.getRequest();

        // Here is the DeviceControl instruction issued by the superior
        String targetGBId = ((SipURI) request.getToHeader().getAddress().getURI()).getUser();
        String channelId = getText(rootElement, "DeviceID");
        // Remote start function
        if (!ObjectUtils.isEmpty(getText(rootElement, "TeleBoot"))) {
            // Reject remote boot command
            log.warn("[deviceControl] Remote start command, disabled, does not allow the upper-level platform to restart the lower-level platform at will");
            try {
                responseAck(request, Response.FORBIDDEN);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[Command sending failed] error message: {}", e.getMessage());
            }
            return;
        }
        DeviceControlType deviceControlType = DeviceControlType.typeOf(rootElement);

        CommonGBChannel channel = channelService.queryOneWithPlatform(platform.getId(), channelId);
        if (channel == null) {
            log.warn("[deviceControl] Channel not found, platform： {}（{}），Channel number：{}", platform.getName(),
                    platform.getServerGBId(), channelId);
            try {
                responseAck(request, Response.NOT_FOUND, "channel not found");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[Command sending failed] error message: {}", e.getMessage());
            }
            return;
        }
        log.info("[deviceControl] command: {}, platform： {}（{}）->{}", deviceControlType, platform.getName(),
                platform.getServerGBId(), channel.getGbId());

        if (!ObjectUtils.isEmpty(deviceControlType)) {
            switch (deviceControlType) {
                case PTZ:
                    handlePtzCmd(channel, rootElement, request, DeviceControlType.PTZ);
                    break;
                case ALARM:
                    handleAlarmCmd(channel, rootElement, request);
                    break;
                case GUARD:
                    handleGuardCmd(channel, rootElement, request, DeviceControlType.GUARD);
                    break;
                case RECORD:
                    handleRecordCmd(channel, rootElement, request, DeviceControlType.RECORD);
                    break;
                case I_FRAME:
                    handleIFameCmd(channel, request);
                    break;
                case TELE_BOOT:
                    handleTeleBootCmd(channel, request);
                    break;
                case DRAG_ZOOM_IN:
                    handleDragZoom(channel, rootElement, request, DeviceControlType.DRAG_ZOOM_IN);
                    break;
                case DRAG_ZOOM_OUT:
                    handleDragZoom(channel, rootElement, request, DeviceControlType.DRAG_ZOOM_OUT);
                    break;
                case HOME_POSITION:
                    handleHomePositionCmd(channel, rootElement, request, DeviceControlType.HOME_POSITION);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Process PTZ commands
     */
    private void handlePtzCmd(CommonGBChannel channel, Element rootElement, SIPRequest request, DeviceControlType type) {
        if (channel.getDataType() == ChannelDataType.GB28181) {

            deviceChannelService.handlePtzCmd(channel.getDataDeviceId(), channel.getGbId(), rootElement, type, ((code, msg, data) -> {
                try {
                    responseAck(request, code, msg);
                }  catch (InvalidArgumentException | SipException | ParseException exception) {
                    log.error("[Command sending failed] PTZ command: {}", exception.getMessage());
                }
            }));
        }else {
            // Analyze PTZ control parameters
            String cmdString = getText(rootElement, type.getVal());
            IFrontEndControlCode frontEndControlCode = FrontEndCode.decode(cmdString);
            if (frontEndControlCode == null) {
                log.info("[INFO news] Unsupported control method");
                try {
                    responseAck(request, Response.FORBIDDEN, "");
                }  catch (InvalidArgumentException | SipException | ParseException exception) {
                    log.error("[Command sending failed] PTZ command: {}", exception.getMessage());
                }
                return;
            }
            switch (frontEndControlCode.getType()){
                case PTZ:
                    channelControlService.ptz(channel, (FrontEndControlCodeForPTZ)frontEndControlCode, ((code, msg, data) -> {
                        try {
                            if (code == ErrorCode.SUCCESS.getCode()) {
                                responseAck(request, Response.OK);
                            }else {
                                responseAck(request, Response.FORBIDDEN);
                            }
                        }  catch (InvalidArgumentException | SipException | ParseException exception) {
                            log.error("[Command sending failed] PTZ command: {}", exception.getMessage());
                        }
                    }));
                    break;
                case FI:
                    channelControlService.fi(channel, (FrontEndControlCodeForFI) frontEndControlCode, ((code, msg, data) -> {
                        try {
                            if (code == ErrorCode.SUCCESS.getCode()) {
                                responseAck(request, Response.OK);
                            }else {
                                responseAck(request, Response.FORBIDDEN);
                            }
                        }  catch (InvalidArgumentException | SipException | ParseException exception) {
                            log.error("[Command sending failed] FIinstructions: {}", exception.getMessage());
                        }
                    }));
                    break;
                case PRESET:
                    channelControlService.preset(channel, (FrontEndControlCodeForPreset) frontEndControlCode, ((code, msg, data) -> {
                        try {
                            if (code == ErrorCode.SUCCESS.getCode()) {
                                responseAck(request, Response.OK);
                            }else {
                                responseAck(request, Response.FORBIDDEN);
                            }
                        }  catch (InvalidArgumentException | SipException | ParseException exception) {
                            log.error("[Command sending failed] Preset command: {}", exception.getMessage());
                        }
                    }));
                    break;
                case TOUR:
                    channelControlService.tour(channel, (FrontEndControlCodeForTour) frontEndControlCode, ((code, msg, data) -> {
                        try {
                            if (code == ErrorCode.SUCCESS.getCode()) {
                                responseAck(request, Response.OK);
                            }else {
                                responseAck(request, Response.FORBIDDEN);
                            }
                        }  catch (InvalidArgumentException | SipException | ParseException exception) {
                            log.error("[Command sending failed] cruise command: {}", exception.getMessage());
                        }
                    }));
                    break;
                case SCAN:
                    channelControlService.scan(channel, (FrontEndControlCodeForScan) frontEndControlCode, ((code, msg, data) -> {
                        try {
                            if (code == ErrorCode.SUCCESS.getCode()) {
                                responseAck(request, Response.OK);
                            }else {
                                responseAck(request, Response.FORBIDDEN);
                            }
                        }  catch (InvalidArgumentException | SipException | ParseException exception) {
                            log.error("[Command sending failed] scan command: {}", exception.getMessage());
                        }
                    }));
                    break;
                case AUXILIARY:
                    channelControlService.auxiliary(channel, (FrontEndControlCodeForAuxiliary) frontEndControlCode, ((code, msg, data) -> {
                        try {
                            if (code == ErrorCode.SUCCESS.getCode()) {
                                responseAck(request, Response.OK);
                            }else {
                                responseAck(request, Response.FORBIDDEN);
                            }
                        }  catch (InvalidArgumentException | SipException | ParseException exception) {
                            log.error("[Command sending failed] Auxiliary switch command: {}", exception.getMessage());
                        }
                    }));
                    break;
                case DRAG_ZOOM:
                    channelControlService.dragZoom(channel, (FrontEndControlCodeForDragZoom) frontEndControlCode, ((code, msg, data) -> {
                        try {
                            if (code == ErrorCode.SUCCESS.getCode()) {
                                responseAck(request, Response.OK);
                            }else {
                                responseAck(request, Response.FORBIDDEN);
                            }
                        }  catch (InvalidArgumentException | SipException | ParseException exception) {
                            log.error("[Command sending failed] Auxiliary switch command: {}", exception.getMessage());
                        }
                    }));
                    break;
                default:
                    log.info("[INFO news] Control method not supported by the device");
                    try {
                        responseAck(request, Response.FORBIDDEN, "");
                    }  catch (InvalidArgumentException | SipException | ParseException exception) {
                        log.error("[Command sending failed] PTZ command: {}", exception.getMessage());
                    }
            }
        }
    }

    /**
     * Handling forced keyframes
     */
    private void handleIFameCmd(CommonGBChannel channel, SIPRequest request) {
        if (channel.getDataType() != ChannelDataType.GB28181) {
            // Only supports national standard PTZ control
            log.warn("[INFO news] Only supports national standard processing of forced key frames, channelID： {}", channel.getGbId());
            try {
                responseAck(request, Response.FORBIDDEN, "");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[Command sending failed] error message: {}", e.getMessage());
            }
            return;
        }
        // Get the device according to the channel ID
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            // Reply if it does not exist404
            log.warn("[INFO news] The device to which the channel belongs does not exist, channelID： {}", channel.getGbId());
            try {
                responseAck(request, Response.NOT_FOUND, "device  not found");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[Command sending failed] error message: {}", e.getMessage());
            }
            return;
        }

        DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(channel.getGbId());
        if (deviceChannel == null) {
            log.warn("[deviceControl] Device raw channel not found, device： {}（{}），Channel number：{}", device.getName(),
                    device.getDeviceId(), channel.getGbId());
            try {
                responseAck(request, Response.NOT_FOUND, "channel not found");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[Command sending failed] error message: {}", e.getMessage());
            }
            return;
        }
        log.info("[deviceControl] Command: force keyframe, device： {}（{}）， channel{}（{}",  device.getName(), device.getDeviceId(),
                deviceChannel.getName(), deviceChannel.getDeviceId());
        try {
            cmder.iFrameCmd(device, deviceChannel.getDeviceId());
            responseAck(request, Response.OK);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[Command sending failed] Force keyframe: {}", e.getMessage());
        }
    }

    /**
     * Handle restart commands
     */
    private void handleTeleBootCmd(CommonGBChannel channel, SIPRequest request) {
        if (channel.getDataType() != ChannelDataType.GB28181) {
            // Only supports national standard PTZ control
            log.warn("[INFO news] Only supports the national standard restart command, channelID： {}", channel.getGbId());
            try {
                responseAck(request, Response.FORBIDDEN, "");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[Command sending failed] error message: {}", e.getMessage());
            }
            return;
        }
        // Get the device according to the channel ID
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            // Reply if it does not exist404
            log.warn("[INFO news] The device to which the channel belongs does not exist, channelID： {}", channel.getGbId());
            try {
                responseAck(request, Response.NOT_FOUND, "device  not found");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[Command sending failed] error message: {}", e.getMessage());
            }
            return;
        }
        try {
            cmder.teleBootCmd(device);
            responseAck(request, Response.OK);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[Command sending failed] Restart: {}", e.getMessage());
        }

    }

    /**
     * Handle pull box control
     */
    private void handleDragZoom(CommonGBChannel channel, Element rootElement, SIPRequest request, DeviceControlType type) {
        if (channel.getDataType() != ChannelDataType.GB28181) {
            // Only supports national standard PTZ control
            log.warn("[deviceControl-DragZoom] Only support the national standard pull box control, channelID： {}", channel.getGbId());
            try {
                responseAck(request, Response.FORBIDDEN, "");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[Command sending failed] error message: {}", e.getMessage());
            }
            return;
        }
        // Get the device according to the channel ID
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            // Reply if it does not exist404
            log.warn("[deviceControl-DragZoom] The device to which the channel belongs does not exist, channelID： {}", channel.getGbId());
            try {
                responseAck(request, Response.NOT_FOUND, "device  not found");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[Command sending failed] error message: {}", e.getMessage());
            }
            return;
        }

        DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(channel.getGbId());
        if (deviceChannel == null) {
            log.warn("[deviceControl-DragZoom] Device raw channel not found, device： {}（{}），Channel number：{}", device.getName(),
                    device.getDeviceId(), channel.getGbId());
            try {
                responseAck(request, Response.NOT_FOUND, "channel not found");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[Command sending failed] error message: {}", e.getMessage());
            }
            return;
        }
        log.info("[deviceControl] command: {}, Equipment： {}（{}）， channel{}（{}", type,  device.getName(), device.getDeviceId(),
                deviceChannel.getName(), deviceChannel.getDeviceId());
        try {
            DragZoomRequest dragZoomRequest = loadElement(rootElement, DragZoomRequest.class);
            DragZoomParam dragZoom = dragZoomRequest.getDragZoomIn();
            if (dragZoom == null) {
                dragZoom = dragZoomRequest.getDragZoomOut();
            }
            StringBuffer cmdXml = new StringBuffer(200);
            cmdXml.append("<" + type.getVal() + ">\r\n");
            cmdXml.append("<Length>" + dragZoom.getLength() + "</Length>\r\n");
            cmdXml.append("<Width>" + dragZoom.getWidth() + "</Width>\r\n");
            cmdXml.append("<MidPointX>" + dragZoom.getMidPointX() + "</MidPointX>\r\n");
            cmdXml.append("<MidPointY>" + dragZoom.getMidPointY() + "</MidPointY>\r\n");
            cmdXml.append("<LengthX>" + dragZoom.getLengthX() + "</LengthX>\r\n");
            cmdXml.append("<LengthY>" + dragZoom.getLengthY() + "</LengthY>\r\n");
            cmdXml.append("</" + type.getVal() + ">\r\n");
            cmder.dragZoomCmd(device, deviceChannel.getDeviceId(), cmdXml.toString());
            responseAck(request, Response.OK);
        } catch (Exception e) {
            log.error("[Command sending failed] Frame control: {}", e.getMessage());
        }

    }

    /**
     * Handle guard bit commands
     */
    private void handleHomePositionCmd(CommonGBChannel channel, Element rootElement, SIPRequest request, DeviceControlType type) {
        if (channel.getDataType() != ChannelDataType.GB28181) {
            // Only supports national standard PTZ control
            log.warn("[INFO news] Only supports the national standard guard bit command, channelID： {}", channel.getGbId());
            try {
                responseAck(request, Response.FORBIDDEN, "");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[Command sending failed] error message: {}", e.getMessage());
            }
            return;
        }
        // Get the device according to the channel ID
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            // Reply if it does not exist404
            log.warn("[INFO news] The device to which the channel belongs does not exist, channelID： {}", channel.getGbId());
            try {
                responseAck(request, Response.NOT_FOUND, "device  not found");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[Command sending failed] error message: {}", e.getMessage());
            }
            return;
        }

        DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(channel.getGbId());
        if (deviceChannel == null) {
            log.warn("[deviceControl] Device raw channel not found, device： {}（{}），Channel number：{}", device.getName(),
                    device.getDeviceId(), channel.getGbId());
            try {
                responseAck(request, Response.NOT_FOUND, "channel not found");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[Command sending failed] error message: {}", e.getMessage());
            }
            return;
        }
        log.info("[deviceControl] command: {}, Equipment： {}（{}）， channel{}（{}", type,  device.getName(), device.getDeviceId(),
                deviceChannel.getName(), deviceChannel.getDeviceId());
        try {
            HomePositionRequest homePosition = loadElement(rootElement, HomePositionRequest.class);
            //To get the entire message body, we only need to modify the request header.
            HomePositionRequest.HomePosition info = homePosition.getHomePosition();
            cmder.homePositionCmd(device, deviceChannel.getDeviceId(), !"0".equals(info.getEnabled()), Integer.parseInt(info.getResetTime()), Integer.parseInt(info.getPresetIndex()), (code, msg, data) -> {
                if (code == ErrorCode.SUCCESS.getCode()) {
                    onOk(request);
                }else {
                    onError(request, code, msg);
                }
            });
        } catch (Exception e) {
            log.error("[Command sending failed] Watch bit setting: {}", e.getMessage());
        }
    }

    /**
     * Handle alarm messages
     */
    private void handleAlarmCmd(CommonGBChannel channel, Element rootElement, SIPRequest request) {
        if (channel.getDataType() != ChannelDataType.GB28181) {
            // Only supports national standard PTZ control
            log.warn("[INFO news] Only supports national standard alarm messages, channelID： {}", channel.getGbId());
            try {
                responseAck(request, Response.FORBIDDEN, "");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[Command sending failed] error message: {}", e.getMessage());
            }
            return;
        }
        // Get the device according to the channel ID
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            // Reply if it does not exist404
            log.warn("[INFO news] The device to which the channel belongs does not exist, channelID： {}", channel.getGbId());
            try {
                responseAck(request, Response.NOT_FOUND, "device  not found");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[Command sending failed] error message: {}", e.getMessage());
            }
            return;
        }
        //Alarm method
        String alarmMethod = "";
        //Alarm type
        String alarmType = "";
        List<Element> info = rootElement.elements("Info");
        if (info != null) {
            for (Element element : info) {
                alarmMethod = getText(element, "AlarmMethod");
                alarmType = getText(element, "AlarmType");
            }
        }
        try {
            cmder.alarmResetCmd(device, alarmMethod, alarmType, (code, msg, data) -> {
                if (code == ErrorCode.SUCCESS.getCode()) {
                    onOk(request);
                }else {
                    onError(request, code, msg);
                }
            });
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[Command sending failed] Alarm message: {}", e.getMessage());
        }
    }

    /**
     * Handle video control
     */
    private void handleRecordCmd(CommonGBChannel channel, Element rootElement, SIPRequest request, DeviceControlType type) {
        if (channel.getDataType() != ChannelDataType.GB28181) {
            // Only supports national standard PTZ control
            log.warn("[INFO news] Only supports national standard information video control, channelID： {}", channel.getGbId());
            try {
                responseAck(request, Response.FORBIDDEN, "");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[Command sending failed] error message: {}", e.getMessage());
            }
            return;
        }
        // Get the device according to the channel ID
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            // Reply if it does not exist404
            log.warn("[INFO news] The device to which the channel belongs does not exist, channelID： {}", channel.getGbId());
            try {
                responseAck(request, Response.NOT_FOUND, "device  not found");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[Command sending failed] error message: {}", e.getMessage());
            }
            return;
        }

        DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(channel.getGbId());
        if (deviceChannel == null) {
            // Reject remote boot command
            log.warn("[deviceControl] Device raw channel not found, device： {}（{}），Channel number：{}", device.getName(),
                    device.getDeviceId(), channel.getGbId());
            try {
                responseAck(request, Response.NOT_FOUND, "channel not found");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[Command sending failed] error message: {}", e.getMessage());
            }
            return;
        }
        log.info("[deviceControl] command: {}, Equipment： {}（{}）， channel{}（{}", type,  device.getName(), device.getDeviceId(),
                deviceChannel.getName(), deviceChannel.getDeviceId());
        //To get the entire message body, we only need to modify the request header.
        String cmdString = getText(rootElement, type.getVal());
        try {
            cmder.recordCmd(device, deviceChannel.getDeviceId(), cmdString, (code, msg, data) -> {
                        if (code == ErrorCode.SUCCESS.getCode()) {
                            onOk(request);
                        }else {
                            onError(request, code, msg);
                        }
                    });
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[Command sending failed] Video control: {}", e.getMessage());
        }
    }

    /**
     * Handle alarm arming/disarm order
     */
    private void handleGuardCmd(CommonGBChannel channel, Element rootElement, SIPRequest request, DeviceControlType type) {
        if (channel.getDataType() != ChannelDataType.GB28181) {
            // Only supports national standard PTZ control
            log.warn("[INFO news] Only supports national standard alarm deployment/Disarm command, channelID： {}", channel.getGbId());
            try {
                responseAck(request, Response.FORBIDDEN, "");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[Command sending failed] error message: {}", e.getMessage());
            }
            return;
        }
        // Get the device according to the channel ID
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            // Reply if it does not exist404
            log.warn("[INFO news] The device to which the channel belongs does not exist, channelID： {}", channel.getGbId());
            try {
                responseAck(request, Response.NOT_FOUND, "device  not found");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[Command sending failed] error message: {}", e.getMessage());
            }
            return;
        }
        //To get the entire message body, we only need to modify the request header.
        String cmdString = getText(rootElement, type.getVal());
        try {
            cmder.guardCmd(device, cmdString,(code, msg, data) -> {
                if (code == ErrorCode.SUCCESS.getCode()) {
                    onOk(request);
                }else {
                    onError(request, code, msg);
                }
            });
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[Command sending failed] arm/disarm order: {}", e.getMessage());
        }
    }




    /**
     * Error response handling
     *
     */
    private void onError(SIPRequest request, Integer code, String msg) {
        // failed reply
        try {
            responseAck(request, code, msg);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[Command sending failed] Reply: {}", e.getMessage());
        }
    }

    private void onError(SIPRequest request, SipSubscribe.EventResult errorResult) {
        onError(request, errorResult.statusCode, errorResult.msg);
    }

    /**
     * Successful response handling
     *
     * @param request     Request
     */
    private void onOk(SIPRequest request) {
        // successful reply
        try {
            responseAck(request, Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[Command sending failed] Reply: {}", e.getMessage());
        }
    }
}
