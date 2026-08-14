package com.genersoft.iot.vmp.web.gb28181;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.enums.MediaStreamUtil;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.service.IInviteStreamService;
import com.genersoft.iot.vmp.gb28181.service.IPlayService;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;

/**
 * APICompatible with: Live broadcast
 */
@SuppressWarnings(value = {"rawtypes", "unchecked"})

@Slf4j
@RestController
@RequestMapping(value = "/api/v1/stream")
@Hidden
public class ApiStreamController {

    @Autowired
    private SIPCommander cmder;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private IDeviceChannelService deviceChannelService;

    @Autowired
    private IPlayService playService;

    @Autowired
    private IInviteStreamService inviteStreamService;

    /**
     * Live broadcast - Start live broadcast
     * @param serial Device number
     * @param channel Channel number default value: 1
     * @param code Channel number, pass /api/v1/device/channellist obtained ChannelList.ID, You can choose to pass this parameter or channel.
     * @param cdn Retweet CDN address, in the form: [rtmp|rtsp]://xxx, encodeURIComponent
     * @param audio Whether to enable audio, enabled by default
     * @param transport Streaming mode, default UDP
     * @param checkchannelstatus Whether to check the channel status, the default is false, which means that the channel status is not checked whether it is online before pulling the stream.
     * @param transportmode When transport=TCP Valid, indicating active and passive streaming mode, the default is passive
     * @param timeout Pull timeout(seconds),
     * @return
     */
    @GetMapping("/start")
    private DeferredResult<JSONObject> start(String serial ,
                                             @RequestParam(required = false)Integer channel ,
                                             @RequestParam(required = false)String code,
                                             @RequestParam(required = false)String cdn,
                                             @RequestParam(required = false)String audio,
                                             @RequestParam(required = false)String transport,
                                             @RequestParam(required = false)String checkchannelstatus ,
                                             @RequestParam(required = false)String transportmode,
                                             @RequestParam(required = false)String timeout

    ){
        DeferredResult<JSONObject> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue() + 10);
        Device device = deviceService.getDeviceByDeviceId(serial);
        if (device == null ) {
            JSONObject resultJSON = new JSONObject();
            resultJSON.put("error","device[ " + serial + " ]not found");
            result.setResult(resultJSON);
            return result;
        }else if (!device.isOnLine()) {
            JSONObject resultJSON = new JSONObject();
            resultJSON.put("error","device[ " + code + " ]offline");
            result.setResult(resultJSON);
            return result;
        }


        DeviceChannel deviceChannel = deviceChannelService.getOne(serial, code);
        if (deviceChannel == null) {
            JSONObject resultJSON = new JSONObject();
            resultJSON.put("error","channel[ " + code + " ]not found");
            result.setResult(resultJSON);
            return result;
        }else if (!deviceChannel.getStatus().equalsIgnoreCase("ON")) {
            JSONObject resultJSON = new JSONObject();
            resultJSON.put("error","channel[ " + code + " ]offline");
            result.setResult(resultJSON);
            return result;
        }

