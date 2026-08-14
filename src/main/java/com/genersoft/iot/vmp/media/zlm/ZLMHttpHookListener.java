package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.enums.MediaStreamUtil;
import com.genersoft.iot.vmp.conf.MediaConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.bean.ResultForOnPublish;
import com.genersoft.iot.vmp.media.event.media.*;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaSendRtpStoppedEvent;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.media.zlm.dto.ZLMServerConfig;
import com.genersoft.iot.vmp.media.zlm.dto.hook.*;
import com.genersoft.iot.vmp.media.zlm.event.HookZlmServerKeepaliveEvent;
import com.genersoft.iot.vmp.media.zlm.event.HookZlmServerStartEvent;
import com.genersoft.iot.vmp.service.IMediaService;
import com.genersoft.iot.vmp.utils.MediaServerUtils;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @description:Hook event monitoring for ZLMediaServer
 * @author: swwheihei
 * @date: 2020May 8, 2019, morning10:46:48
 */
@Slf4j
@RestController
@RequestMapping("/index/hook")
@Hidden
public class ZLMHttpHookListener {

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private IMediaService mediaService;

    @Autowired
    private MediaConfig mediaConfig;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;


    /**
     * The server reports the time regularly, and the reporting interval is configurable. By default, it reports once every 10 seconds.
     */
    @ResponseBody
    @PostMapping(value = "/on_server_keepalive", produces = "application/json;charset=UTF-8")
    public HookResult onServerKeepalive(@RequestBody OnServerKeepaliveHookParam param) {
        try {
            HookZlmServerKeepaliveEvent event = new HookZlmServerKeepaliveEvent(this);
            MediaServer mediaServerItem = mediaServerService.getOne(param.getMediaServerId());
            if (mediaServerItem != null) {
                event.setMediaServerItem(mediaServerItem);
                applicationEventPublisher.publishEvent(event);
            }
        }catch (Exception e) {
            log.info("[ZLM-HOOK-heartbeat] Failed to send notification ", e);
        }
        return HookResult.SUCCESS();
    }

    /**
     * Player authentication event，rtsp/rtmp/http-flv/ws-flv/hlsThe playback will trigger this authentication event。
     */
    @ResponseBody
    @PostMapping(value = "/on_play", produces = "application/json;charset=UTF-8")
    public HookResult onPlay(@RequestBody OnPlayHookParam param) {

        Map<String, String> paramMap = MediaServerUtils.urlParamToMap(param.getParams());
        // Authentication for playback streams
        boolean authenticateResult = mediaService.authenticatePlay(param.getApp(), param.getStream(), paramMap.get("callId"));
        if (!authenticateResult) {
            log.info("[ZLM HOOK] Playback authentication failed：{}->{}", param.getMediaServerId(), param);
            return new HookResult(401, "Unauthorized");
        }
        if (log.isDebugEnabled()){
            log.debug("[ZLM HOOK] Playback authentication successful：{}->{}", param.getMediaServerId(), param);
        }
        return HookResult.SUCCESS();
    }

    /**
     * rtsp/rtmp/rtpPush authentication event。
     */
    @ResponseBody
    @PostMapping(value = "/on_publish", produces = "application/json;charset=UTF-8")
    public HookResultForOnPublish onPublish(@RequestBody OnPublishHookParam param) {

        JSONObject json = (JSONObject) JSON.toJSON(param);

        log.info("[ZLM HOOK]Push authentication：{}->{}", param.getMediaServerId(), param);
        // TODO Speed up processing

        String mediaServerId = json.getString("mediaServerId");
        MediaServer mediaServer = mediaServerService.getOne(mediaServerId);
        if (mediaServer == null) {
            HookResultForOnPublish fail = HookResultForOnPublish.Fail();
            log.warn("[ZLM HOOK]Push authentication response：{}->Can't find the correspondingmediaServer", param.getMediaServerId());
            return fail;
        }

        ResultForOnPublish resultForOnPublish = mediaService.authenticatePublish(mediaServer, param.getApp(), param.getStream(), param.getParams());
        if (resultForOnPublish != null) {
            HookResultForOnPublish successResult = HookResultForOnPublish.getInstance(resultForOnPublish);
            log.info("[ZLM HOOK]Push authentication-allow-response：{}->{}->>>>{}", param.getMediaServerId(), param, successResult);
            return successResult;
        }else {
            HookResultForOnPublish fail = HookResultForOnPublish.Fail();
            log.info("[ZLM HOOK]Push authentication-reject-parameters：{}->{}", param.getMediaServerId(), param);
            return fail;
        }
    }

