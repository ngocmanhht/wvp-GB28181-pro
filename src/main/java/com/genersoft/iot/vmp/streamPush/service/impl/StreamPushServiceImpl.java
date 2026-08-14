package com.genersoft.iot.vmp.streamPush.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.enums.MediaStreamUtil;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.media.MediaArrivalEvent;
import com.genersoft.iot.vmp.media.event.media.MediaDepartureEvent;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaServerOfflineEvent;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaServerOnlineEvent;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.media.zlm.dto.StreamAuthorityInfo;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OriginType;
import com.genersoft.iot.vmp.service.ISendRtpServerService;
import com.genersoft.iot.vmp.service.bean.StreamPushItemFromRedis;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.streamPush.bean.StreamPush;
import com.genersoft.iot.vmp.streamPush.dao.StreamPushMapper;
import com.genersoft.iot.vmp.streamPush.service.IStreamPushService;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.util.*;

@Service
@Slf4j
public class StreamPushServiceImpl implements IStreamPushService {

    @Autowired
    private StreamPushMapper streamPushMapper;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private UserSetting userSetting;

    @Autowired

    private IMediaServerService mediaServerService;

    @Autowired
    private ISendRtpServerService sendRtpServerService;

    @Autowired
    private IGbChannelService gbChannelService;

    /**
     * Processing of incoming streams
     */
    @Async
    @EventListener
    @Transactional
    public void onApplicationEvent(MediaArrivalEvent event) {
        MediaInfo mediaInfo = event.getMediaInfo();
        if (mediaInfo == null) {
            return;
        }
        if (mediaInfo.getOriginType() != OriginType.RTMP_PUSH.ordinal()
                && mediaInfo.getOriginType() != OriginType.RTSP_PUSH.ordinal()
                && mediaInfo.getOriginType() != OriginType.RTC_PUSH.ordinal()) {
            return;
        }

        StreamAuthorityInfo streamAuthorityInfo = redisCatchStorage.getStreamAuthorityInfo(event.getApp(), event.getStream());
        if (streamAuthorityInfo == null) {
            streamAuthorityInfo = StreamAuthorityInfo.getInstanceByHook(event);
        } else {
            streamAuthorityInfo.setOriginType(mediaInfo.getOriginType());
        }
        redisCatchStorage.updateStreamAuthorityInfo(event.getApp(), event.getStream(), streamAuthorityInfo);

        StreamPush streamPushInDb = getPush(event.getApp(), event.getStream());
        if (streamPushInDb == null) {
            StreamPush streamPush = StreamPush.getInstance(event, userSetting.getServerId());
            streamPush.setPushing(true);
            streamPush.setServerId(userSetting.getServerId());
            streamPush.setUpdateTime(DateUtil.getNow());
            streamPush.setPushTime(DateUtil.getNow());
            add(streamPush);
        }else {
            streamPushInDb.setPushTime(DateUtil.getNow());
            streamPushInDb.setPushing(true);
            streamPushInDb.setServerId(userSetting.getServerId());
            streamPushInDb.setMediaServerId(mediaInfo.getMediaServer().getId());
            updatePushStatus(streamPushInDb);
        }
        // Redundant data, for your own use in your own system
        if (!MediaStreamUtil.GB28181_BROADCAST.equals(event.getApp()) && !MediaStreamUtil.GB28181_TALK.equals(event.getApp())) {
            redisCatchStorage.addPushListItem(event.getApp(), event.getStream(), event.getMediaInfo());
        }

        // Send stream change redis message
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("serverId", userSetting.getServerId());
        jsonObject.put("app", event.getApp());
        jsonObject.put("stream", event.getStream());
        jsonObject.put("register", true);
        jsonObject.put("mediaServerId", event.getMediaServer().getId());
        redisCatchStorage.sendStreamChangeMsg(OriginType.values()[event.getMediaInfo().getOriginType()].getType(), jsonObject);
    }

