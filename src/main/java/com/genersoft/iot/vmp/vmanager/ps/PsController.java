package com.genersoft.iot.vmp.vmanager.ps;

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
import com.genersoft.iot.vmp.vmanager.bean.OtherPsSendInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("rawtypes")
@Tag(name = "Third-party PS service docking")
@Slf4j
@RestController
@RequestMapping("/api/ps")
public class PsController {

    @Autowired
    private HookSubscribe hookSubscribe;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private ISendRtpServerService sendRtpServerService;

    @Autowired
    private IReceiveRtpServerService receiveRtpServerService;

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
    public OtherPsSendInfo openRtpServer(Boolean isSend, @RequestParam(required = false)String ssrc, String callId, String stream, Integer tcpMode, String callBack) {

        log.info("[Third-party PS service docking->Enable streaming and obtain streaming information] isSend->{}, ssrc->{}, callId->{}, stream->{}, tcpMode->{}, callBack->{}",
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
        String receiveKey = VideoManagerConstants.WVP_OTHER_RECEIVE_PS_INFO + userSetting.getServerId() + "_" + callId + "_"  + stream;

        RTPServerParam rtpServerParam = new RTPServerParam();
        rtpServerParam.setMediaServer(mediaServer);
        rtpServerParam.setApp(MediaStreamUtil.RTP_APP);
        rtpServerParam.setStreamId(stream);
        rtpServerParam.setSsrc(ssrcInt);
        rtpServerParam.setTcpMode(tcpMode);

        int rtpServerPort = receiveRtpServerService.openCommonRTPServer(rtpServerParam, ((code, msg, data) -> {
            if (callBack == null) {
                return;
            }
            if (code == InviteErrorCode.SUCCESS.getCode()) {
                log.info("[Third-party PS service docking->Enable streaming and obtain streaming information] successful callback，callId->{}, data->{}", callId, data);
                // Write information to redis for later use
                redisTemplate.delete(receiveKey);
                OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
                OkHttpClient client = httpClientBuilder.build();
                String url = callBack + "?callId="  + callId;
                Request request = new Request.Builder().get().url(url).build();
                try {
                    client.newCall(request).execute();
                } catch (IOException e) {
                    log.error("[Third-party PS service docking->Enable streaming and obtain streaming information] successful callback callId->{}, Failed to send callback", callId, e);
                }
            } else {
                log.info("[Third-party PS service docking->Enable streaming and obtain streaming information] Failure callback，callId->{}, code->{}, msg->{}", callId, code, msg);
                // Write information to redis for later use
                redisTemplate.delete(receiveKey);
            }
        }));

        if (rtpServerPort == 0) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Failed to get port");
        }

        // Supplementary authentication parameters
        receiveRtpServerService.addAuthenticateInfo(stream, null, false, false, null);

        OtherPsSendInfo otherPsSendInfo = new OtherPsSendInfo();
        otherPsSendInfo.setReceiveIp(mediaServer.getSdpIp());
        otherPsSendInfo.setReceivePort(rtpServerPort);
        otherPsSendInfo.setCallId(callId);
        otherPsSendInfo.setStream(stream);

        // Write information to redis for later use
        redisTemplate.opsForValue().set(receiveKey, otherPsSendInfo);
        if (isSend != null && isSend) {
            String key = VideoManagerConstants.WVP_OTHER_SEND_PS_INFO + userSetting.getServerId() + "_"  + callId;
            // Pre-created streaming information
            int port = sendRtpServerService.getNextPort(mediaServer);

            otherPsSendInfo.setSendLocalIp(mediaServer.getSdpIp());
            otherPsSendInfo.setSendLocalPort(port);
            // Write information to redis for later use
            redisTemplate.opsForValue().set(key, otherPsSendInfo, 300, TimeUnit.SECONDS);
            log.info("[Third-party PS service docking->Enable streaming and obtain streaming information] result，callId->{}， {}", callId, otherPsSendInfo);
        }
        return otherPsSendInfo;
    }

    @GetMapping(value = "/receive/close")
    @ResponseBody
    @Operation(summary = "Turn off traffic collection", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "stream", description = "flowingID", required = true)
    public void closeRtpServer(String stream) {
        log.info("[Third-party PS service docking->Turn off traffic collection] stream->{}", stream);
        MediaServer mediaServerItem = mediaServerService.getDefaultMediaServer();
        receiveRtpServerService.closeRTPServer(mediaServerItem, MediaStreamUtil.RTP_APP, stream);
        String receiveKey = VideoManagerConstants.WVP_OTHER_RECEIVE_PS_INFO + userSetting.getServerId() + "_*_"  + stream;
        List<Object> scan = RedisUtil.scan(redisTemplate, receiveKey);
        if (!scan.isEmpty()) {
            for (Object key : scan) {
                // Write information to redis for later use
                redisTemplate.delete((String) key);
            }
        }
    }

