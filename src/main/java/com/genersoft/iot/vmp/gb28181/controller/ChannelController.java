package com.genersoft.iot.vmp.gb28181.controller;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.controller.bean.*;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelPlayService;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.gb28181.utils.VectorTileCatch;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.AudioTalkResult;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.beans.PropertyDescriptor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;


@Tag(name  = "Global channel management")
@RestController
@Slf4j
@RequestMapping(value = "/api/common/channel")
public class ChannelController {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private IGbChannelService channelService;

    @Autowired
    private IGbChannelPlayService channelPlayService;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private VectorTileCatch vectorTileCatch;


    @Operation(summary = "Query channel information", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "id", description = "The channel's database is automatically incrementedId", required = true)
    @GetMapping(value = "/one")
    public CommonGBChannel getOne(int id){
        return channelService.getOne(id);
    }

    @Operation(summary = "Get a list of industry codes", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @GetMapping("/industry/list")
    public List<IndustryCodeType> getIndustryCodeList(){
        return channelService.getIndustryCodeList();
    }

    @Operation(summary = "Get encoding list", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @GetMapping("/type/list")
    public List<DeviceType> getDeviceTypeList(){
        return channelService.getDeviceTypeList();
    }

    @Operation(summary = "Get encoding list", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @GetMapping("/network/identification/list")
    public List<NetworkIdentificationType> getNetworkIdentificationTypeList(){
        return channelService.getNetworkIdentificationTypeList();
    }

    @Operation(summary = "update channel", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/update")
    public void update(@RequestBody CommonGBChannel channel){
        BeanWrapperImpl wrapper = new BeanWrapperImpl(channel);
        int count = 0;
        for (PropertyDescriptor pd : wrapper.getPropertyDescriptors()) {
            String name = pd.getName();
            if ("class".equals(name)) continue;
            if (pd.getReadMethod() == null) continue;
            Object val = wrapper.getPropertyValue(name);
            if (val != null) count++;
        }
        Assert.isTrue(count > 1, "No modifications were made");
        channelService.update(channel);
    }


    @Operation(summary = "Reset the national standard channel", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/reset")
    public void reset(@RequestBody ResetParam param){
        Assert.notNull(param.getId(), "Channel ID cannot be empty");
        Assert.notEmpty(param.getChanelFields(), "The field to be reset cannot be empty");
        channelService.reset(param.getId(), param.getChanelFields());
    }

    @Operation(summary = "Add channel", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/add")
    public CommonGBChannel add(@RequestBody CommonGBChannel channel){
        channelService.add(channel);
        return channel;
    }

    @Operation(summary = "Get channel list", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "Current page", required = true)
    @Parameter(name = "count", description = "Number of queries per page", required = true)
    @Parameter(name = "query", description = "Query content")
    @Parameter(name = "online", description = "Is online")
    @Parameter(name = "hasRecordPlan", description = "Whether a recording schedule has been set")
    @Parameter(name = "channelType", description = "Channel type, 0: national standard equipment, 1: push device, 2: pull agent")
    @Parameter(name = "civilCode", description = "Administrative division")
    @Parameter(name = "parentDeviceId", description = "Parent node encoding")
    @GetMapping("/list")
    public PageInfo<CommonGBChannel> queryList(int page, int count,
                                                          @RequestParam(required = false) String query,
                                                          @RequestParam(required = false) Boolean online,
                                                          @RequestParam(required = false) Boolean hasRecordPlan,
                                                          @RequestParam(required = false) Integer channelType,
                                                          @RequestParam(required = false) String civilCode,
                                                          @RequestParam(required = false) String parentDeviceId){
        if (ObjectUtils.isEmpty(query)){
            query = null;
        }
        if (ObjectUtils.isEmpty(civilCode)){
            civilCode = null;
        }
        if (ObjectUtils.isEmpty(parentDeviceId)){
            parentDeviceId = null;
        }
        return channelService.queryList(page, count, query, online, hasRecordPlan, channelType, civilCode, parentDeviceId);
    }

    @Operation(summary = "Get the list of associated administrative division channels", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "Current page", required = true)
    @Parameter(name = "count", description = "Number of queries per page", required = true)
    @Parameter(name = "query", description = "Query content")
    @Parameter(name = "online", description = "Is online")
    @Parameter(name = "channelType", description = "Channel type, 0: national standard equipment, 1: push device, 2: pull agent")
    @Parameter(name = "civilCode", description = "Administrative division")
    @GetMapping("/civilcode/list")
    public PageInfo<CommonGBChannel> queryListByCivilCode(int page, int count,
                                               @RequestParam(required = false) String query,
                                               @RequestParam(required = false) Boolean online,
                                               @RequestParam(required = false) Integer channelType,
                                               @RequestParam(required = false) String civilCode){
        if (ObjectUtils.isEmpty(query)){
            query = null;
        }
        return channelService.queryListByCivilCode(page, count, query, online, channelType, civilCode);
    }


    @Operation(summary = "A list of channels that exist in administrative divisions but cannot be mounted", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "Current page", required = true)
    @Parameter(name = "count", description = "Number of queries per page", required = true)
    @Parameter(name = "query", description = "Query content")
    @Parameter(name = "online", description = "Is online")
    @Parameter(name = "channelType", description = "Channel type, 0: national standard equipment, 1: push device, 2: pull agent")
    @GetMapping("/civilCode/unusual/list")
    public PageInfo<CommonGBChannel> queryListByCivilCodeForUnusual(int page, int count,
                                                          @RequestParam(required = false) String query,
                                                          @RequestParam(required = false) Boolean online,
                                                          @RequestParam(required = false) Integer channelType){
        if (ObjectUtils.isEmpty(query)){
            query = null;
        }
        return channelService.queryListByCivilCodeForUnusual(page, count, query, online, channelType);
    }


    @Operation(summary = "Channel list with parent node number but cannot be mounted", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "Current page", required = true)
    @Parameter(name = "count", description = "Number of queries per page", required = true)
    @Parameter(name = "query", description = "Query content")
    @Parameter(name = "online", description = "Is online")
    @Parameter(name = "channelType", description = "Channel type, 0: national standard equipment, 1: push device, 2: pull agent")
    @GetMapping("/parent/unusual/list")
    public PageInfo<CommonGBChannel> queryListByParentForUnusual(int page, int count,
                                                          @RequestParam(required = false) String query,
                                                          @RequestParam(required = false) Boolean online,
                                                          @RequestParam(required = false) Integer channelType){
        if (ObjectUtils.isEmpty(query)){
            query = null;
        }
        return channelService.queryListByParentForUnusual(page, count, query, online, channelType);
    }

    @Operation(summary = "Clear the list of channels that have administrative divisions but cannot be mounted", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "param", description = "Cleanup parameters, all is true to clean up all abnormal data. Otherwise, clean up according to the incoming device ID.", required = true)
    @PostMapping("/civilCode/unusual/clear")
    public void clearChannelCivilCode(@RequestBody ChannelToRegionParam param){
        channelService.clearChannelCivilCode(param.getAll(), param.getChannelIds());
    }

    @Operation(summary = "Clear the channel list where group nodes exist but cannot be mounted", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "param", description = "Cleanup parameters, all is true to clean up all abnormal data. Otherwise, clean up according to the incoming device ID.", required = true)
    @PostMapping("/parent/unusual/clear")
    public void clearChannelParent(@RequestBody ChannelToRegionParam param){
        channelService.clearChannelParent(param.getAll(), param.getChannelIds());
    }

    @Operation(summary = "Get the associated business group channel list", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "Current page", required = true)
    @Parameter(name = "count", description = "Number of queries per page", required = true)
    @Parameter(name = "query", description = "Query content")
    @Parameter(name = "online", description = "Is online")
    @Parameter(name = "channelType", description = "Channel type, 0: national standard equipment, 1: push device, 2: pull agent")
    @Parameter(name = "groupDeviceId", description = "Parent node under business groupID")
    @GetMapping("/parent/list")
    public PageInfo<CommonGBChannel> queryListByParentId(int page, int count,
                                               @RequestParam(required = false) String query,
                                               @RequestParam(required = false) Boolean online,
                                               @RequestParam(required = false) Integer channelType,
                                               @RequestParam(required = false) String groupDeviceId){
        if (ObjectUtils.isEmpty(query)){
            query = null;
        }
        return channelService.queryListByParentId(page, count, query, online, channelType, groupDeviceId);
    }

    @Operation(summary = "Channel setting administrative divisions", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/region/add")
    public void addChannelToRegion(@RequestBody ChannelToRegionParam param){
        Assert.notEmpty(param.getChannelIds(),"Channel ID cannot be empty");
        Assert.hasLength(param.getCivilCode(),"No administrative divisions added");
        channelService.addChannelToRegion(param.getCivilCode(), param.getChannelIds());
    }

    @Operation(summary = "Channel delete administrative division", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/region/delete")
    public void deleteChannelToRegion(@RequestBody ChannelToRegionParam param){
        Assert.isTrue(!param.getChannelIds().isEmpty() || !ObjectUtils.isEmpty(param.getCivilCode()),"Parameter exception");
        channelService.deleteChannelToRegion(param.getCivilCode(), param.getChannelIds());
    }

    @Operation(summary = "Channel setting administrative divisions-According to national standard equipment", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/region/device/add")
    public void addChannelToRegionByGbDevice(@RequestBody ChannelToRegionByGbDeviceParam param){
        Assert.notEmpty(param.getDeviceIds(),"Parameter exception");
        Assert.hasLength(param.getCivilCode(),"No administrative divisions added");
        channelService.addChannelToRegionByGbDevice(param.getCivilCode(), param.getDeviceIds());
    }

    @Operation(summary = "Channel delete administrative division-According to national standard equipment", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/region/device/delete")
    public void deleteChannelToRegionByGbDevice(@RequestBody ChannelToRegionByGbDeviceParam param){
        Assert.notEmpty(param.getDeviceIds(),"Parameter exception");
        channelService.deleteChannelToRegionByGbDevice(param.getDeviceIds());
    }

    @Operation(summary = "Channel setting business grouping", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/group/add")
    public void addChannelToGroup(@RequestBody ChannelToGroupParam param){
        Assert.notEmpty(param.getChannelIds(),"Channel ID cannot be empty");
        Assert.hasLength(param.getParentId(),"No upper-level group number added");
        Assert.hasLength(param.getBusinessGroup(),"No business group added");
        channelService.addChannelToGroup(param.getParentId(), param.getBusinessGroup(), param.getChannelIds());
    }

    @Operation(summary = "Channel delete business group", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/group/delete")
    public void deleteChannelToGroup(@RequestBody ChannelToGroupParam param){
        Assert.isTrue(!param.getChannelIds().isEmpty()
                || (!ObjectUtils.isEmpty(param.getParentId()) && !ObjectUtils.isEmpty(param.getBusinessGroup())),
                "Parameter exception");
        channelService.deleteChannelToGroup(param.getParentId(), param.getBusinessGroup(), param.getChannelIds());
    }

    @Operation(summary = "Channel setting business grouping-According to national standard equipment", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/group/device/add")
    public void addChannelToGroupByGbDevice(@RequestBody ChannelToGroupByGbDeviceParam param){
        Assert.notEmpty(param.getDeviceIds(),"Parameter exception");
        Assert.hasLength(param.getParentId(),"No upper-level group number added");
        Assert.hasLength(param.getBusinessGroup(),"No business group added");
        channelService.addChannelToGroupByGbDevice(param.getParentId(), param.getBusinessGroup(), param.getDeviceIds());
    }

    @Operation(summary = "Channel delete business group-According to national standard equipment", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/group/device/delete")
    public void deleteChannelToGroupByGbDevice(@RequestBody ChannelToGroupByGbDeviceParam param){
        Assert.notEmpty(param.getDeviceIds(),"Parameter exception");
        channelService.deleteChannelToGroupByGbDevice(param.getDeviceIds());
    }

    @Operation(summary = "Playback channel", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @GetMapping("/play")
    public DeferredResult<WVPResult<StreamContent>> play(HttpServletRequest request,  Integer channelId){
        Assert.notNull(channelId,"Parameter exception");
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "Channel does not exist");

        DeferredResult<WVPResult<StreamContent>> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());

        ErrorCallback<StreamInfo> callback = (code, msg, streamInfo) -> {
            if (code == InviteErrorCode.SUCCESS.getCode()) {
                WVPResult<StreamContent> wvpResult = WVPResult.success();
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
                    if (!ObjectUtils.isEmpty(streamInfo.getMediaServer().getTranscodeSuffix())
                            && !"null".equalsIgnoreCase(streamInfo.getMediaServer().getTranscodeSuffix())) {
                        streamInfo.setStream(streamInfo.getStream() + "_" + streamInfo.getMediaServer().getTranscodeSuffix());
                    }
                    wvpResult.setData(new StreamContent(streamInfo));
                }else {
                    wvpResult.setCode(code);
                    wvpResult.setMsg(msg);
                }
                result.setResult(wvpResult);
            }else {
                result.setResult(WVPResult.fail(code, msg));
            }
        };
        channelPlayService.play(channel, null, userSetting.getRecordSip(), callback);
        return result;
    }

    @Operation(summary = "Stop playing channel", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @GetMapping("/play/stop")
    public void stopPlay(Integer channelId){
        Assert.notNull(channelId,"Parameter exception");
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "Channel does not exist");
        channelPlayService.stopPlay(channel);
    }

    @Operation(summary = "Start intercom", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @GetMapping("/talk/start")
    public AudioTalkResult startTalk(Integer channelId){
        Assert.notNull(channelId,"Parameter exception");
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "Channel does not exist");
        return channelPlayService.startTalk(channel);
    }

    @Operation(summary = "Stop intercom", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @GetMapping("/talk/stop")
    public void stopTalk(Integer channelId){
        Assert.notNull(channelId,"Parameter exception");
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "Channel does not exist");
        channelPlayService.stopTalk(channel);
    }

    @Operation(summary = "Start shouting", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @GetMapping("/broadcast/start")
    public AudioTalkResult startBroadcast(Integer channelId){
        Assert.notNull(channelId,"Parameter exception");
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "Channel does not exist");
        return channelPlayService.startBroadcast(channel);
    }

    @Operation(summary = "stop shouting", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @GetMapping("/broadcast/stop")
    public void stopBroadcast(Integer channelId){
        Assert.notNull(channelId,"Parameter exception");
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "Channel does not exist");
        channelPlayService.stopBroadcast(channel);
    }

    @Operation(summary = "Video query", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "channelID", required = true)
    @Parameter(name = "startTime", description = "start time", required = true)
    @Parameter(name = "endTime", description = "end time", required = true)
    @GetMapping("/playback/query")
    public DeferredResult<WVPResult<List<CommonRecordInfo>>> queryRecord(Integer channelId, String startTime, String endTime){

        DeferredResult<WVPResult<List<CommonRecordInfo>>> result = new DeferredResult<>(Long.valueOf(userSetting.getRecordInfoTimeout()), TimeUnit.MILLISECONDS);
        if (!DateUtil.verification(startTime, DateUtil.formatter)){
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "startTimeThe format is" + DateUtil.PATTERN);
        }
        if (!DateUtil.verification(endTime, DateUtil.formatter)){
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "endTimeThe format is" + DateUtil.PATTERN);
        }
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "Channel does not exist");

        channelPlayService.queryRecord(channel, startTime, endTime, (code, msg, data) -> {
            WVPResult<List<CommonRecordInfo>> wvpResult = new WVPResult<>();
            wvpResult.setCode(code);
            wvpResult.setMsg(msg);
            wvpResult.setData(data);
            result.setResult(wvpResult);
        });
        result.onTimeout(()->{
            WVPResult<List<CommonRecordInfo>> wvpResult = new WVPResult<>();
            wvpResult.setCode(ErrorCode.ERROR100.getCode());
            wvpResult.setMsg("timeout");
            result.setResult(wvpResult);
        });
        return result;
    }

    @Operation(summary = "Video playback", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "channelID", required = true)
    @Parameter(name = "startTime", description = "start time", required = true)
    @Parameter(name = "endTime", description = "end time", required = true)
    @GetMapping("/playback")
    public DeferredResult<WVPResult<StreamContent>> playback(HttpServletRequest request, Integer channelId, String startTime, String endTime){
        Assert.notNull(channelId,"Parameter exception");
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "Channel does not exist");

        DeferredResult<WVPResult<StreamContent>> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());

        ErrorCallback<StreamInfo> callback = (code, msg, streamInfo) -> {
            if (code == InviteErrorCode.SUCCESS.getCode()) {
                WVPResult<StreamContent> wvpResult = WVPResult.success();
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
                    if (!ObjectUtils.isEmpty(streamInfo.getMediaServer().getTranscodeSuffix())
                            && !"null".equalsIgnoreCase(streamInfo.getMediaServer().getTranscodeSuffix())) {
                        streamInfo.setStream(streamInfo.getStream() + "_" + streamInfo.getMediaServer().getTranscodeSuffix());
                    }
                    wvpResult.setData(new StreamContent(streamInfo));
                }else {
                    wvpResult.setCode(code);
                    wvpResult.setMsg(msg);
                }

                result.setResult(wvpResult);
            }else {
                result.setResult(WVPResult.fail(code, msg));
            }
        };
        channelPlayService.playback(channel, DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(startTime),
                DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(endTime), callback);
        return result;
    }

    @Operation(summary = "Stop video playback", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "channelID", required = true)
    @Parameter(name = "stream", description = "flowID", required = true)
    @GetMapping("/playback/stop")
    public void stopPlayback(Integer channelId, String stream){
        Assert.notNull(channelId,"Parameter exception");
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "Channel does not exist");
        channelPlayService.stopPlayback(channel, stream);
    }

    @Operation(summary = "Pause video playback", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "channelID", required = true)
    @Parameter(name = "stream", description = "flowID", required = true)
    @GetMapping("/playback/pause")
    public void pausePlayback(Integer channelId, String stream){
        Assert.notNull(channelId,"Parameter exception");
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "Channel does not exist");
        channelPlayService.playbackPause(channel, stream);
    }

    @Operation(summary = "Resume video playback", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "channelID", required = true)
    @Parameter(name = "stream", description = "flowID", required = true)
    @GetMapping("/playback/resume")
    public void resumePlayback(Integer channelId, String stream){
        Assert.notNull(channelId,"Parameter exception");
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "Channel does not exist");
        channelPlayService.playbackResume(channel, stream);
    }

    @Operation(summary = "Drag video playback", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "channelID", required = true)
    @Parameter(name = "stream", description = "flowID", required = true)
    @Parameter(name = "seekTime", description = "time to play", required = true)
    @GetMapping("/playback/seek")
    public void seekPlayback(Integer channelId, String stream, Long seekTime){
        Assert.notNull(channelId,"Parameter exception");
        Assert.notNull(seekTime,"Parameter exception");
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "Channel does not exist");
        channelPlayService.playbackSeek(channel, stream, seekTime);
    }

    @Operation(summary = "Drag video playback", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "channelID", required = true)
    @Parameter(name = "stream", description = "flowID", required = true)
    @Parameter(name = "speed", description = "Double speed", required = true)
    @GetMapping("/playback/speed")
    public void seekPlayback(Integer channelId, String stream, Double speed){
        Assert.notNull(channelId,"Parameter exception");
        Assert.notNull(speed,"Parameter exception");
        CommonGBChannel channel = channelService.getOne(channelId);
        Assert.notNull(channel, "Channel does not exist");
        channelPlayService.playbackSpeed(channel, stream, speed);
    }

    @Operation(summary = "Get the channel list for the map", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "query", description = "Query content")
    @Parameter(name = "online", description = "Is online")
    @Parameter(name = "hasRecordPlan", description = "Whether a recording schedule has been set")
    @Parameter(name = "channelType", description = "Channel type, 0: national standard equipment, 1: push device, 2: pull agent")
    @Parameter(name = "geoCoordSys", description = "geographical coordinate system， WGS84/GCJ02")
    @GetMapping("/map/list")
    public List<CommonGBChannel> queryListForMap(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean online,
            @RequestParam(required = false) Boolean hasRecordPlan,
            @RequestParam(required = false) Integer channelType){
        if (ObjectUtils.isEmpty(query)){
            query = null;
        }
        return channelService.queryListForMap(query, online, hasRecordPlan, channelType);
    }

    @Operation(summary = "Remove thinning results for maps", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/map/reset-level")
    public void resetLevel(){
        channelService.resetLevel();
    }

    @Operation(summary = "Perform thinning", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/map/thin/draw")
    public String drawThin(@RequestBody DrawThinParam param){
        if(param == null || param.getZoomParam() == null || param.getZoomParam().isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR400);
        }
        return channelService.drawThin(param.getZoomParam(), param.getExtent(), param.getGeoCoordSys());
    }

    @Operation(summary = "Clear unsaved thinning results", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "id", description = "DiluteID", required = true)
    @GetMapping("/map/thin/clear")
    public void clearThin(String id){
        vectorTileCatch.remove(id);
    }

    @Operation(summary = "Saved dilution results", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "id", description = "DiluteID", required = true)
    @GetMapping("/map/thin/save")
    public void saveThin(String id){
        channelService.saveThin(id);
    }

    @Operation(summary = "Get the progress of thinning execution", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "id", description = "DiluteID", required = true)
    @GetMapping("/map/thin/progress")
    public DrawThinProcess thinProgress(String id){
        return channelService.thinProgress(id);
    }

    @Operation(summary = "Provides standard mvt layers for maps", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @GetMapping(value = "/map/tile/{z}/{x}/{y}", produces = "application/x-protobuf")
    @Parameter(name = "geoCoordSys", description = "geographical coordinate system， WGS84/GCJ02")
    public ResponseEntity<byte[]> getTile(@PathVariable int z, @PathVariable int x, @PathVariable int y, String geoCoordSys){

        try {
            byte[] mvt = channelService.getTile(z, x, y, geoCoordSys);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/x-protobuf"));
            if (mvt == null) {
                headers.setContentLength(0);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }
            headers.setContentLength(mvt.length);
            return new ResponseEntity<>(mvt, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Building vector tiles failed： z: {}, x: {}, y:{}", z, x, y, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @Operation(summary = "Provides a thinned standard mvt layer for the map", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @GetMapping(value = "/map/thin/tile/{z}/{x}/{y}", produces = "application/x-protobuf")
    @Parameter(name = "geoCoordSys", description = "geographical coordinate system， WGS84/GCJ02")
    @Parameter(name = "thinId", description = "Dilution resultsID")
    public ResponseEntity<byte[]> getThinTile(@PathVariable int z, @PathVariable int x, @PathVariable int y,
                                              String geoCoordSys, @RequestParam(required = false) String thinId){

        if (ObjectUtils.isEmpty(thinId)) {
            thinId = "DEFAULT";
        }
        String catchKey = z + "_" + x + "_" + y + "_" + geoCoordSys.toUpperCase();
        byte[] mvt = vectorTileCatch.getVectorTile(thinId, catchKey);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/x-protobuf"));
        if (mvt == null) {
            headers.setContentLength(0);
            return ResponseEntity.status(HttpStatus.OK).body(null);
        }

        headers.setContentLength(mvt.length);
        return new ResponseEntity<>(mvt, headers, HttpStatus.OK);
    }


}
