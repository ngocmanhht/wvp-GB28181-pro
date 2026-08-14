package com.genersoft.iot.vmp.media;

import com.genersoft.iot.vmp.conf.MediaConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaServerChangeEvent;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Startup is to load node information from the configuration file and send node status management to control the node status.
 */
@Slf4j
@Component
public class MediaServerConfig{

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private MediaConfig mediaConfig;

    @Autowired
    private UserSetting userSetting;


    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(){
        // Clear the cache information of all online nodes
        mediaServerService.clearMediaServerForOnline();
        MediaServer mediaSerItemInConfig = mediaConfig.buildMediaSer();
        mediaSerItemInConfig.setServerId(userSetting.getServerId());
        mediaServerService.deleteDefault();
        // Send media node change event
        mediaServerService.syncCatchFromDatabase();
        // Get all zlms and enable active connections
        List<MediaServer> all = mediaServerService.getAllFromDatabaseWithOutDefault();
        all.add(mediaSerItemInConfig);
        log.info("[media node] Load node list, total{}nodes", all.size());
        MediaServerChangeEvent event = new MediaServerChangeEvent(this);
        event.setMediaServerItemList(all);
        applicationEventPublisher.publishEvent(event);
    }
}