    @GetMapping(value = "/send/start")
    @ResponseBody
    @Operation(summary = "send stream", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "ssrc", description = "sending streamSSRC", required = true)
    @Parameter(name = "dstIp", description = "target flowIP", required = true)
    @Parameter(name = "dstPort", description = "Target traffic port", required = true)
    @Parameter(name = "app", description = "Application name to be sent", required = true)
    @Parameter(name = "stream", description = "Stream to be sentId", required = true)
    @Parameter(name = "callId", description = "The unique identifier of the entire process. If not transmitted, a random port will be used to send the stream.", required = true)
    @Parameter(name = "isUdp", description = "Is itUDP", required = true)
    public void sendRTP(String ssrc,
                        String dstIp,
                        Integer dstPort,
                        String app,
                        String stream,
                        String callId,
                        Boolean isUdp
        ) {
        log.info("[Third-party PS service docking->send stream] " +
                        "ssrc->{}, \r\n" +
                        "dstIp->{}, \n" +
                        "dstPort->{},  \n" +
                        "app->{}, \n" +
                        "stream->{}, \n" +
                        "callId->{} \n",
                        ssrc,
                        dstIp,
                        dstPort,
                        app,
                        stream,
                        callId);
        MediaServer mediaServer = mediaServerService.getDefaultMediaServer();
        String key = VideoManagerConstants.WVP_OTHER_SEND_PS_INFO + userSetting.getServerId() + "_"  + callId;
        OtherPsSendInfo sendInfo = (OtherPsSendInfo)redisTemplate.opsForValue().get(key);
        if (sendInfo == null) {
            sendInfo = new OtherPsSendInfo();
        }
        sendInfo.setPushApp(app);
        sendInfo.setPushStream(stream);
        sendInfo.setPushSSRC(ssrc);
        SendRtpInfo sendRtpItem = SendRtpInfo.getInstance(app, stream, ssrc, dstIp, dstPort, !isUdp, sendInfo.getSendLocalPort(), null);
        Boolean streamReady = mediaServerService.isStreamReady(mediaServer, app, stream);
        if (streamReady) {
            mediaServerService.startSendRtp(mediaServer, sendRtpItem);
            log.info("[Third-party PS service docking->send stream] Video streaming successful，callId->{}，param->{}", callId, sendRtpItem);
            redisTemplate.opsForValue().set(key, sendInfo);
        }else {
            log.info("[Third-party PS service docking->send stream] The stream does not exist, waiting for the stream to come online.，callId->{}", callId);
            String uuid = UUID.randomUUID().toString();
            Hook hook = Hook.getInstance(HookType.on_media_arrival, app, stream, mediaServer.getId());
            dynamicTask.startDelay(uuid, ()->{
                log.info("[Third-party PS service docking->send stream] Timeout waiting for stream to come online callId->{}", callId);
                redisTemplate.delete(key);
                hookSubscribe.removeSubscribe(hook);
            }, 10000);

            // Subscribe to the zlm startup event, the new zlm will also enter the system from here
            OtherPsSendInfo finalSendInfo = sendInfo;
            hookSubscribe.removeSubscribe(hook);
            hookSubscribe.addSubscribe(hook,
                    (hookData)->{
                        dynamicTask.stop(uuid);
                        log.info("[Third-party PS service docking->send stream] Stream online and start streaming callId->{}", callId);
                        try {
                            Thread.sleep(400);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        mediaServerService.startSendRtp(mediaServer, sendRtpItem);
                        log.info("[Third-party PS service docking->send stream] Video streaming successful，callId->{}，param->{}", callId, sendRtpItem);
                        redisTemplate.opsForValue().set(key, finalSendInfo);
                        hookSubscribe.removeSubscribe(hook);
                    });
        }
    }

    @GetMapping(value = "/send/stop")
    @ResponseBody
    @Operation(summary = "Close send stream")
    @Parameter(name = "callId", description = "The unique identifier of the entire process. If not transmitted, a random port will be used to send the stream.", required = true)
    public void closeSendRTP(String callId) {
        log.info("[Third-party PS service docking->Close send stream] callId->{}", callId);
        String key = VideoManagerConstants.WVP_OTHER_SEND_PS_INFO + userSetting.getServerId() + "_"  + callId;
        OtherPsSendInfo sendInfo = (OtherPsSendInfo)redisTemplate.opsForValue().get(key);
        if (sendInfo == null){
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Not open to traffic");
        }
        MediaServer mediaServerItem = mediaServerService.getDefaultMediaServer();
        boolean result = mediaServerService.stopSendRtp(mediaServerItem, sendInfo.getPushApp(), sendInfo.getStream(), sendInfo.getPushSSRC());
        if (!result) {
            log.info("[Third-party PS service docking->Close send stream] failed callId->{}", callId);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Failed to stop streaming");
        }else {
            log.info("[Third-party PS service docking->Close send stream] success callId->{}", callId);
        }
        redisTemplate.delete(key);
    }


    @GetMapping(value = "/getTestPort")
    @ResponseBody
    public int getTestPort() {
        MediaServer defaultMediaServer = mediaServerService.getDefaultMediaServer();

//        for (int i = 0; i <300; i++) {
//            new Thread(() -> {
//                int nextPort = sendRtpPortManager.getNextPort(defaultMediaServer);
//                try {
//                    Thread.sleep((int)Math.random()*10);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//                System.out.println(nextPort);
//            }).start();
//        }

        return sendRtpServerService.getNextPort(defaultMediaServer);
    }
}
