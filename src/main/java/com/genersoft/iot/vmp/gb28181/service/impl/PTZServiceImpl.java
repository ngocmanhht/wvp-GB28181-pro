package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.Preset;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.service.IPTZService;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.redisMsg.IRedisRpcPlayService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.List;

@Slf4j
@Service
public class PTZServiceImpl implements IPTZService {


    @Autowired
    private SIPCommander cmder;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private IRedisRpcPlayService redisRpcPlayService;

    @Autowired
    private IDeviceChannelService deviceChannelService;

    @Autowired
    private IDeviceService deviceService;


    @Override
    public void ptz(Device device, String channelId, int cmdCode, int horizonSpeed, int verticalSpeed, int zoomSpeed) {
        try {
            cmder.frontEndCmd(device, channelId, cmdCode, horizonSpeed, verticalSpeed, zoomSpeed);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[Command sending failed] PTZ control: {}", e.getMessage());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Command sending failed: " + e.getMessage());
        }
    }

    @Override
    public void frontEndCommand(Device device, String channelId, int cmdCode, int parameter1, int parameter2, int combindCode2) {
        // Determine whether the device belongs to the current platform, if not, initiate an automatic call
        if (!userSetting.getServerId().equals(device.getServerId())) {
            // channelID
            DeviceChannel deviceChannel = deviceChannelService.getOneForSource(device.getDeviceId(), channelId);
            Assert.notNull(deviceChannel, "Channel does not exist");
            String msg = redisRpcPlayService.frontEndCommand(device.getServerId(), deviceChannel.getId(), cmdCode, parameter1, parameter2, combindCode2);
            if (msg != null) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), msg);
            }
            return;
        }
        try {
            cmder.frontEndCmd(device, channelId, cmdCode, parameter1, parameter2, combindCode2);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[Command sending failed] front-end control: {}", e.getMessage());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Command sending failed: " + e.getMessage());
        }
    }

    @Override
    public void frontEndCommand(CommonGBChannel channel, Integer cmdCode, Integer parameter1, Integer parameter2, Integer combindCode2) {
        if (channel.getDataType() != ChannelDataType.GB28181) {
            // Only the national standard channel supports PTZ control
            log.warn("[INFO news] Only the national standard channel supports PTZ control, the channelID： {}", channel.getGbId());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Not supported");
        }
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Device not foundID");
        }
        DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(channel.getGbId());
        frontEndCommand(device, deviceChannel.getDeviceId(), cmdCode, parameter1, parameter2, combindCode2);
    }

    @Override
    public void dragZoomIn(CommonGBChannel channel, int length, int width, int midPointX, int midPointY, int lengthX, int lengthY) {
        if (channel.getDataType() != ChannelDataType.GB28181) {
            log.warn("[INFO news] Only the national standard channel supports PTZ control, the channelID： {}", channel.getGbId());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Not supported");
        }
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Device not foundID");
        }
        DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(channel.getGbId());
        deviceService.dragZoomIn(device, deviceChannel.getDeviceId(), length, width, midPointX, midPointY, lengthX, lengthY);
    }

    @Override
    public void dragZoomOut(CommonGBChannel channel, int length, int width, int midPointX, int midPointY, int lengthX, int lengthY) {
        if (channel.getDataType() != ChannelDataType.GB28181) {
            log.warn("[INFO news] Only the national standard channel supports PTZ control, the channelID： {}", channel.getGbId());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Not supported");
        }
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Device not foundID");
        }
        DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(channel.getGbId());
        deviceService.dragZoomOut(device, deviceChannel.getDeviceId(), length, width, midPointX, midPointY, lengthX, lengthY);
    }

    @Override
    public void homePosition(CommonGBChannel channel, Boolean enabled, Integer resetTime, Integer presetIndex, ErrorCallback<String> callback) {
        if (channel.getDataType() != ChannelDataType.GB28181) {
            log.warn("[INFO news] Only the national standard channel supports the guard bit, and the channelID：{}", channel.getGbId());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Not supported");
        }
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Device not found");
        }
        DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(channel.getGbId());
        if (deviceChannel == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Channel not found");
        }
        deviceService.homePosition(device, deviceChannel.getDeviceId(), enabled, resetTime, presetIndex, callback);
    }

    @Override
    public void queryPresetList(CommonGBChannel channel, ErrorCallback<List<Preset>> callback) {
        if (channel.getDataType() != ChannelDataType.GB28181) {
            // Only the national standard channel supports PTZ control
            log.warn("[INFO news] Only the national standard channel supports PTZ control, the channelID： {}", channel.getGbId());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Not supported");
        }
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Device not found");
        }
        DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(channel.getGbId());
        if (deviceChannel == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Channel not found");
        }
        deviceService.queryPreset(device, deviceChannel.getDeviceId(), callback);
    }


}
