package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.channel.ChannelEvent;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.gb28181.session.CatalogDataManager;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.utils.Coordtransform;
import com.genersoft.iot.vmp.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sip.RequestEvent;
import javax.sip.header.FromHeader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * SIPCommand type: Directory request processing in NOTIFY requests
 */
@Slf4j
@Component
public class NotifyRequestForCatalogProcessor extends SIPRequestProcessorParent {

    private final ConcurrentLinkedQueue<NotifyCatalogChannel> channelList = new ConcurrentLinkedQueue<>();

	private final ConcurrentLinkedQueue<HandlerCatchData> taskQueue = new ConcurrentLinkedQueue<>();

	@Autowired
	private UserSetting userSetting;

	@Autowired
	private EventPublisher eventPublisher;

	@Autowired
	private IRedisCatchStorage redisCatchStorage;

	@Autowired
	private IDeviceChannelService deviceChannelService;

	@Autowired
	private IGbChannelService channelService;

	@Autowired
	private CatalogDataManager catalogDataManager;


	public void process(RequestEvent evt) {
		if (taskQueue.size() >= userSetting.getMaxNotifyCountQueue()) {
			log.error("[notify-directory subscription] The pending message queue is full {}，Returns 486 BUSY_HERE, the message is not processed", userSetting.getMaxNotifyCountQueue());
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
		for (HandlerCatchData take : handlerCatchDataList) {
			if (take == null) {
				continue;
			}
			RequestEvent evt = take.getEvt();
			try {
				FromHeader fromHeader = (FromHeader) evt.getRequest().getHeader(FromHeader.NAME);
				String deviceId = SipUtils.getUserIdFromFromHeader(fromHeader);

				Device device = redisCatchStorage.getDevice(deviceId);
				if (device == null || !device.isOnLine()) {
					log.warn("[Receive catalog subscription]：{}, But the device is offline", (device != null ? device.getDeviceId() : ""));
					continue;
				}
				Element rootElement = getRootElement(evt, device.getCharset());
				if (rootElement == null) {
					log.warn("[ Receive catalog subscription ] content cannot be null, {}", evt.getRequest());
					continue;
				}
				Element deviceListElement = rootElement.element("DeviceList");
				if (deviceListElement == null) {
					log.warn("[ Receive catalog subscription ] content cannot be null, {}", evt.getRequest());
					continue;
				}
				Iterator<Element> deviceListIterator = deviceListElement.elementIterator();
				if (deviceListIterator != null) {

					// TraverseDeviceList
					while (deviceListIterator.hasNext()) {
						Element itemDevice = deviceListIterator.next();
						CatalogChannelEvent catalogChannelEvent = null;
                        try {
                            catalogChannelEvent = CatalogChannelEvent.decode(itemDevice);
							if (catalogChannelEvent.getChannel() == null) {
								log.info("[parseCatalogChannelEvent]Success: However, parsing the channel information failed. The original text is as follows： \n{}", new String(evt.getRequest().getRawContent()));
								continue;
							}
							catalogChannelEvent.getChannel().setDataDeviceId(device.getId());
                            if (catalogChannelEvent.getChannel().getLongitude() != null
                                    && catalogChannelEvent.getChannel().getLatitude() != null
                                    && catalogChannelEvent.getChannel().getLongitude() > 0
                                    && catalogChannelEvent.getChannel().getLatitude() > 0) {
                               if (device.checkWgs84()) {
                                   catalogChannelEvent.getChannel().setGbLongitude(catalogChannelEvent.getChannel().getLongitude());
                                   catalogChannelEvent.getChannel().setGbLatitude(catalogChannelEvent.getChannel().getLatitude());
                               }else {
                                   Double[] wgs84Position = Coordtransform.GCJ02ToWGS84(catalogChannelEvent.getChannel().getLongitude(), catalogChannelEvent.getChannel().getLatitude());
                                   catalogChannelEvent.getChannel().setGbLongitude(wgs84Position[0]);
                                   catalogChannelEvent.getChannel().setGbLatitude(wgs84Position[1]);
                               }
                            }
                        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException |
                                 IllegalAccessException e) {
                            log.error("[parseCatalogChannelEvent]failed，", e);
                            log.error("[parseCatalogChannelEvent]Failure text: \n{}", new String(evt.getRequest().getRawContent(), Charset.forName(device.getCharset())));
							continue;
                        }
						if (log.isDebugEnabled()){
							log.debug("[Receive catalog subscription]：{}/{}-{}", device.getDeviceId(),
									catalogChannelEvent.getChannel().getDeviceId(), catalogChannelEvent.getEvent());
						}
						DeviceChannel channel = catalogChannelEvent.getChannel();
						switch (catalogChannelEvent.getEvent()) {
							case CatalogEvent.ON:
								// Go online
								log.info("[Receive channel online notification] from device: {}, channel {}", device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId());
								channel.setStatus("ON");
								channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.STATUS_CHANGED, channel, device.getDeviceId()));

								if (userSetting.getDeviceStatusNotify()) {
									// Send redis message
									redisCatchStorage.sendDeviceOrChannelStatus(device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId(), true);
								}
								break;
							case CatalogEvent.OFF:
								// Offline
								log.info("[Receive channel offline notification] from device: {}, channel {}", device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId());
								if (userSetting.getRefuseChannelStatusChannelFormNotify()) {
									log.info("[Receive channel offline notification] But the platform is configured to reject this message, coming from the device: {}, channel {}", device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId());
								} else {
									channel.setStatus("OFF");
									channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.STATUS_CHANGED, channel, device.getDeviceId()));
									if (userSetting.getDeviceStatusNotify()) {
										// Send redis message
										redisCatchStorage.sendDeviceOrChannelStatus(device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId(), false);
									}
								}
								break;
							case CatalogEvent.VLOST:
								// Video lost
								log.info("[Receive channel video loss notification] from device: {}, channel {}", device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId());
								if (userSetting.getRefuseChannelStatusChannelFormNotify()) {
									log.info("[Receive channel video loss notification] But the platform is configured to reject this message, coming from the device: {}, channel {}", device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId());
								} else {
									channel.setStatus("OFF");
									channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.STATUS_CHANGED, channel, device.getDeviceId()));

									if (userSetting.getDeviceStatusNotify()) {
										// Send redis message
										redisCatchStorage.sendDeviceOrChannelStatus(device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId(), false);
									}
								}
								break;
							case CatalogEvent.DEFECT:
								// Failure
								log.info("[Receive channel video failure notification] from device: {}, channel {}", device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId());
								if (userSetting.getRefuseChannelStatusChannelFormNotify()) {
									log.info("[Receive channel video failure notification] But the platform is configured to reject this message, coming from the device: {}, channel {}", device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId());
								} else {
									channel.setStatus("OFF");
									channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.STATUS_CHANGED, channel, device.getDeviceId()));

