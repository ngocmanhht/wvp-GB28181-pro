package com.genersoft.iot.vmp.web.custom;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.media.zlm.dto.StreamAuthorityInfo;
import com.genersoft.iot.vmp.service.ICloudRecordService;
import com.genersoft.iot.vmp.service.bean.CloudRecordItem;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.streamPush.bean.StreamPush;
import com.genersoft.iot.vmp.streamPush.service.IStreamPushPlayService;
import com.genersoft.iot.vmp.streamPush.service.IStreamPushService;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.utils.HttpUtils;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.genersoft.iot.vmp.vmanager.cloudRecord.bean.CloudRecordUrl;
import com.genersoft.iot.vmp.web.custom.bean.*;
import com.genersoft.iot.vmp.web.custom.service.CameraChannelService;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Tag(name  = "Third-party interface")
@Slf4j
@RestController
@RequestMapping(value = "/api/sy")
@ConditionalOnProperty(value = "sy.enable", havingValue = "true")
@Hidden
public class CameraChannelController {

    @Autowired
    private CameraChannelService channelService;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private ICloudRecordService cloudRecordService;

    @Autowired
    private IStreamPushPlayService streamPushPlayService;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private IStreamPushService streamPushService;

    @Value("${sy.ptz-control-time-interval}")
    private int ptzControlTimeInterval = 300;

    @GetMapping(value = "/camera/list")
    @ResponseBody
    @Operation(summary = "Query the camera list, only query the cameras under the current virtual organization", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "Current page")
    @Parameter(name = "count", description = "Number of queries per page")
    @Parameter(name = "groupAlias", description = "Group alias")
    @Parameter(name = "geoCoordSys", description = "Coordinate system type：WGS84,GCJ02、BD09")
    @Parameter(name = "status", description = "Camera status")
    public PageInfo<CameraChannel> queryList(@RequestParam(required = false, value = "page", defaultValue = "1" )Integer page,
                                        @RequestParam(required = false, value = "count", defaultValue = "100")Integer count,
                                        String groupAlias,
                                        @RequestParam(required = false) String geoCoordSys,
                                        @RequestParam(required = false) Boolean status){


        return channelService.queryList(page, count, groupAlias, status, geoCoordSys);
    }

    @GetMapping(value = "/camera/list-with-child")
    @ResponseBody
    @Operation(summary = "Query the camera list, query the current virtual organization and all sub-nodes", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "Current page")
    @Parameter(name = "count", description = "Number of queries per page")
    @Parameter(name = "query", description = "Query content")
    @Parameter(name = "sortName", description = "Sort field name")
    @Parameter(name = "order", description = "Sorting method (true: ascending order or false: descending order ）")
    @Parameter(name = "groupAlias", description = "Group alias")
    @Parameter(name = "geoCoordSys", description = "Coordinate system type：WGS84,GCJ02、BD09")
    @Parameter(name = "status", description = "Camera status")
    public PageInfo<CameraChannel> queryListWithChild(@RequestParam(required = false, value = "page", defaultValue = "1" )Integer page,

                                        @RequestParam(required = false, value = "count", defaultValue = "100")Integer count,
                                        @RequestParam(required = false) String query,
                                        @RequestParam(required = false) String sortName,
                                        @RequestParam(required = false) Boolean order,
                                        @RequestParam(required = false) String groupAlias,
                                        @RequestParam(required = false) String geoCoordSys,
                                        @RequestParam(required = false) Boolean status){
        if (ObjectUtils.isEmpty(query)) {
            query = null;
        }
        if (ObjectUtils.isEmpty(sortName)) {
            sortName = null;
        }
        if (ObjectUtils.isEmpty(order)) {
            order = null;
        }
        if (ObjectUtils.isEmpty(groupAlias)) {
            groupAlias = null;
        }

        return channelService.queryListWithChild(page, count, query, sortName, order, groupAlias, status, geoCoordSys);
    }

