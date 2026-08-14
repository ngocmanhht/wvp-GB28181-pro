package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;
import javax.sip.header.FromHeader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * SIPCommand type: Alarm notification request processing in NOTIFY request
 */
@Slf4j
@Component
public class NotifyRequestForAlarm extends SIPRequestProcessorParent {

	private final ConcurrentLinkedQueue<HandlerCatchData> taskQueue = new ConcurrentLinkedQueue<>();

	@Autowired
	private UserSetting userSetting;

	@Autowired
	private EventPublisher eventPublisher;

	@Autowired
	private IRedisCatchStorage redisCatchStorage;

	@Autowired
	private IDeviceChannelService deviceChannelService;


	public void process(RequestEvent evt) {
		if (taskQueue.size() >= userSetting.getMaxNotifyCountQueue()) {
			log.error("[notify-Alarm subscription] The pending message queue is full {}，Return486 BUSY_HERE", userSetting.getMaxNotifyCountQueue());
			return;
		}
		taskQueue.offer(new HandlerCatchData(evt, null, null));
	}

	@Scheduled(fixedDelay = 400)   //Executed every 400 milliseconds
	@Async
	public void executeTaskQueue(){
		if (taskQueue.isEmpty()) {
			return;
		}
		List<HandlerCatchData> handlerCatchDataList = new ArrayList<>();
		int size = taskQueue.size();
		for (int i = 0; i < size; i++) {
			HandlerCatchData poll = taskQueue.poll();
			if (poll != null) {
				handlerCatchDataList.add(poll);
			}
		}
		if (handlerCatchDataList.isEmpty()) {
			return;
		}
		List<DeviceAlarmNotify> deviceAlarmList = new ArrayList<>();
		for (HandlerCatchData take : handlerCatchDataList) {
			if (take == null) {
				continue;
			}
			RequestEvent evt = take.getEvt();
			try {
				FromHeader fromHeader = (FromHeader) evt.getRequest().getHeader(FromHeader.NAME);
				String deviceId = SipUtils.getUserIdFromFromHeader(fromHeader);

				Element rootElement = getRootElement(evt);
				if (rootElement == null) {
					log.error("The message body was not obtained when processing alarm device alarm Notify.{}", evt.getRequest());
					return;
				}
				String channelId = rootElement.elementText("DeviceID");

				Device device = redisCatchStorage.getDevice(deviceId);
				if (device == null) {
					log.warn("[ NotifyAlarm ] Device not found：{}", deviceId);
					return;
				}
				rootElement = getRootElement(evt, device.getCharset());
				if (rootElement == null) {
					log.warn("[ NotifyAlarm ] content cannot be null, {}", evt.getRequest());
					return;
				}
				DeviceAlarmNotify deviceAlarmNotify = DeviceAlarmNotify.fromXml(rootElement);
				deviceAlarmNotify.setDeviceId(deviceId);
				deviceAlarmNotify.setDeviceName(device.getName());
				log.info("[receivedNotify-Alarm]：{}/{}", device.getDeviceId(), deviceAlarmNotify.getChannelId());
				if (deviceAlarmNotify.getAlarmMethod() != null && deviceAlarmNotify.getAlarmMethod() == DeviceAlarmMethod.GPS.getVal()) { // GPSAlarm
					DeviceChannel deviceChannel = deviceChannelService.getOne(device.getDeviceId(), channelId);
					if (deviceChannel == null) {
						log.warn("[Parse alarm notifications] Channel not found：{}/{}", device.getDeviceId(), channelId);
					}else {
						MobilePosition mobilePosition = new MobilePosition();
						mobilePosition.setChannelId(deviceChannel.getId());
						mobilePosition.setChannelDeviceId(deviceChannel.getDeviceId());
						mobilePosition.setCreateTime(DateUtil.getNow());
						mobilePosition.setTimestamp(DateUtil.yyyy_MM_dd_HH_mm_ssToTimestampMs(deviceAlarmNotify.getAlarmTime()));
						mobilePosition.setLongitude(deviceAlarmNotify.getLongitude());
						mobilePosition.setLatitude(deviceAlarmNotify.getLatitude());

						// Update the latitude and longitude of the device channel
						deviceChannel.setLongitude(mobilePosition.getLongitude());
						deviceChannel.setLatitude(mobilePosition.getLatitude());
						deviceChannel.setGpsTime(deviceAlarmNotify.getAlarmTime());

						deviceChannelService.updateChannelGPS(device, deviceChannel, mobilePosition);
					}
				}

				// Reply200 OK
				if (redisCatchStorage.deviceIsOnline(deviceId)) {
					deviceAlarmList.add(deviceAlarmNotify);
				}

			} catch (DocumentException e) {
				log.error("unhandled exception ", e);
			}
		}
		if (deviceAlarmList.isEmpty()) {
			return;
		}
		eventPublisher.deviceAlarmEventPublish(deviceAlarmList);
	}
}
