package com.genersoft.iot.vmp.streamPush.service;

import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.service.bean.StreamPushItemFromRedis;
import com.genersoft.iot.vmp.streamPush.bean.StreamPush;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import com.github.pagehelper.PageInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author lin
 */
public interface IStreamPushService {

    /**
     * get
     */
    PageInfo<StreamPush> getPushList(Integer page, Integer count, String query, Boolean pushing, String mediaServerId);

    List<StreamPush> getPushList(String mediaSererId);

    StreamPush getPush(String app, String streamId);

    boolean stop(StreamPush streamPush);

    /**
     * Stop pushing all the way
     * @param app Application name
     * @param stream flowID
     */
    boolean stopByAppAndStream(String app, String stream);

    /**
     * New node added
     */
    void zlmServerOnline(MediaServer mediaServer);

    /**
     * Node offline
     */
    void zlmServerOffline(MediaServer mediaServer);

    /**
     * Add in batches
     */
    void batchAdd(List<StreamPush> streamPushExcelDtoList);


    /**
     * All offline
     */
    void allOfflineForRedisMsg();

    /**
     * Push streaming offline
     */
    void offlineforRedisMsg(List<StreamPushItemFromRedis> offlineStreams);

    /**
     * Push streaming online
     */
    void onlineForRedisMsg(List<StreamPushItemFromRedis> onlineStreams);

    /**
     * Increase push flow
     */
    boolean add(StreamPush stream);

    boolean update(StreamPush stream);

    /**
     * Get allapp+Streanm Used to determine whether the push list is added or modified
     * @return
     */
    List<String> getAllAppAndStream();

    /**
     * Get statistics
     * @return
     */
    ResourceBaseInfo getOverview();

    Map<String, StreamPush> getAllAppAndStreamMap();

    Map<String, StreamPush> getAllGBId();

    void deleteByAppAndStream(String app, String stream);

    void updatePushStatus(StreamPush streamPush);

    void batchUpdateForRedisMsg(List<StreamPush> streamPushItemForUpdate);

    int delete(int id);

    void batchRemove(Set<Integer> ids);

}
