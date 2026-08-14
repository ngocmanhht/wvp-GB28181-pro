package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.dao.CommonGBChannelMapper;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelPlayService;
import com.genersoft.iot.vmp.gb28181.service.ISourceBroadcastService;
import com.genersoft.iot.vmp.gb28181.service.ISourceDownloadService;
import com.genersoft.iot.vmp.gb28181.service.ISourcePlayService;
import com.genersoft.iot.vmp.gb28181.service.ISourcePlaybackService;
import com.genersoft.iot.vmp.vmanager.bean.AudioTalkResult;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sip.message.Response;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GbChannelPlayServiceImpl implements IGbChannelPlayService {

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private CommonGBChannelMapper channelMapper;

    @Autowired
    private Map<String, ISourcePlayService> sourcePlayServiceMap;

    @Autowired
    private Map<String, ISourcePlaybackService> sourcePlaybackServiceMap;

    @Autowired
    private Map<String, ISourceDownloadService> sourceDownloadServiceMap;

    @Autowired
    private Map<String, ISourceBroadcastService> sourceBroadcastServiceMap;


    @Override
    public void startInvite(CommonGBChannel channel, InviteMessageInfo inviteInfo, Platform platform, ErrorCallback<StreamInfo> callback) {
        if (channel == null || inviteInfo == null || callback == null || channel.getDataType() == null) {
            log.warn("[Universal channel on demand] Parameter exception, channel: {}, inviteInfo: {}, callback: {}", channel != null, inviteInfo != null, callback != null);
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
        log.info("[On-demand universal channel] Type：{}， channel： {}({})", inviteInfo.getSessionName(), channel.getGbName(), channel.getGbDeviceId());

        if ("Play".equalsIgnoreCase(inviteInfo.getSessionName())) {
            play(channel, platform, userSetting.getRecordSip(), callback);
        }else if ("Playback".equals(inviteInfo.getSessionName())) {
            playback(channel, inviteInfo.getStartTime(), inviteInfo.getStopTime(), callback);
        }else if ("Download".equals(inviteInfo.getSessionName())) {
            Integer downloadSpeed = Integer.parseInt(inviteInfo.getDownloadSpeed());
            // National standard channel
            download(channel, inviteInfo.getStartTime(), inviteInfo.getStopTime(), downloadSpeed, callback);
        }else {
            // Unsupported on-demand method
            log.error("[On-demand universal channel] Unsupported on-demand method：{}， {}({})", inviteInfo.getSessionName(), channel.getGbName(), channel.getGbDeviceId());
            throw new PlayException(Response.BAD_REQUEST, "bad request");
        }
    }

    @Override
    public void stopInvite(InviteSessionType type, CommonGBChannel channel, String stream) {
        switch (type) {
            case PLAY:
                stopPlay(channel);
                break;
            case PLAYBACK:
                stopPlayback(channel, stream);
                break;
            case DOWNLOAD:
                stopDownload(channel, stream);
                break;
            default:
                // Channel data abnormality
                log.error("[On-demand universal channel] Type number： {} This type of request is not supported", type);
                throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
    }



    @Override
    public void play(CommonGBChannel channel, Platform platform, Boolean record, ErrorCallback<StreamInfo> callback) {
        log.info("[Universal channel] play, type： {}， No.：{}", ChannelDataType.getDateTypeDesc(channel.getDataType()), channel.getGbDeviceId());
        Integer dataType = channel.getDataType();
        ISourcePlayService sourceChannelPlayService = sourcePlayServiceMap.get(ChannelDataType.PLAY_SERVICE + dataType);
        if (sourceChannelPlayService == null) {
            // Channel data abnormality
            log.error("[On-demand universal channel] Type number： {} Live stream preview is not supported", ChannelDataType.getDateTypeDesc(channel.getDataType()));
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        sourceChannelPlayService.play(channel, platform, record, (code, msg, data) -> {
            if (code == InviteErrorCode.SUCCESS.getCode()) {
                // Log stream ID to database
                if (channel.getDataType() != ChannelDataType.GB28181) {
                    channelMapper.updateStream(channel.getGbId(), data.getStream());
                }
            }
            callback.run(code, msg, data);
        });
    }
    @Override
    public void playback(CommonGBChannel channel, Long startTime, Long stopTime, ErrorCallback<StreamInfo> callback) {
        log.info("[Universal channel] playback, type： {}， No.：{}", ChannelDataType.getDateTypeDesc(channel.getDataType()), channel.getGbDeviceId());
        Integer dataType = channel.getDataType();
        ISourcePlaybackService playbackService = sourcePlaybackServiceMap.get(ChannelDataType.PLAYBACK_SERVICE + dataType);
        if (playbackService == null) {
            // Channel data abnormality
            log.error("[On-demand universal channel] Type number： {} Playback is not supported", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        playbackService.playback(channel, startTime, stopTime, callback);
    }

    @Override
    public void download(CommonGBChannel channel, Long startTime, Long stopTime, Integer downloadSpeed,
                         ErrorCallback<StreamInfo> callback){
        log.info("[Universal channel] Video download, type： {}， No.：{}", ChannelDataType.getDateTypeDesc(channel.getDataType()), channel.getGbDeviceId());
        Integer dataType = channel.getDataType();
        ISourceDownloadService downloadService = sourceDownloadServiceMap.get(ChannelDataType.DOWNLOAD_SERVICE + dataType);
        if (downloadService == null) {
            // Channel data abnormality
            log.error("[On-demand universal channel] Type number： {} Video downloading is not supported", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        downloadService.download(channel, startTime, stopTime, downloadSpeed, callback);
    }

    @Override
    public void stopPlay(CommonGBChannel channel) {
        Integer dataType = channel.getDataType();
        ISourcePlayService sourceChannelPlayService = sourcePlayServiceMap.get(ChannelDataType.PLAY_SERVICE + dataType);
        if (sourceChannelPlayService == null) {
            // Channel data abnormality
            log.error("[On-demand universal channel] Type number： {} Stopping live streaming is not supported", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        sourceChannelPlayService.stopPlay(channel);
        channelMapper.updateStream(channel.getGbId(), null);
    }

    @Override
    public void stopPlayback(CommonGBChannel channel, String stream) {
        log.info("[Universal channel] Stop playback, type： {}， No.：{}", ChannelDataType.getDateTypeDesc(channel.getDataType()), channel.getGbDeviceId());
        Integer dataType = channel.getDataType();
        ISourcePlaybackService playbackService = sourcePlaybackServiceMap.get(ChannelDataType.PLAYBACK_SERVICE + dataType);
        if (playbackService == null) {
            // Channel data abnormality
            log.error("[On-demand universal channel] Type number： {} Playback is not supported", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        playbackService.stopPlayback(channel, stream);
    }

    @Override
    public void stopDownload(CommonGBChannel channel, String stream) {
        log.info("[Universal channel] Stop video downloading, type： {}， No.：{} stream: {}", ChannelDataType.getDateTypeDesc(channel.getDataType()), channel.getGbDeviceId(), stream);
        Integer dataType = channel.getDataType();
        ISourceDownloadService downloadService = sourceDownloadServiceMap.get(ChannelDataType.DOWNLOAD_SERVICE + dataType);
        if (downloadService == null) {
            // Channel data abnormality
            log.error("[On-demand universal channel] Type number： {} Video downloading is not supported", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        downloadService.stopDownload(channel, stream);
    }

    @Override
    public void playbackPause(CommonGBChannel channel, String stream) {
        log.info("[Universal channel] Playback paused, type： {}， No.：{} stream：{}", ChannelDataType.getDateTypeDesc(channel.getDataType()), channel.getGbDeviceId(), stream);
        Integer dataType = channel.getDataType();
        ISourcePlaybackService playbackService = sourcePlaybackServiceMap.get(ChannelDataType.PLAYBACK_SERVICE + dataType);
        if (playbackService == null) {
            // Channel data abnormality
            log.error("[On-demand universal channel] Type number： {} Playback pause is not supported", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        playbackService.playbackPause(channel, stream);
    }

    @Override
    public void playbackResume(CommonGBChannel channel, String stream) {
        log.info("[Universal channel] Playback pause and resume, type： {}， No.：{} stream：{}", ChannelDataType.getDateTypeDesc(channel.getDataType()), channel.getGbDeviceId(), stream);
        Integer dataType = channel.getDataType();
        ISourcePlaybackService playbackService = sourcePlaybackServiceMap.get(ChannelDataType.PLAYBACK_SERVICE + dataType);
        if (playbackService == null) {
            // Channel data abnormality
            log.error("[On-demand universal channel] Type number： {} Playback pause and resume is not supported", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        playbackService.playbackResume(channel, stream);
    }

    @Override
    public void playbackSeek(CommonGBChannel channel, String stream, long seekTime) {
        log.info("[Universal channel] Playback drag play, type： {}， No.：{} stream：{}", ChannelDataType.getDateTypeDesc(channel.getDataType()), channel.getGbDeviceId(), stream);
        Integer dataType = channel.getDataType();
        ISourcePlaybackService playbackService = sourcePlaybackServiceMap.get(ChannelDataType.PLAYBACK_SERVICE + dataType);
        if (playbackService == null) {
            // Channel data abnormality
            log.error("[On-demand universal channel] Type number： {} Playback pause and resume is not supported", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        playbackService.playbackSeek(channel, stream, seekTime);
    }

    @Override
    public void playbackSpeed(CommonGBChannel channel, String stream, Double speed) {
        log.info("[Universal channel] Playback at double speed, type： {}， No.：{} stream：{}", ChannelDataType.getDateTypeDesc(channel.getDataType()), channel.getGbDeviceId(), stream);
        Integer dataType = channel.getDataType();
        ISourcePlaybackService playbackService = sourcePlaybackServiceMap.get(ChannelDataType.PLAYBACK_SERVICE + dataType);
        if (playbackService == null) {
            // Channel data abnormality
            log.error("[On-demand universal channel] Type number： {} Playback pause and resume is not supported", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        playbackService.playbackSpeed(channel, stream, speed);
    }

    @Override
    public void queryRecord(CommonGBChannel channel, String startTime, String endTime, ErrorCallback<List<CommonRecordInfo>> callback) {
        log.info("[Universal channel] Video query, type： {}， No.：{}", ChannelDataType.getDateTypeDesc(channel.getDataType()), channel.getGbDeviceId());
        Integer dataType = channel.getDataType();
        ISourcePlaybackService playbackService = sourcePlaybackServiceMap.get(ChannelDataType.PLAYBACK_SERVICE + dataType);
        if (playbackService == null) {
            // Channel data abnormality
            log.error("[On-demand universal channel] Type number： {} Playback pause and resume is not supported", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        playbackService.queryRecord(channel, startTime, endTime, callback);
    }

    @Override
    public void getSnap(CommonGBChannel channel, ErrorCallback<byte[]> callback) {
        log.info("[Universal channel] Get snapshot, type： {}， No.：{}", ChannelDataType.getDateTypeDesc(channel.getDataType()), channel.getGbDeviceId());
        Integer dataType = channel.getDataType();
        ISourcePlayService sourceChannelPlayService = sourcePlayServiceMap.get(ChannelDataType.PLAY_SERVICE + dataType);
        if (sourceChannelPlayService == null) {
            // Channel data abnormality
            log.error("[Universal channel] Get snapshot type number： {} Live stream preview related services are not supported", ChannelDataType.getDateTypeDesc(channel.getDataType()));
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        sourceChannelPlayService.getSnap(channel, callback);
    }

    @Override
    public AudioTalkResult startTalk(CommonGBChannel channel) {
        log.info("[Universal channel] Start talkback, type： {}， No.：{}", ChannelDataType.getDateTypeDesc(channel.getDataType()), channel.getGbDeviceId());
        Integer dataType = channel.getDataType();
        ISourceBroadcastService broadcastService = sourceBroadcastServiceMap.get(ChannelDataType.BROADCAST_SERVICE + dataType);
        if (broadcastService == null) {
            log.error("[Universal channel] Type number： {} Does not support intercom", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        return broadcastService.startTalk(channel);
    }

    @Override
    public void stopTalk(CommonGBChannel channel) {
        log.info("[Universal channel] stop talk, type： {}， No.：{}", ChannelDataType.getDateTypeDesc(channel.getDataType()), channel.getGbDeviceId());
        Integer dataType = channel.getDataType();
        ISourceBroadcastService broadcastService = sourceBroadcastServiceMap.get(ChannelDataType.BROADCAST_SERVICE + dataType);
        if (broadcastService == null) {
            log.error("[Universal channel] Type number： {} Does not support intercom", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        broadcastService.stopTalk(channel);
    }

    @Override
    public AudioTalkResult startBroadcast(CommonGBChannel channel) {
        log.info("[Universal channel] Start shouting, type： {}， No.：{}", ChannelDataType.getDateTypeDesc(channel.getDataType()), channel.getGbDeviceId());
        Integer dataType = channel.getDataType();
        ISourceBroadcastService broadcastService = sourceBroadcastServiceMap.get(ChannelDataType.BROADCAST_SERVICE + dataType);
        if (broadcastService == null) {
            log.error("[Universal channel] Type number： {} Does not support shouting", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        return broadcastService.startBroadcast(channel);
    }

    @Override
    public void stopBroadcast(CommonGBChannel channel) {
        log.info("[Universal channel] stop shouting, type： {}， No.：{}", ChannelDataType.getDateTypeDesc(channel.getDataType()), channel.getGbDeviceId());
        Integer dataType = channel.getDataType();
        ISourceBroadcastService broadcastService = sourceBroadcastServiceMap.get(ChannelDataType.BROADCAST_SERVICE + dataType);
        if (broadcastService == null) {
            log.error("[Universal channel] Type number： {} Does not support shouting", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        broadcastService.stopBroadcast(channel);
    }
}
