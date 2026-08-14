package com.genersoft.iot.vmp.media.abl;

import com.alibaba.fastjson2.JSONArray;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.media.abl.bean.ABLResult;
import com.genersoft.iot.vmp.media.abl.bean.AblServerConfig;
import com.genersoft.iot.vmp.media.abl.bean.ConfigKeyId;
import com.genersoft.iot.vmp.media.abl.event.HookAblServerKeepaliveEvent;
import com.genersoft.iot.vmp.media.abl.event.HookAblServerStartEvent;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaServerChangeEvent;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaServerDeleteEvent;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manage the status of zlm streaming nodes
 */
@Component
@Slf4j
public class ABLMediaServerStatusManger {
    
    private final Map<Object, MediaServer> offlineABLPrimaryMap = new ConcurrentHashMap<>();
    private final Map<Object, MediaServer> offlineAblsecondaryMap = new ConcurrentHashMap<>();
    private final Map<Object, Long> offlineAblTimeMap = new ConcurrentHashMap<>();

    @Autowired
    private ABLRESTfulUtils ablResTfulUtils;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private DynamicTask dynamicTask;

    @Value("${server.ssl.enabled:false}")
    private boolean sslEnabled;

    @Value("${server.port}")
    private Integer serverPort;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private EventPublisher eventPublisher;

    private final String type = "abl";

    @Async
    @EventListener
    public void onApplicationEvent(MediaServerChangeEvent event) {
        if (event.getMediaServerItemList() == null
                || event.getMediaServerItemList().isEmpty()) {
            return;
        }
        for (MediaServer mediaServer : event.getMediaServerItemList()) {
            if (!type.equals(mediaServer.getType())) {
                continue;
            }
            log.info("[ABL-Add node to be online] ID：" + mediaServer.getId());
            offlineABLPrimaryMap.put(mediaServer.getId(), mediaServer);
            offlineAblTimeMap.put(mediaServer.getId(), System.currentTimeMillis());
        }
        execute();
    }

    @Async
    @EventListener
    public void onApplicationEvent(HookAblServerStartEvent event) {
        if (event.getMediaServerItem() == null
                || !type.equals(event.getMediaServerItem().getType())
                || event.getMediaServerItem().isStatus()) {
            return;
        }
        MediaServer serverItem = mediaServerService.getOne(event.getMediaServerItem().getId());
        if (serverItem == null) {
            return;
        }
        log.info("[ABL-HOOKevent-Service start] ID：" + event.getMediaServerItem().getId());
        online(serverItem, null);
    }

    @Async
    @EventListener
    public void onApplicationEvent(HookAblServerKeepaliveEvent event) {
        if (event.getMediaServerItem() == null) {
            return;
        }
        MediaServer serverItem = mediaServerService.getOne(event.getMediaServerItem().getId());
        if (serverItem == null) {
            return;
        }
        log.info("[ABL-HOOKevent-heartbeat] ID：" + event.getMediaServerItem().getId());
        online(serverItem, null);
    }

    @Async
    @EventListener
    public void onApplicationEvent(MediaServerDeleteEvent event) {
        if (event.getMediaServer() == null) {
            return;
        }
        log.info("[ABL-node removed] ID：" + event.getMediaServer().getServerId());
        offlineABLPrimaryMap.remove(event.getMediaServer().getServerId());
        offlineAblsecondaryMap.remove(event.getMediaServer().getServerId());
        offlineAblTimeMap.remove(event.getMediaServer().getServerId());
    }

