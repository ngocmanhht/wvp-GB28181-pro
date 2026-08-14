package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaServerChangeEvent;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaServerDeleteEvent;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.media.zlm.dto.ZLMResult;
import com.genersoft.iot.vmp.media.zlm.dto.ZLMServerConfig;
import com.genersoft.iot.vmp.media.zlm.event.HookZlmServerKeepaliveEvent;
import com.genersoft.iot.vmp.media.zlm.event.HookZlmServerStartEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manage the status of zlm streaming nodes
 */
@Slf4j
@Component
public class ZLMMediaServerStatusManager {


    private final Map<Object, MediaServer> offlineZlmPrimaryMap = new ConcurrentHashMap<>();
    private final Map<Object, MediaServer> offlineZlmsecondaryMap = new ConcurrentHashMap<>();
    private final Map<Object, Long> offlineZlmTimeMap = new ConcurrentHashMap<>();

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private UserSetting userSetting;

    @Value("${server.ssl.enabled:false}")
    private boolean sslEnabled;

    @Value("${server.port}")
    private Integer serverPort;

    @Value("${server.servlet.context-path:}")
    private String serverServletContextPath;

    @Autowired
    private EventPublisher eventPublisher;

    private final String type = "zlm";

    @Async
    @EventListener
    public void onApplicationEvent(MediaServerChangeEvent event) {
        if (event.getMediaServerItemList() == null
                || event.getMediaServerItemList().isEmpty()) {
            return;
        }
        for (MediaServer mediaServerItem : event.getMediaServerItemList()) {
            if (!type.equals(mediaServerItem.getType())) {
                continue;
            }
            log.info("[ZLM-Add node to be online] ID：{}", mediaServerItem.getId());
            offlineZlmPrimaryMap.put(mediaServerItem.getId(), mediaServerItem);
            offlineZlmTimeMap.put(mediaServerItem.getId(), System.currentTimeMillis());
        }
        execute();
    }

    @Async
    @EventListener
    public void onApplicationEvent(HookZlmServerStartEvent event) {
        if (event.getMediaServer() == null
                || !type.equals(event.getMediaServer().getType())
                || event.getMediaServer().isStatus()) {
            return;
        }
        log.info("[ZLM-HOOKevent-Service start] ID：{}", event.getMediaServer().getId());
        online(event.getMediaServer(), event.getConfig());
    }

    @Async
    @EventListener
    public void onApplicationEvent(HookZlmServerKeepaliveEvent event) {
        if (event.getMediaServerItem() == null) {
            return;
        }
        MediaServer mediaServer = mediaServerService.getOne(event.getMediaServerItem().getId());
        if (mediaServer == null) {
            return;
        }
        log.debug("[ZLM-HOOKevent-heartbeat] ID：{}", event.getMediaServerItem().getId());
        online(mediaServer, null);
    }

    @Async
    @EventListener
    public void onApplicationEvent(MediaServerDeleteEvent event) {
        if (event.getMediaServer() == null) {
            return;
        }
        log.info("[ZLM-node removed] ID：" + event.getMediaServer().getId());
        offlineZlmPrimaryMap.remove(event.getMediaServer().getId());
        offlineZlmsecondaryMap.remove(event.getMediaServer().getId());
        offlineZlmTimeMap.remove(event.getMediaServer().getId());
    }

