package com.genersoft.iot.vmp.jt1078.service.impl;

import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.common.enums.MediaStreamUtil;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.jt1078.bean.*;
import com.genersoft.iot.vmp.jt1078.cmd.JT1078Template;
import com.genersoft.iot.vmp.jt1078.config.JT1078Config;
import com.genersoft.iot.vmp.jt1078.proc.request.J1205;
import com.genersoft.iot.vmp.jt1078.proc.response.*;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078PlayService;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.hook.Hook;
import com.genersoft.iot.vmp.media.event.hook.HookData;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.event.hook.HookType;
import com.genersoft.iot.vmp.media.event.media.MediaArrivalEvent;
import com.genersoft.iot.vmp.media.event.media.MediaDepartureEvent;
import com.genersoft.iot.vmp.media.event.media.MediaNotFoundEvent;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaSendRtpStoppedEvent;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IReceiveRtpServerService;
import com.genersoft.iot.vmp.service.ISendRtpServerService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.service.bean.RTPServerParam;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.utils.MediaServerUtils;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.sip.message.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class jt1078PlayServiceImpl implements Ijt1078PlayService {

    @Autowired
    private ISendRtpServerService sendRtpServerService;

    @Autowired
    private Ijt1078Service jt1078Service;

    @Autowired
    private JT1078Template jt1078Template;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private HookSubscribe subscribe;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private IReceiveRtpServerService receiveRtpServerService;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private JT1078Config jt1078Config;

    /**
     * Processing of incoming streams
     */
    @Async
    @EventListener
    public void onApplicationEvent(MediaArrivalEvent event) {
        if (MediaStreamUtil.JT_TALK.equals(event.getApp()) && event.getStream().endsWith("_talk")) {
            // Received stream speaking to JT
            if (event.getStream().indexOf("_") <= 0) {
                log.info("[JT-Talkback stream arrives] The stream format is wrong, stream should bejt_[phoneNumber]_[channelId]_talk");
                return;
            }
            String[] streamArray = event.getStream().split("_");
            if (streamArray.length != 4) {
                log.info("[JT-Talkback stream arrives] The stream format is wrong, stream should bejt_[phoneNumber]_[channelId]_talk");
                return;
            }
            String phoneNumber = streamArray[1];
            String channelId = streamArray[2];
            JTDevice device = jt1078Service.getDevice(phoneNumber);
            if (device == null) {
                log.info("[JT-Talkback stream arrives] Device not found{}", phoneNumber);
                return;
            }
            sendTalk(device, Integer.valueOf(channelId), event.getMediaServer(), event.getApp(), event.getStream());

        }
    }

    /**
     * Stream departure processing
     */
    @Async
    @EventListener
    public void onApplicationEvent(MediaDepartureEvent event) {

    }

    /**
     * Stream not found processing
     */
    @Async
    @EventListener
    public void onApplicationEvent(MediaNotFoundEvent event) {
        if (!userSetting.getAutoApplyPlay()) {
            return;
        }
        JTMediaStreamType jtMediaStreamType = checkStreamFromJt(event.getApp(), event.getStream());
        if (jtMediaStreamType == null){
            return;
        }
        String[] streamParamArray = event.getStream().split("_");
        String phoneNumber = streamParamArray[1];
        int channelId = Integer.parseInt(streamParamArray[2]);
        String params = event.getParams();
        Map<String, String> paramMap = MediaServerUtils.urlParamToMap(params);
        int type = 0;
        try {
            type = Integer.parseInt(paramMap.get("type"));
        }catch (NumberFormatException ignored) {}
        if (jtMediaStreamType.equals(JTMediaStreamType.PLAY)) {
            play(phoneNumber, channelId, 0, null);
        }else if (jtMediaStreamType.equals(JTMediaStreamType.PLAYBACK)) {
            String startTimeParam = DateUtil.jt1078Toyyyy_MM_dd_HH_mm_ss(streamParamArray[3]);
            String endTimeParam = DateUtil.jt1078Toyyyy_MM_dd_HH_mm_ss(streamParamArray[4]);
            int rate = 0;
            int playbackType = 0;
            int playbackSpeed = 0;
            try {
                rate = Integer.parseInt(paramMap.get("rate"));
                playbackType = Integer.parseInt(paramMap.get("playbackType"));
                playbackSpeed = Integer.parseInt(paramMap.get("playbackSpeed"));
            }catch (NumberFormatException ignored) {}
            playback(phoneNumber, channelId, startTimeParam, endTimeParam, type, rate, playbackType, playbackSpeed, null);
        }
    }


    /**
     * Verify whether the stream belongs to the target
     */
    private JTMediaStreamType checkStreamFromJt(String app, String stream) {
        if (!MediaStreamUtil.isJT1078(app, stream)) {
            return null;
        }
        if (MediaStreamUtil.isJT1078Play(app, stream)) {
            return JTMediaStreamType.PLAY;
        }else if (MediaStreamUtil.isJT1078Playback(app, stream)) {
            return JTMediaStreamType.PLAYBACK;
        }else if (MediaStreamUtil.isJT1078Talk(app, stream)) {
            return JTMediaStreamType.TALK;
        }else {
            return null;
        }
    }

    private final Map<String, List<CommonCallback<WVPResult<StreamInfo>>>> inviteErrorCallbackMap = new ConcurrentHashMap<>();

    @Override
    public void play(String phoneNumber, Integer channelId, int type, CommonCallback<WVPResult<StreamInfo>> callback) {
        JTDevice device = jt1078Service.getDevice(phoneNumber);
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Device does not exist");
        }
        jt1078Template.checkTerminalStatus(phoneNumber);
        JTChannel channel = jt1078Service.getChannel(device.getId(), channelId);
        if (channel == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Channel does not exist");
        }
        play(device, channel, type, callback);
    }

    private void play(JTDevice device, JTChannel channel, int type, CommonCallback<WVPResult<StreamInfo>> callback) {
        String phoneNumber = device.getPhoneNumber();
        int channelId = channel.getChannelId();
        String finalStream = MediaStreamUtil.getJTPlayStreamId(phoneNumber, channelId);
        // Check whether the stream already exists, return if it exists
        String playKey = VideoManagerConstants.INVITE_INFO_1078_PLAY + phoneNumber + ":" + channelId;
        List<CommonCallback<WVPResult<StreamInfo>>> errorCallbacks = inviteErrorCallbackMap.computeIfAbsent(playKey, k -> new ArrayList<>());
        errorCallbacks.add(callback);
        StreamInfo streamInfo = (StreamInfo) redisTemplate.opsForValue().get(playKey);
        if (streamInfo != null) {
            MediaServer mediaServer = streamInfo.getMediaServer();
            if (mediaServer != null) {
                // Query whether the stream exists. If it does not exist, delete the cached data.
                MediaInfo mediaInfo = mediaServerService.getMediaInfo(mediaServer, MediaStreamUtil.RTP_APP, streamInfo.getStream());
                if (mediaInfo != null) {
                    log.info("[JT-on demand] The on-demand video already exists, return directly， phoneNumber： {}， channelId： {}", phoneNumber, channelId);
                    for (CommonCallback<WVPResult<StreamInfo>> errorCallback : errorCallbacks) {
                        errorCallback.run(new WVPResult<>(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo));
                    }
                    return;
                }
            }
            // Clean data
            redisTemplate.delete(playKey);
        }

        MediaServer mediaServer;
        if (org.springframework.util.ObjectUtils.isEmpty(device.getMediaServerId()) || "auto".equals(device.getMediaServerId())) {
            mediaServer = mediaServerService.getMediaServerForMinimumLoad(null);
        } else {
            mediaServer = mediaServerService.getOne(device.getMediaServerId());
        }
        if (mediaServer == null) {
            for (CommonCallback<WVPResult<StreamInfo>> errorCallback : errorCallbacks) {
                errorCallback.run(new WVPResult<>(InviteErrorCode.FAIL.getCode(), "No available media node found", streamInfo));
            }
            return;
        }

        String streamId;
        String streamReplace = null;
        if (mediaServer.isRtpEnable()) {
            log.info("[JT-on demand] The media server supports rtp, enable rtp on-demand， phoneNumber： {}， channelId： {}", phoneNumber, channelId);
            streamId = finalStream;
        }else {
            String phone = StringUtils.leftPad(device.getPhoneNumber(), 12, '0');
            streamId = String.format("%s_%s", phone, channelId);
            streamReplace = finalStream;
        }

        // Enable traffic collection port
        RTPServerParam rtpServerParam = new RTPServerParam();
        rtpServerParam.setMediaServer(mediaServer);
        rtpServerParam.setApp(MediaStreamUtil.RTP_APP);
        rtpServerParam.setStreamId(finalStream);
        rtpServerParam.setPort(0);
        rtpServerParam.setTcpMode(1); // 1 Indicates tcp passive
        rtpServerParam.setOnlyAuto(false);
        rtpServerParam.setDisableAudio(!channel.isHasAudio());

        int port = receiveRtpServerService.openCommonRTPServer(rtpServerParam, (code, msg, hookData) -> {

            if (code == InviteErrorCode.SUCCESS.getCode() && hookData != null ) {
                // hookresponse
                log.info("[JT-on demand] On-demand successful, mobile phone number： {}， channel： {}", phoneNumber, channelId);
                // TODO Send 9105 real-time audio and video transmission status notification and packet loss rate notification
                StreamInfo info = onPublishHandler(mediaServer, hookData, phoneNumber, channelId);

                for (CommonCallback<WVPResult<StreamInfo>> errorCallback : errorCallbacks) {
                    if (errorCallback == null) {
                        continue;
                    }
                    errorCallback.run(new WVPResult<>(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), info));
                }
                redisTemplate.opsForValue().set(playKey, info);
                // screenshot
                String path = "snap";
                String fileName = phoneNumber + "_" + channelId + ".jpg";
                // Request a screenshot
                log.info("[Request a screenshot]: {}", fileName);
                mediaServerService.getSnap(mediaServer, MediaStreamUtil.RTP_APP, finalStream, 15, 1, path, fileName);
            }else {
                if (callback != null) {
                    callback.run(WVPResult.fail(code, msg));
                }
                log.info("[JT-on demand] timeout， phoneNumber： {}， channelId： {}", phoneNumber, channelId);
                for (CommonCallback<WVPResult<StreamInfo>> errorCallback : errorCallbacks) {
                    errorCallback.run(new WVPResult<>(InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getCode(),
                            InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getMsg(), null));
                }
                stopPlay(phoneNumber, channelId);
            }
        });
        if (port <= 0) {
            stopPlay(phoneNumber, channelId);
            return;
        }
        // Supplementary authentication parameters
        receiveRtpServerService.addAuthenticateInfo(streamId, streamReplace, channel.isHasAudio(), jt1078Config.getRecord(), null);

        log.info("[JT-on demand] phoneNumber： {}， channelId： {}，IP: {}, port： {}", phoneNumber, channelId, mediaServer.getSdpIp(), port);
        J9101 j9101 = new J9101();
        j9101.setChannel(channelId);
        j9101.setIp(mediaServer.getSdpIp());
        j9101.setRate(1);
        j9101.setTcpPort(port);
        j9101.setUdpPort(port);
        j9101.setType(type);
        jt1078Template.startLive(phoneNumber, j9101, 6);
    }

    public StreamInfo onPublishHandler(MediaServer mediaServerItem, HookData hookData, String phoneNumber, Integer channelId) {
        StreamInfo streamInfo = mediaServerService.getStreamInfoByAppAndStream(mediaServerItem, MediaStreamUtil.RTP_APP, hookData.getStream(), hookData.getMediaInfo(), null);
        streamInfo.setDeviceId(phoneNumber);
        streamInfo.setChannelId(channelId);
        return streamInfo;
    }

    @Override
    public void stopPlay(String phoneNumber, Integer channelId) {
        String playKey = VideoManagerConstants.INVITE_INFO_1078_PLAY + phoneNumber + ":" + channelId;
        dynamicTask.stop(playKey);
        // Cleanup callback
        List<CommonCallback<WVPResult<StreamInfo>>> generalCallbacks = inviteErrorCallbackMap.get(playKey);
        if (generalCallbacks != null && !generalCallbacks.isEmpty()) {
            for (CommonCallback<WVPResult<StreamInfo>> callback : generalCallbacks) {
                callback.run(new WVPResult<>(InviteErrorCode.ERROR_FOR_FINISH.getCode(), InviteErrorCode.ERROR_FOR_FINISH.getMsg(), null));
            }
        }
        jt1078Template.checkTerminalStatus(phoneNumber);
        StreamInfo streamInfo = (StreamInfo) redisTemplate.opsForValue().get(playKey);
        // Send stop command
        J9102 j9102 = new J9102();
        j9102.setChannel(channelId);
        j9102.setCommand(0);
        j9102.setCloseType(0);
        j9102.setStreamType(1);
        jt1078Template.stopLive(phoneNumber, j9102, 6);
        log.info("[JT-Stop on demand] phoneNumber： {}， channelId： {}", phoneNumber, channelId);
        // Delete cached data
        if (streamInfo != null) {
            // ClosertpServer
            receiveRtpServerService.closeRTPServer(streamInfo.getMediaServer(), streamInfo.getApp(), streamInfo.getStream());
            redisTemplate.delete(playKey);
        }

    }

    @Override
    public void pausePlay(String phoneNumber, Integer channelId) {
        String playKey = VideoManagerConstants.INVITE_INFO_1078_PLAY + phoneNumber + ":" + channelId;
        dynamicTask.stop(playKey);
        StreamInfo streamInfo = (StreamInfo) redisTemplate.opsForValue().get(playKey);
        if (streamInfo == null) {
            log.info("[JT-Pause on demand] No on-demand information found phoneNumber： {}， channelId： {}", phoneNumber, channelId);
        }
        log.info("[JT-Pause on demand] phoneNumber： {}， channelId： {}", phoneNumber, channelId);
        // Send pause command
        J9102 j9102 = new J9102();
        j9102.setChannel(channelId);
        j9102.setCommand(2);
        j9102.setCloseType(0);
        j9102.setStreamType(1);
        jt1078Template.stopLive(phoneNumber, j9102, 6);
    }

    @Override
    public void continueLivePlay(String phoneNumber, Integer channelId) {
        String playKey = VideoManagerConstants.INVITE_INFO_1078_PLAY + phoneNumber + ":" + channelId;
        dynamicTask.stop(playKey);
        StreamInfo streamInfo = (StreamInfo) redisTemplate.opsForValue().get(playKey);
        if (streamInfo == null) {
            log.info("[JT-Continue on demand] No on-demand information found phoneNumber： {}， channelId： {}", phoneNumber, channelId);
        }
        log.info("[JT-Continue on demand] phoneNumber： {}， channelId： {}", phoneNumber, channelId);
        // Send pause command
        J9102 j9102 = new J9102();
        j9102.setChannel(channelId);
        j9102.setCommand(2);
        j9102.setCloseType(0);
        j9102.setStreamType(1);
        jt1078Template.stopLive(phoneNumber, j9102, 6);
    }

    @Override
    public List<J1205.JRecordItem> getRecordList(String phoneNumber, Integer channelId, String startTime, String endTime) {
        log.info("[JT-Query video list] phoneNumber： {}， channelId： {}， startTime： {}， endTime： {}"
                , phoneNumber, channelId, startTime, endTime);
        // Send command to request recording list
        J9205 j9205 = new J9205();
        j9205.setChannelId(channelId);
        j9205.setStartTime(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(startTime));
        j9205.setEndTime(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(endTime));
        j9205.setMediaType(0);
        j9205.setStreamType(0);
        j9205.setStorageType(0);
        List<J1205.JRecordItem> JRecordItemList = (List<J1205.JRecordItem>) jt1078Template.queryBackTime(phoneNumber, j9205, 20);
        if (JRecordItemList == null || JRecordItemList.isEmpty()) {
            return null;
        }
        log.info("[JT-Query video list] phoneNumber： {}， channelId： {}， startTime： {}， endTime： {}, result: {}Article"
                , phoneNumber, channelId, startTime, endTime, JRecordItemList.size());
        return JRecordItemList;
    }



    @Override
    public void playback(String phoneNumber, Integer channelId, String startTime, String endTime, Integer type,
                         Integer rate, Integer playbackType, Integer playbackSpeed, CommonCallback<WVPResult<StreamInfo>> callback) {
        JTDevice device = jt1078Service.getDevice(phoneNumber);
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Device does not exist");
        }
        jt1078Template.checkTerminalStatus(phoneNumber);
        JTChannel channel = jt1078Service.getChannel(device.getId(), channelId);
        if (channel == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Channel does not exist");
        }
        playback(device, channel, startTime, endTime, type, rate, playbackType, playbackSpeed, callback);

    }

    /**
     * Playback
     * @param device  Equipment
     * @param channel channel
     * @param startTime start time
     * @param endTime end time
     * @param type Audio and video resource types: 0. Audio and video 1. Audio 2. Video 3. Video or audio and video
     * @param rate Stream type: 0. All streams 1. Main stream 2. Sub-stream(If this channel only transmits audio, this field is set to0)
     * @param playbackType Playback mode: 0. Normal playback 1. Fast forward playback 2. Key frame fast rewind playback 3. Key frame playback 4. Single frame upload
     * @param playbackSpeed Fast forward or rewind multiple: 0. Invalid 1.1 times 2.2 times 3.4 times 4.8 times 5.16 times (When the playback control is 1 and 2, the content of this field is valid, otherwise it is set0)
     * @param callback end callback
     */
    private void playback(JTDevice device, JTChannel channel, String startTime, String endTime, Integer type,
                         Integer rate, Integer playbackType, Integer playbackSpeed, CommonCallback<WVPResult<StreamInfo>> callback) {

        String phoneNumber = device.getPhoneNumber();
        Integer channelId = channel.getChannelId();
        log.info("[JT-Playback] playback, equipment:{}， channel： {}， start time： {}， end time： {}， Audio and video type： {}， Stream type： {}， " +
                "Playback mode： {}， Fast forward or rewind times： {}", phoneNumber, channelId, startTime, endTime, type, rate, playbackType, playbackSpeed);
        // Check whether the stream already exists, return if it exists
        String playbackKey = VideoManagerConstants.INVITE_INFO_1078_PLAYBACK + phoneNumber + ":" + channelId;
        List<CommonCallback<WVPResult<StreamInfo>>> errorCallbacks = inviteErrorCallbackMap.computeIfAbsent(playbackKey, k -> new ArrayList<>());
        errorCallbacks.add(callback);
        String logInfo = String.format("phoneNumber:%s, channelId:%s, startTime:%s, endTime:%s", phoneNumber, channelId, startTime, endTime);
        StreamInfo streamInfo = (StreamInfo) redisTemplate.opsForValue().get(playbackKey);
        if (streamInfo != null) {

            receiveRtpServerService.closeRTPServer(streamInfo.getMediaServer(), streamInfo.getApp(), streamInfo.getStream());
            // Clean data
            redisTemplate.delete(playbackKey);
        }

        String app = MediaStreamUtil.RTP_APP;
        String finalStream =  MediaStreamUtil.getJTPlaybackStreamId(phoneNumber, channelId,
                DateUtil.yyyy_MM_dd_HH_mm_ssToUrl(startTime), DateUtil.yyyy_MM_dd_HH_mm_ssToUrl(endTime));
        MediaServer mediaServer;
        if (org.springframework.util.ObjectUtils.isEmpty(device.getMediaServerId()) || "auto".equals(device.getMediaServerId())) {
            mediaServer = mediaServerService.getMediaServerForMinimumLoad(null);
        } else {
            mediaServer = mediaServerService.getOne(device.getMediaServerId());
        }
        if (mediaServer == null) {
            for (CommonCallback<WVPResult<StreamInfo>> errorCallback : errorCallbacks) {
                errorCallback.run(new WVPResult<>(InviteErrorCode.FAIL.getCode(), "No available media node found", streamInfo));
            }
            return;
        }
        String streamId;
        String streamReplace = null;
        if (mediaServer.isRtpEnable()) {
            log.info("[JT-on demand] The media server supports rtp, enable rtp on-demand， phoneNumber： {}， channelId： {}", phoneNumber, channelId);
            streamId = finalStream;
        }else {
            String phone = StringUtils.leftPad(device.getPhoneNumber(), 12, '0');
            streamId = String.format("%s_%s", phone, channelId);
            streamReplace = finalStream;
        }

        // Enable traffic collection port
        RTPServerParam rtpServerParam = new RTPServerParam();
        rtpServerParam.setMediaServer(mediaServer);
        rtpServerParam.setApp(MediaStreamUtil.RTP_APP);
        rtpServerParam.setStreamId(finalStream);
        rtpServerParam.setPort(0);
        rtpServerParam.setTcpMode(1); // 1 Indicates tcp passive
        rtpServerParam.setOnlyAuto(false);
        rtpServerParam.setDisableAudio(!channel.isHasAudio());

        int port = receiveRtpServerService.openCommonRTPServer(rtpServerParam, (code, msg, hookData) -> {

            if (code == InviteErrorCode.SUCCESS.getCode() && hookData != null ) {
                // hook response
                log.info("[JT-Playback] Playback successful， logInfo： {}", logInfo);
                StreamInfo info = onPublishHandler(mediaServer, hookData, phoneNumber, channelId);

                for (CommonCallback<WVPResult<StreamInfo>> errorCallback : errorCallbacks) {
                    if (errorCallback == null) {
                        continue;
                    }
                    errorCallback.run(new WVPResult<>(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), info));
                }
                redisTemplate.opsForValue().set(playbackKey, info);
            }else {
                log.info("[JT-Playback] Playback timeout， logInfo： {}", logInfo);
                for (CommonCallback<WVPResult<StreamInfo>> errorCallback : errorCallbacks) {
                    errorCallback.run(new WVPResult<>(InviteErrorCode.ERROR_FOR_SIGNALLING_TIMEOUT.getCode(),
                            InviteErrorCode.ERROR_FOR_SIGNALLING_TIMEOUT.getMsg(), null));
                }
                receiveRtpServerService.closeRTPServer(mediaServer, app, finalStream);
            }
        });
        log.info("[JT-Playback] logInfo： {}， port： {}", logInfo, port);

        // Supplementary authentication parameters
        receiveRtpServerService.addAuthenticateInfo(streamId, streamReplace, channel.isHasAudio(), jt1078Config.getRecord(), null);

        J9201 j9201 = new J9201();
        j9201.setChannel(channelId);
        j9201.setIp(mediaServer.getSdpIp());
        if (rate != null) {
            j9201.setRate(rate);
        }
        if (playbackType != null) {
            j9201.setPlaybackType(playbackType);
        }
        if (playbackSpeed != null) {
            j9201.setPlaybackSpeed(playbackSpeed);
        }

        j9201.setTcpPort(port);
        j9201.setUdpPort(port);
        j9201.setType(type);
        j9201.setStartTime(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(startTime));
        j9201.setEndTime(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(endTime));
        jt1078Template.startBackLive(phoneNumber, j9201, 20);

    }

    @Override
    public void playbackControl(String phoneNumber, Integer channelId, Integer command, Integer playbackSpeed, String time) {
        String playKey = VideoManagerConstants.INVITE_INFO_1078_PLAYBACK + phoneNumber + ":" + channelId;
        dynamicTask.stop(playKey);
        if (command == 2) {
            log.info("[JT-Stop playback] phoneNumber： {}， channelId： {}， command： {}， playbackSpeed： {}， time： {}",
                    phoneNumber, channelId, command, playbackSpeed, time);
            // End playback
            StreamInfo streamInfo = (StreamInfo) redisTemplate.opsForValue().get(playKey);
            // Delete cached data
            if (streamInfo != null) {
                // ClosertpServer
                receiveRtpServerService.closeRTPServer(streamInfo.getMediaServer(), streamInfo.getApp(), streamInfo.getStream());
            }
            // Cleanup callback
            List<CommonCallback<WVPResult<StreamInfo>>> generalCallbacks = inviteErrorCallbackMap.get(playKey);
            if (generalCallbacks != null && !generalCallbacks.isEmpty()) {
                for (CommonCallback<WVPResult<StreamInfo>> callback : generalCallbacks) {
                    if (callback == null) {
                        continue;
                    }
                    callback.run(new WVPResult<>(InviteErrorCode.ERROR_FOR_FINISH.getCode(), InviteErrorCode.ERROR_FOR_FINISH.getMsg(), null));
                }
            }
        }else {
            log.info("[JT-Playback control] phoneNumber： {}， channelId： {}， command： {}， playbackSpeed： {}， time： {}",
                    phoneNumber, channelId, command, playbackSpeed, time);
        }
        // Send stop command
        J9202 j9202 = new J9202();
        j9202.setChannel(channelId);
        j9202.setPlaybackType(command);

        if (playbackSpeed != null) {
            j9202.setPlaybackSpeed(playbackSpeed);

        }
        if (!ObjectUtils.isEmpty(time)) {
            j9202.setPlaybackTime(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(time));
        }
        jt1078Template.controlBackLive(phoneNumber, j9202, 4);
    }

    @Override
    public void stopPlayback(String phoneNumber, Integer channelId) {
        playbackControl(phoneNumber, channelId, 2, null, null);
    }

    /**
     * Monitoring streaming stops
     */
    @EventListener
    public void onApplicationEvent(MediaSendRtpStoppedEvent event) {

        List<SendRtpInfo> sendRtpInfos = sendRtpServerService.queryByStream(event.getStream());
        if (sendRtpInfos.isEmpty()) {
            return;
        }
        for (SendRtpInfo sendRtpInfo : sendRtpInfos) {
            if (!sendRtpInfo.isOnlyAudio() || ObjectUtils.isEmpty(sendRtpInfo.getChannelId())) {
                continue;
            }
            if (!sendRtpInfo.getSsrc().contains("_")) {
                continue;
            }
            sendRtpServerService.delete(sendRtpInfo);
            String playKey = VideoManagerConstants.INVITE_INFO_1078_TALK + sendRtpInfo.getApp() + ":" + sendRtpInfo.getStream();
            redisTemplate.delete(playKey);
        }
    }


    @Override
    public StreamInfo startTalk(String phoneNumber, Integer channelId) {
        // Check whether the stream already exists, return if it exists
        String playKey = VideoManagerConstants.INVITE_INFO_1078_TALK + phoneNumber + ":" + channelId;
        StreamInfo streamInfo = (StreamInfo) redisTemplate.opsForValue().get(playKey);

        if (streamInfo != null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Intercom in progress");
        }

        JTDevice device = jt1078Service.getDevice(phoneNumber);
        Assert.notNull(device, "The standard equipment does not exist");

        String stream = MediaStreamUtil.getJTTalkStreamId(phoneNumber, channelId);

        MediaServer mediaServer;
        if (org.springframework.util.ObjectUtils.isEmpty(device.getMediaServerId()) || "auto".equals(device.getMediaServerId())) {
            mediaServer = mediaServerService.getMediaServerForMinimumLoad(null);
        } else {
            mediaServer = mediaServerService.getOne(device.getMediaServerId());
        }

        // Check if the stream to be sent exists，
        MediaInfo mediaInfo = mediaServerService.getMediaInfo(mediaServer, MediaStreamUtil.JT_TALK, stream);
        Assert.isNull(mediaInfo, "Intercom already exists");
        return mediaServerService.getStreamInfoByAppAndStream(mediaServer, MediaStreamUtil.JT_TALK, stream, null, null, null, false);

    }
    private void sendTalk(JTDevice device, Integer channelId, MediaServer mediaServer, String app, String stream) {
        // Check if the stream to be sent exists，
        MediaInfo mediaInfo = mediaServerService.getMediaInfo(mediaServer, app, stream);
        if (mediaInfo == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), app + "/" + stream + "Stream does not exist");
        }

        String phoneNumber = device.getPhoneNumber();
        String receiveStream = MediaStreamUtil.getJTTalkReceiveStreamId(phoneNumber, channelId);
        // To open the traffic port, zlm needs to set the ssrc field to imei_channel format to send 1078 rtp stream.
        String ssrc = device.getPhoneNumber() + "_" + channelId;
        SendRtpInfo sendRtpInfo = sendRtpServerService.createSendRtpInfo(mediaServer, null, null, ssrc, phoneNumber, MediaStreamUtil.JT_TALK, stream, channelId, true, false);
        sendRtpInfo.setTcpActive(true);
        sendRtpInfo.setUsePs(false);
        sendRtpInfo.setOnlyAudio(true);
        sendRtpInfo.setReceiveStream(receiveStream);

        // Set up hook monitoring
        Hook hook = Hook.getInstance(HookType.on_media_arrival, MediaStreamUtil.RTP_APP, sendRtpInfo.getReceiveStream(), mediaServer.getId());
        subscribe.addSubscribe(hook, (hookData) -> {
            log.info("[JT-intercom] Intercom connection established， phoneNumber： {}， channelId： {}", phoneNumber, channelId);
            subscribe.removeSubscribe(hook);
            // Store streaming information
            sendRtpServerService.update(sendRtpInfo);
        });
        Hook hookForDeparture = Hook.getInstance(HookType.on_media_departure, app, stream, mediaServer.getId());
        subscribe.addSubscribe(hookForDeparture, (hookData) -> {
            log.info("[JT-intercom] Source logout during intercom， app: {}. stream: {}, phoneNumber： {}， channelId： {}", app, stream, phoneNumber, channelId);
            stopTalk(phoneNumber, channelId);
        });

        Integer localPort = mediaServerService.startSendRtpPassive(mediaServer, sendRtpInfo, userSetting.getPlayTimeout());

        log.info("[JT-intercom] phoneNumber： {}， channelId： {}， Transceiver port： {}， app: {}, stream: {}",
                phoneNumber, channelId, localPort, app, stream);
        J9101 j9101 = new J9101();
        j9101.setChannel(channelId);
        j9101.setIp(mediaServer.getSdpIp());
        j9101.setRate(1);
        j9101.setTcpPort(sendRtpInfo.getLocalPort());
        j9101.setUdpPort(sendRtpInfo.getLocalPort());
        j9101.setType(4);
        jt1078Template.startLive(phoneNumber, j9101, 6);

        log.info("[JT-intercom] Intercom message sent successfully， phoneNumber： {}， channelId： {}", phoneNumber, channelId);
        // Store streaming information
