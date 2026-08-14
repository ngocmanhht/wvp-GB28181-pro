package com.genersoft.iot.vmp.gb28181.controller;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelControlService;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;


@Tag(name  = "Global channel front-end control")
@RestController
@Slf4j
@RequestMapping(value = "/api/common/channel/front-end")
public class ChannelFrontEndController {

    @Autowired
    private IGbChannelService channelService;

    @Autowired
    private IGbChannelControlService channelControlService;


    @Operation(summary = "PTZ control", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "channelID", required = true)
    @Parameter(name = "command", description = "control instructions, allowable values: left, right, up, down, upleft, upright, downleft, downright, zoomin, zoomout, stop", required = true)
    @Parameter(name = "panSpeed", description = "horizontal speed(0-100)", required = true)
    @Parameter(name = "tiltSpeed", description = "vertical speed(0-100)", required = true)
    @Parameter(name = "zoomSpeed", description = "Zoom speed(0-100)", required = true)
    @GetMapping("/ptz")
    public DeferredResult<WVPResult<String>> ptz(Integer channelId, String command, Integer panSpeed, Integer tiltSpeed, Integer zoomSpeed){

        if (log.isDebugEnabled()) {
            log.debug("[Universal channel]PTZ control API call，channelId：{} ，command：{} ，panSpeed：{} ，tiltSpeed：{} ，zoomSpeed：{}",channelId, command, panSpeed, tiltSpeed, zoomSpeed);
        }

        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "Channel does not exist");

