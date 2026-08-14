package com.genersoft.iot.vmp.service.bean;

import com.genersoft.iot.vmp.media.event.media.MediaRecordMp4Event;
import com.genersoft.iot.vmp.media.event.media.MediaRecordProcessEvent;
import com.genersoft.iot.vmp.utils.MediaServerUtils;
import lombok.Data;

import java.util.Map;

/**
 * Cloud recording data
 */
@Data
public class CloudRecordItem {
    /**
     * primary key
     */
    private int id;

    /**
     * Application name
     */
    private String app;

    /**
     * flow
     */
    private String stream;

    /**
     * soundID
     */
    private String callId;

    /**
     * start time
     */
    private long startTime;

    /**
     * end time
     */
    private long endTime;

    /**
     * ZLM Id
     */
    private String mediaServerId;

    /**
     * File name
     */
    private String fileName;

    /**
     * file path
     */
    private String filePath;

    /**
     * folder
     */
    private String folder;

    /**
     * Collection, the collected files will not be removed
     */
    private Boolean collect;

    /**
     * Keep, collected files will not be removed
     */
    private Boolean reserve;

    /**
     * file size
     */
    private long fileSize;

    /**
     * File duration
     */
    private double timeLen;

    /**
     * ServicesID
     */
    private String serverId;

    public static CloudRecordItem getInstance(MediaRecordMp4Event param) {
        CloudRecordItem cloudRecordItem = new CloudRecordItem();
        cloudRecordItem.setApp(param.getApp());
        cloudRecordItem.setStream(param.getStream());
        cloudRecordItem.setStartTime(param.getRecordInfo().getStartTime());
        cloudRecordItem.setFileName(param.getRecordInfo().getFileName());
        cloudRecordItem.setFolder(param.getRecordInfo().getFolder());
        cloudRecordItem.setFileSize(param.getRecordInfo().getFileSize());
        cloudRecordItem.setFilePath(param.getRecordInfo().getFilePath());
        cloudRecordItem.setMediaServerId(param.getMediaServer().getId());
        cloudRecordItem.setTimeLen(param.getRecordInfo().getTimeLen());
        cloudRecordItem.setEndTime((param.getRecordInfo().getStartTime() + (long)param.getRecordInfo().getTimeLen()));
        Map<String, String> paramsMap = MediaServerUtils.urlParamToMap(param.getRecordInfo().getParams());
        if (paramsMap.get("callId") != null) {
            cloudRecordItem.setCallId(paramsMap.get("callId"));
        }
        return cloudRecordItem;
    }

    public static CloudRecordItem getInstance(MediaRecordProcessEvent event) {
        CloudRecordItem cloudRecordItem = new CloudRecordItem();
        cloudRecordItem.setApp(event.getApp());
        cloudRecordItem.setStream(event.getStream());
        cloudRecordItem.setFileName(event.getFileName());
        cloudRecordItem.setMediaServerId(event.getMediaServer().getId());
        cloudRecordItem.setTimeLen(event.getCurrentFileDuration() * 1000);
        return cloudRecordItem;
    }

}