    /**
     * rtsp/rtmpThis event is triggered when a stream registers or logs out; this event is not sensitive to replies。
     */
    @ResponseBody
    @PostMapping(value = "/on_stream_changed", produces = "application/json;charset=UTF-8")
    public HookResult onStreamChanged(@RequestBody OnStreamChangedHookParam param) {
        MediaServer mediaServer = mediaServerService.getOne(param.getMediaServerId());
        if (mediaServer == null) {
            return HookResult.SUCCESS();
        }
        if (!ObjectUtils.isEmpty(mediaServer.getTranscodeSuffix())
                && !"null".equalsIgnoreCase(mediaServer.getTranscodeSuffix())
                && param.getStream().endsWith(mediaServer.getTranscodeSuffix())  ) {
            return HookResult.SUCCESS();
        }
        if (param.getSchema().equalsIgnoreCase("rtsp")) {
            if (param.isRegist()) {
                log.info("[ZLM HOOK] stream registration, {}->{}->{}/{}", param.getMediaServerId(), param.getSchema(), param.getApp(), param.getStream());
                String queryParams = param.getParams();
                if (queryParams == null) {
                    try {
                        URL url = new URL("http" + param.getOriginUrl().substring(4));
                        queryParams = url.getQuery();
                    }catch (MalformedURLException ignored) {}
                }
                if (queryParams != null) {
                    param.setParamMap(MediaServerUtils.urlParamToMap(queryParams));
                }else {
                    param.setParamMap(new HashMap<>());
                }
                MediaArrivalEvent mediaArrivalEvent = MediaArrivalEvent.getInstance(this, param, mediaServer, userSetting.getServerId());
                applicationEventPublisher.publishEvent(mediaArrivalEvent);
            } else {
                log.info("[ZLM HOOK] flow logout, {}->{}->{}/{}", param.getMediaServerId(), param.getSchema(), param.getApp(), param.getStream());
                MediaDepartureEvent mediaDepartureEvent = MediaDepartureEvent.getInstance(this, param, mediaServer);
                applicationEventPublisher.publishEvent(mediaDepartureEvent);
            }
        }

        return HookResult.SUCCESS();
    }

    /**
     * Event when no one is watching the stream. Users can use this event to choose whether to close the stream when no one is watching it.。
     */
    @ResponseBody
    @PostMapping(value = "/on_stream_none_reader", produces = "application/json;charset=UTF-8")
    public JSONObject onStreamNoneReader(@RequestBody OnStreamNoneReaderHookParam param) {

        log.info("[ZLM HOOK]Stream unwatched：{}->{}->{}/{}", param.getMediaServerId(), param.getSchema(),
                param.getApp(), param.getStream());

        MediaServer mediaInfo = mediaServerService.getOne(param.getMediaServerId());
        if (mediaInfo == null) {
            JSONObject ret = new JSONObject();
            ret.put("code", 0);
            return ret;
        }
        if (mediaInfo.getTranscodeSuffix() != null && param.getStream().endsWith(mediaInfo.getTranscodeSuffix())) {
            param.setStream(param.getStream().substring(0, param.getStream().lastIndexOf(mediaInfo.getTranscodeSuffix()) - 1));
        }
        if (!ObjectUtils.isEmpty(mediaInfo.getTranscodeSuffix())
                && !"null".equalsIgnoreCase(mediaInfo.getTranscodeSuffix())
                && param.getStream().endsWith(mediaInfo.getTranscodeSuffix())  ) {
            param.setStream(param.getStream().substring(0, param.getStream().lastIndexOf(mediaInfo.getTranscodeSuffix()) -1 ));
        }

        JSONObject ret = new JSONObject();
        boolean close = mediaService.closeStreamOnNoneReader(param.getMediaServerId(), param.getApp(), param.getStream(), param.getSchema());
        log.info("[ZLM HOOK]Whether the stream is closed when no one is watching it：{}, {}->{}->{}/{}", close, param.getMediaServerId(), param.getSchema(),
                param.getApp(), param.getStream());
        ret.put("code", 0);
        ret.put("close", close);
        return ret;
    }

    /**
     * Stream not found event. Users can pull the stream immediately when this event is triggered. This can achieve on-demand streaming; this event is not sensitive to replies.。
     */
    @ResponseBody
    @PostMapping(value = "/on_stream_not_found", produces = "application/json;charset=UTF-8")
    public HookResult onStreamNotFound(@RequestBody OnStreamNotFoundHookParam param) {
        log.info("[ZLM HOOK] Stream not found：{}->{}->{}/{}", param.getMediaServerId(), param.getSchema(), param.getApp(), param.getStream());


        MediaServer mediaServer = mediaServerService.getOne(param.getMediaServerId());
        if (!userSetting.getAutoApplyPlay() || mediaServer == null) {
            return HookResult.SUCCESS();
        }
        MediaNotFoundEvent mediaNotFoundEvent = MediaNotFoundEvent.getInstance(this, param, mediaServer);
        applicationEventPublisher.publishEvent(mediaNotFoundEvent);
        return HookResult.SUCCESS();
    }

