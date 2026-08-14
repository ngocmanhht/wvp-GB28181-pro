package com.genersoft.iot.vmp.vmanager.cloudRecord;

import com.alibaba.fastjson2.JSONArray;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.ICloudRecordService;
import com.genersoft.iot.vmp.service.bean.CloudRecordItem;
import com.genersoft.iot.vmp.service.bean.DownloadFileInfo;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.streamPush.bean.BatchRemoveParam;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.utils.HttpUtils;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.genersoft.iot.vmp.vmanager.cloudRecord.bean.CloudRecordUrl;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@SuppressWarnings("rawtypes")
@Tag(name = "Cloud recording interface")
@Slf4j
@RestController
@RequestMapping("/api/cloud/record")
public class CloudRecordController {


    @Autowired
    private ICloudRecordService cloudRecordService;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private UserSetting userSetting;


    @ResponseBody
    @GetMapping("/date/list")
    @Operation(summary = "Query the date on which cloud recording exists", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "app", description = "Application name", required = true)
    @Parameter(name = "stream", description = "flowID", required = true)
    @Parameter(name = "year", description = "Year, leave it blank to query the current year", required = false)
    @Parameter(name = "month", description = "Month, leave it blank to query the current month", required = false)
    @Parameter(name = "mediaServerId", description = "Streaming media ID, leave it blank to query all", required = false)
    public List<String> openRtpServer(
            @RequestParam(required = true) String app,
            @RequestParam(required = true) String stream,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String mediaServerId

    ) {
        log.info("[Cloud recording] Query the date on which cloud recording exists app->{}, stream->{}, mediaServerId->{}, year->{}, month->{}", app, stream, mediaServerId, year, month);
        Calendar calendar = Calendar.getInstance();
        if (ObjectUtils.isEmpty(year)) {
            year = calendar.get(Calendar.YEAR);
        }
        if (ObjectUtils.isEmpty(month)) {
            month = calendar.get(Calendar.MONTH) + 1;
        }
        List<MediaServer> mediaServers;
        if (!ObjectUtils.isEmpty(mediaServerId)) {
            mediaServers = new ArrayList<>();
            MediaServer mediaServer = mediaServerService.getOne(mediaServerId);
            if (mediaServer == null) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "Streamer not found: " + mediaServerId);
            }
            mediaServers.add(mediaServer);
        } else {
            mediaServers = mediaServerService.getAllOnlineList();
        }
        if (mediaServers.isEmpty()) {
            return new ArrayList<>();
        }

        return cloudRecordService.getDateList(app, stream, year, month, mediaServers);
    }

    @ResponseBody
    @GetMapping("/list")
    @Operation(summary = "Query cloud recordings by page", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "query", description = "Search content", required = false)
    @Parameter(name = "app", description = "Application name", required = false)
    @Parameter(name = "stream", description = "flowID", required = false)
    @Parameter(name = "page", description = "Current page", required = true)
    @Parameter(name = "count", description = "Number of queries per page", required = true)
    @Parameter(name = "startTime", description = "start time(yyyy-MM-dd HH:mm:ss)", required = false)
    @Parameter(name = "endTime", description = "end time(yyyy-MM-dd HH:mm:ss)", required = false)
    @Parameter(name = "mediaServerId", description = "Streaming media ID, leave it blank to query all streaming media", required = false)
    @Parameter(name = "callId", description = "The unique identifier of each recording. If left blank, all streaming media will be queried.", required = false)
    @Parameter(name = "ascOrder", description = "Whether to sort in ascending order, ascending order: true, descending order： false", required = false)
    public PageInfo<CloudRecordItem> openRtpServer(@RequestParam(required = false) String query,
                                                   @RequestParam(required = false) String app,
                                                   @RequestParam(required = false) String stream,
                                                   @RequestParam int page,
                                                   @RequestParam int count,
                                                   @RequestParam(required = false) String startTime,
                                                   @RequestParam(required = false) String endTime,
                                                   @RequestParam(required = false) String mediaServerId,
                                                   @RequestParam(required = false) String callId,
                                                   @RequestParam(required = false) Boolean ascOrder

    ) {
        log.info("[Cloud recording] Query app->{}, stream->{}, mediaServerId->{}, page->{}, count->{}, startTime->{}, endTime->{}, callId->{}", app, stream, mediaServerId, page, count, startTime, endTime, callId);

        List<MediaServer> mediaServers;
        if (!ObjectUtils.isEmpty(mediaServerId)) {
            mediaServers = new ArrayList<>();
            MediaServer mediaServer = mediaServerService.getOne(mediaServerId);
            if (mediaServer == null) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "Streamer not found: " + mediaServerId);
            }
            mediaServers.add(mediaServer);
        } else {
            mediaServers = null;
        }
        if (query != null && ObjectUtils.isEmpty(query.trim())) {
            query = null;
        }
        if (app != null && ObjectUtils.isEmpty(app.trim())) {
            app = null;
        }
        if (stream != null && ObjectUtils.isEmpty(stream.trim())) {
            stream = null;
        }
        if (startTime != null && ObjectUtils.isEmpty(startTime.trim())) {
            startTime = null;
        }
        if (endTime != null && ObjectUtils.isEmpty(endTime.trim())) {
            endTime = null;
        }
        if (callId != null && ObjectUtils.isEmpty(callId.trim())) {
            callId = null;
        }
        return cloudRecordService.getList(page, count, query, app, stream, startTime, endTime, mediaServers, callId, ascOrder);
    }

    @ResponseBody
    @GetMapping("/task/add")
    @Operation(summary = "Add merge task")
    @Parameter(name = "app", description = "Application name", required = false)
    @Parameter(name = "stream", description = "flowID", required = false)
    @Parameter(name = "mediaServerId", description = "streaming mediaID", required = false)
    @Parameter(name = "startTime", description = "AuthenticationID", required = false)
    @Parameter(name = "endTime", description = "AuthenticationID", required = false)
    @Parameter(name = "callId", description = "AuthenticationID", required = false)
    @Parameter(name = "remoteHost", description = "The remote address when returning the address", required = false)
    public String addTask(HttpServletRequest request, @RequestParam(required = false) String app, @RequestParam(required = false) String stream, @RequestParam(required = false) String mediaServerId, @RequestParam(required = false) String startTime, @RequestParam(required = false) String endTime, @RequestParam(required = false) String callId, @RequestParam(required = false) String remoteHost) {
        MediaServer mediaServer;
        if (mediaServerId == null) {
            mediaServer = mediaServerService.getDefaultMediaServer();
        } else {
            mediaServer = mediaServerService.getOne(mediaServerId);
        }
        if (mediaServer == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "No available stream found");
        } else {
            if (remoteHost == null) {
                remoteHost = request.getScheme() + "://" + mediaServer.getIp() + ":" + mediaServer.getRecordAssistPort();
            }
        }
        return cloudRecordService.addTask(app, stream, mediaServer, startTime, endTime, callId, remoteHost, mediaServerId != null);
    }

    @ResponseBody
    @GetMapping("/task/list")
    @Operation(summary = "Query merge task")
    @Parameter(name = "taskId", description = "TaskId", required = false)
    @Parameter(name = "mediaServerId", description = "streaming mediaID", required = false)
    @Parameter(name = "isEnd", description = "Is it over?", required = false)
    public JSONArray queryTaskList(HttpServletRequest request, @RequestParam(required = false) String app, @RequestParam(required = false) String stream, @RequestParam(required = false) String callId, @RequestParam(required = false) String taskId, @RequestParam(required = false) String mediaServerId, @RequestParam(required = false) Boolean isEnd) {
        if (ObjectUtils.isEmpty(mediaServerId)) {
            mediaServerId = null;
        }

        return cloudRecordService.queryTask(app, stream, callId, taskId, mediaServerId, isEnd, request.getScheme());
    }

    @ResponseBody
    @GetMapping("/collect/add")
    @Operation(summary = "Add to favorites")
    @Parameter(name = "app", description = "Application name", required = false)
    @Parameter(name = "stream", description = "flowID", required = false)
    @Parameter(name = "mediaServerId", description = "streaming mediaID", required = false)
    @Parameter(name = "startTime", description = "AuthenticationID", required = false)
    @Parameter(name = "endTime", description = "AuthenticationID", required = false)
    @Parameter(name = "callId", description = "AuthenticationID", required = false)
    @Parameter(name = "recordId", description = "The ID of the video record, used to accurately collect a video file", required = false)
    public int addCollect(@RequestParam(required = false) String app, @RequestParam(required = false) String stream, @RequestParam(required = false) String mediaServerId, @RequestParam(required = false) String startTime, @RequestParam(required = false) String endTime, @RequestParam(required = false) String callId, @RequestParam(required = false) Integer recordId) {
        log.info("[Cloud recording] Add to favorites，app={}，stream={},mediaServerId={},startTime={},endTime={},callId={},recordId={}", app, stream, mediaServerId, startTime, endTime, callId, recordId);
        if (recordId != null) {
            return cloudRecordService.changeCollectById(recordId, true);
        } else {
            return cloudRecordService.changeCollect(true, app, stream, mediaServerId, startTime, endTime, callId);
        }
    }

    @ResponseBody
    @GetMapping("/collect/delete")
    @Operation(summary = "Remove favorites")
    @Parameter(name = "app", description = "Application name", required = false)
    @Parameter(name = "stream", description = "flowID", required = false)
    @Parameter(name = "mediaServerId", description = "streaming mediaID", required = false)
    @Parameter(name = "startTime", description = "AuthenticationID", required = false)
    @Parameter(name = "endTime", description = "AuthenticationID", required = false)
    @Parameter(name = "callId", description = "AuthenticationID", required = false)
    @Parameter(name = "recordId", description = "The ID of the video record, used to accurately remove a video file from the collection", required = false)
    public int deleteCollect(@RequestParam(required = false) String app, @RequestParam(required = false) String stream, @RequestParam(required = false) String mediaServerId, @RequestParam(required = false) String startTime, @RequestParam(required = false) String endTime, @RequestParam(required = false) String callId, @RequestParam(required = false) Integer recordId) {
        log.info("[Cloud recording] Remove favorites，app={}，stream={},mediaServerId={},startTime={},endTime={},callId={},recordId={}", app, stream, mediaServerId, startTime, endTime, callId, recordId);
        if (recordId != null) {
            return cloudRecordService.changeCollectById(recordId, false);
        } else {
            return cloudRecordService.changeCollect(false, app, stream, mediaServerId, startTime, endTime, callId);
        }
    }

    @ResponseBody
    @GetMapping("/play/path")
    @Operation(summary = "Get playback address")
    @Parameter(name = "recordId", description = "video recordedID", required = true)
    public DownloadFileInfo getPlayUrlPath(@RequestParam(required = true) Integer recordId) {
        return cloudRecordService.getPlayUrlPath(recordId);
    }

    @ResponseBody
    @GetMapping("/loadRecord")
    @Operation(summary = "Load the video file to form the playback address")
    @Parameter(name = "app", description = "Application name", required = true)
    @Parameter(name = "stream", description = "flowID", required = true)
    @Parameter(name = "cloudRecordId", description = "Cloud recordingID", required = true)
    public DeferredResult<WVPResult<StreamContent>> loadRecord(
            HttpServletRequest request,
            @RequestParam(required = true) String app,
            @RequestParam(required = true) String stream,
            @RequestParam(required = true) int cloudRecordId
            ) {
        DeferredResult<WVPResult<StreamContent>> result = new DeferredResult<>();

        result.onTimeout(()->{
            log.info("[Loading video file timed out] app={}, stream={}, cloudRecordId={}", app, stream, cloudRecordId);
            WVPResult<StreamContent> wvpResult = new WVPResult<>();
            wvpResult.setCode(ErrorCode.ERROR100.getCode());
            wvpResult.setMsg("Loading video file timed out");
            result.setResult(wvpResult);
        });

        ErrorCallback<StreamInfo> callback = (code, msg, streamInfo) -> {

            WVPResult<StreamContent> wvpResult = new WVPResult<>();
            if (code == InviteErrorCode.SUCCESS.getCode()) {
                wvpResult.setCode(ErrorCode.SUCCESS.getCode());
                wvpResult.setMsg(ErrorCode.SUCCESS.getMsg());

                if (streamInfo != null) {
                    if (userSetting.getUseSourceIpAsStreamIp()) {
                        streamInfo=streamInfo.clone();//deep copy
                        String host;
                        try {
                            URL url=new URL(request.getRequestURL().toString());
                            host=url.getHost();
                        } catch (MalformedURLException e) {
                            host=request.getLocalAddr();
                        }
                        streamInfo.changeStreamIp(host);
                    }
                    if (!org.springframework.util.ObjectUtils.isEmpty(streamInfo.getMediaServer().getTranscodeSuffix()) && !"null".equalsIgnoreCase(streamInfo.getMediaServer().getTranscodeSuffix())) {
                        streamInfo.setStream(streamInfo.getStream() + "_" + streamInfo.getMediaServer().getTranscodeSuffix());
                    }
                    wvpResult.setData(new StreamContent(streamInfo));
                }else {
                    wvpResult.setCode(code);
                    wvpResult.setMsg(msg);
                }
            }else {
                wvpResult.setCode(code);
                wvpResult.setMsg(msg);
            }
            result.setResult(wvpResult);
        };

        cloudRecordService.loadMP4File(app, stream, cloudRecordId, callback);
        return result;
    }

    @ResponseBody
    @GetMapping("/seek")
    @Operation(summary = "Play the positioning video to the specified position")
    @Parameter(name = "mediaServerId", description = "Node usedId", required = true)
    @Parameter(name = "app", description = "Application name", required = true)
    @Parameter(name = "stream", description = "flowID", required = true)
    @Parameter(name = "seek", description = "The time position to be positioned is calculated from the time when the recording starts.", required = true)
    public void seekRecord(
            @RequestParam(required = true) String mediaServerId,
            @RequestParam(required = true) String app,
            @RequestParam(required = true) String stream,
            @RequestParam(required = true) Double seek,
            @RequestParam(required = false) String schema
            ) {
        if (schema == null) {
            schema = "ts";
        }
        cloudRecordService.seekRecord(mediaServerId, app, stream, seek, schema);
    }

    @ResponseBody
    @GetMapping("/speed")
    @Operation(summary = "Set video playback speed")
    @Parameter(name = "mediaServerId", description = "Node usedId", required = true)
    @Parameter(name = "app", description = "Application name", required = true)
    @Parameter(name = "stream", description = "flowID", required = true)
    @Parameter(name = "speed", description = "The video speed to be set", required = true)
    public void setRecordSpeed(
            @RequestParam(required = true) String mediaServerId,
            @RequestParam(required = true) String app,
            @RequestParam(required = true) String stream,
            @RequestParam(required = true) Integer speed,
            @RequestParam(required = false) String schema
    ) {
        if (schema == null) {
            schema = "ts";
        }

        cloudRecordService.setRecordSpeed(mediaServerId, app, stream, speed, schema);
    }

    @ResponseBody
    @DeleteMapping("/delete")
    @Operation(summary = "Delete video files")
    @Parameter(name = "ids", description = "File ID collection", required = true)
    public void deleteFileByIds(@RequestBody BatchRemoveParam ids) {
        cloudRecordService.deleteFileByIds(ids.getIds());
    }

    @ResponseBody
    @GetMapping("/download/zip")
    public void downloadZipFileFromUrl(HttpServletResponse response, Integer[] ids) {
        String idsStr = StringUtils.arrayToCommaDelimitedString(ids);
        log.info("[Download the compressed package of the specified video file] Query ids->{}", idsStr);
        List<Integer> arrayList = new ArrayList<>(List.of(ids));
        List<CloudRecordUrl> cloudRecordItemList = cloudRecordService.getUrlListByIds(arrayList);
        if (ObjectUtils.isEmpty(cloudRecordItemList)) {
            log.warn("[Download the compressed package of the specified video file] Video file not found，ids->{}", idsStr);
            return;
        }

        // Set response headers
        response.setContentType("application/zip");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=record_" + System.currentTimeMillis() + ".zip");

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            for (CloudRecordUrl recordUrl : cloudRecordItemList) {
                try {
                    zos.putNextEntry(new ZipEntry(recordUrl.getFileName()));
                    boolean downloadSuccess = HttpUtils.downLoadFile(recordUrl.getDownloadUrl(), zos);
                    if (!downloadSuccess) {
                        log.warn("[Download the compressed package of the specified video file] Download file failed: {}", recordUrl.getDownloadUrl());
                        zos.closeEntry();
                        continue;
                    }

//                    try (FileInputStream fis = new FileInputStream(recordUrl.getFilePath())) {
//                        byte[] buf = new byte[8192]; // 8KB buffer to improve performance
//                        int len;
//                        while ((len = fis.read(buf)) != -1) {
//                            zos.write(buf, 0, len);
//                        }
//                    }

                    zos.closeEntry();
                } catch (Exception e) {
                    log.error("[Download the compressed package of the specified video file] Processing file failed: {}, Error: {}", recordUrl.getFileName(), e.getMessage());
                    // Continue to the next file
                }
            }
        } catch (IOException e) {
            log.error("[Download the compressed package of the specified video file] Failed to create compressed package, query ids->{}", ids, e);
        }
    }





    /************************* The following interfaces are only suitable for situations where wvp and zlm are deployed on the same server, and wvp has only one zlm node. ***************************************/

    /**
     * Download the compressed package of the specified video file
     * @param query Search content
     * @param app Application name
     * @param stream flowID
     * @param startTime start time(yyyy-MM-dd HH:mm:ss)
     * @param endTime end time(yyyy-MM-dd HH:mm:ss)
     * @param mediaServerId Streaming media ID, leave it blank to query all streaming media
     * @param callId The unique identifier of each recording. If left blank, all streaming media will be queried.
     * @param ids designatedId
     */
    @ResponseBody
    @GetMapping("/zip")
    public void downloadZipFile(HttpServletResponse response, @RequestParam(required = false) String query, @RequestParam(required = false) String app, @RequestParam(required = false) String stream, @RequestParam(required = false) String startTime, @RequestParam(required = false) String endTime, @RequestParam(required = false) String mediaServerId, @RequestParam(required = false) String callId, @RequestParam(required = false) List<Integer> ids

    ) {
        log.info("[Download the compressed package of the specified video file] Query app->{}, stream->{}, mediaServerId->{}, startTime->{}, endTime->{}, callId->{}", app, stream, mediaServerId, startTime, endTime, callId);

        List<MediaServer> mediaServers;
        if (!ObjectUtils.isEmpty(mediaServerId)) {
            mediaServers = new ArrayList<>();
            MediaServer mediaServer = mediaServerService.getOne(mediaServerId);
            if (mediaServer == null) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "Streamer not found: " + mediaServerId);
            }
            mediaServers.add(mediaServer);
        } else {
            mediaServers = mediaServerService.getAll();
        }
        if (mediaServers.isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "No streaming at the moment");
        }
        if (query != null && ObjectUtils.isEmpty(query.trim())) {
            query = null;
        }
        if (app != null && ObjectUtils.isEmpty(app.trim())) {
            app = null;
        }
        if (stream != null && ObjectUtils.isEmpty(stream.trim())) {
            stream = null;
        }
        if (startTime != null && ObjectUtils.isEmpty(startTime.trim())) {
            startTime = null;
        }
        if (endTime != null && ObjectUtils.isEmpty(endTime.trim())) {
            endTime = null;
        }
        if (callId != null && ObjectUtils.isEmpty(callId.trim())) {
            callId = null;
        }
        // Set response headers
        response.setContentType("application/zip");
        response.setCharacterEncoding("UTF-8");
        if (stream != null && callId != null) {
            response.setHeader("Content-Disposition", "attachment;filename=" + stream + "_" + callId + ".zip");
        } else {
            response.setHeader("Content-Disposition", "attachment;filename=cloud_record_" + System.currentTimeMillis() + ".zip");
        }
        List<CloudRecordItem> cloudRecordItemList = cloudRecordService.getAllList(query, app, stream, startTime, endTime, mediaServers, callId, ids);
        if (ObjectUtils.isEmpty(cloudRecordItemList)) {
            return;
        }
        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            for (CloudRecordItem cloudRecordItem : cloudRecordItemList) {
                try {
                    String fileName = DateUtil.timestampMsToUrlToyyyy_MM_dd_HH_mm_ss((long)cloudRecordItem.getStartTime()) + ".mp4";
                    zos.putNextEntry(new ZipEntry(fileName));

                    File file = new File(cloudRecordItem.getFilePath());
                    if (!file.exists() || file.isDirectory()) {
                        log.warn("[Download the compressed package of the specified video file] The file does not exist or is a directory: {}", cloudRecordItem.getFilePath());
                        zos.closeEntry();
                        continue;
                    }

                    try (FileInputStream fis = new FileInputStream(cloudRecordItem.getFilePath())) {
                        byte[] buf = new byte[8192]; // 8KB buffer to improve performance
                        int len;
                        while ((len = fis.read(buf)) != -1) {
                            zos.write(buf, 0, len);
                        }
                    }
                    zos.closeEntry();
                    log.debug("[Download the compressed package of the specified video file] File added successfully: {}", fileName);
                } catch (Exception e) {
                    log.error("[Download the compressed package of the specified video file] Processing file failed: {}, Error: {}", cloudRecordItem.getFilePath(), e.getMessage());
                    // Continue to the next file
                }
            }
        } catch (IOException e) {
            log.error("[Download the compressed package of the specified video file] Failure: Query app->{}, stream->{}, mediaServerId->{}, startTime->{}, endTime->{}, callId->{}", app, stream, mediaServerId, startTime, endTime, callId, e);
        }
    }

    /**
     *
     * @param query Search content
     * @param app Application name
     * @param stream flowID
     * @param startTime start time(yyyy-MM-dd HH:mm:ss)
     * @param endTime end time(yyyy-MM-dd HH:mm:ss)
     * @param mediaServerId Streaming media ID, leave it blank to query all streaming media
     * @param callId The unique identifier of each recording. If left blank, all streaming media will be queried.
     * @param remoteHost The remote address used when splicing playback addresses
     */
    @ResponseBody
    @GetMapping("/list-url")
    @Operation(summary = "Query cloud recordings by page", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "query", description = "Search content", required = false)
    @Parameter(name = "app", description = "Application name", required = false)
    @Parameter(name = "stream", description = "flowID", required = false)
    @Parameter(name = "page", description = "Current page", required = true)
    @Parameter(name = "count", description = "Number of queries per page", required = true)
    @Parameter(name = "startTime", description = "start time(yyyy-MM-dd HH:mm:ss)", required = false)
    @Parameter(name = "endTime", description = "end time(yyyy-MM-dd HH:mm:ss)", required = false)
    @Parameter(name = "mediaServerId", description = "Streaming media ID, leave it blank to query all streaming media", required = false)
    @Parameter(name = "callId", description = "The unique identifier of each recording. If left blank, all streaming media will be queried.", required = false)
    public PageInfo<CloudRecordUrl> getListWithUrl(HttpServletRequest request, @RequestParam(required = false) String query, @RequestParam(required = false) String app, @RequestParam(required = false) String stream, @RequestParam int page, @RequestParam int count, @RequestParam(required = false) String startTime, @RequestParam(required = false) String endTime, @RequestParam(required = false) String mediaServerId, @RequestParam(required = false) String callId, @RequestParam(required = false) String remoteHost

    ) {
        log.info("[Cloud recording] QueryURL app->{}, stream->{}, mediaServerId->{}, page->{}, count->{}, startTime->{}, endTime->{}, callId->{}", app, stream, mediaServerId, page, count, startTime, endTime, callId);

        List<MediaServer> mediaServers;
        if (!ObjectUtils.isEmpty(mediaServerId)) {
            mediaServers = new ArrayList<>();
            MediaServer mediaServer = mediaServerService.getOne(mediaServerId);
            if (mediaServer == null) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "Streamer not found: " + mediaServerId);
            }
            mediaServers.add(mediaServer);
        } else {
            mediaServers = null;
        }
        if (query != null && ObjectUtils.isEmpty(query.trim())) {
            query = null;
        }
        if (app != null && ObjectUtils.isEmpty(app.trim())) {
            app = null;
        }
        if (stream != null && ObjectUtils.isEmpty(stream.trim())) {
            stream = null;
        }
        if (startTime != null && ObjectUtils.isEmpty(startTime.trim())) {
            startTime = null;
        }
        if (endTime != null && ObjectUtils.isEmpty(endTime.trim())) {
            endTime = null;
        }
        if (callId != null && ObjectUtils.isEmpty(callId.trim())) {
            callId = null;
        }
        MediaServer mediaServer = mediaServerService.getDefaultMediaServer();
        if (mediaServer == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Streaming media node not found");
        }
        if (remoteHost == null) {
            remoteHost = request.getScheme() + "://" + request.getLocalAddr() + ":" + (request.getScheme().equals("https") ? mediaServer.getHttpSSlPort() : mediaServer.getHttpPort());
        }
        PageInfo<CloudRecordItem> cloudRecordItemPageInfo = cloudRecordService.getList(page, count, query, app, stream, startTime, endTime, mediaServers, callId, null);
        PageInfo<CloudRecordUrl> cloudRecordUrlPageInfo = new PageInfo<>();
        if (!ObjectUtils.isEmpty(cloudRecordItemPageInfo)) {
            cloudRecordUrlPageInfo.setPageNum(cloudRecordItemPageInfo.getPageNum());
            cloudRecordUrlPageInfo.setPageSize(cloudRecordItemPageInfo.getPageSize());
            cloudRecordUrlPageInfo.setSize(cloudRecordItemPageInfo.getSize());
            cloudRecordUrlPageInfo.setEndRow(cloudRecordItemPageInfo.getEndRow());
            cloudRecordUrlPageInfo.setStartRow(cloudRecordItemPageInfo.getStartRow());
            cloudRecordUrlPageInfo.setPages(cloudRecordItemPageInfo.getPages());
            cloudRecordUrlPageInfo.setPrePage(cloudRecordItemPageInfo.getPrePage());
            cloudRecordUrlPageInfo.setNextPage(cloudRecordItemPageInfo.getNextPage());
            cloudRecordUrlPageInfo.setIsFirstPage(cloudRecordItemPageInfo.isIsFirstPage());
            cloudRecordUrlPageInfo.setIsLastPage(cloudRecordItemPageInfo.isIsLastPage());
            cloudRecordUrlPageInfo.setHasPreviousPage(cloudRecordItemPageInfo.isHasPreviousPage());
            cloudRecordUrlPageInfo.setHasNextPage(cloudRecordItemPageInfo.isHasNextPage());
            cloudRecordUrlPageInfo.setNavigatePages(cloudRecordItemPageInfo.getNavigatePages());
            cloudRecordUrlPageInfo.setNavigateFirstPage(cloudRecordItemPageInfo.getNavigateFirstPage());
            cloudRecordUrlPageInfo.setNavigateLastPage(cloudRecordItemPageInfo.getNavigateLastPage());
            cloudRecordUrlPageInfo.setNavigatepageNums(cloudRecordItemPageInfo.getNavigatepageNums());
            cloudRecordUrlPageInfo.setTotal(cloudRecordItemPageInfo.getTotal());
            List<CloudRecordUrl> cloudRecordUrlList = new ArrayList<>(cloudRecordItemPageInfo.getList().size());
            List<CloudRecordItem> cloudRecordItemList = cloudRecordItemPageInfo.getList();
            for (CloudRecordItem cloudRecordItem : cloudRecordItemList) {
                CloudRecordUrl cloudRecordUrl = new CloudRecordUrl();
                cloudRecordUrl.setId(cloudRecordItem.getId());
                cloudRecordUrl.setDownloadUrl(remoteHost + "/index/api/downloadFile?file_path=" + cloudRecordItem.getFilePath() + "&save_name=" + cloudRecordItem.getStream() + "_" + cloudRecordItem.getCallId() + "_" + DateUtil.timestampMsToUrlToyyyy_MM_dd_HH_mm_ss((long)cloudRecordItem.getStartTime()));
                cloudRecordUrl.setPlayUrl(remoteHost + "/index/api/downloadFile?file_path=" + cloudRecordItem.getFilePath());
                cloudRecordUrlList.add(cloudRecordUrl);
            }
            cloudRecordUrlPageInfo.setList(cloudRecordUrlList);
        }
        return cloudRecordUrlPageInfo;
    }
}