    @Scheduled(fixedDelay = 10*1000)   //Check every 10 seconds
    public void execute(){
        // The offline node that joins for the first time will try every ten seconds within 30 minutes. If it is still not online after 30 minutes, it will try to connect every 30 minutes.
        if (offlineZlmPrimaryMap.isEmpty() && offlineZlmsecondaryMap.isEmpty()) {
            return;
        }
        if (!offlineZlmPrimaryMap.isEmpty()) {
            for (MediaServer mediaServerItem : offlineZlmPrimaryMap.values()) {
                if (offlineZlmTimeMap.get(mediaServerItem.getId()) != null
                        && offlineZlmTimeMap.get(mediaServerItem.getId()) <  System.currentTimeMillis() - 30*60*1000) {
                    offlineZlmsecondaryMap.put(mediaServerItem.getId(), mediaServerItem);
                    offlineZlmPrimaryMap.remove(mediaServerItem.getId());
                    continue;
                }
                log.info("[ZLM-try to connect] ID：{}, address： {}:{}", mediaServerItem.getId(), mediaServerItem.getIp(), mediaServerItem.getHttpPort());
                ZLMResult<List<JSONObject>> mediaServerConfigResult = zlmresTfulUtils.getMediaServerConfig(mediaServerItem);
                ZLMServerConfig zlmServerConfig = null;
                if (mediaServerConfigResult == null) {
                    log.info("[ZLM-try to connect]failed, ID：{}, address： {}:{}", mediaServerItem.getId(), mediaServerItem.getIp(), mediaServerItem.getHttpPort());
                    continue;
                }
                List<JSONObject> data = mediaServerConfigResult.getData();
                if (data == null || data.isEmpty()) {
                    log.info("[ZLM-try to connect]failed, ID：{}, address： {}:{}", mediaServerItem.getId(), mediaServerItem.getIp(), mediaServerItem.getHttpPort());
                }else {
                    zlmServerConfig = JSON.parseObject(JSON.toJSONString(data.get(0)), ZLMServerConfig.class);
                    initPort(mediaServerItem, zlmServerConfig);
                    online(mediaServerItem, zlmServerConfig);
                }
            }
        }
        if (!offlineZlmsecondaryMap.isEmpty()) {
            for (MediaServer mediaServerItem : offlineZlmsecondaryMap.values()) {
                Long lastTryTime = offlineZlmTimeMap.get(mediaServerItem.getId());
                if (lastTryTime != null && lastTryTime < System.currentTimeMillis() - 30*60*1000) {
                    continue;
                }
                log.info("[ZLM-try to connect] ID：{}, address： {}:{}", mediaServerItem.getId(), mediaServerItem.getIp(), mediaServerItem.getHttpPort());
                ZLMResult<List<JSONObject>> mediaServerConfig = zlmresTfulUtils.getMediaServerConfig(mediaServerItem);
                ZLMServerConfig zlmServerConfig = null;
                if (mediaServerConfig == null) {
                    log.info("[ZLM-try to connect]failed, ID：{}, address： {}:{}", mediaServerItem.getId(), mediaServerItem.getIp(), mediaServerItem.getHttpPort());
                    offlineZlmTimeMap.put(mediaServerItem.getId(), System.currentTimeMillis());
                    continue;
                }
                List<JSONObject> data = mediaServerConfig.getData();
                if (data == null || data.isEmpty()) {
                    log.info("[ZLM-try to connect]failed, ID：{}, address： {}:{}", mediaServerItem.getId(), mediaServerItem.getIp(), mediaServerItem.getHttpPort());
                    offlineZlmTimeMap.put(mediaServerItem.getId(), System.currentTimeMillis());
                }else {
                    zlmServerConfig = JSON.parseObject(JSON.toJSONString(data.get(0)), ZLMServerConfig.class);
                    initPort(mediaServerItem, zlmServerConfig);
                    online(mediaServerItem, zlmServerConfig);
                }
            }
        }
    }