    @GetMapping(value = "/camera/cont-with-child")
    @ResponseBody
    @Operation(summary = "Query the total number and online number of cameras in the list", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "groupAlias", description = "Group alias")
    public List<CameraCount> queryCountWithChild(String groupAlias){
        return channelService.queryCountWithChild(groupAlias);
    }

    @GetMapping(value = "/camera/one")
    @ResponseBody
    @Operation(summary = "Query single camera information", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "Channel number")
    @Parameter(name = "deviceCode", description = "National standard number of camera equipment. This parameter does not need to be set for non-national standard cameras.")
    @Parameter(name = "geoCoordSys", description = "Coordinate system type：WGS84,GCJ02、BD09")
    public CameraChannel getOne(String deviceId, @RequestParam(required = false) String deviceCode,
                                  @RequestParam(required = false) String geoCoordSys) {
        return channelService.queryOne(deviceId, deviceCode, geoCoordSys);
    }

    @GetMapping(value = "/camera/update")
    @ResponseBody
    @Operation(summary = "Update camera information", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "Channel number")
    @Parameter(name = "deviceCode", description = "National standard number of camera equipment. This parameter does not need to be set for non-national standard cameras.")
    @Parameter(name = "name", description = "Channel name")
    @Parameter(name = "longitude", description = "longitude")
    @Parameter(name = "latitude", description = "Latitude")
    @Parameter(name = "geoCoordSys", description = "Coordinate system type：WGS84,GCJ02、BD09")
    public void updateCamera(String deviceId,
                                      @RequestParam(required = false) String deviceCode,
                                      @RequestParam(required = false) String name,
                                      @RequestParam(required = false) Double longitude,
                                      @RequestParam(required = false) Double latitude,
                                      @RequestParam(required = false) String geoCoordSys) {
        channelService.updateCamera(deviceId, deviceCode, name, longitude, latitude, geoCoordSys);
    }

