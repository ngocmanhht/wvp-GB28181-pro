package com.genersoft.iot.vmp.gb28181.event;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.DeviceAlarmNotify;
import com.genersoft.iot.vmp.gb28181.bean.MobilePosition;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.event.alarm.DeviceAlarmEvent;
import com.genersoft.iot.vmp.gb28181.event.channel.ChannelEvent;
import com.genersoft.iot.vmp.gb28181.event.device.DeviceOfflineEvent;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.gb28181.event.subscribe.mobilePosition.MobilePositionEvent;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaServerOfflineEvent;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaServerOnlineEvent;
import com.genersoft.iot.vmp.service.redisMsg.IRedisRpcService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @description:EventEvent notification pusher, supports pushing online events and offline events
 * @author: swwheihei
 * @date:   2020May 6, 2019, morning11:30:50
 */
@Slf4j
@Component
public class EventPublisher {

	@Autowired
    private ApplicationEventPublisher applicationEventPublisher;

	@Autowired
	private UserSetting userSetting;

	@Autowired
	private IRedisRpcService redisRpcService;

	/**
	 * Device alarm event
	 */
	public void deviceAlarmEventPublish(List<DeviceAlarmNotify> deviceAlarmList) {
		DeviceAlarmEvent alarmEvent = new DeviceAlarmEvent(this);
		alarmEvent.setDeviceAlarmList(deviceAlarmList);
		applicationEventPublisher.publishEvent(alarmEvent);
	}

	public void mediaServerOfflineEventPublish(MediaServer mediaServer){
		MediaServerOfflineEvent outEvent = new MediaServerOfflineEvent(this);
		outEvent.setMediaServer(mediaServer);
		applicationEventPublisher.publishEvent(outEvent);
	}

	public void mediaServerOnlineEventPublish(MediaServer mediaServer) {
		MediaServerOnlineEvent outEvent = new MediaServerOnlineEvent(this);
		outEvent.setMediaServer(mediaServer);
		applicationEventPublisher.publishEvent(outEvent);
	}

	public void channelEventPublish(CommonGBChannel commonGBChannel, ChannelEvent.ChannelEventMessageType type) {
        channelEventPublish(Collections.singletonList(commonGBChannel), type);
	}

	public void channelEventPublishForUpdate(CommonGBChannel commonGBChannel, CommonGBChannel deviceChannelForOld) {
        log.info("[Channel changes internal distribution-update] {}", commonGBChannel.getGbDeviceId());
        ChannelEvent channelEvent = ChannelEvent.getInstanceForUpdate(this, Collections.singletonList(commonGBChannel), Collections.singletonList(deviceChannelForOld));
        applicationEventPublisher.publishEvent(channelEvent);
	}

	public void channelEventPublishForUpdate(List<CommonGBChannel> channelList, List<CommonGBChannel> channelListForOld) {
        log.info("[Channel changes internal distribution-update] Quantity： {}", channelList.size());
        ChannelEvent channelEvent = ChannelEvent.getInstanceForUpdate(this, channelList, channelListForOld);
        applicationEventPublisher.publishEvent(channelEvent);
	}

    public void channelEventPublish(List<CommonGBChannel> channelList, ChannelEvent.ChannelEventMessageType type) {
        log.info("[Channel changes internal distribution-{}] Quantity： {}", type, channelList.size());
		ChannelEvent channelEvent = ChannelEvent.getInstance(this, type, channelList);
		applicationEventPublisher.publishEvent(channelEvent);
	}

	public void catalogEventPublish(Platform platform, CommonGBChannel deviceChannel, String type) {
		catalogEventPublish(platform, Collections.singletonList(deviceChannel), type);
	}
	public void catalogEventPublish(Platform platform, List<CommonGBChannel> deviceChannels, String type) {
		if (platform != null && !userSetting.getServerId().equals(platform.getServerId())) {
			log.info("[National standard cascade] Directory status push, this upper-level platform is processed by other services, the message has been ignored");
			return;
		}
		CatalogEvent outEvent = new CatalogEvent(this);
		List<CommonGBChannel> channels = new ArrayList<>();
		if (deviceChannels.size() > 1) {
			// Data deduplication
			Set<String> gbIdSet = new HashSet<>();
			for (CommonGBChannel deviceChannel : deviceChannels) {
				if (deviceChannel != null && deviceChannel.getGbDeviceId() != null && !gbIdSet.contains(deviceChannel.getGbDeviceId())) {
					gbIdSet.add(deviceChannel.getGbDeviceId());
					channels.add(deviceChannel);
				}
			}
		}else {
			channels = deviceChannels;
		}
		outEvent.setChannels(channels);
		outEvent.setType(type);
		if (platform != null) {
			outEvent.setPlatform(platform);
		}
		applicationEventPublisher.publishEvent(outEvent);

	}

	public void mobilePositionEventPublish(MobilePosition mobilePosition) {
		MobilePositionEvent event = new MobilePositionEvent(this);
		event.setMobilePositionList(List.of(mobilePosition));
		applicationEventPublisher.publishEvent(event);
	}

	public void mobilePositionsEventPublish(List<? extends MobilePosition> mobilePositionList) {
		MobilePositionEvent event = new MobilePositionEvent(this);
		event.setMobilePositionList(mobilePositionList);
		applicationEventPublisher.publishEvent(event);
	}


    public void deviceOfflineEventPublish(Set<String> deviceIds) {
		DeviceOfflineEvent event = new DeviceOfflineEvent(this);
		event.setDeviceIds(deviceIds);
		applicationEventPublisher.publishEvent(event);
    }


}