    private void online(MediaServer mediaServer, ZLMServerConfig config) {
        MediaServer mediaServerInDb = mediaServerService.getOne(mediaServer.getId());
        if (mediaServerInDb == null || !mediaServerInDb.isStatus()) {
            log.info("[ZLM-Connection successful] ID：{}, address： {}:{}", mediaServer.getId(), mediaServer.getIp(), mediaServer.getHttpPort());
            offlineZlmPrimaryMap.remove(mediaServer.getId());
            offlineZlmsecondaryMap.remove(mediaServer.getId());
            if (config == null) {
                ZLMResult<List<JSONObject>> mediaServerConfig = zlmresTfulUtils.getMediaServerConfig(mediaServer);
                List<JSONObject> data = mediaServerConfig.getData();
                if (data != null && !data.isEmpty()) {
                    config = JSON.parseObject(JSON.toJSONString(data.get(0)), ZLMServerConfig.class);
                }else {
                    log.info("[ZLM-Connection successful] Failed to read streaming configuration ID：{}, address： {}:{}", mediaServer.getId(), mediaServer.getIp(), mediaServer.getHttpPort());
                    return;
                }
            }
            // Send online notification
            eventPublisher.mediaServerOnlineEventPublish(mediaServer);
            mediaServer.setStatus(true);
            mediaServer.setServerId(userSetting.getServerId());
            mediaServer.setHookAliveInterval(config.getHookAliveInterval());
            initPort(mediaServer, config);
            mediaServerService.update(mediaServer);
            setZLMConfig(mediaServer, false);
        }
        offlineZlmPrimaryMap.remove(mediaServer.getId());
        offlineZlmsecondaryMap.remove(mediaServer.getId());
        offlineZlmTimeMap.remove(mediaServer.getId());
        // If two heartbeats are not received, zlm will be considered offline.
        String key = "zlm-keepalive-" + mediaServer.getId();
        dynamicTask.startDelay(key, ()->{
            log.warn("[ZLM-Heartbeat timeout] ID：{}", mediaServer.getId());
            // Actively detect once to avoid misjudgment of offline due to short-term network jitter.
            ZLMResult<List<JSONObject>> probeResult = zlmresTfulUtils.getMediaServerConfig(mediaServer);
            if (probeResult != null && probeResult.getData() != null && !probeResult.getData().isEmpty()) {
                log.info("[ZLM-Heartbeat timeout] Active detection is successful, the service is still online, and the heartbeat timer is reset. ID：{}", mediaServer.getId());
                ZLMServerConfig zlmServerConfig = JSON.parseObject(JSON.toJSONString(probeResult.getData().get(0)), ZLMServerConfig.class);
                initPort(mediaServer, zlmServerConfig);
                online(mediaServer, zlmServerConfig);
                return;
            }
            log.warn("[ZLM-Heartbeat timeout] Active detection failed, confirmed offline ID：{}", mediaServer.getId());
            mediaServer.setStatus(false);
            offlineZlmPrimaryMap.put(mediaServer.getId(), mediaServer);
            offlineZlmTimeMap.put(mediaServer.getId(), System.currentTimeMillis());
            // Send offline notification
            eventPublisher.mediaServerOfflineEventPublish(mediaServer);
            mediaServerService.update(mediaServer);
        }, (int)(mediaServer.getHookAliveInterval() * 2 * 1000));
    }
    private void initPort(MediaServer mediaServerItem, ZLMServerConfig zlmServerConfig) {
        // The port will only be read from the configuration once. Once configured or read, it will no longer be configured.
        mediaServerItem.setHttpSSlPort(zlmServerConfig.getHttpSSLport());
        mediaServerItem.setRtmpPort(zlmServerConfig.getRtmpPort());
        mediaServerItem.setRtmpSSlPort(zlmServerConfig.getRtmpSslPort());
        mediaServerItem.setRtspPort(zlmServerConfig.getRtspPort());
        mediaServerItem.setRtspSSLPort(zlmServerConfig.getRtspSSlport());
        mediaServerItem.setRtpProxyPort(zlmServerConfig.getRtpProxyPort());
        mediaServerItem.setFlvSSLPort(zlmServerConfig.getHttpSSLport());
        mediaServerItem.setWsFlvSSLPort(zlmServerConfig.getHttpSSLport());
        if (Objects.isNull(zlmServerConfig.getTranscodeSuffix())) {
            mediaServerItem.setTranscodeSuffix(null);
        }else {
            mediaServerItem.setTranscodeSuffix(zlmServerConfig.getTranscodeSuffix());
        }
        mediaServerItem.setRtpProxyPort(zlmServerConfig.getRtpProxyPort());
        mediaServerItem.setHookAliveInterval(zlmServerConfig.getHookAliveInterval());
    }

