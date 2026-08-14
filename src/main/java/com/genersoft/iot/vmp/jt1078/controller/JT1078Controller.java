package com.genersoft.iot.vmp.jt1078.controller;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.ftpServer.FtpSetting;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.jt1078.bean.*;
import com.genersoft.iot.vmp.jt1078.controller.bean.*;
import com.genersoft.iot.vmp.jt1078.proc.request.J1205;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078PlayService;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;


@Slf4j
@ConditionalOnProperty(value = "jt1078.enable", havingValue = "true")
@RestController
@Tag(name  = "Ministry standard equipment control")
@RequestMapping("/api/jt1078")
public class JT1078Controller {

    @Resource
    private Ijt1078Service service;

    @Resource
    private Ijt1078PlayService jt1078PlayService;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private FtpSetting ftpSetting;

    @Operation(summary = "JT-Start on demand", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device mobile phone number", required = true)
    @Parameter(name = "channelId", description = "Channel number, usually a number starting from 1", required = true)
    @Parameter(name = "type", description = "Type: 0: audio and video, 1: video, 3: audio", required = true)
    @GetMapping("/live/start")
    public DeferredResult<WVPResult<StreamContent>> startLive(HttpServletRequest request,
                                                              @Parameter(required = true) String phoneNumber,
                                                              @Parameter(required = true) Integer channelId,
                                                              @Parameter(required = false) Integer type) {
        if (type == null || (type != 0 && type != 1 && type != 3)) {
            type = 0;
        }
        DeferredResult<WVPResult<StreamContent>> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());
        result.onTimeout(()->{
            log.info("[JT-On-demand waiting timeout] phoneNumber：{}, channelId：{}, ", phoneNumber, channelId);
            // releasertpserver
            WVPResult<StreamContent> wvpResult = new WVPResult<>();
            wvpResult.setCode(ErrorCode.ERROR100.getCode());
            wvpResult.setMsg("timeout");
            result.setResult(wvpResult);
            jt1078PlayService.stopPlay(phoneNumber, channelId);
        });

        jt1078PlayService.play(phoneNumber, channelId, type, wvpResult -> {
            WVPResult<StreamContent> wvpResultForFinish = new WVPResult<>();
            wvpResultForFinish.setCode(wvpResult.getCode());
            wvpResultForFinish.setMsg(wvpResult.getMsg());
            if (wvpResult.getCode() == InviteErrorCode.SUCCESS.getCode()) {
                StreamInfo streamInfo  = wvpResult.getData();

                if (streamInfo != null) {
                    if (userSetting.getUseSourceIpAsStreamIp()) {
                        streamInfo=streamInfo.clone();//deep copy
                        String host;
                        try {
                            URL url=new URL(request.getRequestURL().toString());
                            host=url.getHost();
                        } catch (MalformedURLException e) {
                            host=request.getLocalAddr();
                        }
                        streamInfo.changeStreamIp(host);
                    }
                    wvpResultForFinish.setData(new StreamContent(streamInfo));
                }
            }
            result.setResult(wvpResultForFinish);
        });

        return result;
    }

