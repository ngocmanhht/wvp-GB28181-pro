package com.genersoft.iot.vmp.gb28181.controller;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.conf.security.SecurityUtils;
import com.genersoft.iot.vmp.conf.security.dto.LoginUser;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.media.zlm.dto.StreamAuthorityInfo;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.streamProxy.service.IStreamProxyService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.net.MalformedURLException;
import java.net.URL;


@Tag(name  = "Media streaming related")
@RestController
@Slf4j
@RequestMapping(value = "/api/media")
public class MediaController {

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IStreamProxyService streamProxyService;

    @Autowired
    private IMediaServerService mediaServerService;


    /**
     * Get the playback address based on the application name and stream id
     * @param app Application name
     * @param stream flowid
     * @return
     */
    @Operation(summary = "Get the playback address based on the application name and stream id", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "app", description = "Application name", required = true)
    @Parameter(name = "stream", description = "flowid", required = true)
    @Parameter(name = "mediaServerId", description = "media serverid")
    @Parameter(name = "callId", description = "Custom authentication carried when pushing the streamID")
    @Parameter(name = "useSourceIpAsStreamIp", description = "Whether to use the request IP as the return addressIP")
    @GetMapping(value = "/stream_info_by_app_and_stream")
    @ResponseBody
    public DeferredResult<WVPResult<StreamContent>> getStreamInfoByAppAndStream(HttpServletRequest request, @RequestParam String app,
                                                                                @RequestParam String stream,
                                                                                @RequestParam(required = false) String mediaServerId,
                                                                                @RequestParam(required = false) String callId,
                                                                                @RequestParam(required = false) Boolean useSourceIpAsStreamIp){
        DeferredResult<WVPResult<StreamContent>> result = new DeferredResult<>();
        boolean authority = false;
        if (callId != null) {
            // Permission verification
            StreamAuthorityInfo streamAuthorityInfo = redisCatchStorage.getStreamAuthorityInfo(app, stream);
            if (streamAuthorityInfo != null
                    && streamAuthorityInfo.getCallId() != null
                    && streamAuthorityInfo.getCallId().equals(callId)) {
                authority = true;
            }else {
                throw new ControllerException(ErrorCode.ERROR400.getCode(), "Failed to obtain playback address authentication");
            }
        }else {
            // Whether the user is logged in, complete information will be returned to the logged in user
            LoginUser userInfo = SecurityUtils.getUserInfo();
            if (userInfo!= null) {
                authority = true;
            }
        }
        StreamInfo streamInfo;
        if (useSourceIpAsStreamIp != null && useSourceIpAsStreamIp) {
            String host = request.getHeader("Host");
            String localAddr = host.split(":")[0];
            log.info("Use{}as a return streamip", localAddr);
            streamInfo = mediaServerService.getStreamInfoByAppAndStreamWithCheck(app, stream, mediaServerId, localAddr, authority);
        }else {
            streamInfo = mediaServerService.getStreamInfoByAppAndStreamWithCheck(app, stream, mediaServerId, authority);
        }

        if (streamInfo != null){
            WVPResult<StreamContent> wvpResult = WVPResult.success();
            wvpResult.setData(new StreamContent(streamInfo));
            result.setResult(wvpResult);
        }else {
            ErrorCallback<StreamInfo> callback = (code, msg, streamInfoStoStart) -> {
                if (code == InviteErrorCode.SUCCESS.getCode()) {
                    WVPResult<StreamContent> wvpResult = WVPResult.success();
                    if (useSourceIpAsStreamIp != null && useSourceIpAsStreamIp) {
                        String host;
                        try {
                            URL url=new URL(request.getRequestURL().toString());
                            host=url.getHost();
                        } catch (MalformedURLException e) {
                            host=request.getLocalAddr();
                        }
                        streamInfoStoStart.changeStreamIp(host);
                    }
                    if (!ObjectUtils.isEmpty(streamInfoStoStart.getMediaServer().getTranscodeSuffix())
                            && !"null".equalsIgnoreCase(streamInfoStoStart.getMediaServer().getTranscodeSuffix())) {
                        streamInfoStoStart.setStream(streamInfoStoStart.getStream() + "_" + streamInfoStoStart.getMediaServer().getTranscodeSuffix());
                    }
                    wvpResult.setData(new StreamContent(streamInfoStoStart));
                    result.setResult(wvpResult);
                }else {
                    result.setResult(WVPResult.fail(code, msg));
                }
            };
            //Failed to obtain the stream. Restart the stream and try again.
            streamProxyService.startByAppAndStream(app, stream, callback);
        }
        return result;
    }
    /**
     * Get the push playback address
     * @param app Application name
     * @param stream flowid
     * @return
     */
    @GetMapping(value = "/getPlayUrl")
    @ResponseBody
    @Operation(summary = "Get the push playback address", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "app", description = "Application name", required = true)
    @Parameter(name = "stream", description = "flowid", required = true)
    @Parameter(name = "mediaServerId", description = "media serverid")
    public StreamContent getPlayUrl(@RequestParam String app, @RequestParam String stream,
                                    @RequestParam(required = false) String mediaServerId){
        boolean authority = false;
        // Whether the user is logged in, complete information will be returned to the logged in user
        LoginUser userInfo = SecurityUtils.getUserInfo();
        if (userInfo!= null) {
            authority = true;
        }
        StreamInfo streamInfo = mediaServerService.getStreamInfoByAppAndStreamWithCheck(app, stream, mediaServerId, authority);
        if (streamInfo == null){
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Failed to obtain playback address");
        }
        return new StreamContent(streamInfo);
    }
}
