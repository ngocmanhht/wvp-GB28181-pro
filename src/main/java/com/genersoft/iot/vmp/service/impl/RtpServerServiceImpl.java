package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.common.enums.MediaStreamUtil;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.session.SSRCFactory;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.bean.ResultForOnPublish;
import com.genersoft.iot.vmp.media.event.hook.Hook;
import com.genersoft.iot.vmp.media.event.hook.HookData;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.event.hook.HookType;
import com.genersoft.iot.vmp.media.event.media.MediaArrivalEvent;
import com.genersoft.iot.vmp.media.event.media.MediaDepartureEvent;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IReceiveRtpServerService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.service.bean.RTPServerParam;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RtpServerServiceImpl implements IReceiveRtpServerService {

    private final static String TIMEOUT_TASK_KEY_PREFIX = "RTP_SERVER_TIMEOUT_TASK";

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private SSRCFactory ssrcFactory;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private HookSubscribe subscribe;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Processing of incoming streams
     */
    @Async
    @org.springframework.context.event.EventListener
    public void onApplicationEvent(MediaArrivalEvent event) {

    }

    /**
     * Stream departure processing
     */
    @Async
    @EventListener
    public void onApplicationEvent(MediaDepartureEvent event) {

    }

    @Override
    public SSRCInfo openGbRTPServer(MediaServer mediaServer, String streamId, String presetSSRC, int tcpMode,
                                    boolean playback, boolean ssrcCheck, boolean onlyAuto, boolean disableAuto,
                                    ErrorCallback<OpenRTPServerResult> callback) {
        if (callback == null) {
            log.warn("[Enable national standard RTP streaming] Failure, the callback isNULL");
            return null;
        }
        if (mediaServer == null) {
            log.warn("[Enable national standard RTP streaming] failed, the media node isNULL");
            return null;
        }

        // Get mediaServer available ssrc
        final String ssrc;
        if (presetSSRC != null) {
            ssrc = presetSSRC;
        } else {
            ssrc = playback ? ssrcFactory.getPlayBackSsrc(mediaServer) : ssrcFactory.getPlaySsrc(mediaServer);
        }
        if (streamId == null) {
            streamId = String.format("%08x", Long.parseLong(ssrc)).toUpperCase();
        }
        if (ssrcCheck && tcpMode > 0) {
            // Currently zlm does not support updating ssrc in tcp mode and temporarily turns off ssrc verification.
            log.warn("[openRTPServer] When connecting to the platform, the lower level may customize ssrc, but zlm flow collection in tcp mode currently cannot update ssrc, and the flow collection may time out. In this case, please use udp flow collection or turn off ssrc verification.");
        }

        SSRCInfo ssrcInfo = new SSRCInfo(0, ssrc, MediaStreamUtil.RTP_APP, streamId);
        RTPServerParam rtpServerParam = new RTPServerParam(mediaServer, MediaStreamUtil.RTP_APP, streamId, Long.parseLong(ssrc), null, onlyAuto, disableAuto, false, tcpMode);
        rtpServerParam.setSsrcCheck(ssrcCheck);
        int rtpServerPort = openCommonRTPServer(rtpServerParam, ((code, msg, data) -> {
            if (code == InviteErrorCode.SUCCESS.getCode()) {
                OpenRTPServerResult openRTPServerResult = new OpenRTPServerResult();
                openRTPServerResult.setHookData(data);
                openRTPServerResult.setSsrcInfo(ssrcInfo);
                callback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), openRTPServerResult);
            } else {
                OpenRTPServerResult openRTPServerResult = new OpenRTPServerResult();
                openRTPServerResult.setSsrcInfo(ssrcInfo);
                callback.run(code, msg, openRTPServerResult);
            }
        }));
        ssrcInfo.setPort(rtpServerPort);
        return new SSRCInfo(rtpServerPort, ssrc, MediaStreamUtil.RTP_APP, streamId);
    }

    @Override
    public SSRCInfo openGbRTPServerForPlay(MediaServer mediaServer, Device device, DeviceChannel channel,
                                           String presetSSRC, boolean record, ErrorCallback<OpenRTPServerResult> callback) {
        if (callback == null) {
            log.warn("[Enable national standard on-demand RTP streaming] Failure, the callback isNULL");
            return null;
        }
        if (mediaServer == null) {
            log.warn("[Enable national standard on-demand RTP streaming] failed, the media node isNULL");
            return null;
        }

        // Get mediaServer available ssrc
        final String ssrc;
        if (presetSSRC != null) {
            ssrc = presetSSRC;
        } else {
            ssrc = ssrcFactory.getPlaySsrc(mediaServer);
        }

        String streamId = String.format("%08x", Long.parseLong(ssrc)).toUpperCase();
        String streamReplace = String.format("%s_%s", device.getDeviceId(), channel.getDeviceId());

        int tcpMode = device.getStreamMode().equals("TCP-ACTIVE")? 2: (device.getStreamMode().equals("TCP-PASSIVE")? 1:0);

        if (device.isSsrcCheck() && tcpMode > 0) {
            log.warn("[Enable national standard on-demand RTP streaming] When connecting to the platform, the lower level may customize ssrc, but zlm flow collection in tcp mode currently cannot update ssrc, and the flow collection may time out. In this case, please use udp flow collection or turn off ssrc verification.");
        }

        SSRCInfo ssrcInfo = new SSRCInfo(0, ssrc, MediaStreamUtil.RTP_APP, streamReplace);
        openRtpServer(mediaServer, ssrcInfo, Long.parseLong(ssrc), !channel.isHasAudio(), false, tcpMode, callback, device.isSsrcCheck());
        addAuthenticateInfo(streamId, streamReplace, channel.isHasAudio(),  record, null);
        return ssrcInfo;
    }

    @Override
    public SSRCInfo openGbRTPServerForPlayback(MediaServer mediaServer, Device device, DeviceChannel channel,
                                               String startTime, String endTime, ErrorCallback<OpenRTPServerResult> callback) {
        if (callback == null) {
            log.warn("[Enable national standard playback RTP streaming] Failure, the callback isNULL");
            return null;
        }
        if (mediaServer == null) {
            log.warn("[Enable national standard playback RTP streaming] failed, the media node isNULL");
            return null;
        }

        // Get mediaServer available ssrc
        String ssrc = ssrcFactory.getPlayBackSsrc(mediaServer);

        String streamId = String.format("%08x", Long.parseLong(ssrc)).toUpperCase();
        String streamReplace = getPlaybackStream(device, channel, startTime, endTime);

        int tcpMode = device.getStreamMode().equals("TCP-ACTIVE")? 2: (device.getStreamMode().equals("TCP-PASSIVE")? 1:0);

        if (device.isSsrcCheck() && tcpMode > 0) {
            log.warn("[Enable national standard playback RTP streaming] When connecting to the platform, the lower level may customize ssrc, but zlm flow collection in tcp mode currently cannot update ssrc, and the flow collection may time out. In this case, please use udp flow collection or turn off ssrc verification.");
        }

        SSRCInfo ssrcInfo = new SSRCInfo(0, ssrc, MediaStreamUtil.RTP_APP, streamReplace);
        openRtpServer(mediaServer, ssrcInfo, Long.parseLong(ssrc), !channel.isHasAudio(), false, tcpMode, callback, device.isSsrcCheck());
        addAuthenticateInfo(streamId, streamReplace,  channel.isHasAudio(), false,null);
        return ssrcInfo;
    }

    @Override
    public String getPlaybackStream(Device device, DeviceChannel channel, String startTime, String endTime) {
        String startTimeStr = startTime.replace("-", "")
                .replace(":", "")
                .replace(" ", "");
        String endTimeTimeStr = endTime.replace("-", "")
                .replace(":", "")
                .replace(" ", "");

        return device.getDeviceId() + "_" + channel.getDeviceId() + "_" + startTimeStr + "_" + endTimeTimeStr;
    }

    @Override
    public SSRCInfo openGbRTPServerForDownload(MediaServer mediaServer, Device device, DeviceChannel channel,
                                               String startTime, String endTime, ErrorCallback<OpenRTPServerResult> callback) {
        if (callback == null) {
            log.warn("[Enable national standard video download and RTP streaming] Failure, the callback isNULL");
            return null;
        }
        if (mediaServer == null) {
            log.warn("[Enable national standard video download and RTP streaming] failed, the media node isNULL");
            return null;
        }

        int tcpMode = device.getStreamMode().equals("TCP-ACTIVE")? 2: (device.getStreamMode().equals("TCP-PASSIVE")? 1:0);

        // Get mediaServer available ssrc
        String ssrc = ssrcFactory.getPlayBackSsrc(mediaServer);

        String streamId = String.format("%08x", Long.parseLong(ssrc)).toUpperCase();
        String streamReplace = String.format("%s_%s_%s_%s", device.getDeviceId(), channel.getDeviceId(),
                startTime.replace("-", "").replace(":", "").replace(" ", ""),
                endTime.replace("-", "").replace(":", "").replace(" ", ""));

        if (device.isSsrcCheck() && tcpMode > 0) {
            log.warn("[Enable national standard video download and RTP streaming] When connecting to the platform, the lower level may customize ssrc, but zlm flow collection in tcp mode currently cannot update ssrc, and the flow collection may time out. In this case, please use udp flow collection or turn off ssrc verification.");
        }

        SSRCInfo ssrcInfo = new SSRCInfo(0, ssrc, MediaStreamUtil.RTP_APP, streamReplace);
        openRtpServer(mediaServer, ssrcInfo, Long.parseLong(ssrc), !channel.isHasAudio(), false, tcpMode, callback, device.isSsrcCheck());

        long difference = DateUtil.getDifference(startTime, endTime) / 1000;

        addAuthenticateInfo(streamId, streamReplace, channel.isHasAudio(), true,  (int) difference);
        return ssrcInfo;
    }

    @Override
    public SSRCInfo openGbRTPServerForBroadcast(MediaServer mediaServer, Platform platform, CommonGBChannel channel,
                                                ErrorCallback<OpenRTPServerResult> callback) {
        if (callback == null) {
            log.warn("[Enable national standard propaganda RTP streaming] Failure, the callback isNULL");
            return null;
        }
        if (mediaServer == null) {
            log.warn("[Enable national standard propaganda RTP streaming] failed, the media node isNULL");
            return null;
        }

        String streamId = null;
        if (mediaServer.isRtpEnable()) {
            streamId = String.format("%s_%s", platform.getServerGBId(), channel.getGbDeviceId());
        }
        // SSRC verification is not performed by default. TODO can be changed to configure later.
        int tcpMode;
        if (userSetting.getBroadcastForPlatform().equalsIgnoreCase("TCP-PASSIVE")) {
            tcpMode = 1;
        }else if (userSetting.getBroadcastForPlatform().equalsIgnoreCase("TCP-ACTIVE")) {
            tcpMode = 2;
        } else {
            tcpMode = 0;
        }

        // Get mediaServer available ssrc
        String ssrc = ssrcFactory.getPlaySsrc(mediaServer);

        SSRCInfo ssrcInfo = new SSRCInfo(0, ssrc, MediaStreamUtil.RTP_APP, streamId);
        openRtpServer(mediaServer, ssrcInfo, Long.parseLong(ssrc), false, true, tcpMode, callback, false);
        return ssrcInfo;
    }

    private void openRtpServer(MediaServer mediaServer, SSRCInfo ssrcInfo, Long checkSsrc, boolean disableAuto, boolean onlyAuto, int tcpMode,
                               ErrorCallback<OpenRTPServerResult> callback) {
        openRtpServer(mediaServer, ssrcInfo, checkSsrc, disableAuto, onlyAuto, tcpMode, callback, false);
    }

    private void openRtpServer(MediaServer mediaServer, SSRCInfo ssrcInfo, Long checkSsrc, boolean disableAuto, boolean onlyAuto, int tcpMode,
                               ErrorCallback<OpenRTPServerResult> callback, boolean ssrcCheck) {

        RTPServerParam rtpServerParam = new RTPServerParam(mediaServer, MediaStreamUtil.RTP_APP, ssrcInfo.getStream(), checkSsrc, null, onlyAuto, disableAuto, false, tcpMode);
        rtpServerParam.setSsrcCheck(ssrcCheck);
        int rtpServerPort = openCommonRTPServer(rtpServerParam, ((code, msg, data) -> {
            if (code == InviteErrorCode.SUCCESS.getCode()) {
                OpenRTPServerResult openRTPServerResult = new OpenRTPServerResult();
                openRTPServerResult.setHookData(data);
                openRTPServerResult.setSsrcInfo(ssrcInfo);
                callback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), openRTPServerResult);
            } else {
                OpenRTPServerResult openRTPServerResult = new OpenRTPServerResult();
                openRTPServerResult.setSsrcInfo(ssrcInfo);
                callback.run(code, msg, openRTPServerResult);
            }
        }));
        ssrcInfo.setPort(rtpServerPort);
    }

    @Override
    public int openCommonRTPServer(RTPServerParam rtpServerParam, ErrorCallback<HookData> callback) {
        if (callback == null) {
            log.warn("[Enable RTP streaming] Failure, the callback isNULL");
            return -1;
        }
        if (rtpServerParam.getMediaServer() == null) {
            log.warn("[Enable RTP streaming] failed, the media node isNULL");
            return -1;
        }

        // Set a scheduled task for stream timeout
        String timeOutTaskKey = String.format("%s_%s_%s_%s", TIMEOUT_TASK_KEY_PREFIX, rtpServerParam.getMediaServer().getId(), rtpServerParam.getApp(), rtpServerParam.getStreamId());

        Hook rtpHook = Hook.getInstance(HookType.on_media_arrival, rtpServerParam.getApp(), rtpServerParam.getStreamId(), rtpServerParam.getMediaServer().getId());
        dynamicTask.startDelay(timeOutTaskKey, () -> {
            // Traffic collection timeout
            // Close the traffic port
            String closeStreamId = rtpServerParam.getMediaServer().isRtpEnable()
                    ? String.format("%08x", rtpServerParam.getSsrc()).toUpperCase() : rtpServerParam.getStreamId();
            mediaServerService.closeRTPServer(rtpServerParam.getMediaServer(), rtpServerParam.getApp(), closeStreamId);
            subscribe.removeSubscribe(rtpHook);
            callback.run(InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getCode(), InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getMsg(), null);
        }, userSetting.getPlayTimeout());
        // Enable monitoring of incoming streams
        subscribe.addSubscribe(rtpHook, (hookData) -> {
            dynamicTask.stop(timeOutTaskKey);
            // hookresponse
            callback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), hookData);
            subscribe.removeSubscribe(rtpHook);
        });

        int rtpServerPort;
        if (rtpServerParam.getMediaServer().isRtpEnable()) {
            String zlmStreamId;
            long checkSsrc;
            if (rtpServerParam.getSsrc() != null) {
                zlmStreamId = String.format("%08x", rtpServerParam.getSsrc()).toUpperCase();
                checkSsrc = rtpServerParam.isSsrcCheck() ? rtpServerParam.getSsrc() : 0L;
            }else {
                zlmStreamId = rtpServerParam.getStreamId();
                checkSsrc = 0L;
            }
            rtpServerPort = mediaServerService.createRTPServer(rtpServerParam.getMediaServer(), rtpServerParam.getApp(), zlmStreamId, checkSsrc, rtpServerParam.getPort(), rtpServerParam.isOnlyAuto(),
                    rtpServerParam.isDisableAudio(), rtpServerParam.isReUsePort(), rtpServerParam.getTcpMode());
        } else {
            rtpServerPort = rtpServerParam.getMediaServer().getRtpProxyPort();
        }
        if (rtpServerPort == 0) {
            callback.run(InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(), "Failed to start RTPServer", null);
            return -1;
        }
        return rtpServerPort;
    }

    @Override
    public void closeRTPServer(MediaServer mediaServer, String app, String stream) {
        if (mediaServer == null) {
            return;
        }
        String timeOutTaskKey = String.format("%s_%s_%s_%s", TIMEOUT_TASK_KEY_PREFIX, mediaServer.getId(), app, stream);
        if (dynamicTask.contains(timeOutTaskKey)) {
            dynamicTask.stop(timeOutTaskKey);
        }
        if (mediaServer.isRtpEnable()) {
            mediaServerService.closeRTPServer(mediaServer, app, stream);
        }
        mediaServerService.closeStreams(mediaServer, app, stream);
    }

    @Override
    public void closeRTPServerByMediaServerId(String mediaServerId, String app, String stream) {
        MediaServer mediaServer = mediaServerService.getOne(mediaServerId);
        if (mediaServer == null) {
            return;
        }
        closeRTPServer(mediaServer, app, stream);
    }

    @Override
    public void addAuthenticateInfoForGb28181Talk(MediaServer mediaServer, String streamId) {
        String streamReplace = null;

        if (!mediaServer.isRtpEnable() ) {
            streamReplace = streamId;
        }

        addAuthenticateInfo(streamId, streamReplace, true, false, null);
    }

    @Override
    public void addAuthenticateInfo(String streamId, String streamReplace, Boolean enableAudio, Boolean enableMp4, Integer mp4MaxSecond) {
        ResultForOnPublish hookResultForOnPublish = new ResultForOnPublish();
        hookResultForOnPublish.setStream_replace(streamReplace);
        hookResultForOnPublish.setEnable_audio(enableAudio);
        hookResultForOnPublish.setEnable_mp4(enableMp4);
        if (mp4MaxSecond != null) {
            // mp4MaxSecond It needs to be slightly longer than the actual stream duration to avoid generating file slices because the stream duration exceeds mp4MaxSecond.
            hookResultForOnPublish.setMp4_max_second(mp4MaxSecond + 10);
        }

        String key = String.format("%s:%s", VideoManagerConstants.RTP_AUTHENTICATE, streamId);
        // Stores authentication information. The expiration time is 60 seconds. If it expires, the authentication will not pass.
        redisTemplate.opsForValue().set(key, hookResultForOnPublish);
        redisTemplate.expire(key, 60, TimeUnit.SECONDS);
     }

     @Override
     public ResultForOnPublish getAuthenticateInfo(String streamId) {
         String key = String.format("%s:%s", VideoManagerConstants.RTP_AUTHENTICATE, streamId);
         Object obj = redisTemplate.opsForValue().get(key);
         if (obj instanceof ResultForOnPublish) {
             return (ResultForOnPublish) obj;
         }
         return null;
     }

     @Override
     public void refreshAuthenticateInfo(String oldStreamId, String newStreamId) {
         if (oldStreamId == null || newStreamId == null || oldStreamId.equals(newStreamId)) {
             return;
         }
         String oldKey = String.format("%s:%s", VideoManagerConstants.RTP_AUTHENTICATE, oldStreamId);
         Object obj = redisTemplate.opsForValue().get(oldKey);
         if (obj instanceof ResultForOnPublish) {
             String newKey = String.format("%s:%s", VideoManagerConstants.RTP_AUTHENTICATE, newStreamId);
             redisTemplate.opsForValue().set(newKey, obj);
             redisTemplate.expire(newKey, 60, TimeUnit.SECONDS);
             redisTemplate.delete(oldKey);
             log.info("[Refresh RTP authentication information] {} -> {}", oldStreamId, newStreamId);
         } else {
             log.warn("[Refresh RTP authentication information] old not foundkey: {}", oldKey);
         }
     }
}
