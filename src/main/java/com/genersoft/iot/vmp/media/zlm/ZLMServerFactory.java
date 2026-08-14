package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson2.JSONArray;
import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.zlm.dto.ZLMResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class ZLMServerFactory {
    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;


    /**
     * turn onrtpServer
     * @param mediaServerItem zlmService instance
     * @param streamId flowId
     * @param ssrc ssrc
     * @param port port， 0/nullto use random
     * @param reUsePort Whether to reuse ports
     * @param tcpMode 0/null udp Mode, 1 tcp passive mode, 2 tcp active mode。
     * @return
     */
    public int createRTPServer(MediaServer mediaServerItem, String app, String streamId, long ssrc, Integer port, Boolean onlyAuto, Boolean disableAudio, Boolean reUsePort, Integer tcpMode) {
        int result = -1;
        // Check whether this rtp server already exists
        ZLMResult<?> rtpInfoResult = zlmresTfulUtils.getRtpInfo(mediaServerItem, streamId);
        if(rtpInfoResult.getCode() == 0){
            if (rtpInfoResult.getExist() != null && rtpInfoResult.getExist()) {
                result = rtpInfoResult.getLocal_port();
                if (result == 0) {
                    // At this point, it means that rtpServer has been created but the stream has not been pushed up yet.
                    // Re-open at this timertpServer
                    Map<String, Object> param = new HashMap<>();
                    param.put("stream_id", streamId);
                    ZLMResult<?> zlmResult = zlmresTfulUtils.closeRtpServer(mediaServerItem, param);
                    if (zlmResult != null ) {
                        if (zlmResult.getCode() == 0) {
                            return createRTPServer(mediaServerItem, app, streamId, ssrc, port, onlyAuto, disableAudio, reUsePort, tcpMode);
                        }else {
                            log.warn("[turn onrtpServer], Restart RtpServer error");
                        }
                    }
                }
                return result;
            }
        }else if(rtpInfoResult.getCode() == -2){
            return result;
        }

        Map<String, Object> param = new HashMap<>();

        if (tcpMode == null) {
            tcpMode = 0;
        }
        param.put("tcp_mode", tcpMode);
        param.put("app", app);
        param.put("stream_id", streamId);
        if (disableAudio != null) {
            param.put("only_track", disableAudio?2:0);
        }

        if (reUsePort != null) {
            param.put("re_use_port", reUsePort?"1":"0");
        }
        // If the push port is set to 0, a random port will be used.
        if (port == null) {
            param.put("port", 0);
        }else {
            param.put("port", port);
        }
        if (onlyAuto != null) {
            param.put("only_audio", onlyAuto?"1":"0");
        }
        if (ssrc != 0) {
            param.put("ssrc", ssrc);
        }

        ZLMResult<?> zlmResult = zlmresTfulUtils.openRtpServer(mediaServerItem, param);
        if (zlmResult != null) {
            if (zlmResult.getCode() == 0) {
                result= zlmResult.getPort();
            }else {
                log.error("Failed to create RTP Server {}: ", zlmResult.getMsg());
            }
        }else {
            //  Check ZLM status
            log.error("Failed to create RTP Server {}: Please check ZLM service", param.get("port"));
        }
        return result;
    }

    public boolean closeRtpServer(MediaServer serverItem, String app, String streamId) {
        boolean result = false;
        if (serverItem !=null){
            Map<String, Object> param = new HashMap<>();
            param.put("app", app);
            param.put("stream_id", streamId);
            ZLMResult<?> zlmResult = zlmresTfulUtils.closeRtpServer(serverItem, param);
            if (zlmResult != null ) {
                if (zlmResult.getCode() == 0) {
                    result = zlmResult.getHit() >= 1;
                }else {
                    log.error("Failed to close RTP Server: " + zlmResult.getMsg());
                }
            }else {
                //  Check ZLM status
                log.error("Failed to close RTP Server: Please check the ZLM service");
            }
        }
        return result;
    }

    public void closeRtpServer(MediaServer serverItem, String app, String streamId, CommonCallback<Boolean> callback) {
        if (serverItem == null) {
            if (callback != null) {
                callback.run(false);
            }
            return;
        }
        Map<String, Object> param = new HashMap<>();
        param.put("app", app);
        param.put("stream_id", streamId);
        zlmresTfulUtils.closeRtpServer(serverItem, param, zlmResult -> {
            if (zlmResult.getCode() == 0) {
                if (callback != null) {
                    callback.run(zlmResult.getHit() >= 1);
                }
                return;
            }else {
                log.error("Failed to close RTP Server: " + zlmResult.getMsg());
            }
            if (callback != null) {
                callback.run(false);
            }
        });
    }


    /**
     * callzlm RESTFUL API —— startSendRtp
     */
    public ZLMResult<?> startSendRtpStream(MediaServer mediaServerItem, Map<String, Object>param) {
        return zlmresTfulUtils.startSendRtp(mediaServerItem, param);
    }

    /**
     * callzlm RESTFUL API —— startSendRtpPassive
     */
    public ZLMResult<?> startSendRtpPassive(MediaServer mediaServerItem, Map<String, Object>param) {
        return zlmresTfulUtils.startSendRtpPassive(mediaServerItem, param);
    }

    public ZLMResult<?> startSendRtpPassive(MediaServer mediaServerItem, Map<String, Object> param, ZLMRESTfulUtils.ResultCallback callback) {
        return zlmresTfulUtils.startSendRtpPassive(mediaServerItem, param, callback);
    }

    public ZLMResult<?> startSendRtpTalk(MediaServer mediaServer, Map<String, Object> param, ZLMRESTfulUtils.ResultCallback callback) {
        return zlmresTfulUtils.startSendRtpTalk(mediaServer, param, callback);
    }

    /**
     * Query whether the stream to be retweeted is ready
     */
    public Boolean isStreamReady(MediaServer mediaServerItem, String app, String streamId) {
        ZLMResult<?> zlmResult = zlmresTfulUtils.getMediaList(mediaServerItem, app, streamId);
        if (zlmResult == null || zlmResult.getCode() == -2) {
            return null;
        }
        ZLMResult<JSONArray> result = (ZLMResult<JSONArray>) zlmResult;
        return  (result.getCode() == 0
                && result.getData() != null
                && !result.getData().isEmpty());
    }

    public ZLMResult<?> startSendRtp(MediaServer mediaInfo, SendRtpInfo sendRtpItem) {
        String is_Udp = sendRtpItem.isTcp() ? "0" : "1";
        log.info("rtp/{}Start pushing, target={}:{}，SSRC={}", sendRtpItem.getStream(), sendRtpItem.getIp(), sendRtpItem.getPort(), sendRtpItem.getSsrc());
        Map<String, Object> param = new HashMap<>(12);
        param.put("vhost","__defaultVhost__");
        param.put("app",sendRtpItem.getApp());
        param.put("stream",sendRtpItem.getStream());
        param.put("ssrc", sendRtpItem.getSsrc());
        param.put("src_port", sendRtpItem.getLocalPort());
        param.put("pt", sendRtpItem.getPt());
        param.put("use_ps", sendRtpItem.isUsePs() ? "1" : "0");
        param.put("only_audio", sendRtpItem.isOnlyAudio() ? "1" : "0");
        if (!sendRtpItem.isTcp()) {
            // udpEnable rtcp keepalive in mode
            param.put("udp_rtcp_timeout", sendRtpItem.isRtcp()? "1":"0");
        }

        if (mediaInfo == null) {
            return null;
        }
        // If it is non-strict mode, you need to turn off port occupation.
        ZLMResult<?> zlmResult = null;
        if (sendRtpItem.getLocalPort() != 0) {
            if (sendRtpItem.isTcpActive()) {
                zlmResult = startSendRtpPassive(mediaInfo, param);
            }else {
                param.put("is_udp", is_Udp);
                param.put("dst_url", sendRtpItem.getIp());
                param.put("dst_port", sendRtpItem.getPort());
                zlmResult = startSendRtpStream(mediaInfo, param);
            }
        }else {
            if (sendRtpItem.isTcpActive()) {
                zlmResult = startSendRtpPassive(mediaInfo, param);
            }else {
                param.put("is_udp", is_Udp);
                param.put("dst_url", sendRtpItem.getIp());
                param.put("dst_port", sendRtpItem.getPort());
                zlmResult = startSendRtpStream(mediaInfo, param);
            }
        }
        return zlmResult;
    }

    public Boolean updateRtpServerSSRC(MediaServer mediaServerItem, String app, String streamId, String ssrc) {
        boolean result = false;
        ZLMResult<?> zlmResult = zlmresTfulUtils.updateRtpServerSSRC(mediaServerItem, app, streamId, ssrc);
        if (zlmResult.getCode() == 0) {
            result= true;
            log.info("[updateRTPServer] success");
        } else {
            log.error("[updateRTPServer] failed: {}, streamId：{}，ssrc：{}", zlmResult.getMsg(),
                    streamId, ssrc);
        }
        return result;
    }

    public ZLMResult<?> stopSendRtpStream(MediaServer mediaServerItem, SendRtpInfo sendRtpItem) {
        Map<String, Object> param = new HashMap<>();
        param.put("vhost", "__defaultVhost__");
        param.put("app", sendRtpItem.getApp());
        param.put("stream", sendRtpItem.getStream());
        param.put("ssrc", sendRtpItem.getSsrc());
        return zlmresTfulUtils.stopSendRtp(mediaServerItem, param);
    }


}
