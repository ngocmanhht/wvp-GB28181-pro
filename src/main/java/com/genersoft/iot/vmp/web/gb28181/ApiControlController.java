package com.genersoft.iot.vmp.web.gb28181;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;

/**
 * APICompatible with: Device Control
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/control")
@Hidden
public class ApiControlController {

    @Autowired
    private SIPCommander cmder;

    @Autowired
    private IDeviceService deviceService;

    /**
     * Device control - PTZ control
     * @param serial Device number
     * @param command Control command allowed value: left, right, up, down, upleft, upright, downleft, downright, zoomin, zoomout, stop
     * @param channel Channel number
     * @param code Channel number
     * @param speed speed(0~255) Default value: 129
     */
    @GetMapping(value = "/ptz")
    private void ptz(String serial,String command,
                            @RequestParam(required = false)Integer channel,
                            @RequestParam(required = false)String code,
                            @RequestParam(required = false)Integer speed){

        if (log.isDebugEnabled()) {
            log.debug("Analog interface> Device PTZ control API call，deviceId：{} ，channelId：{} ，command：{} ，speed：{} ",
                    serial, code, command, speed);
        }
        if (channel == null) {channel = 0;}
        if (speed == null) {speed = 0;}
        Device device = deviceService.getDeviceByDeviceId(serial);
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "device[ " + serial + " ]not found");
        }
        int cmdCode = -1;
        switch (command){
            case "left":
                cmdCode = 2;
                break;
            case "right":
                cmdCode = 1;
                break;
            case "up":
                cmdCode = 8;
                break;
            case "down":
                cmdCode = 4;
                break;
            case "upleft":
                cmdCode = 10;
                break;
            case "upright":
                cmdCode = 9;
                break;
            case "downleft":
                cmdCode = 6;
                break;
            case "downright":
                cmdCode = 5;
                break;
            case "zoomin":
                cmdCode = 16;
                break;
            case "zoomout":
                cmdCode = 32;
                break;
            case "stop":
                cmdCode = 0;
                break;
            default:
                break;
        }
        if (cmdCode == -1) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Unrecognized command：" + command);
        }
        // Default value 50
        try {
            cmder.frontEndCmd(device, code, cmdCode, speed, speed, speed);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[Command sending failed] PTZ control: {}", e.getMessage());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Command sending failed: " + e.getMessage());
        }
    }

    /**
     * Device control - Preset position control
     * @param serial Device number
     * @param code Channel number, pass /api/v1/device/channellist obtained ChannelList.ID, You can choose to pass this parameter or channel.
     * @param channel Channel number, default value: 1
     * @param command Control command allowed value: set, goto, remove
     * @param preset Preset number(1~255)
     * @param name Preset position name, command=set valid when
     */
    @GetMapping(value = "/preset")
    private void list(String serial,String command,
                            @RequestParam(required = false)Integer channel,
                            @RequestParam(required = false)String code,
                            @RequestParam(required = false)String name,
                            @RequestParam(required = false)Integer preset){

        if (log.isDebugEnabled()) {
            log.debug("Analog interface> Preset position control API call，deviceId：{} ，channelId：{} ，command：{} ，name：{} ，preset：{} ",
                    serial, code, command, name, preset);
        }

        if (channel == null) {channel = 0;}
        Device device = deviceService.getDeviceByDeviceId(serial);
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "device[ " + serial + " ]not found");
        }
        int cmdCode = 0;
        switch (command){
            case "set":
                cmdCode = 129;
                break;
            case "goto":
                cmdCode = 130;
                break;
            case "remove":
                cmdCode = 131;
                break;
            default:
                break;
        }
        try {
            cmder.frontEndCmd(device, code, cmdCode, 0, preset, 0);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[Command sending failed] Preset position control: {}", e.getMessage());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Command sending failed: " + e.getMessage());
        }
    }
}