    @Scheduled(fixedDelay = 10*1000)   //Check every 10 seconds
    public void execute(){
        // The offline node that joins for the first time will try every ten seconds within 30 minutes. If it is still not online after 30 minutes, it will try to connect every 30 minutes.
        if (offlineABLPrimaryMap.isEmpty() && offlineAblsecondaryMap.isEmpty()) {
            return;
        }
        if (!offlineABLPrimaryMap.isEmpty()) {
            for (MediaServer mediaServerItem : offlineABLPrimaryMap.values()) {
                Long lastTryTime = offlineAblTimeMap.get(mediaServerItem.getId());
                if (lastTryTime != null && lastTryTime < System.currentTimeMillis() - 30*60*1000) {
                    offlineAblsecondaryMap.put(mediaServerItem.getId(), mediaServerItem);
                    offlineABLPrimaryMap.remove(mediaServerItem.getId());
                    continue;
                }
                log.info("[ABL-try to connect] ID：{}, address： {}:{}", mediaServerItem.getId(), mediaServerItem.getIp(), mediaServerItem.getHttpPort());
                ABLResult ablResult = ablResTfulUtils.getServerConfig(mediaServerItem);
                AblServerConfig ablServerConfig = null;
                if (ablResult.getCode() != 0) {
                    log.info("[ABL-try to connect]failed, ID：{}, address： {}:{}", mediaServerItem.getId(), mediaServerItem.getIp(), mediaServerItem.getHttpPort());
                    continue;
                }
                JSONArray params = ablResult.getParams();

                if (params == null || params.isEmpty()) {
                    log.info("[ABL-try to connect]failed, ID：{}, address： {}:{}", mediaServerItem.getId(), mediaServerItem.getIp(), mediaServerItem.getHttpPort());
                }else {
                    ablServerConfig = AblServerConfig.getInstance(params);
                    initPort(mediaServerItem, ablServerConfig);
                    online(mediaServerItem, ablServerConfig);
                }
            }
        }
        if (!offlineAblsecondaryMap.isEmpty()) {
            for (MediaServer mediaServerItem : offlineAblsecondaryMap.values()) {
                Long lastTryTime = offlineAblTimeMap.get(mediaServerItem.getId());
                if (lastTryTime != null && lastTryTime < System.currentTimeMillis() - 30*60*1000) {
                    continue;
                }
                log.info("[ABL-try to connect] ID：{}, address： {}:{}", mediaServerItem.getId(), mediaServerItem.getIp(), mediaServerItem.getHttpPort());
                ABLResult ablResult = ablResTfulUtils.getServerConfig(mediaServerItem);
                AblServerConfig ablServerConfig = null;
                if (ablResult.getCode() != 0) {
                    log.info("[ABL-try to connect]failed, ID：{}, address： {}:{}", mediaServerItem.getId(), mediaServerItem.getIp(), mediaServerItem.getHttpPort());
                    offlineAblTimeMap.put(mediaServerItem.getId(), System.currentTimeMillis());
                    continue;
                }
                JSONArray params = ablResult.getParams();
                if (params == null || params.isEmpty()) {
                    log.info("[ABL-try to connect]failed, ID：{}, address： {}:{}", mediaServerItem.getId(), mediaServerItem.getIp(), mediaServerItem.getHttpPort());
                    offlineAblTimeMap.put(mediaServerItem.getId(), System.currentTimeMillis());
                }else {
                    ablServerConfig = AblServerConfig.getInstance(params);
                    initPort(mediaServerItem, ablServerConfig);
                    online(mediaServerItem, ablServerConfig);
                }
            }
        }
    }

