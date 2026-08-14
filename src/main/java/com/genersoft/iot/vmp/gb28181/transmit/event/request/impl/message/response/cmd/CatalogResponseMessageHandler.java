package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.cmd;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IGroupService;
import com.genersoft.iot.vmp.gb28181.service.IRegionService;
import com.genersoft.iot.vmp.gb28181.session.CatalogDataManager;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.ResponseMessageHandler;
import com.genersoft.iot.vmp.utils.Coordtransform;
import gov.nist.javax.sip.message.SIPRequest;

import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Directory Query Replies
 */
@Slf4j
@Component
public class CatalogResponseMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private final String cmdType = "Catalog";

    @Autowired
    private ResponseMessageHandler responseMessageHandler;

    @Autowired
    private IDeviceChannelService deviceChannelService;

    @Autowired
    private CatalogDataManager catalogDataCatch;

    @Autowired
    private IRegionService regionService;

    @Autowired
    private IGroupService groupService;

    @Autowired
    private SipConfig sipConfig;

    @Override
    public void afterPropertiesSet() throws Exception {
        responseMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element element) {
        // Reply200 OK
        try {
            responseAckAsync((SIPRequest) evt.getRequest(), Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[Command sending failed] Directory Query Reply: {}", e.getMessage());
        }

        int sn = 0;
        // Global exception capture to ensure that the next one can be handled
        try {
            Element rootElement = null;
            try {
                rootElement = getRootElement(evt, device.getCharset());
            } catch (DocumentException e) {
                log.error("[xmlparse] failed： ", e);
                return;
            }
            if (rootElement == null) {
                log.warn("[ receive channel ] content cannot be null, {}", evt.getRequest());
                return;
            }
            Element deviceListElement = rootElement.element("DeviceList");
            Element sumNumElement = rootElement.element("SumNum");
            Element snElement = rootElement.element("SN");

            sn = Integer.parseInt(snElement.getText());
            int sumNum = Integer.parseInt(sumNumElement.getText());

            if (sumNum == 0) {
                log.info("[receive channel]Equipment:{}of: 0", device.getDeviceId());
                // The data has been completely received
                deviceChannelService.cleanChannelsForDevice(device.getId());
                // Push empty data, otherwise it will not end in time
                catalogDataCatch.put(device.getDeviceId(), sn, 0, device,
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
                catalogDataCatch.setChannelSyncEnd(device.getDeviceId(), sn, null);
                return;
            } else {
                Iterator<Element> deviceListIterator = deviceListElement.elementIterator();
                if (deviceListIterator != null) {
                    List<DeviceChannel> channelList = new ArrayList<>();
                    List<Region> regionList = new ArrayList<>();
                    List<Group> groupList = new ArrayList<>();
                    // TraverseDeviceList
                    while (deviceListIterator.hasNext()) {
                        Element itemDevice = deviceListIterator.next();
                        Element channelDeviceElement = itemDevice.element("DeviceID");
                        if (channelDeviceElement == null) {
                            // Decrease the total by one to avoid incorrect final total and inability to determine the problem.
                            continue;
                        }
                        // Parse content from xml to DeviceChannel object
                        DeviceChannel channel = DeviceChannel.decode(itemDevice);
                        if (channel.getDeviceId() == null) {
                            log.info("[Receive catalog subscription]：But parsing failed {}", new String(evt.getRequest().getRawContent()));
                            continue;
                        }
                        channel.setDataDeviceId(device.getId());
                        if (channel.getParentId() != null && channel.getParentId().equals(sipConfig.getId())) {
                            channel.setParentId(null);
                        }
                        // Parse channel type
                        if (channel.getDeviceId().length() <= 8) {
                            // Administrative division
                            Region region = Region.getInstance(channel);
                            regionList.add(region);
                            channel.setChannelType(1);
                        }else if (channel.getDeviceId().length() == 20){
                            // business grouping/virtual organization
                            Group group = Group.getInstance(channel);
                            if (group != null) {
                                channel.setParental(1);
                                channel.setChannelType(2);
                                groupList.add(group);
                            }
                            if (channel.getLongitude() != null && channel.getLatitude() != null && channel.getLongitude() > 0 && channel.getLatitude() > 0) {
                                Double[] wgs84Position = Coordtransform.GCJ02ToWGS84(channel.getLongitude(), channel.getLatitude());
                                channel.setGbLongitude(wgs84Position[0]);
                                channel.setGbLatitude(wgs84Position[1]);
                            }
                        }
                        channelList.add(channel);
                    }

                    catalogDataCatch.put(device.getDeviceId(), sn, sumNum, device,
                            channelList, regionList, groupList);
                    log.info("[receive channel]Equipment: {} -> {}a，{}/{}", device.getDeviceId(), channelList.size(), catalogDataCatch.size(device.getDeviceId(), sn), sumNum);

                    if (catalogDataCatch.size(device.getDeviceId(), sn) > 0
                            && catalogDataCatch.size(device.getDeviceId(), sn) == catalogDataCatch.sumNum(device.getDeviceId(), sn)) {
                        ReentrantLock lock = catalogDataCatch.getDeviceWriteLock(device.getDeviceId());
                        if (!lock.tryLock()) {
                            log.info("[sync channel] Equipment {} Incoming to database, skip repeated writing", device.getDeviceId());
                            return;
                        }
                        try {
                            if (catalogDataCatch.isEnd(device.getDeviceId(), sn)) {
                                return;
                            }
                            List<DeviceChannel> channels = catalogDataCatch.getDeviceChannelList(device.getDeviceId(), sn);
                            if (!channels.isEmpty()) {
                                deviceChannelService.resetChannels(device.getId(), channels);
                            }
                            List<Region> regions = catalogDataCatch.getRegionList(device.getDeviceId(), sn);
                            if (regions != null && !regions.isEmpty()) {
                                regionService.batchAdd(regions);
                            }
                            List<Group> groups = catalogDataCatch.getGroupList(device.getDeviceId(), sn);
                            if (groups != null && !groups.isEmpty()) {
                                groupService.batchAdd(groups);
                            }
                            catalogDataCatch.setChannelSyncEnd(device.getDeviceId(), sn, null);
                        } catch (Exception e) {
                            log.warn("[sync channel] If the direct storage fails, the timer will take care of the problem.", e);
                            catalogDataCatch.setComplete(device.getDeviceId(), sn);
                        } finally {
                            lock.unlock();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[receive channel] Unhandled exception found, \r\n{}", evt.getRequest());
            log.error("[receive channel] Unusual content： ", e);
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, Platform parentPlatform, Element rootElement) {

    }

    public SyncStatus getChannelSyncProgress(String deviceId) {
        return catalogDataCatch.getSyncStatus(deviceId);
    }

    public boolean isSyncRunning(String deviceId) {
        return catalogDataCatch.isSyncRunning(deviceId);
    }

    public void setChannelSyncReady(Device device, int sn) {
        catalogDataCatch.addReady(device, sn);
    }

    public void setChannelSyncEnd(String deviceId, int sn, String errorMsg) {
        catalogDataCatch.setChannelSyncEnd(deviceId, sn, errorMsg);
    }
}