    public void setZLMConfig(MediaServer mediaServerItem, boolean restart) {
        log.info("[media service node] Setting up ：{} -> {}:{}",
                mediaServerItem.getId(), mediaServerItem.getIp(), mediaServerItem.getHttpPort());
        String protocol = sslEnabled ? "https" : "http";
        String hookPrefix = String.format("%s://%s:%s%s/index/hook", protocol, mediaServerItem.getHookIp(), serverPort, (serverServletContextPath == null || "/".equals(serverServletContextPath)) ? "" : serverServletContextPath);

        Map<String, Object> param = new HashMap<>();
        if (mediaServerItem.getRtspPort() != 0) {
            param.put("ffmpeg.snap", "%s -rtsp_transport tcp -i %s -y -f mjpeg -frames:v 1 %s");
        }
        param.put("hook.enable","1");
        param.put("hook.on_flow_report","");
        param.put("hook.on_play",String.format("%s/on_play", hookPrefix));
        param.put("hook.on_http_access","");
        param.put("hook.on_publish", String.format("%s/on_publish", hookPrefix));
        param.put("hook.on_record_ts","");
        param.put("hook.on_rtsp_auth","");
        param.put("hook.on_rtsp_realm","");
        param.put("hook.on_server_started",String.format("%s/on_server_started", hookPrefix));
        param.put("hook.on_shell_login","");
        param.put("hook.on_stream_changed",String.format("%s/on_stream_changed", hookPrefix));
        param.put("hook.on_stream_none_reader",String.format("%s/on_stream_none_reader", hookPrefix));
        param.put("hook.on_stream_not_found",String.format("%s/on_stream_not_found", hookPrefix));
        param.put("hook.on_server_keepalive",String.format("%s/on_server_keepalive", hookPrefix));
        param.put("hook.on_send_rtp_stopped",String.format("%s/on_send_rtp_stopped", hookPrefix));
        param.put("hook.on_rtp_server_timeout",String.format("%s/on_rtp_server_timeout", hookPrefix));
        param.put("hook.on_record_mp4",String.format("%s/on_record_mp4", hookPrefix));
        param.put("hook.timeoutSec","30");
        // After the push stream is disconnected, you can reconnect within the timeout period to continue pushing the stream, so that the player will continue to play.。
        // Set to 0 to disable this feature(Disconnection of the push stream will cause the player to be disconnected immediately)
        // This parameter should not be greater than the player timeout
        // Optimize this message to receive stream logout events faster
        param.put("protocol.continue_push_ms", "3000" );
        // The maximum waiting time for uninitialized tracks, in milliseconds. After the timeout, uninitialized tracks will be ignored. Set this option to optimize irregular streams with audio errors.，
        // When zlm supports setting to turn off audio for each rtpServer, you can not set this option.
        if (mediaServerItem.isRtpEnable() && !ObjectUtils.isEmpty(mediaServerItem.getRtpPortRange())) {
            param.put("rtp_proxy.port_range", mediaServerItem.getRtpPortRange().replace(",", "-"));
        }

        if (!ObjectUtils.isEmpty(mediaServerItem.getRecordPath())) {
            File recordPathFile = new File(mediaServerItem.getRecordPath());
            param.put("protocol.mp4_save_path", recordPathFile.getParentFile().getPath());
            param.put("protocol.downloadRoot", recordPathFile.getParentFile().getPath());
            param.put("record.appName", recordPathFile.getName());
        }

        ZLMResult<?> zlmResult = zlmresTfulUtils.setServerConfig(mediaServerItem, param);

        if (zlmResult != null && zlmResult.getCode() == 0) {
            if (restart) {
                log.info("[media service node] The setting is successful, restart to ensure that the configuration takes effect. {} -> {}:{}",
                        mediaServerItem.getId(), mediaServerItem.getIp(), mediaServerItem.getHttpPort());
                zlmresTfulUtils.restartServer(mediaServerItem);
            }else {
                log.info("[media service node] Setup successful {} -> {}:{}",
                        mediaServerItem.getId(), mediaServerItem.getIp(), mediaServerItem.getHttpPort());
            }
        }else {
            log.info("[media service node] Failed to set up media service node {} -> {}:{}",
                    mediaServerItem.getId(), mediaServerItem.getIp(), mediaServerItem.getHttpPort());
        }
    }

}