    private void online(MediaServer mediaServer, AblServerConfig config) {
        if (config == null) {
            ABLResult ablResult = ablResTfulUtils.getServerConfig(mediaServer);
            JSONArray data = ablResult.getParams();
            if (data != null && !data.isEmpty()) {
                config = AblServerConfig.getInstance(data);
            }else {
                log.info("[ABL-Connection successful] Failed to read streaming configuration ID：{}, address： {}:{}", mediaServer.getId(), mediaServer.getIp(), mediaServer.getHttpPort());
                return;
            }
        }
        offlineABLPrimaryMap.remove(mediaServer.getId());
        offlineAblsecondaryMap.remove(mediaServer.getId());
        offlineAblTimeMap.remove(mediaServer.getId());
        log.info("[ABL-Connection successful] ID：{}, address： {}:{}", mediaServer.getId(), mediaServer.getIp(), mediaServer.getHttpPort());
        mediaServer.setStatus(true);
        mediaServer.setHookAliveInterval(10F);
        initPort(mediaServer, config);
        // Send online notification
        eventPublisher.mediaServerOnlineEventPublish(mediaServer);
        mediaServerService.update(mediaServer);
        // If two heartbeats are not received, zlm will be considered offline.
        String key = "ABL-keepalive-" + mediaServer.getId();
        dynamicTask.startDelay(key, ()->{
            log.warn("[ABL-Heartbeat timeout] ID：{}", mediaServer.getId());
            mediaServer.setStatus(false);
            offlineABLPrimaryMap.put(mediaServer.getId(), mediaServer);
            offlineAblTimeMap.put(mediaServer.getId(), System.currentTimeMillis());
            // TODO Send offline notification
            mediaServerService.update(mediaServer);
        }, (int)(mediaServer.getHookAliveInterval() * 2 * 1000));
    }
    private void initPort(MediaServer mediaServer, AblServerConfig ablServerConfig) {
        // The port will only be read from the configuration once. Once configured or read, it will no longer be configured.
        if (ablServerConfig.getRtmpPort() != null && mediaServer.getRtmpPort() != ablServerConfig.getRtmpPort()) {
            mediaServer.setRtmpPort(ablServerConfig.getRtmpPort());
        }
        if (ablServerConfig.getRtspPort() != null && mediaServer.getRtspPort() != ablServerConfig.getRtspPort()) {
            mediaServer.setRtspPort(ablServerConfig.getRtspPort());
        }
        if (ablServerConfig.getHttpFlvPort() != null && mediaServer.getFlvPort() != ablServerConfig.getHttpFlvPort()) {
            mediaServer.setFlvPort(ablServerConfig.getHttpFlvPort());
        }
        if (ablServerConfig.getHttpMp4Port() != null && mediaServer.getMp4Port() != ablServerConfig.getHttpMp4Port()) {
            mediaServer.setMp4Port(ablServerConfig.getHttpMp4Port());
        }
        if (ablServerConfig.getWsFlvPort() != null && mediaServer.getWsFlvPort() != ablServerConfig.getWsFlvPort()) {
            mediaServer.setWsFlvPort(ablServerConfig.getWsFlvPort());
        }
        if (ablServerConfig.getPsTsRecvPort() != null && mediaServer.getRtpProxyPort() != ablServerConfig.getPsTsRecvPort()) {
            mediaServer.setRtpProxyPort(ablServerConfig.getPsTsRecvPort());
        }
        if (ablServerConfig.getJtt1078RecvPort() != null && mediaServer.getRtpProxyPort() != ablServerConfig.getJtt1078RecvPort()) {
            mediaServer.setJttProxyPort(ablServerConfig.getJtt1078RecvPort());
        }
        mediaServer.setHookAliveInterval(10F);
    }

    public void setAblConfig(MediaServer mediaServerItem, boolean restart, AblServerConfig config) {
        try {
            if (config.getHookEnable() == 0) {
                log.info("[media service node-ABL]  Turn on HOOK function ：{}", mediaServerItem.getId());
                ABLResult ablResult = ablResTfulUtils.setConfigParamValue(mediaServerItem, "hook_enable", "1");
                if (ablResult.getCode() == 0) {
                    log.info("[media service node-ABL]  Turn on HOOK function successfully ：{}", mediaServerItem.getId());
                }else {
                    log.info("[media service node-ABL]  Failed to enable HOOK function ：{}->{}", mediaServerItem.getId(), ablResult.getMemo());
                }
            }
        }catch (Exception e) {
            log.info("[media service node-ABL]  Failed to enable HOOK function ：{}", mediaServerItem.getId(), e);
        }
        // settings relatedHOOK
        String[] hookUrlArray = {
                "on_stream_arrive",
                "on_stream_none_reader",
                "on_record_mp4",
                "on_stream_disconnect",
                "on_stream_not_found",
                "on_server_started",
                "on_publish",
                "on_play",
                "on_record_progress",
                "on_server_keepalive",
                "on_stream_not_arrive",
                "on_delete_record_mp4",
        };

        String protocol = sslEnabled ? "https" : "http";
        String hookPrefix = String.format("%s://%s:%s/index/hook/abl", protocol, mediaServerItem.getHookIp(), serverPort);
        Field[] fields = AblServerConfig.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                if (field.isAnnotationPresent(ConfigKeyId.class)) {
                    ConfigKeyId configKeyId = field.getAnnotation(ConfigKeyId.class);
                    for (String hook : hookUrlArray) {
                        if (configKeyId.value().equals(hook)) {
                            String hookUrl =  String.format("%s/%s", hookPrefix, hook);
                            field.setAccessible(true);
                            // Use reflection to obtain the value and compare it to see if it is the same as the configuration. If it is different, set it.
                            if (!hookUrl.equals(field.get(config))) {
                                ABLResult ablResult = ablResTfulUtils.setConfigParamValue(mediaServerItem, hook, hookUrl);
                                if (ablResult.getCode() == 0) {
                                    log.info("[media service node-ABL]  settingsHOOK {} success ：{}", hook, mediaServerItem.getId());
                                }else {
                                    log.info("[media service node-ABL]  settingsHOOK {} failed ：{}->{}", hook, mediaServerItem.getId(), ablResult.getMemo());
                                }
                            }
                        }
                    }
                }
            }catch (Exception e) {
                log.info("[media service node-ABL]  Failed to set HOOK ：{}", mediaServerItem.getId(), e);
            }
        }




