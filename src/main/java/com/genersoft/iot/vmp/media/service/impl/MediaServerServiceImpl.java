package com.genersoft.iot.vmp.media.service.impl;

import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.MediaConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpInfo;
import com.genersoft.iot.vmp.gb28181.bean.TalkRtpInfo;
import com.genersoft.iot.vmp.gb28181.service.IInviteStreamService;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.bean.RecordInfo;
import com.genersoft.iot.vmp.media.event.media.MediaArrivalEvent;
import com.genersoft.iot.vmp.media.event.media.MediaDepartureEvent;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaServerDeleteEvent;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaServerOfflineEvent;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaServerOnlineEvent;
import com.genersoft.iot.vmp.media.service.IMediaNodeServerService;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.media.zlm.dto.StreamAuthorityInfo;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OriginType;
import com.genersoft.iot.vmp.service.bean.DownloadFileInfo;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.MediaServerLoad;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.dao.MediaServerMapper;
import com.genersoft.iot.vmp.streamProxy.bean.StreamProxy;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Media server node management
 */
@Slf4j
@Service
public class MediaServerServiceImpl implements IMediaServerService {

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private MediaServerMapper mediaServerMapper;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IInviteStreamService inviteStreamService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private Map<String, IMediaNodeServerService> nodeServerServiceMap;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private MediaConfig mediaConfig;


    /**
     * Processing of incoming streams
     */
    @Async
    @org.springframework.context.event.EventListener
    public void onApplicationEvent(MediaArrivalEvent event) {
        if ("rtsp".equals(event.getSchema())) {
            log.info("Flow Changes: Register app->{}, stream->{}", event.getApp(), event.getStream());
            addCount(event.getMediaServer().getId());
            String type = OriginType.values()[event.getMediaInfo().getOriginType()].getType();
            redisCatchStorage.addStream(event.getMediaServer(), type, event.getApp(), event.getStream(), event.getMediaInfo());
        }
    }

    /**
     * Stream departure processing
     */
    @Async
    @EventListener
    public void onApplicationEvent(MediaDepartureEvent event) {
        if ("rtsp".equals(event.getSchema())) {
            log.info("Flow change: logout, app->{}, stream->{}", event.getApp(), event.getStream());
            removeCount(event.getMediaServer().getId());
            MediaInfo mediaInfo = redisCatchStorage.getStreamInfo(
                    event.getApp(), event.getStream(), event.getMediaServer().getId());
            if (mediaInfo == null) {
                return;
            }
            String type = OriginType.values()[mediaInfo.getOriginType()].getType();
            redisCatchStorage.removeStream(mediaInfo.getMediaServer().getId(), type, event.getApp(), event.getStream());
        }
    }

    /**
     * Streaming media node online
     */
    @Async
    @EventListener
    @Transactional
    public void onApplicationEvent(MediaServerOnlineEvent event) {
        // Check if there are any unprocessed RTP streams

    }

    /**
     * Streaming media node offline
     */
    @Async
    @EventListener
    @Transactional
    public void onApplicationEvent(MediaServerOfflineEvent event) {

    }


    /**
     * initialization
     */
    @Override
    public void updateVmServer(List<MediaServer> mediaServerList) {
        log.info("[media service node] Cache initialization ");
        for (MediaServer mediaServer : mediaServerList) {
            if (ObjectUtils.isEmpty(mediaServer.getId())) {
                continue;
            }
            // Query redis to see if this existsmediaServer
            String key = VideoManagerConstants.MEDIA_SERVER_PREFIX + userSetting.getServerId();
            Boolean hasKey = redisTemplate.hasKey(key);
            if (hasKey != null && !hasKey) {
                redisTemplate.opsForHash().put(key, mediaServer.getId(), mediaServer);
            }
        }
    }

