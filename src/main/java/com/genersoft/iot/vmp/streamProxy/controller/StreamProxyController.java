package com.genersoft.iot.vmp.streamProxy.controller;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.streamProxy.bean.StreamProxy;
import com.genersoft.iot.vmp.streamProxy.service.IStreamProxyPlayService;
import com.genersoft.iot.vmp.streamProxy.service.IStreamProxyService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

@SuppressWarnings("rawtypes")
/**
 * Pull proxy interface
 */
@Tag(name = "Streaming agent", description = "")
@RestController
@Slf4j
@RequestMapping(value = "/api/proxy")
public class StreamProxyController {

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private IStreamProxyService streamProxyService;

    @Autowired
    private IStreamProxyPlayService streamProxyPlayService;

    @Autowired
    private UserSetting userSetting;


    @Operation(summary = "Paginated query flow proxy", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "Current page")
    @Parameter(name = "count", description = "Number of queries per page")
    @Parameter(name = "query", description = "Query content")
    @Parameter(name = "pulling", description = "Is streaming")
    @Parameter(name = "mediaServerId", description = "streaming mediaID")
    @GetMapping(value = "/list")
    @ResponseBody
    public PageInfo<StreamProxy> list(@RequestParam(required = false)Integer page,
                                      @RequestParam(required = false)Integer count,
                                      @RequestParam(required = false)String query,
                                      @RequestParam(required = false)Boolean pulling,
                                      @RequestParam(required = false)String mediaServerId){

        if (ObjectUtils.isEmpty(mediaServerId)) {
            mediaServerId = null;
        }
        if (ObjectUtils.isEmpty(query)) {
            query = null;
        }
        return streamProxyService.getAll(page, count, query, pulling, mediaServerId);
    }

    @Operation(summary = "Query Streaming Agent", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "app", description = "Application name")
    @Parameter(name = "stream", description = "flowId")
    @GetMapping(value = "/one")
    @ResponseBody
    public StreamProxy one(String app, String stream){

        return streamProxyService.getStreamProxyByAppAndStream(app, stream);
    }

    @Operation(summary = "Add new agent", security = @SecurityRequirement(name = JwtUtils.HEADER), parameters = {
            @Parameter(name = "param", description = "proxy parameters", required = true),
    })
    @PostMapping(value = "/add")
    @ResponseBody
    public StreamProxy add(@RequestBody StreamProxy param){
        log.info("Add proxy： " + JSONObject.toJSONString(param));
        if (ObjectUtils.isEmpty(param.getRelatesMediaServerId())) {
            param.setRelatesMediaServerId(null);
        }
        if (ObjectUtils.isEmpty(param.getType())) {
            param.setType("default");
        }
        if (ObjectUtils.isEmpty(param.getGbId())) {
            param.setGbDeviceId(null);
        }
        param.setServerId(userSetting.getServerId());
        streamProxyService.add(param);
        return param;
    }

    @Operation(summary = "update agent", security = @SecurityRequirement(name = JwtUtils.HEADER), parameters = {
            @Parameter(name = "param", description = "proxy parameters", required = true),
    })
    @PostMapping(value = "/update")
    @ResponseBody
    public StreamProxy update(@RequestBody StreamProxy param){
        log.info("update agent： " + JSONObject.toJSONString(param));
        if (param.getId() == 0) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "Missing agent informationID");
        }
        if (ObjectUtils.isEmpty(param.getRelatesMediaServerId())) {
            param.setRelatesMediaServerId(null);
        }
        if (ObjectUtils.isEmpty(param.getGbId())) {
            param.setGbDeviceId(null);
        }
        streamProxyService.update(param);
        return param;
    }

    @GetMapping(value = "/ffmpeg_cmd/list")
    @ResponseBody
    @Operation(summary = "Get ffmpeg.cmd template", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "mediaServerId", description = "streaming mediaID", required = true)
    public Map<String, String> getFFmpegCMDs(@RequestParam String mediaServerId){
        log.debug("Get node[ {} ]ffmpeg.cmdTemplate", mediaServerId );

        MediaServer mediaServerItem = mediaServerService.getOne(mediaServerId);
        if (mediaServerItem == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "streaming media： " + mediaServerId + "not found");
        }
        return streamProxyService.getFFmpegCMDs(mediaServerItem);
    }

    @DeleteMapping(value = "/del")
    @ResponseBody
    @Operation(summary = "Remove proxy", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "app", description = "Application name", required = true)
    @Parameter(name = "stream", description = "flowid", required = true)
    public void del(@RequestParam String app, @RequestParam String stream){
        log.info("Remove proxy： " + app + "/" + stream);
        if (app == null || stream == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), app == null ?"appcannot benull":"streamcannot benull");
        }else {
            streamProxyService.delteByAppAndStream(app, stream);
        }
    }

    @DeleteMapping(value = "/delete")
    @ResponseBody
    @Operation(summary = "Remove proxy", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "id", description = "agentID", required = true)
    public void delte(int id){
        log.info("Remove proxy： {}", id);
        streamProxyService.delete(id);
    }

    @GetMapping(value = "/start")
    @ResponseBody
    @Operation(summary = "play agent", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "id", description = "agentId", required = true)
    public DeferredResult<WVPResult<StreamContent>> start(HttpServletRequest request, int id){
        log.info("play agent： {}", id);
        StreamProxy streamProxy = streamProxyService.getStreamProxy(id);
        Assert.notNull(streamProxy, "Agent information does not exist");

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

        streamProxyPlayService.start(id, null, callback);
        return result;
    }

    @GetMapping(value = "/stop")
    @ResponseBody
    @Operation(summary = "Stop playing", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "id", description = "agentId", required = true)
    public void stop(int id){
        log.info("Stop playing： {}", id);
        streamProxyPlayService.stop(id);
    }
}