    @Operation(summary = "JT-End on demand", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device mobile phone number", required = true)
    @Parameter(name = "channelId", description = "Channel national standard number, usually a number starting from 1", required = true)
    @GetMapping("/live/stop")
    public void stopLive(HttpServletRequest request,
                                                              @Parameter(required = true) String phoneNumber,
                                                              @Parameter(required = true) Integer channelId) {
        jt1078PlayService.stopPlay(phoneNumber, channelId);
    }

    @Operation(summary = "JT-Voice intercom", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device mobile phone number", required = true)
    @Parameter(name = "channelId", description = "Channel national standard number, usually a number starting from 1", required = true)
    @GetMapping("/talk/start")
    public StreamContent startTalk(HttpServletRequest request,
                         @Parameter(required = true) String phoneNumber,
                         @Parameter(required = true) Integer channelId) {

        StreamInfo streamInfo = jt1078PlayService.startTalk(phoneNumber, channelId);
        if (userSetting.getUseSourceIpAsStreamIp()) {
            String host;
            try {
                URL url=new URL(request.getRequestURL().toString());
                host=url.getHost();
            } catch (MalformedURLException e) {
                host=request.getLocalAddr();
            }
            streamInfo.changeStreamIp(host);
        }
        streamInfo.setIp("localhost");
        return new StreamContent(streamInfo);
    }

    @Operation(summary = "JT-End talk", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device mobile phone number", required = true)
    @Parameter(name = "channelId", description = "Channel national standard number, usually a number starting from 1", required = true)
    @GetMapping("/talk/stop")
    public void stopTalk(HttpServletRequest request,
                         @Parameter(required = true) String phoneNumber,
                         @Parameter(required = true) Integer channelId) {
        jt1078PlayService.stopTalk(phoneNumber, channelId);
    }


    @Operation(summary = "JT-Pause on demand", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device mobile phone number", required = true)
    @Parameter(name = "channelId", description = "Channel national standard number, usually a number starting from 1", required = true)
    @GetMapping("/live/pause")
    public void pauseLive(HttpServletRequest request,
                         @Parameter(required = true) String phoneNumber,
                         @Parameter(required = true) Integer channelId) {
        jt1078PlayService.pausePlay(phoneNumber, channelId);
    }

    @Operation(summary = "JT-Continue on demand", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device mobile phone number", required = true)
    @Parameter(name = "channelId", description = "Channel national standard number, usually a number starting from 1", required = true)
    @GetMapping("/live/continue")
    public void continueLive(HttpServletRequest request,
                          @Parameter(required = true) String phoneNumber,
                          @Parameter(required = true) Integer channelId) {

        jt1078PlayService.continueLivePlay(phoneNumber, channelId);
    }

    @Operation(summary = "JT-Switch code stream type", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device mobile phone number", required = true)
    @Parameter(name = "channelId", description = "Channel national standard number, usually a number starting from 1", required = true)
    @Parameter(name = "streamType", description = "0:main stream; 1:substream", required = true)
    @GetMapping("/live/switch")
    public void changeStreamType(HttpServletRequest request,
                             @Parameter(required = true) String phoneNumber,
                             @Parameter(required = true) Integer channelId,
                             @Parameter(required = true) Integer streamType) {
        service.changeStreamType(phoneNumber, channelId, streamType);
    }

    @Operation(summary = "JT-Video-Query resource list", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device mobile phone number", required = true)
    @Parameter(name = "channelId", description = "Channel national standard number, usually a number starting from 1", required = true)
    @Parameter(name = "startTime", description = "start time, format： yyyy-MM-dd HH:mm:ss", required = true)
    @Parameter(name = "endTime", description = "end time, format： yyyy-MM-dd HH:mm:ss", required = true)
    @GetMapping("/record/list")
    public WVPResult<List<J1205.JRecordItem>> playbackList(HttpServletRequest request,
                                                                     @Parameter(required = true) String phoneNumber,
                                                                     @Parameter(required = true) Integer channelId,
                                                                     @Parameter(required = true) String startTime,
                                                                     @Parameter(required = true) String endTime
    ) {
        List<J1205.JRecordItem> recordList = jt1078PlayService.getRecordList(phoneNumber, channelId, startTime, endTime);
        if (recordList == null) {
            return WVPResult.fail(ErrorCode.ERROR100);
        }else {
            return WVPResult.success(recordList);
        }
    }
    @Operation(summary = "JT-Video-Start playback", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device mobile phone number", required = true)
    @Parameter(name = "channelId", description = "Channel national standard number, usually a number starting from 1", required = true)
    @Parameter(name = "startTime", description = "start time, format： yyyy-MM-dd HH:mm:ss", required = true)
    @Parameter(name = "endTime", description = "end time, format： yyyy-MM-dd HH:mm:ss", required = true)
    @Parameter(name = "type", description = "0.Audio and video 1. Audio 2. Video 3. Video or audio and video", required = true)
    @Parameter(name = "rate", description = "0.All code streams 1. Main code stream 2. Sub code stream(If this channel only transmits audio, this field is set to0)", required = true)
    @Parameter(name = "playbackType", description = "0.Normal playback 1. Fast forward playback 2. Key frame fast rewind playback 3. Key frame playback 4. Single frame upload", required = true)
    @Parameter(name = "playbackSpeed", description = "0.Invalid 1.1 times 2.2 times 3.4 times 4.8 times 5.16 times (When the playback control is 1 and 2, the content of this field is valid, otherwise it is set0)", required = true)
    @GetMapping("/playback/start")
    public DeferredResult<WVPResult<StreamContent>> recordLive(HttpServletRequest request,
                                                              @Parameter(required = true) String phoneNumber,
                                                              @Parameter(required = true) Integer channelId,
                                                              @Parameter(required = true) String startTime,
                                                              @Parameter(required = true) String endTime,
                                                              @Parameter(required = false) Integer type,
                                                              @Parameter(required = false) Integer rate,
                                                              @Parameter(required = false) Integer playbackType,
                                                              @Parameter(required = false) Integer playbackSpeed

    ) {
        DeferredResult<WVPResult<StreamContent>> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());
        result.onTimeout(()->{
            log.info("[JT-Playback-Wait timeout] phoneNumber：{}, channelId：{}, ", phoneNumber, channelId);
            // releasertpserver
            WVPResult<StreamContent> wvpResult = new WVPResult<>();
            wvpResult.setCode(ErrorCode.ERROR100.getCode());
            wvpResult.setMsg("Playback timeout");
            result.setResult(wvpResult);
            jt1078PlayService.stopPlay(phoneNumber, channelId);
        });

        jt1078PlayService.playback(phoneNumber, channelId, startTime, endTime,type, rate, playbackType, playbackSpeed,  wvpResult -> {
            WVPResult<StreamContent> wvpResultForFinish = new WVPResult<>();
            wvpResultForFinish.setCode(wvpResult.getCode());
            wvpResultForFinish.setMsg(wvpResult.getMsg());
            if (wvpResult.getCode() == InviteErrorCode.SUCCESS.getCode()) {
                StreamInfo streamInfo  = wvpResult.getData();
                if (streamInfo != null) {
                    if (userSetting.getUseSourceIpAsStreamIp()) {
                        String host;
                        try {
                            URL url=new URL(request.getRequestURL().toString());
                            host=url.getHost();
                        } catch (MalformedURLException e) {
                            host=request.getLocalAddr();
                        }
                        streamInfo.changeStreamIp(host);
                    }
                    wvpResultForFinish.setData(new StreamContent(streamInfo));
                }
            }
            result.setResult(wvpResultForFinish);
        });

        return result;
    }

    @Operation(summary = "JT-Video-Playback control", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device mobile phone number", required = true)
    @Parameter(name = "channelId", description = "Channel national standard number, usually a number starting from 1", required = true)
    @Parameter(name = "command", description = "0:Start playback; 1:Pause playback; 2:End playback; 3:Fast forward playback; 4:Keyframe fast rewind playback; 5:Drag playback; 6:Keyframe playback", required = true)
    @Parameter(name = "playbackSpeed", description = "0.Invalid 1.1 times 2.2 times 3.4 times 4.8 times 5.16 times (When the playback control is 3 and 4, the content of this field is valid, otherwise it is set0)", required = false)
    @Parameter(name = "time", description = "Drag playback position(time)", required = false)
    @GetMapping("/playback/control")
    public void recordControl(@Parameter(required = true) String phoneNumber,
                              @Parameter(required = true) Integer channelId,
                              @Parameter(required = false) Integer command,
                              @Parameter(required = false) String time,
                              @Parameter(required = false) Integer playbackSpeed

    ) {
        jt1078PlayService.playbackControl(phoneNumber, channelId, command, playbackSpeed, time);
    }

    @Operation(summary = "JT-Video-End playback", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device mobile phone number", required = true)
    @Parameter(name = "channelId", description = "Channel national standard number, usually a number starting from 1", required = true)
    @GetMapping("/playback/stop")
    public void stopPlayback(HttpServletRequest request,
                         @Parameter(required = true) String phoneNumber,
                         @Parameter(required = true) Integer channelId) {
        jt1078PlayService.stopPlayback(phoneNumber, channelId);
    }

    @Operation(summary = "JT-Video-Get download address", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device mobile phone number", required = true)
    @Parameter(name = "channelId", description = "Channel national standard number, usually a number starting from 1", required = true)
    @Parameter(name = "startTime", description = "start time, format： yyyy-MM-dd HH:mm:ss", required = true)
    @Parameter(name = "endTime", description = "end time, format： yyyy-MM-dd HH:mm:ss", required = true)
    @Parameter(name = "alarmSign", description = "Alarm sign", required = true)
    @Parameter(name = "mediaType", description = "Audio and video resource types: 0. Audio and video 1. Audio 2. Video 3. Video or audio and video", required = true)
    @Parameter(name = "streamType", description = "Stream type: 0. All streams 1. Main stream 2. Sub-stream(If this channel only transmits audio, this field is set to0)", required = true)
    @Parameter(name = "storageType", description = "memory type", required = true)
    @GetMapping("/playback/downloadUrl")
    public String getRecordTempUrl(HttpServletRequest request,
                                   @Parameter(required = true) String phoneNumber,
                                   @Parameter(required = true) Integer channelId,
                                   @Parameter(required = true) String startTime,
                                   @Parameter(required = true) String endTime,
                                   @Parameter(required = false) Integer alarmSign,
                                   @Parameter(required = false) Integer mediaType,
                                   @Parameter(required = false) Integer streamType,
                                   @Parameter(required = false) Integer storageType

    ){
        log.info("[JT-Video] download, device:{}， channel： {}， start time： {}， end time： {}，Alarm sign: {}, Audio and video type： {}， Stream type： {}，memory type： {}， ",
                phoneNumber, channelId, startTime, endTime, alarmSign, mediaType, streamType, storageType);
        if (!ftpSetting.getEnable()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "The ftp service is not enabled and the video cannot be downloaded.");
        }
        return service.getRecordTempUrl(phoneNumber, channelId, startTime, endTime, alarmSign, mediaType, streamType, storageType);
    }

    @Operation(summary = "JT-Video-Download", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "path", description = "Temporary download path", required = true)
    @GetMapping("/playback/download")
    public void download(HttpServletRequest request, HttpServletResponse response, @Parameter(required = true) String path) throws IOException {
        if (!ftpSetting.getEnable()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "The ftp service is not enabled and the video cannot be downloaded.");
        }
        DeferredResult<String> result = new DeferredResult<>();
        ServletOutputStream outputStream = response.getOutputStream();
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(path + ".mp4", "UTF-8"));
//        response.setContentLength(394983300);
        response.setStatus(HttpServletResponse.SC_OK);
        service.recordDownload(path, outputStream);
    }

    @Operation(summary = "JT-PTZ control", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device mobile phone number", required = true)
    @Parameter(name = "channelId", description = "Channel national standard number, usually a number starting from 1", required = true)
    @Parameter(name = "command", description = "control instructions, allowable values: left, right, up, down, zoomin, zoomout, irisin, irisout, focusnear, focusfar, stop", required = true)
    @Parameter(name = "speed", description = "speed(0-255)， command,value left, right, up, downvalid when", required = true)
    @GetMapping("/ptz")
    public void ptz(String phoneNumber, Integer channelId, String command, int speed){

        log.info("[JT-PTZ control] phoneNumber：{}, channelId：{}, command: {}, speed: {}", phoneNumber, channelId, command, speed);
        service.ptzControl(phoneNumber, channelId, command, speed);
    }

    @Operation(summary = "JT-Fill light switch", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device mobile phone number", required = true)
    @Parameter(name = "channelId", description = "Channel national standard number, usually a number starting from 1", required = true)
    @Parameter(name = "command", description = "control instructions, allowable values: on off", required = true)
    @GetMapping("/fill-light")
    public void fillLight(String phoneNumber, Integer channelId, String command){

        log.info("[JT-Fill light switch] phoneNumber：{}, channelId：{}, command: {}", phoneNumber, channelId, command);
        service.supplementaryLight(phoneNumber, channelId, command);
    }

    @Operation(summary = "JT-wiper switch", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device mobile phone number", required = true)
    @Parameter(name = "channelId", description = "Channel national standard number, usually a number starting from 1", required = true)
    @Parameter(name = "command", description = "control instructions, allowable values: on off", required = true)
    @GetMapping("/wiper")
    public void wiper(String phoneNumber, Integer channelId, String command){

        log.info("[JT-wiper switch] phoneNumber：{}, channelId：{}, command: {}", phoneNumber, channelId, command);
        service.wiper(phoneNumber, channelId, command);
    }

    @Operation(summary = "JT-Query terminal parameters", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device mobile phone number", required = true)
    @GetMapping("/config/get")
    public JTDeviceConfig config(String phoneNumber, String[] params){

        log.info("[JT-Query terminal parameters] phoneNumber：{}", phoneNumber);
        return service.queryConfig(phoneNumber, params);
    }

    @Operation(summary = "JT-Set terminal parameters", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device number", required = true)
    @Parameter(name = "config", description = "Terminal parameters", required = true)
    @PostMapping("/config/set")
    public void setConfig(@RequestBody SetConfigParam config){

        log.info("[JT-Set terminal parameters] parameters: {}", config.toString());
        service.setConfig(config.getPhoneNumber(), config.getConfig());
    }

    @Operation(summary = "terminal control-Connect to the specified server", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "control", description = "Terminal control parameters", required = true)
    @PostMapping("/control/connection")
    public void connectionControl(@RequestBody ConnectionControlParam control){

        log.info("[JT-terminal control] parameters: {}", control.toString());
        service.connectionControl(control.getPhoneNumber(), control.getControl());
    }

    @Operation(summary = "JT-terminal control-reset", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device number", required = true)
    @PostMapping("/control/reset")
    public void resetControl(String phoneNumber){

        log.info("[JT-reset] phoneNumber: {}", phoneNumber);
        service.resetControl(phoneNumber);
    }

    @Operation(summary = "JT-terminal control-Factory reset", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device number", required = true)
    @PostMapping("/control/factory-reset")
    public void factoryResetControl(String phoneNumber){

        log.info("[JT-Factory reset] phoneNumber: {}", phoneNumber);
        service.factoryResetControl(phoneNumber);
    }

    @Operation(summary = "JT-Query terminal properties", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device number", required = true)
    @GetMapping("/attribute")
    public JTDeviceAttribute attribute(String phoneNumber){

        log.info("[JT-Query terminal properties] phoneNumber: {}", phoneNumber);
        return service.attribute(phoneNumber);
    }

    @Operation(summary = "JT-Query location information", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device number", required = true)
    @GetMapping("/position-info")
    public JTPositionBaseInfo queryPositionInfo(String phoneNumber){

        log.info("[JT-Query location information] phoneNumber: {}", phoneNumber);
        return service.queryPositionInfo(phoneNumber);
    }

    @Operation(summary = "JT-Temporary location tracking control", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device number", required = true)
    @Parameter(name = "timeInterval", description = "Time interval, unit is second, stop tracking when the time interval is 0, no subsequent fields are required to stop tracking.", required = true)
    @Parameter(name = "validityPeriod", description = "Location tracking validity period, in seconds. After receiving the location tracking control message, the terminal will send a location report according to the time interval in the message before the expiration date of the validity period.", required = true)
    @GetMapping("/control/temp-position-tracking")
    public void tempPositionTrackingControl(String phoneNumber, Integer timeInterval, Long validityPeriod){

        log.info("[JT-Temporary location tracking control] phoneNumber: {}, time interval {}Seconds, location tracking validity period {}seconds", phoneNumber, timeInterval, validityPeriod);
        service.tempPositionTrackingControl(phoneNumber, timeInterval, validityPeriod);
    }

    @Operation(summary = "JT-Manually confirm alarm message", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device number", required = true)
    @Parameter(name = "timeInterval", description = "Time interval, unit is second, stop tracking when the time interval is 0, no subsequent fields are required to stop tracking.", required = true)
    @Parameter(name = "validityPeriod", description = "Location tracking validity period, in seconds. After receiving the location tracking control message, the terminal will send a location report according to the time interval in the message before the expiration date of the validity period.", required = true)
    @PostMapping("/confirmation-alarm-message")
    public void confirmationAlarmMessage(@RequestBody ConfirmationAlarmMessageParam param){

        log.info("[JT-Manually confirm alarm message] parameters: {}", param);
        service.confirmationAlarmMessage(param.getPhoneNumber(), param.getAlarmPackageNo(), param.getAlarmMessageType());
    }

    @Operation(summary = "JT-Link detection", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device number", required = true)
    @GetMapping("/link-detection")
    public Integer linkDetection(String phoneNumber){

        log.info("[JT-Link detection] phoneNumber: {}", phoneNumber);
        return service.linkDetection(phoneNumber);
    }

    @Operation(summary = "JT-Text message delivery", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "textMessageParam", description = "Text message delivery parameters", required = true)
    @PostMapping("/text-msg")
    public WVPResult<Integer> textMessage(@RequestBody TextMessageParam textMessageParam){

        log.info("[JT-Text message delivery] textMessageParam: {}", textMessageParam);
        int result = service.textMessage(textMessageParam.getPhoneNumber(), textMessageParam.getSign(), textMessageParam.getTextType(), textMessageParam.getContent());
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "JT-Call back", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device number", required = true)
    @Parameter(name = "sign", description = "Flag: 0: normal call, 1: monitoring", required = true)
    @Parameter(name = "destPhoneNumber", description = "call back phone number", required = true)
    @GetMapping("/telephone-callback")
    public WVPResult<Integer> telephoneCallback(String phoneNumber, Integer sign, String destPhoneNumber){

        log.info("[JT-Call back] phoneNumber: {}, sign: {}, phoneNumber: {},", phoneNumber, sign, phoneNumber);
        int result = service.telephoneCallback(phoneNumber, sign, destPhoneNumber);
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "JT-Set up phone book", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "setPhoneBookParam", description = "Set phonebook parameters", required = true)
    @PostMapping("/set-phone-book")
    public WVPResult<Integer> setPhoneBook(@RequestBody SetPhoneBookParam setPhoneBookParam){

        log.info("[JT-Set up phone book] setPhoneBookParam: {}", setPhoneBookParam);
        int result = service.setPhoneBook(setPhoneBookParam.getPhoneNumber(), setPhoneBookParam.getType(), setPhoneBookParam.getPhoneBookContactList());
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "JT-door control", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device number", required = true)
    @Parameter(name = "open", description = "open car door", required = true)
    @GetMapping("/control/door")
    public WVPResult<Integer> controlDoor(String phoneNumber, Boolean open){

        log.info("[JT-door control] phoneNumber: {}, open: {},", phoneNumber, open);
        JTPositionBaseInfo positionBaseInfo = service.controlDoor(phoneNumber, open);
        if (positionBaseInfo == null || positionBaseInfo.getStatus() == null) {
            return WVPResult.fail(ErrorCode.ERROR100.getCode(), "control failure");
        }
        if (open == !positionBaseInfo.getStatus().isDoorLocking()) {
            return WVPResult.success(null);
        }else {
            return WVPResult.fail(ErrorCode.ERROR100.getCode(), "control failure");
        }
    }

    @Operation(summary = "JT-Update circular area", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "areaParam", description = "Set zone parameters", required = true)
    @PostMapping("/area/circle/update")
    public WVPResult<Integer> updateAreaForCircle(@RequestBody SetAreaParam areaParam){

        log.info("[JT-Update circular area] areaParam: {},", areaParam);
        int result = service.setAreaForCircle(0, areaParam.getPhoneNumber(), areaParam.getCircleAreaList());
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "JT-Add circular area", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "areaParam", description = "Set zone parameters", required = true)
    @PostMapping("/area/circle/add")
    public WVPResult<Integer> addAreaForCircle(@RequestBody SetAreaParam areaParam){

        log.info("[JT-Add circular area] areaParam: {},", areaParam);
        int result = service.setAreaForCircle(1, areaParam.getPhoneNumber(), areaParam.getCircleAreaList());
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "JT-Modify circular area", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "areaParam", description = "Set zone parameters", required = true)
    @PostMapping("/area/circle/edit")
    public WVPResult<Integer> editAreaForCircle(@RequestBody SetAreaParam areaParam){

        log.info("[JT-Modify circular area] areaParam: {},", areaParam);
        int result = service.setAreaForCircle(2, areaParam.getPhoneNumber(), areaParam.getCircleAreaList());
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "JT-Delete circular area", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device number", required = true)
    @Parameter(name = "ids", description = "The id of the circular area to be deleted, for example1,2,3", required = true)
    @GetMapping("/area/circle/delete")
    public WVPResult<Integer> deleteAreaForCircle(String phoneNumber, @RequestParam(value = "ids", required = false) List<Long> ids){

        log.info("[JT-Delete circular area] phoneNumber: {}, ids:{}", phoneNumber, ids);
        int result = service.deleteAreaForCircle(phoneNumber, ids);
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "JT-Query circular area", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device number", required = true)
    @GetMapping("/area/circle/query")
    public WVPResult<List<JTAreaOrRoute>> queryAreaForCircle(String phoneNumber, @RequestParam(value = "ids", required = false) List<Long> ids){

        log.info("[JT-Query circular area] phoneNumber: {}, ids:{}", phoneNumber, ids);
        List<JTAreaOrRoute> result = service.queryAreaForCircle(phoneNumber, ids);
        if (result != null) {
            return WVPResult.success(result);
        }else {
            return WVPResult.fail(ErrorCode.ERROR100);
        }
    }


    @Operation(summary = "JT-Update rectangular area", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "areaParam", description = "Set zone parameters", required = true)
    @PostMapping("/area/rectangle/update")
    public WVPResult<Integer> updateAreaForRectangle(@RequestBody SetAreaParam areaParam){

        log.info("[JT-Update rectangular area] areaParam: {},", areaParam);
        int result = service.setAreaForRectangle(0, areaParam.getPhoneNumber(), areaParam.getRectangleAreas());
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "JT-Add rectangular area", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "areaParam", description = "Set zone parameters", required = true)
    @PostMapping("/area/rectangle/add")
    public WVPResult<Integer> addAreaForRectangle(@RequestBody SetAreaParam areaParam){

        log.info("[JT-Add rectangular area] areaParam: {},", areaParam);
        int result = service.setAreaForRectangle(1, areaParam.getPhoneNumber(), areaParam.getRectangleAreas());
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "JT-Modify rectangular area", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "areaParam", description = "Set zone parameters", required = true)
    @PostMapping("/area/rectangle/edit")
    public WVPResult<Integer> editAreaForRectangle(@RequestBody SetAreaParam areaParam){

        log.info("[JT-Modify rectangular area] areaParam: {},", areaParam);
        int result = service.setAreaForRectangle(2, areaParam.getPhoneNumber(), areaParam.getRectangleAreas());
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "JT-Delete rectangular area", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device number", required = true)
    @Parameter(name = "ids", description = "The id of the circular area to be deleted, for example1,2,3", required = true)
    @GetMapping("/area/rectangle/delete")
    public WVPResult<Integer> deleteAreaForRectangle(String phoneNumber, @RequestParam(value = "ids", required = false) List<Long> ids){

        log.info("[JT-Delete rectangular area] phoneNumber: {}, ids:{}", phoneNumber, ids);
        int result = service.deleteAreaForRectangle(phoneNumber, ids);
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "JT-Query rectangular area", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device number", required = true)
    @GetMapping("/area/rectangle/query")
    public WVPResult<List<JTAreaOrRoute>> queryAreaForRectangle(String phoneNumber, @RequestParam(value = "ids", required = false) List<Long> ids){

        log.info("[JT-Query rectangular area] phoneNumber: {}, ids:{}", phoneNumber, ids);
        List<JTAreaOrRoute> result = service.queryAreaForRectangle(phoneNumber, ids);
        if (result != null) {
            return WVPResult.success(result);
        }else {
            return WVPResult.fail(ErrorCode.ERROR100);
        }
    }

    @Operation(summary = "JT-Set polygon area", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "areaParam", description = "Set zone parameters", required = true)
    @PostMapping("/area/polygon/set")
    public WVPResult<Integer> setAreaForPolygon(@RequestBody SetAreaParam areaParam){

        log.info("[JT-Set polygon area] areaParam: {},", areaParam);
        int result = service.setAreaForPolygon(areaParam.getPhoneNumber(), areaParam.getPolygonArea());
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "JT-Delete polygon area", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device number", required = true)
    @Parameter(name = "ids", description = "The id of the circular area to be deleted, for example1,2,3", required = true)
    @GetMapping("/area/polygon/delete")
    public WVPResult<Integer> deleteAreaForPolygon(String phoneNumber, @RequestParam(value = "ids", required = false) List<Long> ids){

        log.info("[JT-Delete polygon area] phoneNumber: {}, ids:{}", phoneNumber, ids);
        int result = service.deleteAreaForPolygon(phoneNumber, ids);
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "JT-Query polygon area", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device number", required = true)
    @GetMapping("/area/polygon/query")
    public WVPResult<List<JTAreaOrRoute>> queryAreaForPolygon(String phoneNumber, @RequestParam(value = "ids", required = false) List<Long> ids){

        log.info("[JT-Query polygon area] phoneNumber: {}, ids:{}", phoneNumber, ids);
        List<JTAreaOrRoute> result = service.queryAreaForPolygon(phoneNumber, ids);
        if (result != null) {
            return WVPResult.success(result);
        }else {
            return WVPResult.fail(ErrorCode.ERROR100);
        }
    }

    @Operation(summary = "JT-Set route", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "areaParam", description = "Set zone parameters", required = true)
    @PostMapping("/route/set")
    public WVPResult<Integer> setRoute(@RequestBody SetAreaParam areaParam){

        log.info("[JT-Set route] areaParam: {},", areaParam);
        int result = service.setRoute(areaParam.getPhoneNumber(), areaParam.getRoute());
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "JT-Delete route", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device number", required = true)
    @Parameter(name = "ids", description = "The id of the circular area to be deleted, for example1,2,3", required = true)
    @GetMapping("/route/delete")
    public WVPResult<Integer> deleteRoute(String phoneNumber, @RequestParam(value = "ids", required = false) List<Long> ids){

        log.info("[JT-Delete route] phoneNumber: {}, ids:{}", phoneNumber, ids);
        int result = service.deleteRoute(phoneNumber, ids);
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "JT-Get directions", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device number", required = true)
    @GetMapping("/route/query")
    public WVPResult<List<JTAreaOrRoute>> queryRoute(String phoneNumber, @RequestParam(value = "ids", required = false) List<Long> ids){

        log.info("[JT-Get directions] phoneNumber: {}, ids:{}", phoneNumber, ids);
        List<JTAreaOrRoute> result = service.queryRoute(phoneNumber, ids);
        if (result != null) {
            return WVPResult.success(result);
        }else {
            return WVPResult.fail(ErrorCode.ERROR100);
        }
    }

    // TODO To be implemented: driving record data collection command, driving record data upload, driving record parameter download command, electronic waybill reporting, CAN bus data upload

    @Operation(summary = "JT-Report driver identification information request", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device number", required = true)
    @GetMapping("/driver-information")
    public WVPResult<JTDriverInformation> queryDriverInformation(String phoneNumber){

        log.info("[JT-Report driver identification information request] phoneNumber: {}", phoneNumber);
        JTDriverInformation jtDriverInformation = service.queryDriverInformation(phoneNumber);
        if (jtDriverInformation != null) {
            return WVPResult.success(jtDriverInformation);
        }else {
            return WVPResult.fail(ErrorCode.ERROR100);
        }
    }

    @Operation(summary = "JT-Camera shooting command immediately", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device number", required = true)
    @PostMapping("/shooting")
    public WVPResult<List<Long>> shooting(@RequestBody ShootingParam param){

        log.info("[JT-Camera shooting command immediately] param: {}", param );
        List<Long> ids = service.shooting(param.getPhoneNumber(), param.getShootingCommand());
        if (ids != null) {
            return WVPResult.success(ids);
        }else {
            return WVPResult.fail(ErrorCode.ERROR100);
        }
    }

    @Operation(summary = "JT-Snapshot", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device number", required = true)
    @Parameter(name = "channelId", description = "Channel number", required = true)
    @GetMapping("/snap")
    public void snap(HttpServletResponse response, String phoneNumber, Integer channelId){

        log.info("[JT-Snapshot] Device number: {}, Channel number: {}", phoneNumber, channelId );
        Assert.notNull(channelId, "Missing channel number");
        try {
            ServletOutputStream outputStream = response.getOutputStream();
            response.setContentType(MediaType.IMAGE_JPEG_VALUE);
//            response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(phoneNumber + "_" + channelId + ".jpg", "UTF-8"));
            byte[] data = service.snap(phoneNumber, channelId);
            outputStream.write(data);
            outputStream.flush();
        }catch (Exception e) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "JT-Stored multimedia data retrieval", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "param", description = "Store multimedia data parameters", required = true)
    @PostMapping("/media/list")
    public WVPResult<List<JTMediaDataInfo>> queryMediaData(@RequestBody QueryMediaDataParam param){

        log.info("[JT-Stored multimedia data retrieval] param: {}", param );
        List<JTMediaDataInfo> ids = service.queryMediaData(param.getPhoneNumber(), param.getQueryMediaDataCommand());
        if (ids != null) {
            return WVPResult.success(ids);
        }else {
            return WVPResult.fail(ErrorCode.ERROR100);
        }
    }

    @Operation(summary = "JT-Single stored multimedia data upload", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device number", required = true)
    @Parameter(name = "mediaId", description = "multimediaID", required = true)
    @GetMapping("/media/upload/one/upload")
    public void uploadOneMedia(HttpServletResponse response, String phoneNumber, Long mediaId){

        log.info("[JT-Single stored multimedia data upload] Device number: {}, multimediaID: {}", phoneNumber, mediaId );
        Assert.notNull(mediaId, "Missing channel number");
        try {
            ServletOutputStream outputStream = response.getOutputStream();
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            service.uploadOneMedia(phoneNumber, mediaId, outputStream, false);
        }catch (Exception e) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "JT-Deletion of single stored multimedia data", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device number", required = true)
    @Parameter(name = "mediaId", description = "multimediaID", required = true)
    @GetMapping("/media/upload/one/delete")
    public void deleteOneMedia(HttpServletResponse response, String phoneNumber, Long mediaId){

        log.info("[JT-Single stored multimedia data upload] Device number: {}, multimediaID: {}", phoneNumber, mediaId );
        Assert.notNull(mediaId, "Missing channel number");
        try {
            ServletOutputStream outputStream = response.getOutputStream();
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            service.uploadOneMedia(phoneNumber, mediaId, outputStream, true);
        }catch (Exception e) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), e.getMessage());
        }
    }

//    @Operation(summary = "JT-Store multimedia data upload commands", security = @SecurityRequirement(name = JwtUtils.HEADER))
//    @Parameter(name = "param", description = "Store multimedia data parameters", required = true)
//    @PostMapping("/media-data-upload")
//    public DeferredResult<WVPResult<List<String>>> updateMediaData(@RequestBody QueryMediaDataParam param){
//
//        log.info("[JT-Store multimedia data upload commands] param: {}", param );
//        DeferredResult<WVPResult<List<String>>> deferredResult = new DeferredResult<>(30000L);
//        List<String> resultList = new ArrayList<>();
//
//        deferredResult.onTimeout(()->{
//            log.info("[JT-Storage multimedia data upload command timed out] param: {}", param );
//            WVPResult<List<String>> fail = WVPResult.fail(ErrorCode.ERROR100);
//            fail.setMsg("timeout");
//            fail.setData(resultList);
//            deferredResult.setResult(fail);
//        });
//        List<JTMediaDataInfo> ids;
//        if (param.getMediaId() != null) {
//            ids = new ArrayList<>();
//            JTMediaDataInfo mediaDataInfo = new JTMediaDataInfo();
//            mediaDataInfo.setId(param.getMediaId());
//            ids.add(mediaDataInfo);
//        }else {
//            ids = service.queryMediaData(param.getPhoneNumber(), param.getQueryMediaDataCommand());
//        }
//        if (ids.isEmpty()) {
//            deferredResult.setResult(WVPResult.fail(ErrorCode.ERROR100));
//            return deferredResult;
//        }
//        Map<String, JTMediaDataInfo> idMap= new HashMap<>();
//        for (JTMediaDataInfo mediaDataInfo : ids) {
//            idMap.put(mediaDataInfo.getId() + ".jpg", mediaDataInfo);
//        }
//        // Turn on file monitoring
//        FileAlterationObserver observer = new FileAlterationObserver(new File("mediaEvent"));
//        observer.addListener(new FileAlterationListenerAdaptor() {
//            @Override
//            public void onFileCreate(File file) {
//               if (idMap.containsKey(file.getName())) {
//                   idMap.remove(file.getName());
//                   resultList.add("mediaEvent" + File.separator + file.getName());
//                   if (idMap.isEmpty()) {
//                       deferredResult.setResult(WVPResult.success(resultList));
//                   }
//               }
//            }
//        });
//        FileAlterationMonitor monitor = new FileAlterationMonitor(5, observer);
//        try {
//            monitor.start();
//        } catch (Exception e) {
//            log.info("[JT-Storage multimedia data upload command monitoring file failed] param: {}", param );
//            deferredResult.setResult(null);
//            return deferredResult;
//        }
//        taskExecutor.execute(()->{
//            if (param.getMediaId() != null) {
//                service.uploadMediaDataForSingle(param.getPhoneNumber(), param.getMediaId(), param.getDelete());
//            }else {
//                service.uploadMediaData(param.getPhoneNumber(), param.getQueryMediaDataCommand());
//            }
//
//        });
//        return deferredResult;
//    }

    @Operation(summary = "JT-Start recording", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device mobile phone number", required = true)
    @Parameter(name = "time", description = "Recording time, unit is seconds(s) ,0 Indicates always recording", required = false)
    @Parameter(name = "save", description = "0:Real-time upload; 1: save", required = false)
    @Parameter(name = "samplingRate", description = "Audio sample rate， 0:8K；1:11K；2:23K；3:32K", required = false)
    @GetMapping("/record/start")
    public void startRecord(HttpServletRequest request,
                         @Parameter(required = true) String phoneNumber,
                         @Parameter(required = false) Integer time,
                         @Parameter(required = false) Integer save,
                         @Parameter(required = false) Integer samplingRate
                            ) {
        if (ObjectUtils.isEmpty(time)) {
            time = 0;
        }
        if (ObjectUtils.isEmpty(save)) {
            save = 0;
        }
        if (ObjectUtils.isEmpty(samplingRate)) {
            samplingRate = 0;
        }
        service.record(phoneNumber, 1, time, save, samplingRate);
    }

    @Operation(summary = "JT-Stop recording", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device mobile phone number", required = true)
    @Parameter(name = "time", description = "Recording time, unit is seconds(s) ,0 Indicates always recording", required = false)
    @Parameter(name = "save", description = "0:Real-time upload; 1: save", required = false)
    @Parameter(name = "samplingRate", description = "Audio sample rate， 0:8K；1:11K；2:23K；3:32K", required = false)
    @GetMapping("/record/stop")
    public void stopRecord(HttpServletRequest request,
                            @Parameter(required = true) String phoneNumber,
                            @Parameter(required = false) Integer time,
                            @Parameter(required = false) Integer save,
                            @Parameter(required = false) Integer samplingRate
    ) {
        if (ObjectUtils.isEmpty(time)) {
            time = 0;
        }
        if (ObjectUtils.isEmpty(save)) {
            save = 0;
        }
        if (ObjectUtils.isEmpty(samplingRate)) {
            samplingRate = 0;
        }
        service.record(phoneNumber, 0, time, save, samplingRate);
    }

    @Operation(summary = "JT-Query terminal audio and video attributes", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "Device mobile phone number", required = true)
    @GetMapping("/media/attribute")
    public JTMediaAttribute queryMediaAttribute( @Parameter(required = true) String phoneNumber
    ) {
        return service.queryMediaAttribute(phoneNumber);
    }

    // TODO Video alarm reporting


}