    @PostMapping(value = "/camera/list/ids")
    @ResponseBody
    @Operation(summary = "Query multiple camera information based on number", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public List<CameraChannel> queryListByDeviceIds(@RequestBody IdsQueryParam param) {
        return channelService.queryListByDeviceIds(param.getDeviceIds(), param.getGeoCoordSys());
    }

    @GetMapping(value = "/camera/list/box")
    @ResponseBody
    @Operation(summary = "Query camera based on rectangle", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "minLongitude", description = "minimum longitude")
    @Parameter(name = "maxLongitude", description = "maximum longitude")
    @Parameter(name = "minLatitude", description = "minimum latitude")
    @Parameter(name = "maxLatitude", description = "maximum latitude")
    @Parameter(name = "level", description = "map level")
    @Parameter(name = "groupAlias", description = "Group alias")
    @Parameter(name = "geoCoordSys", description = "Coordinate system type：WGS84,GCJ02、BD09")
    public List<CameraChannel> queryListInBox(Double minLongitude, Double maxLongitude,
                                              Double minLatitude, Double maxLatitude,
                                              @RequestParam(required = false) Integer level,
                                              String groupAlias,
                                              @RequestParam(required = false) String geoCoordSys) {
        return channelService.queryListInBox(minLongitude, maxLongitude, minLatitude, maxLatitude, level, groupAlias, geoCoordSys);
    }

    @PostMapping(value = "/camera/list/polygon")
    @ResponseBody
    @Operation(summary = "Query camera based on polygon", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public List<CameraChannel> queryListInPolygon(@RequestBody PolygonQueryParam param) {
        return channelService.queryListInPolygon(param.getPosition(), param.getGroupAlias(), param.getLevel(), param.getGeoCoordSys());
    }

    @GetMapping(value = "/camera/list/circle")
    @ResponseBody
    @Operation(summary = "Query cameras based on circular range", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "centerLongitude", description = "Longitude of circle center")
    @Parameter(name = "centerLatitude", description = "Circle center latitude")
    @Parameter(name = "radius", description = "The radius of the query range, in meters")
    @Parameter(name = "level", description = "map level")
    @Parameter(name = "groupAlias", description = "Group alias")
    @Parameter(name = "geoCoordSys", description = "Coordinate system type：WGS84,GCJ02、BD09")
    public List<CameraChannel> queryListInCircle(Double centerLongitude, Double centerLatitude, Double radius, String groupAlias,
                                                 @RequestParam(required = false) String geoCoordSys, @RequestParam(required = false) Integer level) {
        return channelService.queryListInCircle(centerLongitude, centerLatitude, radius, level, groupAlias, geoCoordSys);
    }

    @GetMapping(value = "/camera/list/address")
    @ResponseBody
    @Operation(summary = "Obtain cameras based on installation address and surveillance location", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "address", description = "Installation address")
    @Parameter(name = "directionType", description = "Monitoring position", required = false)
    @Parameter(name = "geoCoordSys", description = "Coordinate system type：WGS84,GCJ02、BD09")
    public List<CameraChannel> queryListByAddressAndDirectionType(String address, @RequestParam(required = false) Integer directionType, @RequestParam(required = false) String geoCoordSys) {
        return channelService.queryListByAddressAndDirectionType(address, directionType, geoCoordSys);
    }

    @GetMapping(value = "/camera/control/play")
    @ResponseBody
    @Operation(summary = "Play camera", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "Channel number")
    @Parameter(name = "deviceCode", description = "National standard number of camera equipment. This parameter does not need to be set for non-national standard cameras.")
    public DeferredResult<WVPResult<CameraStreamContent>> play(HttpServletRequest request, String deviceId, @RequestParam(required = false) String deviceCode) {

        log.info("[SY-Play camera] APIcall，deviceId：{} ，deviceCode：{} ",deviceId, deviceCode);
        DeferredResult<WVPResult<CameraStreamContent>> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());

        ErrorCallback<CameraStreamInfo> callback = (code, msg, cameraStreamInfo) -> {
            if (code == InviteErrorCode.SUCCESS.getCode()) {
                StreamInfo streamInfo = cameraStreamInfo.getStreamInfo();
                CommonGBChannel channel = cameraStreamInfo.getChannel();
                WVPResult<CameraStreamContent> wvpResult = WVPResult.success();
                if (cameraStreamInfo.getStreamInfo() != null) {
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
                    if (!ObjectUtils.isEmpty(streamInfo.getMediaServer().getTranscodeSuffix())
                            && !"null".equalsIgnoreCase(streamInfo.getMediaServer().getTranscodeSuffix())) {
                        streamInfo.setStream(streamInfo.getStream() + "_" + streamInfo.getMediaServer().getTranscodeSuffix());
                    }
                    CameraStreamContent cameraStreamContent = new CameraStreamContent(streamInfo);
                    cameraStreamContent.setName(channel.getGbName());
                    if (channel.getGbPtzType() != null) {
                        cameraStreamContent.setControlType(
                                (channel.getGbPtzType() == 1 || channel.getGbPtzType() == 4 || channel.getGbPtzType() == 5) ? 1 : 0);
                    }else {
                        cameraStreamContent.setControlType(0);
                    }

                    wvpResult.setData(cameraStreamContent);
                }else {
                    wvpResult.setCode(code);
                    wvpResult.setMsg(msg);
                }
                result.setResult(wvpResult);
            }else {
                result.setResult(WVPResult.fail(code, msg));
            }
        };
        channelService.play(deviceId, deviceCode, callback);
        return result;
    }

    @GetMapping(value = "/camera/control/stop")
    @ResponseBody
    @Operation(summary = "Stop playing camera", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "Channel number")
    @Parameter(name = "deviceCode", description = "National standard number of camera equipment. This parameter does not need to be set for non-national standard cameras.")
    public void stopPlay(String deviceId, @RequestParam(required = false) String deviceCode) {
        log.info("[SY-Stop playing camera] APIcall，deviceId：{} ，deviceCode：{} ",deviceId, deviceCode);
        channelService.stopPlay(deviceId, deviceCode);
    }

    @Operation(summary = "PTZ control", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "Channel number")
    @Parameter(name = "deviceCode", description = "National standard number of camera equipment. This parameter does not need to be set for non-national standard cameras.")
    @Parameter(name = "command", description = "control instructions, allowable values: left, right, up, down, upleft, upright, downleft, downright, zoomin, zoomout, stop", required = true)
    @Parameter(name = "speed", description = "speed(0-100)", required = true)
    @GetMapping("/camera/control/ptz")
    public DeferredResult<WVPResult<String>> ptz(String deviceId, @RequestParam(required = false) String deviceCode, String command, Integer speed){

        log.info("[SY-PTZ control] APIcall，deviceId：{} ，deviceCode：{} ，command：{} ，speed：{} ",deviceId, deviceCode, command, speed);

        DeferredResult<WVPResult<String>> result = new DeferredResult<>();

        result.onTimeout(()->{
            WVPResult<String> wvpResult = WVPResult.fail(ErrorCode.ERROR100.getCode(), "Request timeout");
            result.setResult(wvpResult);
        });

        channelService.ptz(deviceId, deviceCode, command, speed, (code, msg, data) -> {
            WVPResult<String> wvpResult = new WVPResult<>();
            wvpResult.setCode(code);
            wvpResult.setMsg(msg);
            wvpResult.setData(data);
            result.setResult(wvpResult);
        });
        // Automatic sending stops after setting time interval
        if (!command.equalsIgnoreCase("stop")) {
            dynamicTask.startDelay(UUID.randomUUID().toString(), () -> {
                channelService.ptz(deviceId, deviceCode, "stop", speed, (code, msg, data) -> {});
            }, ptzControlTimeInterval);
        }
        return result;
    }

    @GetMapping(value = "/camera/list-for-mobile")
    @ResponseBody
    @Operation(summary = "Query the list of mobile device cameras", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "Current page")
    @Parameter(name = "count", description = "Number of queries per page")
    @Parameter(name = "topGroupAlias", description = "Group alias")
    public PageInfo<CameraChannel> queryListForMobile(@RequestParam(required = false, value = "page", defaultValue = "1" )Integer page,
                                                      @RequestParam(required = false, value = "count", defaultValue = "100")Integer count,
                                                      @RequestParam(required = false) String topGroupAlias){

        return channelService.queryListForMobile(page, count, topGroupAlias);
    }


    @Operation(summary = "Get the push playback address", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "app", description = "Application name", required = true)
    @Parameter(name = "stream", description = "flowid", required = true)
    @Parameter(name = "callId", description = "Custom authentication carried when pushing the streamID", required = true)
    @GetMapping(value = "/push/play")
    @ResponseBody
    public DeferredResult<WVPResult<StreamContent>> getStreamInfoByAppAndStream(HttpServletRequest request,
                                                                                String app,
                                                                                String stream,
                                                                                String callId){
        StreamPush streamPush = streamPushService.getPush(app, stream);
        Assert.notNull(streamPush, "Address does not exist");

        // Permission verification
        StreamAuthorityInfo streamAuthorityInfo = redisCatchStorage.getStreamAuthorityInfo(app, stream);
        if (streamAuthorityInfo == null
                || streamAuthorityInfo.getCallId() == null
                || !streamAuthorityInfo.getCallId().equals(callId)) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Playback address authentication failed");
        }

        DeferredResult<WVPResult<StreamContent>> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());
        result.onTimeout(()->{
            WVPResult<StreamContent> fail = WVPResult.fail(ErrorCode.ERROR100.getCode(), "Timeout waiting for push stream");
            result.setResult(fail);
        });

