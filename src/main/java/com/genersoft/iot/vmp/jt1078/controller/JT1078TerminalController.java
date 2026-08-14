package com.genersoft.iot.vmp.jt1078.controller;

import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.jt1078.bean.JTChannel;
import com.genersoft.iot.vmp.jt1078.bean.JTDevice;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;



@Slf4j
@ConditionalOnProperty(value = "jt1078.enable", havingValue = "true")
@RestController
@Tag(name  = "Department standard terminal and channel management")
@RequestMapping("/api/jt1078/terminal")
public class JT1078TerminalController {

    @Resource
    Ijt1078Service service;

    @Operation(summary = "JT-Query department standard equipment by page", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "Current page", required = true)
    @Parameter(name = "count", description = "Number of queries per page", required = true)
    @Parameter(name = "query", description = "Query content")
    @Parameter(name = "online", description = "Is online")
    @GetMapping("/list")
    public PageInfo<JTDevice> getDevices(int page, int count,
                                         @RequestParam(required = false) String query,
                                         @RequestParam(required = false) Boolean online) {
        return service.getDeviceList(page, count, query, online);
    }

    @Operation(summary = "Update device", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "device", description = "Equipment", required = true)
    @PostMapping("/update")
    public void updateDevice(JTDevice device){
        assert device.getId() > 0;
        assert device.getPhoneNumber() != null;
        service.updateDevice(device);
    }

    @Operation(summary = "JT-Add new device", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "device", description = "Equipment", required = true)
    @PostMapping("/add")
    public void addDevice(JTDevice device){
        assert device.getPhoneNumber() != null;
        String phoneNumber = device.getPhoneNumber().replaceFirst("^0*", "");
        device.setPhoneNumber(phoneNumber);
        service.addDevice(device);
    }
    @Operation(summary = "Remove device", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device mobile phone number", required = true)
    @DeleteMapping("/delete")
    public void addDevice(String phoneNumber){
        assert phoneNumber != null;
        service.deleteDeviceByPhoneNumber(phoneNumber);
    }
    @Operation(summary = "Query device", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device mobile phone number", required = true)
    @GetMapping("/query")
    public JTDevice getDevice(Integer deviceId){
        return service.getDeviceById(deviceId);
    }


    @Operation(summary = "JT-Query department label channel", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "Current page", required = true)
    @Parameter(name = "count", description = "Number of queries per page", required = true)
    @Parameter(name = "deviceId", description = "EquipmentID", required = true)
    @Parameter(name = "query", description = "Query content")
    @GetMapping("/channel/list")
    public PageInfo<JTChannel> getChannels(int page, int count,
                                           @RequestParam(required = true) Integer deviceId,
                                           @RequestParam(required = false) String query) {
        assert deviceId != null;
        return service.getChannelList(page, count, deviceId, query);
    }

    @Operation(summary = "JT-Query a single part label channel", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "id", description = "channel databaseID", required = true)
    @GetMapping("/channel/one")
    public JTChannel getChannel(Integer id) {
        assert id != null;
        return service.getChannelByDbId(id);
    }

    @Operation(summary = "JT-update channel", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channel", description = "channel", required = true)
    @PostMapping("/channel/update")
    public void updateChannel(@RequestBody JTChannel channel){
        assert channel.getId() > 0;
        assert channel.getChannelId() != null;
        service.updateChannel(channel);
    }

    @Operation(summary = "JT-Add new channel", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channel", description = "channel", required = true)
    @PostMapping("/channel/add")
    public JTChannel addChannel(@RequestBody JTChannel channel){
        assert channel.getChannelId() != null;
        assert channel.getTerminalDbId() != 0;
        service.addChannel(channel);
        return channel;
    }
    @Operation(summary = "JT-Delete channel", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "id", description = "channel databaseID", required = true)
    @DeleteMapping("/channel/delete")
    public void deleteChannel(Integer id){
        service.deleteChannelById(id);
    }
}