    /**
     * Stream departure processing
     */
    @Async
    @EventListener
    @Transactional
    public void onApplicationEvent(MediaDepartureEvent event) {

        // The type is obtained from the redis record when the compatible stream is logged out
        MediaInfo mediaInfo = redisCatchStorage.getPushListItem(event.getApp(), event.getStream());

        if (mediaInfo != null) {
            log.info("[push information] Query that there is a push cache in redis and start cleaning it.，{}/{}", event.getApp(), event.getStream());
            String type = OriginType.values()[mediaInfo.getOriginType()].getType();
            // Redundant data, for your own use in your own system
            redisCatchStorage.removePushListItem(event.getApp(), event.getStream(), event.getMediaServer().getId());
            // Send stream change redis message
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("serverId", userSetting.getServerId());
            jsonObject.put("app", event.getApp());
            jsonObject.put("stream", event.getStream());
            jsonObject.put("register", false);
            jsonObject.put("mediaServerId", event.getMediaServer().getId());
            redisCatchStorage.sendStreamChangeMsg(type, jsonObject);
        }
        StreamPush streamPush = getPush(event.getApp(), event.getStream());
        if (streamPush == null) {
            return;
        }
        if (streamPush.getGbDeviceId() != null) {
            streamPush.setPushing(false);
            updatePushStatus(streamPush);
        }else {
            deleteByAppAndStream(event.getApp(), event.getStream());
        }
    }

    /**
     * Streaming media node online
     */
    @Async
    @EventListener
    @Transactional
    public void onApplicationEvent(MediaServerOnlineEvent event) {
        zlmServerOnline(event.getMediaServer());
    }

    /**
     * Streaming media node offline
     */
    @Async
    @EventListener
    @Transactional
    public void onApplicationEvent(MediaServerOfflineEvent event) {
        zlmServerOffline(event.getMediaServer());
    }

