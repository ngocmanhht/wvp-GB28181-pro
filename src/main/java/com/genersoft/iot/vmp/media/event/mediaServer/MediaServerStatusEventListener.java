package com.genersoft.iot.vmp.media.event.mediaServer;

import com.genersoft.iot.vmp.gb28181.service.IPlayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


/**
 * @description: Online event listener, after monitoring that the device is offline, modify the device's offline status. There are two sources for device online：
 *               1、The device actively logs out and sends a logout command.
 *               2、The device is offline for unknown reasons and the heartbeat times out.
 * @author: swwheihei
 * @date: 2020May 6, 2018, afternoon1:51:23
 */
@Slf4j
@Component
public class MediaServerStatusEventListener {
	
	@Autowired
	private IPlayService playService;

	@Async
	@EventListener
	public void onApplicationEvent(MediaServerOnlineEvent event) {
		log.info("[media node] Go online ID：" + event.getMediaServer().getId());
		playService.zlmServerOnline(event.getMediaServer());
	}

	@Async
	@EventListener
	public void onApplicationEvent(MediaServerOfflineEvent event) {

		log.info("[media node] Offline，ID：" + event.getMediaServer().getId());
		// Handling ZLM offline
		playService.zlmServerOffline(event.getMediaServer());
	}
}
