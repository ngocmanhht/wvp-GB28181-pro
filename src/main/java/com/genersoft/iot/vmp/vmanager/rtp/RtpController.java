package com.genersoft.iot.vmp.vmanager.rtp;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.common.enums.MediaStreamUtil;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.hook.Hook;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.event.hook.HookType;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IReceiveRtpServerService;
import com.genersoft.iot.vmp.service.ISendRtpServerService;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.service.bean.RTPServerParam;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.OtherRtpSendInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("rawtypes")
@Tag(name = "Third-party service docking")
@Slf4j
@RestController
@RequestMapping("/api/rtp")
public class RtpController {

    @Autowired
    private ISendRtpServerService sendRtpServerService;

    @Autowired
    private IReceiveRtpServerService receiveRtpServerService;

    @Autowired
    private HookSubscribe hookSubscribe;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;


    @GetMapping(value = "/receive/open")
    @ResponseBody
    @Operation(summary = "Enable streaming and obtain streaming information", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "isSend", description = "Whether to send or not. If false, only stream collection will be enabled. If true, push stream information will be returned at the same time.", required = true)
    @Parameter(name = "callId", description = "The unique identifier of the entire process, in order to be associated with subsequent interfaces", required = true)
    @Parameter(name = "ssrc", description = "SSRC of the source stream. If not passed, the source will not be verified.ssrc", required = false)
    @Parameter(name = "stream", description = "forming a flowID", required = true)
    @Parameter(name = "tcpMode", description = "Traffic collection mode, 0 is UDP, 1 is TCP passive", required = true)
    @Parameter(name = "callBack", description = "Callback address. If the flow collection times out, the channel callback notification will be sent. The callback is a get request, and the parameters arecallId", required = true)
    public OtherRtpSendInfo openRtpServer(Boolean isSend, @RequestParam(required = false)String ssrc, String callId, String stream, Integer tcpMode, String callBack) {

        log.info("[Third-party service docking->Enable streaming and obtain streaming information] isSend->{}, ssrc->{}, callId->{}, stream->{}, tcpMode->{}, callBack->{}",
                isSend, ssrc, callId, stream, tcpMode==0?"UDP":"TCPPassive", callBack);

        MediaServer mediaServer = mediaServerService.getDefaultMediaServer();
        if (mediaServer == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(),"None availableMediaServer");
        }
        if (stream == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(),"streamParameters cannot be empty");
        }
        if (isSend != null && isSend && callId == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(),"isSendWhen true, CallID cannot be empty");
        }
        long ssrcInt = 0;
        if (ssrc != null) {
            try {
                ssrcInt = Long.parseLong(ssrc);
            }catch (NumberFormatException e) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(),"ssrcFormat error");
            }
        }
        String receiveKey = VideoManagerConstants.WVP_OTHER_RECEIVE_RTP_INFO + userSetting.getServerId() + "_" + callId + "_"  + stream;

        RTPServerParam rtpServerParam = new RTPServerParam();
        rtpServerParam.setMediaServer(mediaServer);
        rtpServerParam.setApp(MediaStreamUtil.RTP_APP);
        rtpServerParam.setStreamId(stream);
        rtpServerParam.setSsrc(ssrcInt);
        rtpServerParam.setTcpMode(tcpMode);


        int rtpServerPortForVideo =  receiveRtpServerService.openCommonRTPServer(rtpServerParam, ((code, msg, data) -> {
            if (callBack == null) {
                return;
            }
            if (code == InviteErrorCode.SUCCESS.getCode()) {
                log.info("[Enable streaming and obtain streaming information] Video stream received successfully，callId->{}，stream->{}", callId, stream);
                OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
                OkHttpClient client = httpClientBuilder.build();
                String url = callBack + "?callId="  + callId;
                Request request = new Request.Builder().get().url(url).build();
                try {
                    client.newCall(request).execute();
                } catch (IOException e) {
                    log.error("[Third-party service docking->Enable streaming and obtain streaming information] Timeout waiting for traffic collection callId->{}, Failed to send callback", callId, e);
                }
            }else {
                log.info("[Enable streaming and obtain streaming information] Video stream collection failed，callId->{}，stream->{}", callId, stream);
            }
        }));
        // Supplementary authentication parameters
        receiveRtpServerService.addAuthenticateInfo(stream, null, false, false, null);
        rtpServerParam.setStreamId(stream + "_a");

        int rtpServerPortForAudio =  receiveRtpServerService.openCommonRTPServer(rtpServerParam, ((code, msg, data) -> {
            if (code == InviteErrorCode.SUCCESS.getCode()) {
                log.info("[Enable streaming and obtain streaming information] Audio stream received successfully，callId->{}，stream->{}", callId, stream);
            }else {
                log.info("[Enable streaming and obtain streaming information] Audio stream collection failed，callId->{}，stream->{}", callId, stream);
            }
        }));

        // Supplementary authentication parameters
        receiveRtpServerService.addAuthenticateInfo(rtpServerParam.getStreamId(), null, true, false, null);

        if (rtpServerPortForVideo == 0 || rtpServerPortForAudio == 0) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Failed to get port");
        }
        String key = VideoManagerConstants.WVP_OTHER_SEND_RTP_INFO + userSetting.getServerId() + "_"  + callId;
        OtherRtpSendInfo otherRtpSendInfo = new OtherRtpSendInfo();
        otherRtpSendInfo.setReceiveIp(mediaServer.getSdpIp());
        otherRtpSendInfo.setReceivePortForVideo(rtpServerPortForVideo);
        otherRtpSendInfo.setReceivePortForAudio(rtpServerPortForAudio);
        otherRtpSendInfo.setCallId(callId);
        otherRtpSendInfo.setStream(stream);

        // Write information to redis for later use
        redisTemplate.opsForValue().set(receiveKey, otherRtpSendInfo);
        if (isSend != null && isSend) {
            // Pre-created streaming information
            int portForVideo = sendRtpServerService.getNextPort(mediaServer);
            int portForAudio = sendRtpServerService.getNextPort(mediaServer);

            otherRtpSendInfo.setSendLocalIp(mediaServer.getSdpIp());
            otherRtpSendInfo.setSendLocalPortForVideo(portForVideo);
            otherRtpSendInfo.setSendLocalPortForAudio(portForAudio);
            // Write information to redis for later use
            redisTemplate.opsForValue().set(key, otherRtpSendInfo, 300, TimeUnit.SECONDS);
            log.info("[Third-party service docking->Enable streaming and obtain streaming information] result，callId->{}， {}", callId, otherRtpSendInfo);
        }
        // Write information to redis for later use
        redisTemplate.opsForValue().set(key, otherRtpSendInfo, 300, TimeUnit.SECONDS);
        return otherRtpSendInfo;
    }

    @GetMapping(value = "/receive/close")
    @ResponseBody
    @Operation(summary = "Turn off traffic collection", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "stream", description = "flowingID", required = true)
    public void closeRtpServer(String stream) {
        log.info("[Third-party service docking->Turn off traffic collection] stream->{}", stream);
        MediaServer mediaServerItem = mediaServerService.getDefaultMediaServer();
        receiveRtpServerService.closeRTPServer(mediaServerItem, MediaStreamUtil.RTP_APP, stream);
        receiveRtpServerService.closeRTPServer(mediaServerItem, MediaStreamUtil.RTP_APP, stream+ "_a");
        String receiveKey = VideoManagerConstants.WVP_OTHER_RECEIVE_RTP_INFO + userSetting.getServerId() + "_*_"  + stream;
        List<Object> scan = RedisUtil.scan(redisTemplate, receiveKey);
        if (scan.size() > 0) {
            for (Object key : scan) {
                // Write information to redis for later use
                redisTemplate.delete((String)key);
            }
        }
    }

    @GetMapping(value = "/send/start")
    @ResponseBody
    @Operation(summary = "send stream", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "ssrc", description = "sending streamSSRC", required = true)
    @Parameter(name = "dstIpForAudio", description = "Target audio streamingIP", required = false)
    @Parameter(name = "dstIpForVideo", description = "Target video collectionIP", required = false)
    @Parameter(name = "dstPortForAudio", description = "Target audio collection port", required = false)
    @Parameter(name = "dstPortForVideo", description = "Target video streaming port", required = false)
    @Parameter(name = "app", description = "Application name to be sent", required = true)
    @Parameter(name = "stream", description = "Stream to be sentId", required = true)
    @Parameter(name = "callId", description = "The unique identifier of the entire process. If not transmitted, a random port will be used to send the stream.", required = true)
    @Parameter(name = "isUdp", description = "Is itUDP", required = true)
    @Parameter(name = "ptForAudio", description = "rtpaudiopt", required = false)
    @Parameter(name = "ptForVideo", description = "rtpvideopt", required = false)
    public void sendRTP(String ssrc,
                        @RequestParam(required = false)String dstIpForAudio,
                        @RequestParam(required = false)String dstIpForVideo,
                        @RequestParam(required = false)Integer dstPortForAudio,
                        @RequestParam(required = false)Integer dstPortForVideo,
                        String app,
                        String stream,
                        String callId,
                        Boolean isUdp,
                        @RequestParam(required = false)Integer ptForAudio,
                        @RequestParam(required = false)Integer ptForVideo
        ) {
        log.info("[Third-party service docking->send stream] " +
                        "ssrc->{}, \r\n" +
                        "dstIpForAudio->{}, \n" +
                        "dstIpForAudio->{}, \n" +
                        "dstPortForAudio->{},  \n" +
                        "dstPortForVideo->{}, \n" +
                        "app->{}, \n" +
                        "stream->{}, \n" +
                        "callId->{}, \n" +
                        "ptForAudio->{}, \n" +
                        "ptForVideo->{}",
                        ssrc,
                        dstIpForAudio,
                        dstIpForVideo,
                        dstPortForAudio,
                        dstPortForVideo,
                        app,
                        stream,
                        callId,
                        ptForAudio,
                        ptForVideo);
        if (!((dstPortForAudio > 0 && !ObjectUtils.isEmpty(dstPortForAudio) || (dstPortForVideo > 0 && !ObjectUtils.isEmpty(dstIpForVideo))))) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "At least one set of audio or video send parameters should exist");
        }
        MediaServer mediaServer = mediaServerService.getDefaultMediaServer();
        String key = VideoManagerConstants.WVP_OTHER_SEND_RTP_INFO + userSetting.getServerId() + "_"  + callId;
        OtherRtpSendInfo sendInfo = (OtherRtpSendInfo)redisTemplate.opsForValue().get(key);
        if (sendInfo == null) {
            sendInfo = new OtherRtpSendInfo();
        }
        sendInfo.setPushApp(app);
        sendInfo.setPushStream(stream);
        sendInfo.setPushSSRC(ssrc);


        SendRtpInfo sendRtpItemForVideo;
        SendRtpInfo sendRtpItemForAudio;
        if (!ObjectUtils.isEmpty(dstIpForAudio) && dstPortForAudio > 0) {
            sendRtpItemForAudio = SendRtpInfo.getInstance(app, stream, ssrc, dstIpForAudio, dstPortForAudio, !isUdp, sendInfo.getSendLocalPortForAudio(), ptForAudio);
        } else {
            sendRtpItemForAudio = null;
        }
        if (!ObjectUtils.isEmpty(dstIpForVideo) && dstPortForVideo > 0) {
            sendRtpItemForVideo = SendRtpInfo.getInstance(app, stream, ssrc, dstIpForAudio, dstPortForAudio, !isUdp, sendInfo.getSendLocalPortForVideo(), ptForVideo);
        } else {
            sendRtpItemForVideo = null;
        }

        Boolean streamReady = mediaServerService.isStreamReady(mediaServer, app, stream);
        if (streamReady) {
            if (sendRtpItemForVideo != null) {
                mediaServerService.startSendRtp(mediaServer,  sendRtpItemForVideo);
                log.info("[Third-party service docking->send stream] Video streaming successful，callId->{}，param->{}", callId, sendRtpItemForVideo);
                redisTemplate.opsForValue().set(key, sendInfo);
            }
            if(sendRtpItemForAudio != null) {
                mediaServerService.startSendRtp(mediaServer, sendRtpItemForAudio);
                log.info("[Third-party service docking->send stream] Audio streaming successful，callId->{}，param->{}", callId, sendRtpItemForAudio);
                redisTemplate.opsForValue().set(key, sendInfo);
            }
        }else {
            log.info("[Third-party service docking->send stream] The stream does not exist, waiting for the stream to come online.，callId->{}", callId);
            String uuid = UUID.randomUUID().toString();
            Hook hook = Hook.getInstance(HookType.on_media_arrival, app, stream, mediaServer.getId());
            dynamicTask.startDelay(uuid, ()->{
                log.info("[Third-party service docking->send stream] Timeout waiting for stream to come online callId->{}", callId);
                redisTemplate.delete(key);
                hookSubscribe.removeSubscribe(hook);
            }, 10000);

            // Subscribe to the zlm startup event, the new zlm will also enter the system from here
            hookSubscribe.removeSubscribe(hook);
            OtherRtpSendInfo finalSendInfo = sendInfo;
            hookSubscribe.addSubscribe(hook,
                    (hookData)->{
                        dynamicTask.stop(uuid);
                        log.info("[Third-party service docking->send stream] Stream online and start streaming callId->{}", callId);
                        try {
                            Thread.sleep(400);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        if (sendRtpItemForVideo != null) {
                            mediaServerService.startSendRtp(mediaServer, sendRtpItemForVideo);
                            log.info("[Third-party service docking->send stream] Video streaming successful，callId->{}，param->{}", callId, sendRtpItemForVideo);
                            redisTemplate.opsForValue().set(key, finalSendInfo);
                        }
                        if(sendRtpItemForAudio != null) {
                            mediaServerService.startSendRtp(mediaServer, sendRtpItemForAudio);
                            log.info("[Third-party service docking->send stream] Audio streaming successful，callId->{}，param->{}", callId, sendRtpItemForAudio);
                            redisTemplate.opsForValue().set(key, finalSendInfo);
                        }
                        hookSubscribe.removeSubscribe(hook);
                    });
        }
    }

    @GetMapping(value = "/send/stop")
    @ResponseBody
    @Operation(summary = "Close send stream", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "callId", description = "The unique identifier of the entire process. If not transmitted, a random port will be used to send the stream.", required = true)
    public void closeSendRTP(String callId) {
        log.info("[Third-party service docking->Close send stream] callId->{}", callId);
        String key = VideoManagerConstants.WVP_OTHER_SEND_RTP_INFO + userSetting.getServerId() + "_"  + callId;
        OtherRtpSendInfo sendInfo = (OtherRtpSendInfo)redisTemplate.opsForValue().get(key);
        if (sendInfo == null){
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Not open to traffic");
        }
        MediaServer mediaServerItem = mediaServerService.getDefaultMediaServer();
        mediaServerService.stopSendRtp(mediaServerItem, sendInfo.getPushApp(), sendInfo.getPushStream(), sendInfo.getPushSSRC());
        log.info("[Third-party service docking->Close send stream] success callId->{}", callId);
        redisTemplate.delete(key);
    }

}
