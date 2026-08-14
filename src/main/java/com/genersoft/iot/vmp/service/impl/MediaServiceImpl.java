package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.enums.MediaStreamUtil;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.service.ISourceOtherService;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.bean.ResultForOnPublish;
import com.genersoft.iot.vmp.media.zlm.dto.StreamAuthorityInfo;
import com.genersoft.iot.vmp.service.IMediaService;
import com.genersoft.iot.vmp.service.IReceiveRtpServerService;
import com.genersoft.iot.vmp.service.IRecordPlanService;
import com.genersoft.iot.vmp.service.IUserService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.streamProxy.bean.StreamProxy;
import com.genersoft.iot.vmp.streamProxy.service.IStreamProxyService;
import com.genersoft.iot.vmp.utils.MediaServerUtils;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements IMediaService {

    private final IRedisCatchStorage redisCatchStorage;

    private final IStreamProxyService streamProxyService;

    private final UserSetting userSetting;

    private final IUserService userService;

    private final IReceiveRtpServerService receiveRtpServerService;

    private final IRecordPlanService recordPlanService;

    private final Map<String, ISourceOtherService> sourceOtherServiceMap;


    @Override
    public boolean authenticatePlay(String app, String stream, String callId) {
        if (app == null || stream == null) {
            return false;
        }
        if (MediaStreamUtil.RTP_APP.equals(app)) {
            return true;
        }
        StreamAuthorityInfo streamAuthorityInfo = redisCatchStorage.getStreamAuthorityInfo(app, stream);
        if (streamAuthorityInfo == null || streamAuthorityInfo.getCallId() == null) {
            return true;
        }
        return streamAuthorityInfo.getCallId().equals(callId);
    }

    @Override
    public ResultForOnPublish authenticatePublish(MediaServer mediaServer, String app, String stream, String params) {

        if (MediaStreamUtil.RTP_APP.equals(app)) {
            return receiveRtpServerService.getAuthenticateInfo(stream);
        }else {
            ResultForOnPublish result = new ResultForOnPublish();
            // app For non-RTP_APP streams, if it is national standard intercom or broadcast, the sound will be obtained by default and will not be recorded. For other streams, first check whether there is a proxy configuration. If there is no proxy configuration, then authenticate.
            if (MediaStreamUtil.GB28181_TALK.equals(app) || MediaStreamUtil.GB28181_BROADCAST.equals(app) || MediaStreamUtil.JT_TALK.equals(app)) {
                result.setEnable_mp4(false);
                result.setEnable_audio(true);
                return result;
            }
            if (MediaStreamUtil.LOAD_MP4_APP.equals(app) ) {
                result.setEnable_mp4(false);
                result.setEnable_audio(true);
                return result;
            }
            StreamProxy streamProxyItem = streamProxyService.getStreamProxyByAppAndStream(app, stream);
            if (streamProxyItem != null) {
                result.setEnable_audio(streamProxyItem.isEnableAudio());
                result.setEnable_mp4(streamProxyItem.isEnableMp4());
                return result;
            }
            if (userSetting.getPushAuthority()) {
                // Authentication for push streaming
                Map<String, String> paramMap = MediaServerUtils.urlParamToMap(params);
                // Push authentication
                if (params == null) {
                    log.info("Push authentication failed: Missing necessary parameters：sign=md5(usertablepushKey)");
                    throw new ControllerException(ErrorCode.ERROR401.getCode(), "Unauthorized");
                }

                String sign = paramMap.get("sign");
                if (sign == null) {
                    log.info("Push authentication failed: Missing necessary parameters：sign=md5(usertablepushKey)");
                    throw new ControllerException(ErrorCode.ERROR401.getCode(), "Unauthorized");
                }
                // Push custom playback authentication code
                String callId = paramMap.get("callId");
                // Authentication configuration
                boolean hasAuthority = userService.checkPushAuthority(callId, sign);
                if (!hasAuthority) {
                    log.info("Push authentication failed: sign does not have permission: callId={}. sign={}", callId, sign);
                    throw new ControllerException(ErrorCode.ERROR401.getCode(), "Unauthorized");
                }
                StreamAuthorityInfo streamAuthorityInfo = StreamAuthorityInfo.getInstanceByHook(app, stream, mediaServer.getId());
                streamAuthorityInfo.setCallId(callId);
                streamAuthorityInfo.setSign(sign);
                // Authentication passed
                redisCatchStorage.updateStreamAuthorityInfo(app, stream, streamAuthorityInfo);
            }
            result.setEnable_audio(true);
            result.setEnable_mp4(userSetting.getRecordPushLive());
            return result;
        }
    }

    @Override
    public boolean closeStreamOnNoneReader(String mediaServerId, String app, String stream, String schema) {
        if (recordPlanService.recording(app, stream) != null) {
            return false;
        }
        if (MediaStreamUtil.LOAD_MP4_APP.equals(app)) {
            // mp4On-demand streaming, will not be closed if no one is watching
            return true;
        }

        for (ISourceOtherService sourceOtherService : sourceOtherServiceMap.values()) {
            try {
                Boolean result = sourceOtherService.closeStreamOnNoneReader(mediaServerId, app, stream, schema);
                if (result != null) {
                    return result;
                }
            }catch (Exception e) {
                log.error("Failed to call other services to close unattended streams， app={}, stream={}, schema={}", app, stream, schema, e);
            }
        }

        // Streaming agent
        StreamProxy streamProxy = streamProxyService.getStreamProxyByAppAndStream(app, stream);
        if (streamProxy != null) {
            if (streamProxy.isEnableDisableNoneReader()) {
                // No one watching disabled
                // Modify data
                streamProxyService.stopByAppAndStream(app, stream);
                return true;
            } else {
                // No one is watching and no processing is done
                return false;
            }
        } else {
            return userSetting.getStreamOnDemand();
        }
    }
}
