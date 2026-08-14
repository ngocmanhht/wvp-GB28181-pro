package com.genersoft.iot.vmp.service;

import com.alibaba.fastjson2.JSONArray;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.service.bean.CloudRecordItem;
import com.genersoft.iot.vmp.service.bean.DownloadFileInfo;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.vmanager.cloudRecord.bean.CloudRecordUrl;
import com.github.pagehelper.PageInfo;

import java.util.List;
import java.util.Set;

/**
 * Cloud video management
 * @author lin
 */
public interface ICloudRecordService {

    /**
     * Paginate back to cloud recording list
     */
    PageInfo<CloudRecordItem> getList(int page, int count, String query,  String app, String stream, String startTime, String endTime, List<MediaServer> mediaServerItems, String callId, Boolean ascOrder);

    /**
     * Get all dates
     */
    List<String> getDateList(String app, String stream, int year, int month, List<MediaServer> mediaServerItems);

    /**
     * Add merge task
     */
    String addTask(String app, String stream, MediaServer mediaServerItem, String startTime,
                   String endTime, String callId, String remoteHost, boolean filterMediaServer);


    /**
     * Query merge task list
     */
    JSONArray queryTask(String app, String stream, String callId, String taskId, String mediaServerId, Boolean isEnd, String scheme);

    /**
     * Collect videos. Favorite videos will not be deleted when they expire.
     */
    int changeCollect(boolean result, String app, String stream, String mediaServerId, String startTime, String endTime, String callId);

    /**
     * Add specified video collection
     */
    int changeCollectById(Integer recordId, boolean result);

    /**
     * Get playback address
     */
    DownloadFileInfo getPlayUrlPath(Integer recordId);

    List<CloudRecordItem> getAllList(String query, String app, String stream, String startTime, String endTime, List<MediaServer> mediaServerItems, String callId, List<Integer> ids);

    /**
     * Load video files to form a video stream
     */
    void loadMP4FileForDate(String app, String stream, String date, ErrorCallback<StreamInfo> callback);

    void seekRecord(String mediaServerId,String app, String stream, Double seek, String schema);

    void setRecordSpeed(String mediaServerId, String app, String stream, Integer speed, String schema);

    void deleteFileByIds(Set<Integer> ids);

    void loadMP4File(String app, String stream, int cloudRecordId, ErrorCallback<StreamInfo> callback);

    List<CloudRecordUrl> getUrlListByIds(List<Integer> ids);

    List<CloudRecordUrl> getUrlList(String app, String stream, String callId);
}