        if (panSpeed == null) {
            panSpeed = 50;
        }else if (panSpeed < 0 || panSpeed > 100) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "panSpeed for 0-100number");
        }
        if (tiltSpeed == null) {
            tiltSpeed = 50;
        }else if (tiltSpeed < 0 || tiltSpeed > 100) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "tiltSpeed for 0-100number");
        }
        if (zoomSpeed == null) {
            zoomSpeed = 50;
        }else if (zoomSpeed < 0 || zoomSpeed > 100) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "zoomSpeed for 0-100number");
        }

        FrontEndControlCodeForPTZ controlCode = new FrontEndControlCodeForPTZ();
        controlCode.setPanSpeed(panSpeed);
        controlCode.setTiltSpeed(tiltSpeed);
        controlCode.setZoomSpeed(zoomSpeed);
        switch (command){
            case "left":
                controlCode.setPan(0);
                break;
            case "right":
                controlCode.setPan(1);
                break;
            case "up":
                controlCode.setTilt(0);
                break;
            case "down":
                controlCode.setTilt(1);
                break;
            case "upleft":
                controlCode.setPan(0);
                controlCode.setTilt(0);
                break;
            case "upright":
                controlCode.setTilt(0);
                controlCode.setPan(1);
                break;
            case "downleft":
                controlCode.setPan(0);
                controlCode.setTilt(1);
                break;
            case "downright":
                controlCode.setTilt(1);
                controlCode.setPan(1);
                break;
            case "zoomin":
                controlCode.setZoom(1);
                break;
            case "zoomout":
                controlCode.setZoom(0);
                break;
            default:
                break;
        }

        DeferredResult<WVPResult<String>> result = new DeferredResult<>();

        result.onTimeout(()->{
            WVPResult<String> wvpResult = WVPResult.fail(ErrorCode.ERROR100.getCode(), "Request timeout");
            result.setResult(wvpResult);
        });

        channelControlService.ptz(channel, controlCode, (code, msg, data) -> {
            WVPResult<String> wvpResult = new WVPResult<>();
            wvpResult.setCode(code);
            wvpResult.setMsg(msg);
            wvpResult.setData(data);
            result.setResult(wvpResult);
        });
        return result;
    }


    @Operation(summary = "Aperture control", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "Channel national standard number", required = true)
    @Parameter(name = "command", description = "control instructions, allowable values: in, out, stop", required = true)
    @Parameter(name = "speed", description = "aperture speed(0-100)", required = true)
    @GetMapping("/fi/iris")
    public DeferredResult<WVPResult<String>> iris(Integer channelId, String command, Integer speed){

        if (log.isDebugEnabled()) {
            log.debug("[Universal channel]Aperture control API call，channelId：{} ，command：{} ，speed：{} ",channelId, command, speed);
        }

        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "Channel does not exist");

        if (speed == null) {
            speed = 50;
        }else if (speed < 0 || speed > 100) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "speed for 0-100number");
        }

        FrontEndControlCodeForFI controlCode = new FrontEndControlCodeForFI();
        controlCode.setIrisSpeed(speed);

        switch (command){
            case "in":
                controlCode.setIris(1);
                break;
            case "out":
                controlCode.setIris(0);
                break;
            default:
                break;
        }

        DeferredResult<WVPResult<String>> result = new DeferredResult<>();

        result.onTimeout(()->{
            WVPResult<String> wvpResult = WVPResult.fail(ErrorCode.ERROR100.getCode(), "Request timeout");
            result.setResult(wvpResult);
        });

        ErrorCallback<String> callback = (code, msg, data) -> {
            WVPResult<String> wvpResult = new WVPResult<>();
            wvpResult.setCode(code);
            wvpResult.setMsg(msg);
            wvpResult.setData(data);
            result.setResult(wvpResult);
        };

        channelControlService.fi(channel, controlCode, callback);

        return result;
    }

    @Operation(summary = "focus control", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "Channel national standard number", required = true)
    @Parameter(name = "command", description = "control instructions, allowable values: near, far, stop", required = true)
    @Parameter(name = "speed", description = "focus speed(0-100)", required = true)
    @GetMapping("/fi/focus")
    public DeferredResult<WVPResult<String>> focus(Integer channelId, String command, Integer speed){

        if (log.isDebugEnabled()) {
            log.debug("[Universal channel]Focus control API calls，channelId：{} ，command：{} ，speed：{} ", channelId, command, speed);
        }

        if (speed == null) {
            speed = 50;
        }else if (speed < 0 || speed > 100) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "speed for 0-100number");
        }

        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "Channel does not exist");

        FrontEndControlCodeForFI controlCode = new FrontEndControlCodeForFI();
        controlCode.setFocusSpeed(speed);
        switch (command){
            case "near":
                controlCode.setFocus(0);
                break;
            case "far":
                controlCode.setFocus(1);
                break;
            default:
                break;
        }

        DeferredResult<WVPResult<String>> result = new DeferredResult<>();

        result.onTimeout(()->{
            WVPResult<String> wvpResult = WVPResult.fail(ErrorCode.ERROR100.getCode(), "Request timeout");
            result.setResult(wvpResult);
        });

        ErrorCallback<String> callback = (code, msg, data) -> {
            WVPResult<String> wvpResult = new WVPResult<>();
            wvpResult.setCode(code);
            wvpResult.setMsg(msg);
            wvpResult.setData(data);
            result.setResult(wvpResult);
        };

        channelControlService.fi(channel, controlCode, callback);
        return result;
    }

    @Operation(summary = "Query preset position", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "Channel national standard number", required = true)
    @GetMapping("/preset/query")
    public DeferredResult<WVPResult<List<Preset>>> queryPreset(Integer channelId) {
        if (log.isDebugEnabled()) {
            log.debug("[Universal channel] Preset position query API call, {}", channelId);
        }

        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "Channel does not exist");

        DeferredResult<WVPResult<List<Preset>>> result = new DeferredResult<>();

        result.onTimeout(()->{
            WVPResult<List<Preset>> wvpResult = WVPResult.fail(ErrorCode.ERROR100.getCode(), "Request timeout");
            result.setResult(wvpResult);
        });

        ErrorCallback<List<Preset>> callback = (code, msg, data) -> {
            WVPResult<List<Preset>> wvpResult = new WVPResult<>();
            wvpResult.setCode(code);
            wvpResult.setMsg(msg);
            wvpResult.setData(data);
            result.setResult(wvpResult);
        };

        channelControlService.queryPreset(channel, callback);

        return result;
    }

    private DeferredResult<WVPResult<String>> controlPreset(Integer channelId, FrontEndControlCodeForPreset controlCode) {
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "Channel does not exist");


        DeferredResult<WVPResult<String>> result = new DeferredResult<>();

        result.onTimeout(()->{
            WVPResult<String> wvpResult = WVPResult.fail(ErrorCode.ERROR100.getCode(), "Request timeout");
            result.setResult(wvpResult);
        });

        ErrorCallback<String> callback = (code, msg, data) -> {
            WVPResult<String> wvpResult = new WVPResult<>();
            wvpResult.setCode(code);
            wvpResult.setMsg(msg);
            wvpResult.setData(data);
            result.setResult(wvpResult);
        };

        channelControlService.preset(channel, controlCode, callback);
        return result;
    }

    @Operation(summary = "Preset command-Set preset position", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "Channel national standard number", required = true)
    @Parameter(name = "presetId", description = "Preset number", required = true)
    @Parameter(name = "presetName", description = "Preset position name", required = true)
    @GetMapping("/preset/add")
    public DeferredResult<WVPResult<String>> addPreset(Integer channelId, Integer presetId, String presetName) {
        FrontEndControlCodeForPreset controlCode = new FrontEndControlCodeForPreset();
        controlCode.setCode(1);
        controlCode.setPresetId(presetId);
        controlCode.setPresetName(presetName);

        return controlPreset(channelId, controlCode);
    }

    @Operation(summary = "Preset command-Call preset position", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "Channel national standard number", required = true)
    @Parameter(name = "presetId", description = "Preset number(1-100)", required = true)
    @GetMapping("/preset/call")
    public DeferredResult<WVPResult<String>> callPreset(Integer channelId, Integer presetId) {
        FrontEndControlCodeForPreset controlCode = new FrontEndControlCodeForPreset();
        controlCode.setCode(2);
        controlCode.setPresetId(presetId);

        return controlPreset(channelId, controlCode);
    }

    @Operation(summary = "Preset command-Delete preset position", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "Channel national standard number", required = true)
    @Parameter(name = "presetId", description = "Preset number(1-100)", required = true)
    @GetMapping("/preset/delete")
    public DeferredResult<WVPResult<String>> deletePreset(Integer channelId, Integer presetId) {

        FrontEndControlCodeForPreset controlCode = new FrontEndControlCodeForPreset();
        controlCode.setCode(3);
        controlCode.setPresetId(presetId);

        return controlPreset(channelId, controlCode);
    }

    private DeferredResult<WVPResult<String>> tourControl(Integer channelId, FrontEndControlCodeForTour controlCode) {
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "Channel does not exist");

        DeferredResult<WVPResult<String>> result = new DeferredResult<>();

        result.onTimeout(()->{
            WVPResult<String> wvpResult = WVPResult.fail(ErrorCode.ERROR100.getCode(), "Request timeout");
            result.setResult(wvpResult);
        });

        ErrorCallback<String> callback = (code, msg, data) -> {
            WVPResult<String> wvpResult = new WVPResult<>();
            wvpResult.setCode(code);
            wvpResult.setMsg(msg);
            wvpResult.setData(data);
            result.setResult(wvpResult);
        };

        channelControlService.tour(channel, controlCode, callback);
        return result;
    }

    @Operation(summary = "cruise command-Join a cruise spot", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "Channel national standard number", required = true)
    @Parameter(name = "tourId", description = "Cruise group number", required = true)
    @Parameter(name = "presetId", description = "Preset number", required = true)
    @GetMapping("/tour/point/add")
    public DeferredResult<WVPResult<String>> addTourPoint(Integer channelId, Integer tourId, Integer presetId) {

        FrontEndControlCodeForTour controlCode = new FrontEndControlCodeForTour();
        controlCode.setCode(1);
        controlCode.setPresetId(presetId);
        controlCode.setTourId(tourId);

        return tourControl(channelId, controlCode);
    }

    @Operation(summary = "cruise command-Delete a cruise point", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "Channel national standard number", required = true)
    @Parameter(name = "tourId", description = "Cruise group number(1-100)", required = true)
    @Parameter(name = "presetId", description = "Preset number(0-100, When 0, delete the entire cruise)", required = true)
    @GetMapping("/tour/point/delete")
    public DeferredResult<WVPResult<String>> deleteCruisePoint(Integer channelId, Integer tourId, Integer presetId) {
        FrontEndControlCodeForTour controlCode = new FrontEndControlCodeForTour();
        controlCode.setCode(2);
        controlCode.setPresetId(presetId);
        controlCode.setTourId(tourId);

        return tourControl(channelId, controlCode);
    }

    @Operation(summary = "cruise command-Set cruise speed", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "Channel national standard number", required = true)
    @Parameter(name = "tourId", description = "Cruise group number(0-100)", required = true)
    @Parameter(name = "speed", description = "cruising speed(1-4095)", required = true)
    @Parameter(name = "presetId", description = "Preset number", required = true)
    @GetMapping("/tour/speed")
    public DeferredResult<WVPResult<String>> setCruiseSpeed(Integer channelId, Integer tourId, Integer speed, Integer presetId) {
        FrontEndControlCodeForTour controlCode = new FrontEndControlCodeForTour();
        controlCode.setCode(3);
        controlCode.setTourSpeed(speed);
        controlCode.setTourId(tourId);
        controlCode.setPresetId(presetId);
        return tourControl(channelId, controlCode);
    }

    @Operation(summary = "cruise command-Set cruise stop time", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "Channel national standard number", required = true)
    @Parameter(name = "tourId", description = "Cruise group number", required = true)
    @Parameter(name = "time", description = "Cruise stop time(1-4095)", required = true)
    @Parameter(name = "presetId", description = "Preset number", required = true)
    @GetMapping("/tour/time")
    public DeferredResult<WVPResult<String>> setCruiseTime(Integer channelId, Integer tourId, Integer time, Integer presetId) {
        FrontEndControlCodeForTour controlCode = new FrontEndControlCodeForTour();
        controlCode.setCode(4);
        controlCode.setTourTime(time);
        controlCode.setTourId(tourId);
        controlCode.setPresetId(presetId);
        return tourControl(channelId, controlCode);
    }

    @Operation(summary = "cruise command-start cruise", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "Channel national standard number", required = true)
    @Parameter(name = "tourId", description = "Cruise group number)", required = true)
    @GetMapping("/tour/start")
    public DeferredResult<WVPResult<String>> startCruise(Integer channelId, Integer tourId) {
        FrontEndControlCodeForTour controlCode = new FrontEndControlCodeForTour();
        controlCode.setCode(5);
        controlCode.setTourId(tourId);
        return tourControl(channelId, controlCode);
    }

    @Operation(summary = "cruise command-Stop cruising", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "Channel national standard number", required = true)
    @Parameter(name = "tourId", description = "Cruise group number", required = true)
    @GetMapping("/tour/stop")
    public DeferredResult<WVPResult<String>> stopCruise(Integer channelId, Integer tourId) {
        FrontEndControlCodeForTour controlCode = new FrontEndControlCodeForTour();
        controlCode.setCode(6);
        controlCode.setTourId(tourId);
        return tourControl(channelId, controlCode);
    }

    private DeferredResult<WVPResult<String>> scanControl(Integer channelId, FrontEndControlCodeForScan controlCode) {

        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "Channel does not exist");
        DeferredResult<WVPResult<String>> result = new DeferredResult<>();

        result.onTimeout(()->{
            WVPResult<String> wvpResult = WVPResult.fail(ErrorCode.ERROR100.getCode(), "Request timeout");
            result.setResult(wvpResult);
        });

        ErrorCallback<String> callback = (code, msg, data) -> {
            WVPResult<String> wvpResult = new WVPResult<>();
            wvpResult.setCode(code);
            wvpResult.setMsg(msg);
            wvpResult.setData(data);
            result.setResult(wvpResult);
        };
        channelControlService.scan(channel, controlCode, callback);

        return result;

    }

    @Operation(summary = "scan command-Start automatic scan", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "Channel national standard number", required = true)
    @Parameter(name = "scanId", description = "Scan group number(0-100)", required = true)
    @GetMapping("/scan/start")
    public DeferredResult<WVPResult<String>> startScan(Integer channelId, Integer scanId) {
        FrontEndControlCodeForScan controlCode = new FrontEndControlCodeForScan();
        controlCode.setCode(1);
        controlCode.setScanId(scanId);
        return scanControl(channelId, controlCode);

    }

    @Operation(summary = "scan command-Stop automatic scanning", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "Channel national standard number", required = true)
    @Parameter(name = "scanId", description = "Scan group number(0-100)", required = true)
    @GetMapping("/scan/stop")
    public DeferredResult<WVPResult<String>> stopScan(Integer channelId, Integer scanId) {
        FrontEndControlCodeForScan controlCode = new FrontEndControlCodeForScan();
        controlCode.setCode(5);
        controlCode.setScanId(scanId);
        return scanControl(channelId, controlCode);
    }

    @Operation(summary = "scan command-Set up automatic scanning of the left border", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "Channel national standard number", required = true)
    @Parameter(name = "scanId", description = "Scan group number(0-100)", required = true)
    @GetMapping("/scan/set/left")
    public DeferredResult<WVPResult<String>> setScanLeft(Integer channelId, Integer scanId) {
        FrontEndControlCodeForScan controlCode = new FrontEndControlCodeForScan();
        controlCode.setCode(2);
        controlCode.setScanId(scanId);
        return scanControl(channelId, controlCode);
    }

    @Operation(summary = "scan command-Set up automatic scanning of the right border", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "Channel national standard number", required = true)
    @Parameter(name = "scanId", description = "Scan group number(0-100)", required = true)
    @GetMapping("/scan/set/right")
    public DeferredResult<WVPResult<String>> setScanRight(Integer channelId, Integer scanId) {
        FrontEndControlCodeForScan controlCode = new FrontEndControlCodeForScan();
        controlCode.setCode(3);
        controlCode.setScanId(scanId);
        return scanControl(channelId, controlCode);
    }


    @Operation(summary = "scan command-Set automatic scan speed", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "Channel national standard number", required = true)
    @Parameter(name = "scanId", description = "Scan group number(0-100)", required = true)
    @Parameter(name = "speed", description = "Auto scan speed(1-4095)", required = true)
    @GetMapping("/scan/set/speed")
    public DeferredResult<WVPResult<String>> setScanSpeed(Integer channelId, Integer scanId, Integer speed) {
        FrontEndControlCodeForScan controlCode = new FrontEndControlCodeForScan();
        controlCode.setCode(4);
        controlCode.setScanId(scanId);
        controlCode.setScanSpeed(speed);
        return scanControl(channelId, controlCode);
    }


    @Operation(summary = "Auxiliary switch control instructions-Wiper control", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "Channel national standard number", required = true)
    @Parameter(name = "command", description = "control instructions, allowable values: on, off", required = true)
    @GetMapping("/wiper")
    public DeferredResult<WVPResult<String>> wiper(Integer channelId, String command){

        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "Channel does not exist");

        FrontEndControlCodeForWiper controlCode = new FrontEndControlCodeForWiper();

        switch (command){
            case "on":
                controlCode.setCode(1);
                break;
            case "off":
                controlCode.setCode(2);
                break;
            default:
                break;
        }
        DeferredResult<WVPResult<String>> result = new DeferredResult<>();

        result.onTimeout(()->{
            WVPResult<String> wvpResult = WVPResult.fail(ErrorCode.ERROR100.getCode(), "Request timeout");
            result.setResult(wvpResult);
        });

        ErrorCallback<String> callback = (code, msg, data) -> {
            WVPResult<String> wvpResult = new WVPResult<>();
            wvpResult.setCode(code);
            wvpResult.setMsg(msg);
            wvpResult.setData(data);
            result.setResult(wvpResult);
        };

        channelControlService.wiper(channel, controlCode, callback);

        return result;
    }

    @Operation(summary = "Auxiliary switch control instructions", security = @SecurityRequirement(name = JwtUtils.HEADER))

    @Parameter(name = "channelId", description = "Channel national standard number", required = true)
    @Parameter(name = "command", description = "control instructions, allowable values: on, off", required = true)
    @Parameter(name = "auxiliaryId", description = "Switch number", required = true)
    @GetMapping("/auxiliary")
    public DeferredResult<WVPResult<String>> auxiliarySwitch(Integer channelId, String command, Integer auxiliaryId){

        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "Channel does not exist");

        FrontEndControlCodeForAuxiliary controlCode = new FrontEndControlCodeForAuxiliary();
        controlCode.setAuxiliaryId(auxiliaryId);
        switch (command){
            case "on":
                controlCode.setCode(1);
                break;
            case "off":
                controlCode.setCode(2);
                break;
            default:
                break;
        }
        DeferredResult<WVPResult<String>> result = new DeferredResult<>();

        result.onTimeout(()->{
            WVPResult<String> wvpResult = WVPResult.fail(ErrorCode.ERROR100.getCode(), "Request timeout");
            result.setResult(wvpResult);
        });

        ErrorCallback<String> callback = (code, msg, data) -> {
            WVPResult<String> wvpResult = new WVPResult<>();
            wvpResult.setCode(code);
            wvpResult.setMsg(msg);
            wvpResult.setData(data);
            result.setResult(wvpResult);
        };
        channelControlService.auxiliary(channel, controlCode, callback);
        return result;
    }

    @Operation(summary = "Watch bit setting", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "channelID", required = true)
    @Parameter(name = "enabled", description = "Whether to enable guard position", required = true)
    @Parameter(name = "resetTime", description = "Automatic homing time interval (seconds）")
    @Parameter(name = "presetIndex", description = "Call preset number")
    @GetMapping("/home_position")
    public DeferredResult<WVPResult<String>> homePosition(Integer channelId, Boolean enabled,
                                                           @RequestParam(required = false) Integer resetTime,
                                                           @RequestParam(required = false) Integer presetIndex){

        if (log.isDebugEnabled()) {
            log.debug("[Universal channel]Guard bit setting API call，channelId：{} ，enabled：{} ，resetTime：{} ，presetIndex：{}", channelId, enabled, resetTime, presetIndex);
        }

        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "Channel does not exist");

        DeferredResult<WVPResult<String>> result = new DeferredResult<>();

        result.onTimeout(()->{
            WVPResult<String> wvpResult = WVPResult.fail(ErrorCode.ERROR100.getCode(), "Request timeout");
            result.setResult(wvpResult);
        });

        ErrorCallback<String> callback = (code, msg, data) -> {
            WVPResult<String> wvpResult = new WVPResult<>();
            wvpResult.setCode(code);
            wvpResult.setMsg(msg);
            wvpResult.setData(data);
            result.setResult(wvpResult);
        };

        channelControlService.homePosition(channel, enabled, resetTime, presetIndex, callback);
        return result;
    }

    @Operation(summary = "Scroll down to enlarge", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "channelID", required = true)
    @Parameter(name = "length", description = "Play window length pixel value", required = true)
    @Parameter(name = "width", description = "Play window width pixel value", required = true)
    @Parameter(name = "midPointX", description = "The horizontal axis coordinate pixel value of the center of the pull box", required = true)
    @Parameter(name = "midPointY", description = "The vertical axis coordinate pixel value of the center of the pull box", required = true)
    @Parameter(name = "lengthX", description = "Frame length in pixels", required = true)
    @Parameter(name = "lengthY", description = "Pull box width pixel value", required = true)
    @GetMapping("/drag_zoom_in")
    public DeferredResult<WVPResult<String>> dragZoomIn(Integer channelId, int length, int width, int midPointX, int midPointY, int lengthX, int lengthY){

        if (log.isDebugEnabled()) {
            log.debug("[Universal channel]Pull box to enlarge API call，channelId：{} ，length：{} ，width：{} ，midPointX：{} ，midPointY：{} ，lengthX：{} ，lengthY：{}",channelId, length, width, midPointX, midPointY, lengthX, lengthY);
        }

        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "Channel does not exist");

        FrontEndControlCodeForDragZoom controlCode = new FrontEndControlCodeForDragZoom();
        controlCode.setCode(1);
        controlCode.setLength(length);
        controlCode.setWidth(width);
        controlCode.setMidPointX(midPointX);
        controlCode.setMidPointY(midPointY);
        controlCode.setLengthX(lengthX);
        controlCode.setLengthY(lengthY);

        DeferredResult<WVPResult<String>> result = new DeferredResult<>();

        result.onTimeout(()->{
            WVPResult<String> wvpResult = WVPResult.fail(ErrorCode.ERROR100.getCode(), "Request timeout");
            result.setResult(wvpResult);
        });

        ErrorCallback<String> callback = (code, msg, data) -> {
            WVPResult<String> wvpResult = new WVPResult<>();
            wvpResult.setCode(code);
            wvpResult.setMsg(msg);
            wvpResult.setData(data);
            result.setResult(wvpResult);
        };
        channelControlService.dragZoom(channel, controlCode, callback);
        return result;
    }

    @Operation(summary = "Zoom out", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "channelID", required = true)
    @Parameter(name = "length", description = "Play window length pixel value", required = true)
    @Parameter(name = "width", description = "Play window width pixel value", required = true)
    @Parameter(name = "midPointX", description = "The horizontal axis coordinate pixel value of the center of the pull box", required = true)
    @Parameter(name = "midPointY", description = "The vertical axis coordinate pixel value of the center of the pull box", required = true)
    @Parameter(name = "lengthX", description = "Frame length in pixels", required = true)
    @Parameter(name = "lengthY", description = "Pull box width pixel value", required = true)
    @GetMapping("/drag_zoom_out")
    public DeferredResult<WVPResult<String>> dragZoomOut(Integer channelId, Integer length, Integer width, Integer midPointX, Integer midPointY, Integer lengthX, Integer lengthY){

        if (log.isDebugEnabled()) {
            log.debug("[Universal channel]Zoom out API call，channelId：{} ，length：{} ，width：{} ，midPointX：{} ，midPointY：{} ，lengthX：{} ，lengthY：{}",channelId, length, width, midPointX, midPointY, lengthX, lengthY);
        }

        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "Channel does not exist");

        FrontEndControlCodeForDragZoom controlCode = new FrontEndControlCodeForDragZoom();
        controlCode.setCode(2);
        controlCode.setLength(length);
        controlCode.setWidth(width);
        controlCode.setMidPointX(midPointX);
        controlCode.setMidPointY(midPointY);
        controlCode.setLengthX(lengthX);
        controlCode.setLengthY(lengthY);

        DeferredResult<WVPResult<String>> result = new DeferredResult<>();

        result.onTimeout(()->{
            WVPResult<String> wvpResult = WVPResult.fail(ErrorCode.ERROR100.getCode(), "Request timeout");
            result.setResult(wvpResult);
        });

        ErrorCallback<String> callback = (code, msg, data) -> {
            WVPResult<String> wvpResult = new WVPResult<>();
            wvpResult.setCode(code);
            wvpResult.setMsg(msg);
            wvpResult.setData(data);
            result.setResult(wvpResult);
        };
        channelControlService.dragZoom(channel, controlCode, callback);
        return result;
    }
}