//        sendRtpServerService.update(sendRtpInfo);
    }

    @Override
    public void stopTalk(String phoneNumber, Integer channelId) {
        String playKey = VideoManagerConstants.INVITE_INFO_1078_TALK + phoneNumber + ":" + channelId;
        dynamicTask.stop(playKey);
        StreamInfo streamInfo = (StreamInfo) redisTemplate.opsForValue().get(playKey);
        // Send stop command
        J9102 j9102 = new J9102();
        j9102.setChannel(channelId);
        j9102.setCommand(4);
        j9102.setCloseType(0);
        j9102.setStreamType(1);
        jt1078Template.stopLive(phoneNumber, j9102, 6);
        log.info("[JT-Stop intercom] phoneNumber： {}， channelId： {}", phoneNumber, channelId);
        // Delete cached data
        if (streamInfo != null) {
            redisTemplate.delete(playKey);
            // Close rtpServer
            receiveRtpServerService.closeRTPServer(streamInfo.getMediaServer(), streamInfo.getApp(), streamInfo.getStream());
        }
        // Cleanup callback
        List<CommonCallback<WVPResult<StreamInfo>>> generalCallbacks = inviteErrorCallbackMap.get(playKey);
        if (generalCallbacks != null && !generalCallbacks.isEmpty()) {
            for (CommonCallback<WVPResult<StreamInfo>> callback : generalCallbacks) {
                callback.run(new WVPResult<>(InviteErrorCode.ERROR_FOR_FINISH.getCode(), InviteErrorCode.ERROR_FOR_FINISH.getMsg(), null));
            }
        }
    }

    @Override
    public void start(Integer channelId, Boolean record, ErrorCallback<StreamInfo> callback) {
        JTChannel channel = jt1078Service.getChannelByDbId(channelId);
        Assert.notNull(channel, "Channel does not exist");
        JTDevice device = jt1078Service.getDeviceById(channel.getTerminalDbId());
        Assert.notNull(device, "Device does not exist");
        jt1078Template.checkTerminalStatus(device.getPhoneNumber());
        play(device, channel, 0,
                result -> callback.run(result.getCode(), result.getMsg(), result.getData()));
    }

    @Override
    public void stop(Integer channelId) {
        JTChannel channel = jt1078Service.getChannelByDbId(channelId);
        Assert.notNull(channel, "Channel does not exist");
        JTDevice device = jt1078Service.getDeviceById(channel.getTerminalDbId());
        Assert.notNull(device, "Device does not exist");
        stopPlay(device.getPhoneNumber(), channel.getChannelId());
    }

    @Override
    public void playBack(Integer channelId, Long startTime, Long stopTime, ErrorCallback<StreamInfo> callback) {
        if (startTime == null || stopTime == null) {
            throw new PlayException(Response.BAD_REQUEST, "bad request");
        }
        JTChannel channel = jt1078Service.getChannelByDbId(channelId);
        Assert.notNull(channel, "Channel does not exist");
        JTDevice device = jt1078Service.getDeviceById(channel.getTerminalDbId());
        Assert.notNull(device, "Device does not exist");
        jt1078Template.checkTerminalStatus(device.getPhoneNumber());
        String startTimeStr = DateUtil.timestampTo_yyyy_MM_dd_HH_mm_ss(startTime);
        String stopTimeStr = DateUtil.timestampTo_yyyy_MM_dd_HH_mm_ss(stopTime);
        playback(device, channel, startTimeStr, stopTimeStr, 0, 1, 0, 0,
                result -> callback.run(result.getCode(), result.getMsg(), result.getData()));
    }
}