        streamPushPlayService.start(streamPush.getId(), (code, msg, streamInfo) -> {
            if (code == 0 && streamInfo != null) {
                streamInfo=streamInfo.clone();//deep copy
                String host;
                try {
                    URL url=new URL(request.getRequestURL().toString());
                    host=url.getHost();
                } catch (MalformedURLException e) {
                    host=request.getLocalAddr();
                }
                streamInfo.changeStreamIp(host);
                WVPResult<StreamContent> success = WVPResult.success(new StreamContent(streamInfo));
                result.setResult(success);
            }
        }, null, null);
        return result;
    }

    @Operation(summary = "Get the push playback address (without checking）", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "app", description = "Application name", required = true)
    @Parameter(name = "stream", description = "flowid", required = true)
    @Parameter(name = "callId", description = "Custom authentication carried when pushing the streamID", required = true)
    @GetMapping(value = "/push/play-without-check")
    @ResponseBody
    public StreamContent getStreamInfoByAppAndStreamWithoutCheck(HttpServletRequest request,
                                                                                String app,
                                                                                String stream,
                                                                                String callId){

        MediaServer mediaServer = mediaServerService.getDefaultMediaServer();
        Assert.notNull(mediaServer, "Streaming server does not exist");
        StreamInfo streamInfo = mediaServerService.getStreamInfoByAppAndStream(mediaServer, app, stream, null, callId);
        streamInfo=streamInfo.clone();//deep copy
        String host;
        try {
            URL url=new URL(request.getRequestURL().toString());
            host=url.getHost();
        } catch (MalformedURLException e) {
            host=request.getLocalAddr();
        }
        streamInfo.changeStreamIp(host);
        return new StreamContent(streamInfo);
    }

    @ResponseBody
    @GetMapping("/record/collect/add")
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
    @GetMapping("/record/collect/delete")
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

    /************************* The following interfaces are only suitable for situations where wvp and zlm are deployed on the same server, and wvp has only one zlm node. ***************************************/

    /**
     * Download the compressed package of the specified video file
     * @param app Application name
     * @param stream flowID
     * @param callId The unique identifier of each recording. If left blank, all streaming media will be queried.
     */
    @ResponseBody
    @GetMapping("/record/zip")
    public void downloadZipFile(HttpServletResponse response,
                                @RequestParam(required = false) String app,
                                @RequestParam(required = false) String stream,
                                @RequestParam(required = false) String callId

    ) {
        log.info("[Download the compressed package of the specified video file] Query app->{}, stream->{}, callId->{}", app, stream, callId);

        if (app != null && ObjectUtils.isEmpty(app.trim())) {
            app = null;
        }
        if (stream != null && ObjectUtils.isEmpty(stream.trim())) {
            stream = null;
        }
        if (callId != null && ObjectUtils.isEmpty(callId.trim())) {
            callId = null;
        }
        // Set response headers
        response.setContentType("application/zip");
        response.setCharacterEncoding("UTF-8");
        if (stream != null && callId != null) {
            response.addHeader("Content-Disposition", "attachment;filename=" + stream + "_" + callId + ".zip");
        }
        List<CloudRecordUrl> cloudRecordItemList = cloudRecordService.getUrlList(app, stream, callId);
        if (ObjectUtils.isEmpty(cloudRecordItemList)) {
            log.warn("[Download the compressed package of the specified video file] Video file not found，app->{}, stream->{}, callId->{}", app, stream, callId);
            return;
        }

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
                    zos.closeEntry();
                } catch (Exception e) {
                    log.error("[Download the compressed package of the specified video file] Processing file failed: {}, Error: {}", recordUrl.getFileName(), e.getMessage());
                    // Continue to the next file
                }
            }
        } catch (IOException e) {
            log.error("[Download the compressed package of the specified video file] Failed to create compressed package, query app->{}, stream->{}, callId->{}", app, stream, callId, e);
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
    @GetMapping("/record/list-url")
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

    @GetMapping(value = "/forceClose")
    @ResponseBody
    @Operation(summary = "Forcibly stop streaming", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public void stop(String app, String stream){
        streamPushPlayService.stop(app, stream);
    }

    @GetMapping(value = "/camera/meeting/list")
    @ResponseBody
    @Operation(summary = "Query conference equipment", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "topGroupAlias", description = "Group alias")
    public List<CameraChannel> queryMeetingChannelList(String topGroupAlias){
        return channelService.queryMeetingChannelList(topGroupAlias);
    }

    @GetMapping(value = "/test")
    @ResponseBody
    public SYMember test(String device){
        return channelService.getMember(device);
    }


}