//        Map<String, Object> param = new HashMap<>();
//        param.put("api.secret",mediaServerItem.getSecret()); // -profile:v Baseline
//        if (mediaServerItem.getRtspPort() != 0) {
//            param.put("ffmpeg.snap", "%s -rtsp_transport tcp -i %s -y -f mjpeg -frames:v 1 %s");
//        }
//        param.put("hook.enable","1");
//        param.put("hook.on_flow_report","");
//        param.put("hook.on_play",String.format("%s/on_play", hookPrefix));
//        param.put("hook.on_http_access","");
//        param.put("hook.on_publish", String.format("%s/on_publish", hookPrefix));
//        param.put("hook.on_record_ts","");
//        param.put("hook.on_rtsp_auth","");
//        param.put("hook.on_rtsp_realm","");
//        param.put("hook.on_server_started",String.format("%s/on_server_started", hookPrefix));
//        param.put("hook.on_shell_login","");
//        param.put("hook.on_stream_changed",String.format("%s/on_stream_changed", hookPrefix));
//        param.put("hook.on_stream_none_reader",String.format("%s/on_stream_none_reader", hookPrefix));
//        param.put("hook.on_stream_not_found",String.format("%s/on_stream_not_found", hookPrefix));
//        param.put("hook.on_server_keepalive",String.format("%s/on_server_keepalive", hookPrefix));
//        param.put("hook.on_send_rtp_stopped",String.format("%s/on_send_rtp_stopped", hookPrefix));
//        param.put("hook.on_rtp_server_timeout",String.format("%s/on_rtp_server_timeout", hookPrefix));
//        param.put("hook.on_record_mp4",String.format("%s/on_record_mp4", hookPrefix));
//        param.put("hook.timeoutSec","30");
//        param.put("hook.alive_interval", mediaServerItem.getHookAliveInterval());
//        // After the push stream is disconnected, you can reconnect within the timeout period to continue pushing the stream, so that the player will continue to play.。
//        // Set to 0 to disable this feature(Disconnection of the push stream will cause the player to be disconnected immediately)
//        // This parameter should not be greater than the player timeout
//        // Optimize this message to receive stream logout events faster
//        param.put("protocol.continue_push_ms", "3000" );
//        // The maximum waiting time for uninitialized tracks, in milliseconds. After the timeout, uninitialized tracks will be ignored. Set this option to optimize irregular streams with audio errors.，
//        // When zlm supports setting to turn off audio for each rtpServer, you can not set this option.
//        if (mediaServerItem.isRtpEnable() && !ObjectUtils.isEmpty(mediaServerItem.getRtpPortRange())) {
//            param.put("rtp_proxy.port_range", mediaServerItem.getRtpPortRange().replace(",", "-"));
//        }
//
//        if (!ObjectUtils.isEmpty(mediaServerItem.getRecordPath())) {
//            File recordPathFile = new File(mediaServerItem.getRecordPath());
//            param.put("protocol.mp4_save_path", recordPathFile.getParentFile().getPath());
//            param.put("protocol.downloadRoot", recordPathFile.getParentFile().getPath());
//            param.put("record.appName", recordPathFile.getName());
//        }
//
//        JSONObject responseJSON = ablResTfulUtils.setConfigParamValue(mediaServerItem, param);
//
//        if (responseJSON != null && responseJSON.getInteger("code") == 0) {
//            if (restart) {
//                log.info("[media service node] The setting is successful, restart to ensure that the configuration takes effect. {} -> {}:{}",
//                        mediaServerItem.getId(), mediaServerItem.getIp(), mediaServerItem.getHttpPort());
//                ablResTfulUtils.restartServer(mediaServerItem);
//            }else {
//                log.info("[media service node] Setup successful {} -> {}:{}",
//                        mediaServerItem.getId(), mediaServerItem.getIp(), mediaServerItem.getHttpPort());
//            }
//        }else {
//            log.info("[media service node] Failed to set up media service node {} -> {}:{}",
//                    mediaServerItem.getId(), mediaServerItem.getIp(), mediaServerItem.getHttpPort());
//        }
    }

}