    @Override
    public int createRTPServer(MediaServer mediaServer, String app,  String streamId, long ssrc, Integer port, boolean onlyAuto, boolean disableAudio, boolean reUsePort, Integer tcpMode) {
        int rtpServerPort;
        if (mediaServer.isRtpEnable()) {
            IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
            if (mediaNodeServerService == null) {
                log.info("[openRTPServer] failed, type of mediaServer： {}，The corresponding implementation class was not found", mediaServer.getType());
                return 0;
            }
            rtpServerPort = mediaNodeServerService.createRTPServer(mediaServer, app, streamId, ssrc, port, onlyAuto, disableAudio, reUsePort, tcpMode);
        } else {
            rtpServerPort = mediaServer.getRtpProxyPort();
        }
        return rtpServerPort;
    }

    @Override
    public List<String> listRtpServer(MediaServer mediaServer) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[openRTPServer] failed, type of mediaServer： {}，The corresponding implementation class was not found", mediaServer.getType());
            return new ArrayList<>();
        }
        return mediaNodeServerService.listRtpServer(mediaServer);
    }

    @Override
    public void closeRTPServer(MediaServer mediaServer, String app, String streamId) {
        if (mediaServer == null) {
            return;
        }
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[closeRTPServer] failed, type of mediaServer： {}，The corresponding implementation class was not found", mediaServer.getType());
            return;
        }
        mediaNodeServerService.closeRtpServer(mediaServer, app, streamId, null);
    }

    @Override
    public void closeRTPServer(MediaServer mediaServer, String app, String streamId, CommonCallback<Boolean> callback) {
        if (mediaServer == null) {
            callback.run(false);
            return;
        }
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[closeRTPServer] failed, type of mediaServer： {}，The corresponding implementation class was not found", mediaServer.getType());
            return;
        }
        mediaNodeServerService.closeRtpServer(mediaServer, app, streamId, callback);
    }

    @Override
    public Boolean updateRtpServerSSRC(MediaServer mediaServer, String app, String streamId, String ssrc) {
        if (mediaServer == null) {
            return false;
        }
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[updateRtpServerSSRC] failed, type of mediaServer： {}，The corresponding implementation class was not found", mediaServer.getType());
            return false;
        }
        return mediaNodeServerService.updateRtpServerSSRC(mediaServer, app, streamId, ssrc);
    }

    /**
     * The media service node resets its push information after restarting, and sends a TODO stop command to the device being used.
     */
    @Override
    public void clearRTPServer(MediaServer mediaServer) {
    }

    @Override
    public void update(MediaServer mediaServer) {
        if (mediaServerMapper.queryOne(mediaServer.getId()) != null) {
            mediaServerMapper.update(mediaServer);
        }else {
            mediaServerMapper.add(mediaServer);
        }

        String key = VideoManagerConstants.MEDIA_SERVER_PREFIX + userSetting.getServerId();
        redisTemplate.opsForHash().put(key, mediaServer.getId(), mediaServer);
        if (mediaServer.isStatus()) {
            resetOnlineServerItem(mediaServer);
        }
    }


    @Override
    public List<MediaServer> getAllOnlineList() {
        List<MediaServer> result = new ArrayList<>();
        String key = VideoManagerConstants.MEDIA_SERVER_PREFIX + userSetting.getServerId();
        String onlineKey = VideoManagerConstants.ONLINE_MEDIA_SERVERS_PREFIX + userSetting.getServerId();
        List<Object> values = redisTemplate.opsForHash().values(key);
        for (Object value : values) {
            if (Objects.isNull(value)) {
                continue;
            }
            MediaServer mediaServer = (MediaServer) value;
            // check status
            Double aDouble = redisTemplate.opsForZSet().score(onlineKey, mediaServer.getId());
            if (aDouble != null) {
                mediaServer.setStatus(true);
            }
            result.add(mediaServer);
        }
        result.sort((serverItem1, serverItem2)->{
            int sortResult = 0;
            LocalDateTime localDateTime1 = LocalDateTime.parse(serverItem1.getCreateTime(), DateUtil.formatter);
            LocalDateTime localDateTime2 = LocalDateTime.parse(serverItem2.getCreateTime(), DateUtil.formatter);

            sortResult = localDateTime1.compareTo(localDateTime2);
            return  sortResult;
        });
        return result;
    }

    @Override
    public List<MediaServer> getAll() {
        List<MediaServer> mediaServerList = mediaServerMapper.queryAll(userSetting.getServerId());
        if (mediaServerList.isEmpty()) {
            return new ArrayList<>();
        }
        for (MediaServer mediaServer : mediaServerList) {
            MediaServer mediaServerInRedis = getOne(mediaServer.getId());
            if (mediaServerInRedis != null) {
                mediaServer.setStatus(mediaServerInRedis.isStatus());
            }
        }
        return mediaServerList;
    }


    @Override
    public List<MediaServer> getAllFromDatabaseWithOutDefault() {
        return mediaServerMapper.queryAllWithOutDefault(userSetting.getServerId());
    }

    @Override
    public List<MediaServer> getAllOnline() {
        String key = VideoManagerConstants.ONLINE_MEDIA_SERVERS_PREFIX + userSetting.getServerId();
        Set<Object> mediaServerIdSet = redisTemplate.opsForZSet().reverseRange(key, 0, -1);

        List<MediaServer> result = new ArrayList<>();
        if (mediaServerIdSet != null && !mediaServerIdSet.isEmpty()) {
            for (Object mediaServerId : mediaServerIdSet) {
                String mediaServerIdStr = (String) mediaServerId;
                String serverKey = VideoManagerConstants.MEDIA_SERVER_PREFIX + userSetting.getServerId();
                result.add((MediaServer) redisTemplate.opsForHash().get(serverKey, mediaServerIdStr));
            }
        }
        Collections.reverse(result);
        return result;
    }

    /**
     * Get a single media service node server
     * @param mediaServerId serviceid
     * @return mediaServer
     */
    @Override
    public MediaServer getOne(String mediaServerId) {
        if (mediaServerId == null) {
            return null;
        }
        String key = VideoManagerConstants.MEDIA_SERVER_PREFIX + userSetting.getServerId();
        MediaServer mediaServer = (MediaServer) redisTemplate.opsForHash().get(key, mediaServerId);
        if (mediaServer == null) {
            // Try to get from database
            mediaServer = mediaServerMapper.queryOneWithServerId(mediaServerId, userSetting.getServerId());
            if (mediaServer != null) {
                redisTemplate.opsForHash().put(key, mediaServer.getId(), mediaServer);
            }
        }
        return mediaServer;
    }

    /**
     * Obtain node information in the cluster without distinguishing between belongingwvp
     */
    @Override
    public MediaServer getOneFromCluster(String mediaServerId) {
        if (mediaServerId == null) {
            return null;
        }
        String scanKey = String.format("%s*", VideoManagerConstants.MEDIA_SERVER_PREFIX);
        List<Object> values = RedisUtil.scan(redisTemplate, scanKey);
        if (values.isEmpty()) {
            return null;
        }
        for (Object value : values) {
            MediaServer mediaServer = (MediaServer) redisTemplate.opsForHash().get((String) value, mediaServerId);
            if (mediaServer != null){
                return mediaServer;
            }
        }
        return null;
    }


    @Override
    public MediaServer getDefaultMediaServer() {
        return mediaServerMapper.queryDefault(userSetting.getServerId());
    }

    @Override
    public void clearMediaServerForOnline() {
        String key = VideoManagerConstants.ONLINE_MEDIA_SERVERS_PREFIX + userSetting.getServerId();
        redisTemplate.delete(key);
    }

    @Override
    public void add(MediaServer mediaServer) {
        mediaServer.setCreateTime(DateUtil.getNow());
        mediaServer.setUpdateTime(DateUtil.getNow());
        if (mediaServer.getHookAliveInterval() == null || mediaServer.getHookAliveInterval() == 0F) {
            mediaServer.setHookAliveInterval(10F);
        }
        if (mediaServer.getType() == null) {
            log.info("[Add media node] Failed, mediaServer type: empty");
            return;
        }
        if (mediaServerMapper.queryOne(mediaServer.getId()) != null) {
            log.info("[Add media node] Failure, the media service ID already exists, please modify the media server configuration, {}", mediaServer.getId());
            throw new ControllerException(ErrorCode.ERROR100.getCode(),"Save failed, media serviceID [ " + mediaServer.getId() + " ] Exists, please modify the media server configuration");
        }
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[Add media node] failed, type of mediaServer： {}，The corresponding implementation class was not found", mediaServer.getType());
            return;
        }

        mediaServerMapper.add(mediaServer);
        if (mediaServer.isStatus()) {
            mediaNodeServerService.online(mediaServer);
        }
    }

    @Override
    public void resetOnlineServerItem(MediaServer serverItem) {
        // Update cache
        String key = VideoManagerConstants.ONLINE_MEDIA_SERVERS_PREFIX + userSetting.getServerId();
        // Use the score of zset as the current concurrency, and the default value is set to0
        if (redisTemplate.opsForZSet().score(key, serverItem.getId()) == null) {  // If it does not exist, set the default value. If it exists, reset it.
            redisTemplate.opsForZSet().add(key, serverItem.getId(), 0L);
            // Query the number of service flows
            int count = getMediaList(serverItem);
            redisTemplate.opsForZSet().add(key, serverItem.getId(), count);
        }else {
            clearRTPServer(serverItem);
        }
    }

    private int getMediaList(MediaServer serverItem) {

        return 0;
    }


    @Override
    public void addCount(String mediaServerId) {
        if (mediaServerId == null) {
            return;
        }
        String key = VideoManagerConstants.ONLINE_MEDIA_SERVERS_PREFIX + userSetting.getServerId();
        redisTemplate.opsForZSet().incrementScore(key, mediaServerId, 1);

    }

    @Override
    public void removeCount(String mediaServerId) {
        String key = VideoManagerConstants.ONLINE_MEDIA_SERVERS_PREFIX + userSetting.getServerId();
        redisTemplate.opsForZSet().incrementScore(key, mediaServerId, - 1);
    }

    /**
     * Get the node with the lowest load
     * @return mediaServer
     */
    @Override
    public MediaServer getMediaServerForMinimumLoad(Boolean hasAssist) {
        String key = VideoManagerConstants.ONLINE_MEDIA_SERVERS_PREFIX + userSetting.getServerId();
        Long size = redisTemplate.opsForZSet().zCard(key);
        if (size  == null || size == 0) {
            log.info("There are no online nodes when getting the lowest loaded node");
            return null;
        }

        // Get the one with the lowest score and the lowest concurrency
        Set<Object> objects = redisTemplate.opsForZSet().range(key, 0, -1);
        ArrayList<Object> mediaServerObjectS = new ArrayList<>(objects);
        MediaServer mediaServer = null;
        if (hasAssist == null) {
            String mediaServerId = (String)mediaServerObjectS.get(0);
            mediaServer = getOne(mediaServerId);
        }else if (hasAssist) {
            for (Object mediaServerObject : mediaServerObjectS) {
                String mediaServerId = (String)mediaServerObject;
                MediaServer serverItem = getOne(mediaServerId);
                if (serverItem.getRecordAssistPort() > 0) {
                    mediaServer = serverItem;
                    break;
                }
            }
        }else if (!hasAssist) {
            for (Object mediaServerObject : mediaServerObjectS) {
                String mediaServerId = (String)mediaServerObject;
                MediaServer serverItem = getOne(mediaServerId);
                if (serverItem.getRecordAssistPort() == 0) {
                    mediaServer = serverItem;
                    break;
                }
            }
        }

        return mediaServer;
    }

    @Override
    public MediaServer checkMediaServer(String ip, int port, String secret, String type) {
        if (mediaServerMapper.queryOneByHostAndPort(ip, port, userSetting.getServerId()) != null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "This connection already exists");
        }

        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(type);
        if (mediaNodeServerService == null) {
            log.info("[closeRTPServer] failed, type of mediaServer： {}，The corresponding implementation class was not found", type);
            return null;
        }
        MediaServer mediaServer = mediaNodeServerService.checkMediaServer(ip, port, secret);
        if (mediaServer != null) {
            if (mediaServerMapper.queryOne(mediaServer.getId()) != null) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "media servicesID [" + mediaServer.getId() + " ] Exists, please modify the media server configuration");
            }
        }
        return mediaServer;
    }

    @Override
    public boolean checkMediaRecordServer(String ip, int port) {
        boolean result = false;
        OkHttpClient client = new OkHttpClient();
        String url = String.format("http://%s:%s/index/api/record",  ip, port);
        Request request = new Request.Builder()
                .get()
                .url(url)
                .build();
        try {
            Response response = client.newCall(request).execute();
            if (response != null) {
                result = true;
            }
        } catch (Exception e) {}

        return result;
    }

    @Override
    public void delete(MediaServer mediaServer) {
        mediaServerMapper.delOne(mediaServer.getId(), userSetting.getServerId());
        redisTemplate.opsForZSet().remove(VideoManagerConstants.ONLINE_MEDIA_SERVERS_PREFIX + userSetting.getServerId(), mediaServer.getId());
        String key = VideoManagerConstants.MEDIA_SERVER_PREFIX + userSetting.getServerId();
        redisTemplate.delete(key);
        // Send node removal notification
        MediaServerDeleteEvent event = new MediaServerDeleteEvent(this);
        event.setMediaServer(mediaServer);
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public MediaServer getOneFromDatabase(String mediaServerId) {
        return mediaServerMapper.queryOne(mediaServerId);
    }

    @Override
    public void syncCatchFromDatabase() {
        List<MediaServer> allInCatch = getAllOnlineList();
        List<MediaServer> allInDatabase = mediaServerMapper.queryAll(userSetting.getServerId());
        Map<String, MediaServer> mediaServerMap = new HashMap<>();

        for (MediaServer mediaServer : allInDatabase) {
            mediaServerMap.put(mediaServer.getId(), mediaServer);
        }
        for (MediaServer mediaServer : allInCatch) {
            // Clear data that does not exist in the data but is cached in redis
            if (!mediaServerMap.containsKey(mediaServer.getId())) {
                delete(mediaServer);
            }
        }
    }

    @Override
    public MediaServerLoad getLoad(MediaServer mediaServer) {
        MediaServerLoad result = new MediaServerLoad();
        result.setId(mediaServer.getId());
        result.setPush(redisCatchStorage.getPushStreamCount(mediaServer.getId()));
        result.setProxy(redisCatchStorage.getProxyStreamCount(mediaServer.getId()));

        result.setGbReceive(inviteStreamService.getStreamInfoCount(mediaServer.getId()));
        result.setGbSend(redisCatchStorage.getGbSendCount(mediaServer.getId()));
        return result;
    }

    @Override
    public List<MediaServer> getAllWithAssistPort() {
        return mediaServerMapper.queryAllWithAssistPort(userSetting.getServerId());
    }


    @Override
    public boolean stopSendRtp(MediaServer mediaServer, String app, String stream, String ssrc) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[stopSendRtp] failed, type of mediaServer： {}，The corresponding implementation class was not found", mediaServer.getType());
            return false;
        }
        return mediaNodeServerService.stopSendRtp(mediaServer, app, stream, ssrc);
    }

    @Override
    public boolean initStopSendRtp(MediaServer mediaInfo, String app, String stream, String ssrc) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaInfo.getType());
        if (mediaNodeServerService == null) {
            log.info("[stopSendRtp] failed, type of mediaServer： {}，The corresponding implementation class was not found", mediaInfo.getType());
            return false;
        }
        return mediaNodeServerService.initStopSendRtp(mediaInfo, app, stream, ssrc);
    }

    @Override
    public boolean deleteRecordDirectory(MediaServer mediaServer, String app, String stream, String date, String fileName) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[stopSendRtp] failed, type of mediaServer： {}，The corresponding implementation class was not found", mediaServer.getType());
            return false;
        }
        return mediaNodeServerService.deleteRecordDirectory(mediaServer, app, stream, date, fileName);
    }

    @Override
    public List<StreamInfo> getMediaList(MediaServer mediaServer, String app, String stream, String callId) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[getMediaList] failed, type of mediaServer： {}，The corresponding implementation class was not found", mediaServer.getType());
            return new ArrayList<>();
        }
        return mediaNodeServerService.getMediaList(mediaServer, app, stream, callId);
    }

    @Override
    public Boolean connectRtpServer(MediaServer mediaServer, String address, int port, String app, String stream) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[connectRtpServer] failed, type of mediaServer： {}，The corresponding implementation class was not found", mediaServer.getType());
            return false;
        }
        return mediaNodeServerService.connectRtpServer(mediaServer, address, port, app, stream);
    }

    @Override
    public byte[] getSnap(MediaServer mediaServer, String app, String stream, int timeoutSec, int expireSec, String path, String fileName) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[getSnap] failed, type of mediaServer： {}，The corresponding implementation class was not found", mediaServer.getType());
            return null;
        }
        return mediaNodeServerService.getSnap(mediaServer, app, stream, timeoutSec, expireSec, path, fileName);
    }

    @Override
    public MediaInfo getMediaInfo(MediaServer mediaServer, String app, String stream) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[getMediaInfo] failed, type of mediaServer： {}，The corresponding implementation class was not found", mediaServer.getType());
            return null;
        }
        return mediaNodeServerService.getMediaInfo(mediaServer, app, stream);
    }

    @Override
    public Boolean pauseRtpCheck(MediaServer mediaServer, String streamKey) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[pauseRtpCheck] failed, type of mediaServer： {}，The corresponding implementation class was not found", mediaServer.getType());
            return false;
        }
        return mediaNodeServerService.pauseRtpCheck(mediaServer, streamKey);
    }

    @Override
    public boolean resumeRtpCheck(MediaServer mediaServer, String streamKey) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[pauseRtpCheck] failed, type of mediaServer： {}，The corresponding implementation class was not found", mediaServer.getType());
            return false;
        }
        return mediaNodeServerService.resumeRtpCheck(mediaServer, streamKey);
    }

    @Override
    public String getFfmpegCmd(MediaServer mediaServer, String cmdKey) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[getFfmpegCmd] failed, type of mediaServer： {}，The corresponding implementation class was not found", mediaServer.getType());
            return null;
        }
        return mediaNodeServerService.getFfmpegCmd(mediaServer, cmdKey);
    }

    @Override
    public void closeStreams(MediaServer mediaServer, String app, String stream) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[closeStreams] failed, type of mediaServer： {}，The corresponding implementation class was not found", mediaServer.getType());
            return;
        }
        mediaNodeServerService.closeStreams(mediaServer, app, stream);
    }

    @Override
    public WVPResult<String> addStreamProxy(MediaServer mediaServer, String app, String stream, String url,
                                            boolean enableAudio, boolean enableMp4, String rtpType, Integer timeout) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[addStreamProxy] failed, type of mediaServer： {}，The corresponding implementation class was not found", mediaServer.getType());
            return WVPResult.fail(ErrorCode.ERROR400);
        }
        return mediaNodeServerService.addStreamProxy(mediaServer, app, stream, url, enableAudio, enableMp4, rtpType, timeout);
    }

    @Override
    public Boolean delFFmpegSource(MediaServer mediaServer, String streamKey) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[delFFmpegSource] failed, type of mediaServer： {}，The corresponding implementation class was not found", mediaServer.getType());
            return false;
        }
        return mediaNodeServerService.delFFmpegSource(mediaServer, streamKey);
    }

    @Override
    public Boolean delStreamProxy(MediaServer mediaServerItem, String streamKey) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServerItem.getType());
        if (mediaNodeServerService == null) {
            log.info("[delStreamProxy] failed, type of mediaServer： {}，The corresponding implementation class was not found", mediaServerItem.getType());
            return false;
        }
        return mediaNodeServerService.delStreamProxy(mediaServerItem, streamKey);
    }

    @Override
    public Map<String, String> getFFmpegCMDs(MediaServer mediaServer) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[getFFmpegCMDs] failed, type of mediaServer： {}，The corresponding implementation class was not found", mediaServer.getType());
            return new HashMap<>();
        }
        return mediaNodeServerService.getFFmpegCMDs(mediaServer);
    }

    @Override
    public StreamInfo getStreamInfoByAppAndStream(MediaServer mediaServerItem, String app, String stream, MediaInfo mediaInfo, String callId) {
        return getStreamInfoByAppAndStream(mediaServerItem, app, stream, mediaInfo, null, callId, true);
    }

    @Override
    public StreamInfo getStreamInfoByAppAndStreamWithCheck(String app, String stream, String mediaServerId, String addr, boolean authority) {
        if (mediaServerId == null) {
            mediaServerId = mediaConfig.getId();
        }
        MediaServer mediaInfo = getOne(mediaServerId);
        if (mediaInfo == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "The media node used was not found");
        }
        String calld = null;
        StreamAuthorityInfo streamAuthorityInfo = redisCatchStorage.getStreamAuthorityInfo(app, stream);
        if (streamAuthorityInfo != null) {
            calld = streamAuthorityInfo.getCallId();
        }
        List<StreamInfo> streamInfoList = getMediaList(mediaInfo, app, stream, calld);
        if (streamInfoList == null || streamInfoList.isEmpty()) {
            return null;
        }else {
            StreamInfo streamInfo = streamInfoList.get(0);
            if (addr != null && !addr.isEmpty()) {
                streamInfo.changeStreamIp(addr);
            }
            return streamInfo;
        }
    }



    @Override
    public StreamInfo getStreamInfoByAppAndStreamWithCheck(String app, String stream, String mediaServerId, boolean authority) {
        return getStreamInfoByAppAndStreamWithCheck(app, stream, mediaServerId, null, authority);
    }

    @Override
    public StreamInfo getStreamInfoByAppAndStream(MediaServer mediaServer, String app, String stream, MediaInfo mediaInfo, String addr, String callId, boolean isPlay) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[getStreamInfoByAppAndStream] failed, type of mediaServer： {}，The corresponding implementation class was not found", mediaServer.getType());
            return null;
        }
        return mediaNodeServerService.getStreamInfoByAppAndStream(mediaServer, app, stream, mediaInfo, addr, callId, isPlay);
    }

    @Override
    public Boolean isStreamReady(MediaServer mediaServer, String app, String streamId) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[isStreamReady] failed, type of mediaServer： {}，The corresponding implementation class was not found", mediaServer.getType());
            return false;
        }
        MediaInfo mediaInfo = mediaNodeServerService.getMediaInfo(mediaServer, app, streamId);
        return mediaInfo != null;
    }

    @Override
    public Integer startSendRtpPassive(MediaServer mediaServer, SendRtpInfo sendRtpItem, Integer timeout) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[startSendRtpPassive] failed, type of mediaServer： {}，The corresponding implementation class was not found", mediaServer.getType());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "The implementation class corresponding to mediaServer was not found");
        }
        return mediaNodeServerService.startSendRtpPassive(mediaServer, sendRtpItem, timeout);
    }

    @Override
    public Integer startSendRtpTalk(MediaServer mediaServer, TalkRtpInfo talkRtpInfo, Integer timeout) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[startSendRtpPassive] failed, type of mediaServer： {}，The corresponding implementation class was not found", mediaServer.getType());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "The implementation class corresponding to mediaServer was not found");
        }
        return mediaNodeServerService.startSendRtpTalk(mediaServer, talkRtpInfo, timeout);
    }


    @Override
    public void startSendRtp(MediaServer mediaServer, SendRtpInfo sendRtpItem) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[startSendRtpStream] failed, type of mediaServer： {}，The corresponding implementation class was not found", mediaServer.getType());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "The implementation class corresponding to mediaServer was not found");
        }
        sendRtpItem.setRtcp(true);

        log.info("[Start streaming] {}/{}, target={}:{}，SSRC={}, RTCP={}", sendRtpItem.getApp(), sendRtpItem.getStream(),
                sendRtpItem.getIp(), sendRtpItem.getPort(), sendRtpItem.getSsrc(), sendRtpItem.isRtcp());
        mediaNodeServerService.startSendRtpStream(mediaServer, sendRtpItem);
    }



    @Override
    public MediaServer getMediaServerByAppAndStream(String app, String stream) {
        List<MediaServer> mediaServerList = getAll();
        for (MediaServer mediaServer : mediaServerList) {
            MediaInfo mediaInfo = getMediaInfo(mediaServer, app, stream);
            if (mediaInfo != null) {
                return mediaServer;
            }
        }
        return null;
    }

    @Override
    public StreamInfo getMediaByAppAndStream(String app, String stream) {

        List<MediaServer> mediaServerList = getAll();
        for (MediaServer mediaServer : mediaServerList) {
            MediaInfo mediaInfo = getMediaInfo(mediaServer, app, stream);
            if (mediaInfo != null) {
                return getStreamInfoByAppAndStream(mediaServer, app, stream, mediaInfo, mediaInfo.getCallId());
            }
        }
        return null;
    }

    @Override
    public Long updateDownloadProcess(MediaServer mediaServer, String app, String stream) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[updateDownloadProcess] failed, type of mediaServer： {}，The corresponding implementation class was not found", mediaServer.getType());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "The implementation class corresponding to mediaServer was not found");
        }
        return mediaNodeServerService.updateDownloadProcess(mediaServer, app, stream);
    }

    @Override
    public String startProxy(MediaServer mediaServer, StreamProxy streamProxy) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[startProxy] failed, type of mediaServer： {}，The corresponding implementation class was not found", mediaServer.getType());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "The implementation class corresponding to mediaServer was not found");
        }
        return mediaNodeServerService.startProxy(mediaServer, streamProxy);
    }

    @Override
    public void stopProxy(MediaServer mediaServer, String streamKey, String type) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[stopProxy] failed, type of mediaServer： {}，The corresponding implementation class was not found", mediaServer.getType());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "The implementation class corresponding to mediaServer was not found");
        }
        mediaNodeServerService.stopProxy(mediaServer, streamKey, type);
    }

    @Override
    public void loadMP4FileForDate(MediaServer mediaServer, String app, String stream, String date, String dateDir, ErrorCallback<StreamInfo> callback) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[loadMP4FileForDate] failed, type of mediaServer： {}，The corresponding implementation class was not found", mediaServer.getType());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "The implementation class corresponding to mediaServer was not found");
        }
        mediaNodeServerService.loadMP4FileForDate(mediaServer, app, stream, date, dateDir, callback);

    }

    @Override
    public void loadMP4File(MediaServer mediaServer, String app, String stream, String filePath, String fileName, ErrorCallback<StreamInfo> callback) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[loadMP4File] failed, type of mediaServer： {}，The corresponding implementation class was not found", mediaServer.getType());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "The implementation class corresponding to mediaServer was not found");
        }
        mediaNodeServerService.loadMP4File(mediaServer, app, stream, filePath, fileName, callback);
    }

    @Override
    public void deleteDefault() {
        mediaServerMapper.deleteDefault(userSetting.getServerId());
    }

    @Override
    public void seekRecordStamp(MediaServer mediaServer, String app, String stream, Double stamp, String schema) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[seekRecordStamp] failed, type of mediaServer： {}，The corresponding implementation class was not found", mediaServer.getType());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "The implementation class corresponding to mediaServer was not found");
        }
        mediaNodeServerService.seekRecordStamp(mediaServer, app, stream, stamp, schema);
    }

    @Override
    public void setRecordSpeed(MediaServer mediaServer, String app, String stream, Integer speed, String schema) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[setRecordSpeed] failed, type of mediaServer： {}，The corresponding implementation class was not found", mediaServer.getType());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "The implementation class corresponding to mediaServer was not found");
        }
        mediaNodeServerService.setRecordSpeed(mediaServer, app, stream, speed, schema);
    }

    @Override
    public DownloadFileInfo getDownloadFilePath(MediaServer mediaServer, RecordInfo recordInfo) {
        IMediaNodeServerService mediaNodeServerService = nodeServerServiceMap.get(mediaServer.getType());
        if (mediaNodeServerService == null) {
            log.info("[setRecordSpeed] failed, type of mediaServer： {}，The corresponding implementation class was not found", mediaServer.getType());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "The implementation class corresponding to mediaServer was not found");
        }
        return mediaNodeServerService.getDownloadFilePath(mediaServer, recordInfo);
    }
}