    /**
     * Server startup event, which can be used to monitor server crashes and restarts; this event is not sensitive to replies。
     */
    @ResponseBody
    @PostMapping(value = "/on_server_started", produces = "application/json;charset=UTF-8")
    public HookResult onServerStarted(HttpServletRequest request, @RequestBody JSONObject jsonObject) {

        jsonObject.put("ip", request.getRemoteAddr());
        ZLMServerConfig zlmServerConfig = JSON.to(ZLMServerConfig.class, jsonObject);
        zlmServerConfig.setIp(request.getRemoteAddr());
        log.info("[ZLM HOOK] zlm start {}", zlmServerConfig.getGeneralMediaServerId());
        try {
            HookZlmServerStartEvent event = new HookZlmServerStartEvent(this);
            MediaServer mediaServer = mediaServerService.getOne(zlmServerConfig.getMediaServerId());
            if (mediaServer == null && Objects.equals(mediaConfig.getId(), zlmServerConfig.getGeneralMediaServerId())) {
                mediaServer = mediaConfig.buildMediaSer();
            }
            if (mediaServer != null) {
                event.setMediaServer(mediaServer);
                event.setConfig(zlmServerConfig);
                applicationEventPublisher.publishEvent(event);
            }else {
                log.info("[ZLM HOOK] This zlm is not connected {}", zlmServerConfig.getGeneralMediaServerId());
            }
        }catch (Exception e) {
            log.info("[ZLM-HOOK-ZLMstart] Failed to send notification ", e);
        }

        return HookResult.SUCCESS();
    }

    /**
     * sendrtp(startSendRtp)Callback when passively closed
     */
    @ResponseBody
    @PostMapping(value = "/on_send_rtp_stopped", produces = "application/json;charset=UTF-8")
    public HookResult onSendRtpStopped(HttpServletRequest request, @RequestBody OnSendRtpStoppedHookParam param) {

        log.info("[ZLM HOOK] rtpSend Close：{}->{}/{}", param.getMediaServerId(), param.getApp(), param.getStream());

        // Find the corresponding superior push stream and stop sending.
        if (!MediaStreamUtil.RTP_APP.equals(param.getApp())) {
            return HookResult.SUCCESS();
        }
        try {
            MediaSendRtpStoppedEvent event = new MediaSendRtpStoppedEvent(this);
            MediaServer mediaServerItem = mediaServerService.getOne(param.getMediaServerId());
            if (mediaServerItem != null) {
                event.setMediaServer(mediaServerItem);
                applicationEventPublisher.publishEvent(event);
            }
        }catch (Exception e) {
            log.info("[ZLM-HOOK-rtpSend Close] Failed to send notification ", e);
        }

        return HookResult.SUCCESS();
    }

    /**
     * rtpServerTraffic collection timeout
     */
    @ResponseBody
    @PostMapping(value = "/on_rtp_server_timeout", produces = "application/json;charset=UTF-8")
    public HookResult onRtpServerTimeout(@RequestBody OnRtpServerTimeoutHookParam
            param) {
        log.info("[ZLM HOOK] rtpServerTraffic collection timeout：{}->{}({})", param.getMediaServerId(), param.getStream_id(), param.getSsrc());

        try {
            MediaRtpServerTimeoutEvent event = new MediaRtpServerTimeoutEvent(this);
            MediaServer mediaServerItem = mediaServerService.getOne(param.getMediaServerId());
            if (mediaServerItem != null) {
                event.setMediaServer(mediaServerItem);
                event.setApp(MediaStreamUtil.RTP_APP);
                applicationEventPublisher.publishEvent(event);
            }
        }catch (Exception e) {
            log.info("[ZLM-HOOK-rtpServerTraffic collection timeout] Failed to send notification ", e);
        }

        return HookResult.SUCCESS();
    }

    /**
     * Recording completion event
     */
    @ResponseBody
    @PostMapping(value = "/on_record_mp4", produces = "application/json;charset=UTF-8")
    public HookResult onRecordMp4(HttpServletRequest request, @RequestBody OnRecordMp4HookParam param) {
        log.info("[ZLM HOOK] Video recording completed: duration: {}, {}->{}",param.getTime_len(), param.getMediaServerId(), param.getFile_path());

        try {
            MediaServer mediaServerItem = mediaServerService.getOne(param.getMediaServerId());
            if (mediaServerItem != null) {
                MediaRecordMp4Event event = MediaRecordMp4Event.getInstance(this, param, mediaServerItem);
                event.setMediaServer(mediaServerItem);
                applicationEventPublisher.publishEvent(event);
            }
        }catch (Exception e) {
            log.info("[ZLM-HOOK-rtpServerTraffic collection timeout] Failed to send notification ", e);
        }

        return HookResult.SUCCESS();
    }
}