        result.onTimeout(()->{
            log.info("Play wait timeout");
            JSONObject resultJSON = new JSONObject();
            resultJSON.put("error","timeout");
            result.setResult(resultJSON);
            inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, deviceChannel.getId());
            deviceChannelService.stopPlay(deviceChannel.getId());
            // clean upRTP server
        });

        MediaServer newMediaServerItem = playService.getNewMediaServerItem(device);

        playService.play(newMediaServerItem, serial, code, null, (errorCode, msg, data) -> {
            if (errorCode == InviteErrorCode.SUCCESS.getCode()) {
                if (data != null) {
                    StreamInfo streamInfo = (StreamInfo)data;
                    JSONObject resultJjson = new JSONObject();
                    resultJjson.put("StreamID", streamInfo.getStream());
                    resultJjson.put("DeviceID", serial);
                    resultJjson.put("ChannelID", code);
                    resultJjson.put("ChannelName", deviceChannel.getName());
                    resultJjson.put("ChannelCustomName", "");
                    if (streamInfo.getTranscodeStream() != null) {
                        resultJjson.put("FLV", streamInfo.getTranscodeStream().getFlv().getUrl());
                    }else {
                        resultJjson.put("FLV", streamInfo.getFlv().getUrl());

                    }
                    if(streamInfo.getHttps_flv() != null) {
                        if (streamInfo.getTranscodeStream() != null) {
                            resultJjson.put("HTTPS_FLV", streamInfo.getTranscodeStream().getHttps_flv().getUrl());
                        }else {
                            resultJjson.put("HTTPS_FLV", streamInfo.getHttps_flv().getUrl());
                        }
                    }

                    if (streamInfo.getTranscodeStream() != null) {
                        resultJjson.put("WS_FLV", streamInfo.getTranscodeStream().getWs_flv().getUrl());
                    }else {
                        resultJjson.put("WS_FLV", streamInfo.getWs_flv().getUrl());
                    }

                    if(streamInfo.getWss_flv() != null) {
                        if (streamInfo.getTranscodeStream() != null) {
                            resultJjson.put("WSS_FLV", streamInfo.getTranscodeStream().getWss_flv().getUrl());
                        }else {
                            resultJjson.put("WSS_FLV", streamInfo.getWss_flv().getUrl());
                        }
                    }
                    resultJjson.put("RTMP", streamInfo.getRtmp().getUrl());
                    if (streamInfo.getRtmps() != null) {
                        resultJjson.put("RTMPS", streamInfo.getRtmps().getUrl());
                    }
                    resultJjson.put("HLS", streamInfo.getHls().getUrl());
                    if (streamInfo.getHttps_hls() != null) {
                        resultJjson.put("HTTPS_HLS", streamInfo.getHttps_hls().getUrl());
                    }
                    resultJjson.put("RTSP", streamInfo.getRtsp().getUrl());
                    if (streamInfo.getRtsps() != null) {
                        resultJjson.put("RTSPS", streamInfo.getRtsps().getUrl());
                    }
                    resultJjson.put("WEBRTC", streamInfo.getRtc().getUrl());
                    if (streamInfo.getRtcs() != null) {
                        resultJjson.put("HTTPS_WEBRTC", streamInfo.getRtcs().getUrl());
                    }
                    resultJjson.put("CDN", "");
                    resultJjson.put("SnapURL", "");
                    resultJjson.put("Transport", device.getTransport());
                    resultJjson.put("StartAt", "");
                    resultJjson.put("Duration", "");
                    resultJjson.put("SourceVideoCodecName", "");
                    resultJjson.put("SourceVideoWidth", "");
                    resultJjson.put("SourceVideoHeight", "");
                    resultJjson.put("SourceVideoFrameRate", "");
                    resultJjson.put("SourceAudioCodecName", "");
                    resultJjson.put("SourceAudioSampleRate", "");
                    resultJjson.put("AudioEnable", "");
                    resultJjson.put("Ondemand", "");
                    resultJjson.put("InBytes", "");
                    resultJjson.put("InBitRate", "");
                    resultJjson.put("OutBytes", "");
                    resultJjson.put("NumOutputs", "");
                    resultJjson.put("CascadeSize", "");
                    resultJjson.put("RelaySize", "");
                    resultJjson.put("ChannelPTZType", "0");
                    result.setResult(resultJjson);
                }else {
                    JSONObject resultJjson = new JSONObject();
                    resultJjson.put("error", "channel[ " + code + " ] " + msg);
                    result.setResult(resultJjson);
                }
            }else {
                JSONObject resultJjson = new JSONObject();
                resultJjson.put("error", "channel[ " + code + " ] " + msg);
                result.setResult(resultJjson);
            }
        });

        return result;
    }

    /**
     * Live broadcast - Live stream stopped
     * @param serial Device number
     * @param channel Channel number
     * @param code Channel national standard number
     * @param check_outputs
     * @return
     */
    @GetMapping("/stop")
    @ResponseBody
    private JSONObject stop(String serial ,
                             @RequestParam(required = false)Integer channel ,
                             @RequestParam(required = false)String code,
                             @RequestParam(required = false)String check_outputs

    ){


        Device device = deviceService.getDeviceByDeviceId(serial);
        if (device == null) {
            JSONObject result = new JSONObject();
            result.put("error","Device not found");
            return result;
        }
        DeviceChannel deviceChannel = deviceChannelService.getOne(serial, code);
        if (deviceChannel == null) {
            JSONObject result = new JSONObject();
            result.put("error","Channel not found");
            return result;
        }
        InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, deviceChannel.getId());
        if (inviteInfo == null) {
            JSONObject result = new JSONObject();
            result.put("error","Flow information not found");
            return result;
        }

        try {
            cmder.streamByeCmd(device, code, MediaStreamUtil.RTP_APP, inviteInfo.getStream(), null, null);
        } catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException e) {
            log.error("[Stop on demand] Sending BYE failed: {}", e.getMessage());
        } finally {
            inviteStreamService.removeInviteInfo(inviteInfo);
            deviceChannelService.stopPlay(inviteInfo.getChannelId());
        }
        return null;
    }

    /**
     * Live broadcast - Live stream keep alive
     * @param serial Device number
     * @param channel Channel number
     * @param code Channel national standard number
     * @return
     */
    @GetMapping("/touch")
    @ResponseBody
    private JSONObject touch(String serial ,String t,
                            @RequestParam(required = false)Integer channel ,
                            @RequestParam(required = false)String code,
                            @RequestParam(required = false)String autorestart,
                            @RequestParam(required = false)String audio,
                            @RequestParam(required = false)String cdn
    ){
        return null;
    }
}
