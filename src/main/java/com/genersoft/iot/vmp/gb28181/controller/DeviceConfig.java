package com.genersoft.iot.vmp.gb28181.controller;

import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

@Slf4j
@Tag(name = "National standard equipment configuration")
@RestController
@RequestMapping("/api/device/config")
public class DeviceConfig {

    @Autowired
    private IDeviceService deviceService;

    @GetMapping("/set/basicParam")
    @Operation(summary = "Set up basic configuration", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "basicParam", description = "Basic configuration parameters", required = true)
    public DeferredResult<WVPResult<String>> homePositionApi(BasicParam basicParam) {
        if (log.isDebugEnabled()) {
            log.debug("Basic configuration setting command API call");
        }
        Assert.notNull(basicParam.getDeviceId(), "Device ID must exist");

        Device device = deviceService.getDeviceByDeviceId(basicParam.getDeviceId());
        Assert.notNull(device, "Device does not exist");

        DeferredResult<WVPResult<String>> deferredResult = new DeferredResult<>();
        deviceService.deviceBasicConfig(device, basicParam, (code, msg, data) -> {
            deferredResult.setResult(new WVPResult<>(code, msg, data));
        });

        deferredResult.onTimeout(() -> {
            log.warn("[Device configuration] timeout, {}", device.getDeviceId());
            deferredResult.setResult(WVPResult.fail(ErrorCode.ERROR100.getCode(), "timeout"));
        });
        return deferredResult;
    }

    @GetMapping("/set/videoParamOpt")
    @Operation(summary = "Set video parameters", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "videoParamOpt", description = "Video parameters", required = true)
    public DeferredResult<WVPResult<String>> setVideoParamOpt(VideoParamOpt videoParamOpt) {
        if (log.isDebugEnabled()) {
            log.debug("Video parameter setting command API call");
        }
        Assert.notNull(videoParamOpt.getDeviceId(), "Device ID must exist");

        Device device = deviceService.getDeviceByDeviceId(videoParamOpt.getDeviceId());
        Assert.notNull(device, "Device does not exist");

        DeferredResult<WVPResult<String>> deferredResult = new DeferredResult<>();
        deviceService.deviceVideoParamConfig(device, videoParamOpt, (code, msg, data) -> {
            deferredResult.setResult(new WVPResult<>(code, msg, data));
        });

        deferredResult.onTimeout(() -> {
            log.warn("[Device configuration] timeout, {}", device.getDeviceId());
            deferredResult.setResult(WVPResult.fail(ErrorCode.ERROR100.getCode(), "timeout"));
        });
        return deferredResult;
    }

    @Operation(summary = "Query basic parameter configuration", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
    @Parameter(name = "channelId", description = "Channel national standard number")
    @GetMapping("/query/basicParam")
    public DeferredResult<WVPResult<BasicParam>> queryBasicParam(String deviceId,
                                                                  @RequestParam(required = false) String channelId) {
        Device device = deviceService.getDeviceByDeviceId(deviceId);
        Assert.notNull(device, "Device does not exist");
        DeferredResult<WVPResult<BasicParam>> deferredResult = new DeferredResult<>();
        deviceService.deviceConfigQuery(device, channelId, BasicParam.class, (code, msg, data) -> {
            data.setDeviceId(deviceId);
            data.setChannelId(channelId);
            deferredResult.setResult(new WVPResult<>(code, msg, data));
        });
        deferredResult.onTimeout(() -> {
            log.warn("[Get device configuration] timeout, {}", device.getDeviceId());
            deferredResult.setResult(WVPResult.fail(ErrorCode.ERROR100.getCode(), "timeout"));
        });
        return deferredResult;
    }

    @Operation(summary = "Query video parameter range", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
    @Parameter(name = "channelId", description = "Channel national standard number")
    @GetMapping("/query/videoParamOpt")
    public DeferredResult<WVPResult<VideoParamOpt>> queryVideoParamOpt(String deviceId,
                                                                        @RequestParam(required = false) String channelId) {
        Device device = deviceService.getDeviceByDeviceId(deviceId);
        Assert.notNull(device, "Device does not exist");
        DeferredResult<WVPResult<VideoParamOpt>> deferredResult = new DeferredResult<>();
        deviceService.deviceConfigQuery(device, channelId, VideoParamOpt.class, (code, msg, data) -> {
            deferredResult.setResult(new WVPResult<>(code, msg, data));
        });
        deferredResult.onTimeout(() -> {
            log.warn("[Get device configuration] timeout, {}", device.getDeviceId());
            deferredResult.setResult(WVPResult.fail(ErrorCode.ERROR100.getCode(), "timeout"));
        });
        return deferredResult;
    }

    @Operation(summary = "Query SVAC encoding configuration", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
    @Parameter(name = "channelId", description = "Channel national standard number")
    @GetMapping("/query/svacEncodeConfig")
    public DeferredResult<WVPResult<SVACEncodeConfig>> querySVACEncodeConfig(String deviceId,
                                                                              @RequestParam(required = false) String channelId) {
        Device device = deviceService.getDeviceByDeviceId(deviceId);
        Assert.notNull(device, "Device does not exist");
        DeferredResult<WVPResult<SVACEncodeConfig>> deferredResult = new DeferredResult<>();
        deviceService.deviceConfigQuery(device, channelId, SVACEncodeConfig.class, (code, msg, data) -> {
            deferredResult.setResult(new WVPResult<>(code, msg, data));
        });
        deferredResult.onTimeout(() -> {
            log.warn("[Get device configuration] timeout, {}", device.getDeviceId());
            deferredResult.setResult(WVPResult.fail(ErrorCode.ERROR100.getCode(), "timeout"));
        });
        return deferredResult;
    }

    @Operation(summary = "Query SVAC decoding configuration", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
    @Parameter(name = "channelId", description = "Channel national standard number")
    @GetMapping("/query/svacDecodeConfig")
    public DeferredResult<WVPResult<SVACDecodeConfig>> querySVACDecodeConfig(String deviceId,
                                                                              @RequestParam(required = false) String channelId) {
        Device device = deviceService.getDeviceByDeviceId(deviceId);
        Assert.notNull(device, "Device does not exist");
        DeferredResult<WVPResult<SVACDecodeConfig>> deferredResult = new DeferredResult<>();
        deviceService.deviceConfigQuery(device, channelId, SVACDecodeConfig.class, (code, msg, data) -> {
            deferredResult.setResult(new WVPResult<>(code, msg, data));
        });
        deferredResult.onTimeout(() -> {
            log.warn("[Get device configuration] timeout, {}", device.getDeviceId());
            deferredResult.setResult(WVPResult.fail(ErrorCode.ERROR100.getCode(), "timeout"));
        });
        return deferredResult;
    }

}