									if (userSetting.getDeviceStatusNotify()) {
										// Send redis message
										redisCatchStorage.sendDeviceOrChannelStatus(device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId(), false);
									}
								}
								break;
							case CatalogEvent.ADD:
								// increase
								log.info("[Receive notification of adding channel] from device: {}, channel {}", device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId());
								// Determine whether this channel exists
								DeviceChannel deviceChannel = deviceChannelService.getOneForSource(device.getId(), catalogChannelEvent.getChannel().getDeviceId());
								if (deviceChannel != null) {
									log.info("[Add channel] Already exists, no notification is sent, only updates are made, the device: {}, channel {}", device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId());
									channel.setId(deviceChannel.getId());
									channel.setHasAudio(deviceChannel.isHasAudio());
									channel.setUpdateTime(DateUtil.getNow());
									channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.UPDATE, channel, device.getDeviceId()));

								} else {
									catalogChannelEvent.getChannel().setUpdateTime(DateUtil.getNow());
									catalogChannelEvent.getChannel().setCreateTime(DateUtil.getNow());
									channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.ADD, channel, device.getDeviceId()));

									if (userSetting.getDeviceStatusNotify()) {
										// Send redis message
										redisCatchStorage.sendChannelAddOrDelete(device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId(), true);
									}
								}

								break;
							case CatalogEvent.DEL:
								// Delete
								log.info("[Receive channel deletion notification] from device: {}, channel {}", device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId());
								channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.DELETE, channel, device.getDeviceId()));

								if (userSetting.getDeviceStatusNotify()) {
									// Send redis message
									redisCatchStorage.sendChannelAddOrDelete(device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId(), false);
								}
								break;
							case CatalogEvent.UPDATE:
								// update
								log.info("[Receive update channel notification] from device: {}, channel {}", device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId());
								// Determine whether this channel exists
								DeviceChannel deviceChannelForUpdate = deviceChannelService.getOneForSource(device.getId(), catalogChannelEvent.getChannel().getDeviceId());
								if (deviceChannelForUpdate != null) {
									channel.setId(deviceChannelForUpdate.getId());
									channel.setHasAudio(deviceChannelForUpdate.isHasAudio());
									channel.setUpdateTime(DateUtil.getNow());
									channel.setUpdateTime(DateUtil.getNow());
									channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.UPDATE, channel, device.getDeviceId()));

								} else {
									catalogChannelEvent.getChannel().setCreateTime(DateUtil.getNow());
									catalogChannelEvent.getChannel().setUpdateTime(DateUtil.getNow());
									channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.ADD, channel, device.getDeviceId()));

									if (userSetting.getDeviceStatusNotify()) {
										// Send redis message
										redisCatchStorage.sendChannelAddOrDelete(device.getDeviceId(), catalogChannelEvent.getChannel().getDeviceId(), true);
									}
								}
								break;
							default:
								log.warn("[ NotifyCatalog ] event not found ： {}", catalogChannelEvent.getEvent());

						}
					}
				}

			} catch (DocumentException e) {
				log.error("unhandled exception ", e);
			}
		}
		if (!channelList.isEmpty()) {
			executeSave();
		}
	}

	@Transactional
	public void executeSave() {
		int size = channelList.size();
		List<NotifyCatalogChannel> channelListForSave = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			channelListForSave.add(channelList.poll());
		}

		Map<String, List<NotifyCatalogChannel>> grouped = new HashMap<>();
		for (NotifyCatalogChannel item : channelListForSave) {
			grouped.computeIfAbsent(item.getDeviceId(), k -> new ArrayList<>()).add(item);
		}

		for (Map.Entry<String, List<NotifyCatalogChannel>> entry : grouped.entrySet()) {
			if (catalogDataManager.isSyncing(entry.getKey())) {
				log.info("[NOTIFY] Equipment {} Synchronizing, skip this subscription notification", entry.getKey());
				continue;
			}
			for (NotifyCatalogChannel notifyCatalogChannel : entry.getValue()) {
					try {
						switch (notifyCatalogChannel.getType()) {
							case STATUS_CHANGED:
								deviceChannelService.updateChannelStatusForNotify(notifyCatalogChannel.getChannel());
								CommonGBChannel channelForStatus = channelService.queryCommonChannelByDeviceChannel(notifyCatalogChannel.getChannel());
								if ("ON".equals(notifyCatalogChannel.getChannel().getStatus()) ) {
									eventPublisher.channelEventPublish(channelForStatus, ChannelEvent.ChannelEventMessageType.ON);
								}else {
									eventPublisher.channelEventPublish(channelForStatus, ChannelEvent.ChannelEventMessageType.OFF);
								}
								break;
							case ADD:
								deviceChannelService.addChannel(notifyCatalogChannel.getChannel());
								CommonGBChannel channelForAdd = channelService.getOne(notifyCatalogChannel.getChannel().getId());
								eventPublisher.channelEventPublish(channelForAdd, ChannelEvent.ChannelEventMessageType.ADD);
								break;
							case UPDATE:
								CommonGBChannel oldCommonChannel = channelService.getOne(notifyCatalogChannel.getChannel().getId());
								deviceChannelService.updateChannelForNotify(notifyCatalogChannel.getChannel());
								CommonGBChannel channel = channelService.getOne(oldCommonChannel.getGbId());
								eventPublisher.channelEventPublishForUpdate(channel, oldCommonChannel);
								break;
							case DELETE:
								CommonGBChannel oldCommonChannelForDelete = channelService.queryCommonChannelByDeviceChannel(notifyCatalogChannel.getChannel());
								deviceChannelService.deleteForNotify(notifyCatalogChannel.getChannel());
								eventPublisher.channelEventPublish(oldCommonChannelForDelete, ChannelEvent.ChannelEventMessageType.DEL);
								break;
						}
					}catch (Exception e) {
						log.error("[Store received channel-Abnormal]Type：{}，No.：{}", notifyCatalogChannel.getType(),
								notifyCatalogChannel.getChannel().getDeviceId(), e);
					}
			}
		}
	}
}