    @Override
    public PageInfo<StreamPush> getPushList(Integer page, Integer count, String query, Boolean pushing, String mediaServerId) {
        PageHelper.startPage(page, count);
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }
        List<StreamPush> all = streamPushMapper.selectAll(query, pushing, mediaServerId);
        return new PageInfo<>(all);
    }

    @Override
    public List<StreamPush> getPushList(String mediaServerId) {
        return streamPushMapper.selectAllByMediaServerIdWithOutGbID(mediaServerId);
    }


    @Override
    public StreamPush getPush(String app, String stream) {
        return streamPushMapper.selectByAppAndStream(app, stream);
    }

    @Override
    @Transactional
    public boolean add(StreamPush stream) {
        log.info("[Add push flow] app: {}, stream: {}, National standard number: {}", stream.getApp(), stream.getStream(), stream.getGbDeviceId());
        StreamPush streamPushInDb = streamPushMapper.selectByAppAndStream(stream.getApp(), stream.getStream());
        if (streamPushInDb != null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Application name+Stream ID already exists");
        }
        stream.setUpdateTime(DateUtil.getNow());
        stream.setCreateTime(DateUtil.getNow());
        int addResult = streamPushMapper.add(stream);
        if (addResult <= 0) {
            return false;
        }
        if (ObjectUtils.isEmpty(stream.getGbDeviceId())) {
            return true;
        }
        CommonGBChannel channel = gbChannelService.queryByDeviceId(stream.getGbDeviceId());
        if (channel != null) {
            log.info("[Add push flow]Failed, the national standard number already exists: {} app: {}, stream: {}, ", stream.getGbDeviceId(), stream.getApp(), stream.getStream());
        }
        int addChannelResult = gbChannelService.add(stream.buildCommonGBChannel());
        return addChannelResult > 0;
    }

    @Override
    @Transactional
    public void deleteByAppAndStream(String app, String stream) {
        log.info("[Delete push stream] app: {}, stream: {}, ", app, stream);
        StreamPush streamPush = streamPushMapper.selectByAppAndStream(app, stream);
        if (streamPush == null) {
            log.info("[Delete push stream]failed, does not exist app: {}, stream: {}, ", app, stream);
            return;
        }
        if (streamPush.isPushing()) {
            stop(streamPush);
        }
        if (streamPush.getGbId() > 0) {
            gbChannelService.delete(streamPush.getGbId());
        }
        streamPushMapper.del(streamPush.getId());
    }
    @Override
    @Transactional
    public boolean update(StreamPush streamPush) {
        Assert.notNull(streamPush, "Push information cannot beNULL");
        Assert.isTrue(streamPush.getId() > 0, "Push information ID must exist");
        log.info("[Update push flow]：id: {}, app: {}, stream: {}, ", streamPush.getId(), streamPush.getApp(), streamPush.getStream());
        StreamPush streamPushInDb = streamPushMapper.queryOne(streamPush.getId());
        if (!streamPushInDb.getApp().equals(streamPush.getApp()) || !streamPushInDb.getStream().equals(streamPush.getStream())) {
            // appOr stream changes
            StreamPush streamPushInDbForAppAndStream = streamPushMapper.selectByAppAndStream(streamPush.getApp(), streamPush.getStream());
            if (streamPushInDbForAppAndStream != null && !streamPushInDbForAppAndStream.getId().equals(streamPush.getId())) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "Application name+Stream ID already exists");
            }
        }
        streamPush.setUpdateTime(DateUtil.getNow());
        streamPushMapper.update(streamPush);
        if (streamPush.getGbId() > 0) {
            gbChannelService.update(streamPush.buildCommonGBChannel());
        }
        return true;
    }


    @Override
    @Transactional
    public boolean stop(StreamPush streamPush) {
        log.info("[Actively stop streaming] id: {}, app: {}, stream: {}, ", streamPush.getId(), streamPush.getApp(), streamPush.getStream());
        MediaServer mediaServer = null;
        if (streamPush.getMediaServerId() == null) {
            log.info("[Actively stop streaming]MediaServer not found, start automatic retrieval id: {}, app: {}, stream: {}, ", streamPush.getId(), streamPush.getApp(), streamPush.getStream());
            mediaServer = mediaServerService.getMediaServerByAppAndStream(streamPush.getApp(), streamPush.getStream());
            if (mediaServer != null) {
                log.info("[Actively stop streaming] Retrieved MediaServer as{}， id: {}, app: {}, stream: {}, ", mediaServer.getId(), streamPush.getId(), streamPush.getApp(), streamPush.getStream());
            }else {
                log.info("[Actively stop streaming]No use foundMediaServer id: {}, app: {}, stream: {}, ", streamPush.getId(), streamPush.getApp(), streamPush.getStream());
            }
        }else {
            mediaServer = mediaServerService.getOne(streamPush.getMediaServerId());
            if (mediaServer == null) {
                log.info("[Actively stop streaming]Used not foundMediaServer： {}，Start automatic search id: {}, app: {}, stream: {}, ",streamPush.getMediaServerId(),  streamPush.getId(), streamPush.getApp(), streamPush.getStream());
                mediaServer = mediaServerService.getMediaServerByAppAndStream(streamPush.getApp(), streamPush.getStream());
                if (mediaServer != null) {
                    log.info("[Actively stop streaming] Retrieved MediaServer as{}， id: {}, app: {}, stream: {}, ", mediaServer.getId(), streamPush.getId(), streamPush.getApp(), streamPush.getStream());
                }else {
                    log.info("[Actively stop streaming]No use foundMediaServer id: {}, app: {}, stream: {}, ", streamPush.getId(), streamPush.getApp(), streamPush.getStream());
                }
            }
        }
        if (mediaServer != null) {
            mediaServerService.closeStreams(mediaServer, streamPush.getApp(), streamPush.getStream());
            mediaServerService.stopSendRtp(mediaServer, streamPush.getApp(), streamPush.getStream(), null);
        }
        streamPush.setPushing(false);
        if (userSetting.getUsePushingAsStatus()) {
            CommonGBChannel commonGBChannel = streamPush.buildCommonGBChannel();
            if (commonGBChannel != null) {
                gbChannelService.offline(commonGBChannel);
            }
        }
        sendRtpServerService.deleteByStream(streamPush.getStream());
        streamPush.setUpdateTime(DateUtil.getNow());
        streamPushMapper.update(streamPush);
        return true;
    }

    @Override
    @Transactional
    public boolean stopByAppAndStream(String app, String stream) {
        log.info("[Actively stop streaming] ： app: {}, stream: {}, ", app, stream);
        StreamPush streamPushItem = streamPushMapper.selectByAppAndStream(app, stream);
        if (streamPushItem != null) {
            stop(streamPushItem);
        }
        return true;
    }

    @Override
    @Transactional
    public void zlmServerOnline(MediaServer mediaServer) {
        // Synchronize zlm push information
        if (mediaServer == null) {
            return;
        }
        // database record
        List<StreamPush> pushList = getPushList(mediaServer.getId());
        Map<String, StreamPush> pushItemMap = new HashMap<>();
        // redisrecord
        List<MediaInfo> mediaInfoList = redisCatchStorage.getStreams(mediaServer.getId(), "PUSH");
        Map<String, MediaInfo> streamInfoPushItemMap = new HashMap<>();
        if (!pushList.isEmpty()) {
            for (StreamPush streamPushItem : pushList) {
                if (ObjectUtils.isEmpty(streamPushItem.getGbId())) {
                    pushItemMap.put(streamPushItem.getApp() + streamPushItem.getStream(), streamPushItem);
                }
            }
        }
        if (!mediaInfoList.isEmpty()) {
            for (MediaInfo mediaInfo : mediaInfoList) {
                if (mediaInfo == null) {
                    continue;
                }
                streamInfoPushItemMap.put(mediaInfo.getApp() + mediaInfo.getStream(), mediaInfo);
            }
        }
        // Obtain all push authentication information and clean up expired ones
        List<StreamAuthorityInfo> allStreamAuthorityInfo = redisCatchStorage.getAllStreamAuthorityInfo();
        Map<String, StreamAuthorityInfo> streamAuthorityInfoInfoMap = new HashMap<>();
        for (StreamAuthorityInfo streamAuthorityInfo : allStreamAuthorityInfo) {
            streamAuthorityInfoInfoMap.put(streamAuthorityInfo.getApp() + streamAuthorityInfo.getStream(), streamAuthorityInfo);
        }
        List<StreamInfo> mediaList = mediaServerService.getMediaList(mediaServer, null, null, null);
        if (mediaList == null) {
            return;
        }
        List<StreamPush> streamPushItems = handleJSON(mediaList);
        if (streamPushItems != null) {
            for (StreamPush streamPushItem : streamPushItems) {
                pushItemMap.remove(streamPushItem.getApp() + streamPushItem.getStream());
                streamInfoPushItemMap.remove(streamPushItem.getApp() + streamPushItem.getStream());
                streamAuthorityInfoInfoMap.remove(streamPushItem.getApp() + streamPushItem.getStream());
            }
        }
        List<StreamPush> changedStreamPushList = new ArrayList<>(pushItemMap.values());
        if (!changedStreamPushList.isEmpty()) {
            for (StreamPush streamPush : changedStreamPushList) {
                stop(streamPush);
            }
        }

        Collection<MediaInfo> mediaInfos = streamInfoPushItemMap.values();
        if (!mediaInfos.isEmpty()) {
            String type = "PUSH";
            for (MediaInfo mediaInfo : mediaInfos) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("serverId", userSetting.getServerId());
                jsonObject.put("app", mediaInfo.getApp());
                jsonObject.put("stream", mediaInfo.getStream());
                jsonObject.put("register", false);
                jsonObject.put("mediaServerId", mediaServer.getId());
                redisCatchStorage.sendStreamChangeMsg(type, jsonObject);
                // Remove information flowing from redis
                redisCatchStorage.removeStream(mediaServer.getId(), "PUSH", mediaInfo.getApp(), mediaInfo.getStream());
                // Redundant data, for your own use in your own system
                redisCatchStorage.removePushListItem(mediaInfo.getApp(), mediaInfo.getStream(), mediaServer.getId());
            }
        }
        if (!pushItemMap.isEmpty()) {
            for (StreamPush streamPush : pushItemMap.values()) {
                // If there is no national standard number, delete it from the database
                delete(streamPush.getId());
            }
        }

        Collection<StreamAuthorityInfo> streamAuthorityInfos = streamAuthorityInfoInfoMap.values();
        if (!streamAuthorityInfos.isEmpty()) {
            for (StreamAuthorityInfo streamAuthorityInfo : streamAuthorityInfos) {
                // Remove information flowing from redis
                redisCatchStorage.removeStreamAuthorityInfo(streamAuthorityInfo.getApp(), streamAuthorityInfo.getStream());
            }
        }
    }

    @Override
    @Transactional
    public void zlmServerOffline(MediaServer mediaServer) {
        List<StreamPush> streamPushItems = streamPushMapper.selectAllByMediaServerId(mediaServer.getId());
        if (!streamPushItems.isEmpty()) {
            for (StreamPush streamPushItem : streamPushItems) {
                stop(streamPushItem);
            }
        }
        // Remove streams without GBId
        streamPushMapper.deleteWithoutGBId(mediaServer.getId());
        // Send stream stop message
        String type = "PUSH";
        // Send redis message
        List<MediaInfo> mediaInfoList = redisCatchStorage.getStreams(mediaServer.getId(), type);
        if (!mediaInfoList.isEmpty()) {
            for (MediaInfo mediaInfo : mediaInfoList) {
                // Remove information flowing from redis
                redisCatchStorage.removeStream(mediaServer.getId(), type, mediaInfo.getApp(), mediaInfo.getStream());
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("serverId", userSetting.getServerId());
                jsonObject.put("app", mediaInfo.getApp());
                jsonObject.put("stream", mediaInfo.getStream());
                jsonObject.put("register", false);
                jsonObject.put("mediaServerId", mediaServer.getId());
                redisCatchStorage.sendStreamChangeMsg(type, jsonObject);

                // Redundant data, for your own use in your own system
                redisCatchStorage.removePushListItem(mediaInfo.getApp(), mediaInfo.getStream(), mediaServer.getId());
            }
        }
    }

    @Override
    @Transactional
    public void batchAdd(List<StreamPush> streamPushItems) {
        streamPushMapper.addAll(streamPushItems);
        List<CommonGBChannel> commonGBChannels = new ArrayList<>();
        for (StreamPush streamPush : streamPushItems) {
            if (!ObjectUtils.isEmpty(streamPush.getGbDeviceId())) {
                commonGBChannels.add(streamPush.buildCommonGBChannel());
            }
        }
        gbChannelService.batchAdd(commonGBChannels);
    }

    @Override
    public void allOfflineForRedisMsg() {
        String serverId = redisCatchStorage.chooseOneServer(null);
        boolean permission = userSetting.getServerId().equals(serverId);
        List<StreamPush> streamPushList = streamPushMapper.selectAll(null, null, null);
        if (streamPushList.isEmpty()) {
            return;
        }
        List<CommonGBChannel> commonGBChannelList = new ArrayList<>();
        for (StreamPush streamPush : streamPushList) {
            CommonGBChannel commonGBChannel = streamPush.buildCommonGBChannel();
            if (commonGBChannel != null) {
                commonGBChannelList.add(streamPush.buildCommonGBChannel());
            }
        }
        gbChannelService.offline(commonGBChannelList, permission);
    }

    @Override
    public void offlineforRedisMsg(List<StreamPushItemFromRedis> offlineStreams) {
        String serverId = redisCatchStorage.chooseOneServer(null);
        boolean permission = userSetting.getServerId().equals(serverId);
        // Update some devices offline
        List<StreamPush> streamPushList = streamPushMapper.getListInList(offlineStreams);
        if (streamPushList.isEmpty()) {
            log.info("[Push streaming equipment] No operable data found during device offline operation。");
            return;
        }
        List<CommonGBChannel> commonGBChannelList = gbChannelService.queryListByStreamPushList(streamPushList);
        gbChannelService.offline(commonGBChannelList, permission);
    }

    @Override
    public void onlineForRedisMsg(List<StreamPushItemFromRedis> onlineStreams) {
        if (onlineStreams.isEmpty()) {
            log.info("[Device online] The push device list is empty");
            return;
        }
        String serverId = redisCatchStorage.chooseOneServer(null);
        boolean permission = userSetting.getServerId().equals(serverId);
        // Update some devices onlinestreamPushService
        List<StreamPush> streamPushList = streamPushMapper.getListInList(onlineStreams);
        if (streamPushList.isEmpty()) {
            for (StreamPushItemFromRedis onlineStream : onlineStreams) {
                log.info("[Device online] These channels were not found： {}/{}", onlineStream.getApp(), onlineStream.getStream());
            }
            return;
        }
        List<CommonGBChannel> commonGBChannelList = gbChannelService.queryListByStreamPushList(streamPushList);
        gbChannelService.online(commonGBChannelList, permission);
    }

    @Override
    public List<String> getAllAppAndStream() {
        return streamPushMapper.getAllAppAndStream();
    }

    @Override
    public ResourceBaseInfo getOverview() {
        int total = streamPushMapper.getAllCount();
        int online = streamPushMapper.getAllPushing(userSetting.getUsePushingAsStatus());

        return new ResourceBaseInfo(total, online);
    }

    @Override
    public Map<String, StreamPush> getAllAppAndStreamMap() {
        return streamPushMapper.getAllAppAndStreamMap();
    }

    @Override
    public Map<String, StreamPush> getAllGBId() {
        return streamPushMapper.getAllGBId();
    }

    @Override
    @Transactional
    public void updatePushStatus(StreamPush streamPush) {
        if (userSetting.getUsePushingAsStatus()) {
            streamPush.setGbStatus(streamPush.isPushing()?"ON":"OFF");
        }
        streamPushMapper.updatePushStatus(streamPush);
        if (ObjectUtils.isEmpty(streamPush.getGbDeviceId())) {
            return;
        }
        if (userSetting.getUsePushingAsStatus()) {
            if ("ON".equalsIgnoreCase(streamPush.getGbStatus()) ) {
                gbChannelService.online(streamPush.buildCommonGBChannel());
            }else {
                gbChannelService.offline(streamPush.buildCommonGBChannel());
            }
        }
    }

    private List<StreamPush> handleJSON(List<StreamInfo> streamInfoList) {
        if (streamInfoList == null || streamInfoList.isEmpty()) {
            return null;
        }
        Map<String, StreamPush> result = new HashMap<>();
        for (StreamInfo streamInfo : streamInfoList) {
            // Do not save the flow of national standard reasoning and pull agent
            if (streamInfo.getOriginType() == OriginType.RTSP_PUSH.ordinal()
                    || streamInfo.getOriginType() == OriginType.RTMP_PUSH.ordinal()
                    || streamInfo.getOriginType() == OriginType.RTC_PUSH.ordinal() ) {
                String key = streamInfo.getApp() + "_" + streamInfo.getStream();
                StreamPush streamPushItem = result.get(key);
                if (streamPushItem == null) {
                    streamPushItem = StreamPush.getInstance(streamInfo);
                    result.put(key, streamPushItem);
                }
            }
        }
        return new ArrayList<>(result.values());
    }

    @Override
    @Transactional
    public void batchUpdateForRedisMsg(List<StreamPush> streamPushItemForUpdate) {
        String serverId = redisCatchStorage.chooseOneServer(null);
        boolean permission = userSetting.getServerId().equals(serverId);
        if (permission) {
            streamPushMapper.batchUpdate(streamPushItemForUpdate);
        }
        List<CommonGBChannel> commonGBChannels = new ArrayList<>();
        for (StreamPush streamPush : streamPushItemForUpdate) {
            if (!ObjectUtils.isEmpty(streamPush.getGbDeviceId())) {
                commonGBChannels.add(streamPush.buildCommonGBChannel());
            }
        }
        gbChannelService.batchUpdateForStreamPushRedisMsg(commonGBChannels, permission);
    }

    @Override
    @Transactional
    public int delete(int id) {
        StreamPush streamPush = streamPushMapper.queryOne(id);
        if (streamPush == null) {
            return 0;
        }
        if(streamPush.isPushing()) {
            MediaServer mediaServer = mediaServerService.getOne(streamPush.getMediaServerId());
            mediaServerService.closeStreams(mediaServer, streamPush.getApp(), streamPush.getStream());
        }
        if (streamPush.getGbDeviceId() != null) {
            gbChannelService.delete(streamPush.getGbId());
        }
        return streamPushMapper.del(id);
    }

    @Override
    @Transactional
    public void batchRemove(Set<Integer> ids) {
        List<StreamPush> streamPushList = streamPushMapper.selectInSet(ids);
        if (streamPushList.isEmpty()) {
            return;
        }
        Set<Integer> channelIds = new HashSet<>();
        streamPushList.stream().forEach(streamPush -> {
            if (streamPush.getGbDeviceId() != null) {
                channelIds.add(streamPush.getGbId());
            }
        });
        streamPushMapper.batchDel(streamPushList);
        gbChannelService.delete(channelIds);
    }
}
