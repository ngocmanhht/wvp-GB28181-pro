package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.common.enums.DeviceControlType;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.dao.DeviceChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.DeviceMapper;
import com.genersoft.iot.vmp.gb28181.dao.MobilePositionMapper;
import com.genersoft.iot.vmp.gb28181.dao.PlatformChannelMapper;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.record.RecordInfoEndEvent;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IInviteStreamService;
import com.genersoft.iot.vmp.gb28181.service.IPlatformChannelService;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.redisMsg.IRedisRpcPlayService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.utils.Coordtransform;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import com.genersoft.iot.vmp.web.gb28181.dto.DeviceChannelExtend;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.getText;

/**
 * @author lin
 */
@Slf4j
@Service
public class DeviceChannelServiceImpl implements IDeviceChannelService {

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private IInviteStreamService inviteStreamService;

    @Autowired
    private DeviceChannelMapper channelMapper;

    @Autowired
    private PlatformChannelMapper platformChannelMapper;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private MobilePositionMapper deviceMobilePositionMapper;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IPlatformChannelService platformChannelService;

    @Autowired
    private IRedisRpcPlayService redisRpcPlayService;

    @Autowired
    private ISIPCommander commander;

    // Waiting for the results of recording video query
    private final Map<String, SynchronousQueue<RecordInfo>> topicSubscribers = new ConcurrentHashMap<>();

    /**
     * Monitor the recording query end event
     */
    @Async
    @EventListener
    public void onApplicationEvent(RecordInfoEndEvent event) {
        SynchronousQueue<RecordInfo> queue = topicSubscribers.get("record" + event.getRecordInfo().getSn());
        if (queue != null) {
            queue.offer(event.getRecordInfo());
        }
    }



    @Override
    public int updateChannels(Device device, List<DeviceChannel> channels) {
        if (CollectionUtils.isEmpty(channels)) {
            return 0;
        }
        // Add ginseng and remove weight
        Set<String> dedupSet = new HashSet<>();
        List<DeviceChannel> uniqueChannels = new ArrayList<>();
        for (DeviceChannel ch : channels) {
            if (dedupSet.add(ch.getDeviceId())) {
                uniqueChannels.add(ch);
            }
        }
        List<DeviceChannel> upsertChannels = new ArrayList<>();
        int result = 0;
        List<DeviceChannel> channelList = channelMapper.queryChannelsByDeviceDbId(device.getId());
        if (channelList.isEmpty()) {
            String now = DateUtil.getNow();
            for (DeviceChannel channel : uniqueChannels) {
                channel.setDataDeviceId(device.getId());
                InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
                if (inviteInfo != null && inviteInfo.getStreamInfo() != null) {
                    channel.setStreamId(inviteInfo.getStreamInfo().getStream());
                }
                channel.setUpdateTime(now);
                channel.setCreateTime(now);
                upsertChannels.add(channel);
            }
        } else {
            HashMap<String, DeviceChannel> channelsInStore = new HashMap<>();
            for (DeviceChannel deviceChannel : channelList) {
                channelsInStore.put(deviceChannel.getDataDeviceId() + deviceChannel.getDeviceId(), deviceChannel);
            }
            String now = DateUtil.getNow();
            for (DeviceChannel channel : uniqueChannels) {
                InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
                if (inviteInfo != null && inviteInfo.getStreamInfo() != null) {
                    channel.setStreamId(inviteInfo.getStreamInfo().getStream());
                }
                channel.setUpdateTime(now);
                DeviceChannel deviceChannelInDb = channelsInStore.get(channel.getDataDeviceId() + channel.getDeviceId());
                if (deviceChannelInDb != null) {
                    channel.setId(deviceChannelInDb.getId());
                    channel.setCreateTime(deviceChannelInDb.getCreateTime());
                } else {
                    channel.setCreateTime(now);
                }
                upsertChannels.add(channel);
            }
        }

        int limitCount = 500;
        if (!upsertChannels.isEmpty()) {
            for (int i = 0; i < upsertChannels.size(); i += limitCount) {
                int end = Math.min(i + limitCount, upsertChannels.size());
                List<DeviceChannel> batchList = upsertChannels.subList(i, end);
                result += channelMapper.batchUpsert(batchList);
            }
        }
        return result;
    }

    @Override
    public ResourceBaseInfo getOverview() {
        int online = channelMapper.getOnlineCount();
        int total = channelMapper.getAllChannelCount();
        return new ResourceBaseInfo(total, online);
    }

    @Override
    public void online(DeviceChannel channel) {
        channelMapper.online(channel.getId());
    }

    @Override
    public void offline(DeviceChannel channel) {
        channelMapper.offline(channel.getId());
    }

    @Override
    public void deleteForNotify(DeviceChannel channel) {
        channelMapper.deleteForNotify(channel);
    }

    @Override
    public DeviceChannel getOne(String deviceId, String channelId){
        Device device = deviceMapper.getDeviceByDeviceId(deviceId);
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Device not found：" + deviceId);
        }
        return channelMapper.getOneByDeviceId(device.getId(), channelId);
    }

    @Override
    public DeviceChannel getOneForSource(String deviceId, String channelId){
        Device device = deviceMapper.getDeviceByDeviceId(deviceId);
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Device not found：" + deviceId);
        }
        return channelMapper.getOneByDeviceIdForSource(device.getId(), channelId);
    }

    @Override
    public DeviceChannel getOneForSource(int deviceDbId, String channelId) {
        return channelMapper.getOneByDeviceIdForSource(deviceDbId, channelId);
    }

    @Override
    public DeviceChannel getOneBySourceId(int deviceDbId, String channelId) {
        return channelMapper.getOneBySourceChannelId(deviceDbId, channelId);
    }

    @Override
    public void updateChannelStreamIdentification(DeviceChannel channel) {
        Assert.hasLength(channel.getStreamIdentification(), "Code stream identifier must exist");
        if (ObjectUtils.isEmpty(channel.getStreamIdentification())) {
            log.info("[Reset channel stream type] Equipment: {}, Code stream： {}", channel.getDeviceId(), channel.getStreamIdentification());
        }else {
            log.info("[Update channel code stream type] Equipment: {}, channel：{}， Code stream： {}", channel.getDeviceId(), channel.getDeviceId(),
                    channel.getStreamIdentification());
        }
        if (channel.getId() > 0) {
            channelMapper.updateChannelStreamIdentification(channel);
        }else {
            channelMapper.updateAllChannelStreamIdentification(channel.getStreamIdentification());
        }
    }

    @Override
    public List<DeviceChannel> queryChaneListByDeviceId(String deviceId) {
        Device device = deviceMapper.getDeviceByDeviceId(deviceId);
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Channel not found：" + deviceId);
        }
        return channelMapper.queryChannelsByDeviceDbId(device.getId());
    }

    @Override
    public List<Integer> queryChaneIdListByDeviceDbIds(List<Integer> deviceDbIds) {
        return channelMapper.queryChaneIdListByDeviceDbIds(deviceDbIds);
    }

    @Override
    public void handlePtzCmd(@NotNull Integer dataDeviceId, @NotNull Integer gbId, Element rootElement, DeviceControlType type, ErrorCallback<String> callback) {

        // Get the device according to the channel ID
        Device device = deviceMapper.query(dataDeviceId);
        if (device == null) {
            // Reply if it does not exist404
            log.warn("[INFO news] The device to which the channel belongs does not exist, deviceID： {}", dataDeviceId);
            callback.run(Response.NOT_FOUND, "device  not found", null);
            return;
        }

        DeviceChannel deviceChannel = channelMapper.getOneForSource(gbId);
        if (deviceChannel == null) {
            log.warn("[deviceControl] Device raw channel not found, device： {}（{}），Channel number：{}", device.getName(),
                    device.getDeviceId(), gbId);
            callback.run(Response.NOT_FOUND, "channel  not found", null);
            return;
        }
        log.info("[deviceControl] command: {}, Equipment： {}（{}）， channel{}（{}", type,  device.getName(), device.getDeviceId(),
                deviceChannel.getName(), deviceChannel.getDeviceId());
        String cmdString = getText(rootElement, type.getVal());
        try {
            commander.fronEndCmd(device, deviceChannel.getDeviceId(), cmdString, errorResult->{
                        callback.run(errorResult.statusCode, errorResult.msg, null);
                    }, errorResult->{
                        callback.run(errorResult.statusCode, errorResult.msg, null);
                    });
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[Command sending failed] PTZ/front end: {}", e.getMessage());
        }
    }

    @Override
    public void updateChannelGPS(Device device, DeviceChannel deviceChannel, MobilePosition mobilePosition) {

        if (device.getGeoCoordSys().equalsIgnoreCase("GCJ02")) {
            Double[] wgs84Position = Coordtransform.GCJ02ToWGS84(mobilePosition.getLongitude(), mobilePosition.getLatitude());
            mobilePosition.setLongitude(wgs84Position[0]);
            mobilePosition.setLatitude(wgs84Position[1]);

            Double[] wgs84PositionForChannel = Coordtransform.GCJ02ToWGS84(deviceChannel.getLongitude(), deviceChannel.getLatitude());
            deviceChannel.setGbLongitude(wgs84PositionForChannel[0]);
            deviceChannel.setGbLatitude(wgs84PositionForChannel[1]);
        }

        if (userSetting.getSavePositionHistory()) {
            deviceMobilePositionMapper.insertNewPosition(mobilePosition);
        }

        if (deviceChannel.getDeviceId().equals(device.getDeviceId())) {
            deviceChannel.setDeviceId(null);
        }
        if (deviceChannel.getGpsTime() == null) {
            deviceChannel.setGpsTime(DateUtil.getNow());
        }

        int updated = channelMapper.updatePosition(deviceChannel);
        if (updated == 0) {
            return;
        }

        List<DeviceChannel> deviceChannels = new ArrayList<>();
        if (deviceChannel.getDeviceId() == null) {
            // The deviceId reported here by some devices is the same as the channel ID. In this case, all channels under the device are updated.
            List<DeviceChannel> deviceChannelsInDb = queryChaneListByDeviceId(device.getDeviceId());
            deviceChannels.addAll(deviceChannelsInDb);
        }else {
            deviceChannels.add(deviceChannel);
        }
        if (deviceChannels.isEmpty()) {
            return;
        }
        if (deviceChannels.size() > 100) {
            log.warn("[Send notification after updating channel location information] The device may be a platform, and the reported location information does not indicate the channel number.，" +
                    "Causes all channels to be updated in position， deviceId:{}", device.getDeviceId());
        }
        for (DeviceChannel channel : deviceChannels) {
            // Send a mobile location subscription message to the upper-level platform that is associated with the channel and has enabled mobile location subscription.
            mobilePosition.setChannelId(channel.getId());
            mobilePosition.setChannelDeviceId(channel.getDeviceId());
            try {
                eventPublisher.mobilePositionEventPublish(mobilePosition);
            }catch (Exception e) {
                log.error("[Failed to forward mobile location to superior] ", e);
            }
        }
    }

    @Override
    public void startPlay(Integer channelId, String stream) {
        channelMapper.startPlay(channelId, stream);
    }

    @Override
    public void stopPlay(Integer channelId) {
        channelMapper.stopPlayById(channelId);
    }

    @Override
    public void cleanChannelsForDevice(int deviceId) {
        channelMapper.cleanChannelsByDeviceId(deviceId);
    }

    @Override
    @Transactional
    public boolean resetChannels(int deviceDbId, List<DeviceChannel> deviceChannelList) {
        if (CollectionUtils.isEmpty(deviceChannelList)) {
            return false;
        }
        List<DeviceChannel> allChannels = channelMapper.queryAllChannelsForRefresh(deviceDbId);
        Map<String,DeviceChannel> allChannelMap = new HashMap<>();
        if (!allChannels.isEmpty()) {
            for (DeviceChannel deviceChannel : allChannels) {
                allChannelMap.put(deviceChannel.getDataDeviceId() + deviceChannel.getDeviceId(), deviceChannel);
            }
        }
        // Add ginseng and remove weight
        Set<String> dedupSet = new HashSet<>();
        List<DeviceChannel> uniqueChannels = new ArrayList<>();
        for (DeviceChannel ch : deviceChannelList) {
            if (dedupSet.add(ch.getDeviceId())) {
                uniqueChannels.add(ch);
            }
        }

        List<DeviceChannel> upsertChannels = new ArrayList<>();
        List<DeviceChannel> deleteChannels = new ArrayList<>();
        Map<String, Integer> subContMap = new HashMap<>();

        for (DeviceChannel deviceChannel : uniqueChannels) {
            DeviceChannel channelInDb = allChannelMap.get(deviceChannel.getDataDeviceId() + deviceChannel.getDeviceId());
            if (channelInDb != null) {
                deviceChannel.setStreamId(channelInDb.getStreamId());
                deviceChannel.setHasAudio(channelInDb.isHasAudio());
                deviceChannel.setId(channelInDb.getId());
                deviceChannel.setCreateTime(channelInDb.getCreateTime());
                if (channelInDb.getStatus() != null && !channelInDb.getStatus().equalsIgnoreCase(deviceChannel.getStatus())){
                    List<Platform> platformList = platformChannelMapper.queryParentPlatformByChannelId(deviceChannel.getDeviceId());
                    if (!CollectionUtils.isEmpty(platformList)){
                        platformList.forEach(platform->{
                            eventPublisher.catalogEventPublish(platform, deviceChannel.buildCommonGBChannelForStatus(), deviceChannel.getStatus().equals("ON")? CatalogEvent.ON:CatalogEvent.OFF);
                        });
                    }
                }
                deviceChannel.setUpdateTime(DateUtil.getNow());
            } else {
                deviceChannel.setCreateTime(DateUtil.getNow());
                deviceChannel.setUpdateTime(DateUtil.getNow());
            }
            allChannelMap.remove(deviceChannel.getDataDeviceId() + deviceChannel.getDeviceId());
            upsertChannels.add(deviceChannel);
            if (!ObjectUtils.isEmpty(deviceChannel.getParentId())) {
                subContMap.merge(deviceChannel.getParentId(), 1, Integer::sum);
            }
        }
        deleteChannels.addAll(allChannelMap.values());

        for (DeviceChannel channel : upsertChannels) {
            if (subContMap.get(channel.getDeviceId()) != null){
                Integer count = subContMap.get(channel.getDeviceId());
                if (count > 0) {
                    channel.setSubCount(count);
                    channel.setParental(1);
                }
            }
        }

        if(CollectionUtils.isEmpty(upsertChannels)){
            log.info("Channel reset, data is empty={}" , deviceChannelList);
            return false;
        }
        int limitCount = 500;
        if (!upsertChannels.isEmpty()) {
            for (int i = 0; i < upsertChannels.size(); i += limitCount) {
                int end = Math.min(i + limitCount, upsertChannels.size());
                List<DeviceChannel> batchList = upsertChannels.subList(i, end);
                channelMapper.batchUpsert(batchList);
            }
        }
        if (!deleteChannels.isEmpty()) {
            try {
                // These channels may be associated, and the upper-level platform needs to delete them and send messages at the same time.
                List<Integer> ids = new ArrayList<>();
                deleteChannels.stream().forEach(deviceChannel -> {
                    ids.add(deviceChannel.getId());
                });
                platformChannelService.removeChannels(ids);
            }catch (Exception e) {
                log.error("[Failed to remove channel national standard cascade sharing]", e);
            }
            if (deleteChannels.size() > limitCount) {
                for (int i = 0; i < deleteChannels.size(); i += limitCount) {
                    int toIndex = i + limitCount;
                    if (i + limitCount > deleteChannels.size()) {
                        toIndex = deleteChannels.size();
                    }
                    channelMapper.batchDel(deleteChannels.subList(i, toIndex));
                }
            }else {
                channelMapper.batchDel(deleteChannels);
            }
        }
        return true;

    }

    @Override
    public PageInfo<DeviceChannel> getSubChannels(int deviceDbId, String channelId, String query, Boolean channelType, Boolean online, int page, int count) {
        PageHelper.startPage(page, count);
        String civilCode = null;
        String parentId = null;
        String businessGroupId = null;
        if (channelId.length() <= 8) {
            civilCode = channelId;
        }else {
            GbCode decode = GbCode.decode(channelId);
            if (Integer.parseInt(decode.getTypeCode()) == 215) {
                businessGroupId = channelId;
            }else {
                parentId = channelId;
            }
        }
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }
        List<DeviceChannel> all = channelMapper.queryChannels(deviceDbId, civilCode, businessGroupId, parentId, query, false, channelType, online, null, null);
        return new PageInfo<>(all);
    }

    @Override
    public List<DeviceChannelExtend> queryChannelExtendsByDeviceId(String deviceId, List<String> channelIds, Boolean online) {
        return channelMapper.queryChannelsWithDeviceInfo(deviceId, null,null, null, online,channelIds);
    }

    @Override
    public PageInfo queryChannelsByDeviceId(String deviceId, String query, Boolean hasSubChannel, Boolean online, int page, int count) {
        Device device = deviceMapper.getDeviceByDeviceId(deviceId);
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Device not found：" + deviceId);
        }
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }
        PageHelper.startPage(page, count);
        List<DeviceChannel> all = channelMapper.queryChannels(device.getId(), null, null, null, query, false, hasSubChannel, online, null, null);
        return new PageInfo<>(all);
    }

    @Override
    public PageInfo<DeviceChannel> queryChannels(String query, Boolean queryParent, Boolean hasSubChannel, Boolean online, Boolean hasStream, int page, int count) {
        PageHelper.startPage(page, count);
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }
        List<DeviceChannel> all = channelMapper.queryChannels(null, null, null, null, query, queryParent, hasSubChannel, online, null, hasStream);
        return new PageInfo<>(all);
    }

    @Override
    public List<Device> queryDeviceWithAsMessageChannel() {
        return deviceMapper.queryDeviceWithAsMessageChannel();
    }

    @Override
    public DeviceChannel getRawChannel(int id) {
        return deviceMapper.getRawChannel(id);
    }

    @Override
    public DeviceChannel getOneById(Integer channelId) {
        return channelMapper.getOne(channelId);
    }

    @Override
    public DeviceChannel getOneForSourceById(Integer channelId) {
        return channelMapper.getOneForSource(channelId);
    }

    @Override
    public DeviceChannel getBroadcastChannel(int deviceDbId) {
        List<DeviceChannel> channels = channelMapper.queryChannelsByDeviceDbId(deviceDbId);
        if (channels.size() == 1) {
            return channels.get(0);
        }
        for (DeviceChannel channel : channels) {
            // Get 137 type
            if (SipUtils.isFrontEnd(channel.getDeviceId())) {
                return channel;
            }
        }
        return null;
    }

    @Override
    public void changeAudio(Integer channelId, Boolean audio) {
        channelMapper.changeAudio(channelId, audio);
    }

    @Override
    public void updateChannelStatusForNotify(DeviceChannel channel) {
        channelMapper.updateStatus(channel);
    }

    @Override
    public void addChannel(DeviceChannel channel) {
        channel.setDataType(ChannelDataType.GB28181);
        channel.setDataDeviceId(channel.getDataDeviceId());
        channelMapper.add(channel);
    }

    @Override
    public void updateChannelForNotify(DeviceChannel channel) {
        channelMapper.updateChannelForNotify(channel);
    }

    @Override
    public void queryRecordInfo(Device device, DeviceChannel channel, String startTime, String endTime, ErrorCallback<RecordInfo> callback) {
        log.info("Video query API call，deviceId：{}，channelId：{}，startTime：{}，endTime：{}", device.getDeviceId(), channel.getDeviceId(), startTime, endTime);
        if (!userSetting.getServerId().equals(device.getServerId())){
            redisRpcPlayService.queryRecordInfo(device.getServerId(), channel.getId(), startTime, endTime, callback);
            return;
        }
        try {
            int sn  =  (int)((Math.random()*9+1)*100000);
            commander.recordInfoQuery(device, channel.getDeviceId(), startTime, endTime, sn, null, null, eventResult -> {
                try {
                    // The message is sent successfully, listening and waiting for the data to arrive.
                    SynchronousQueue<RecordInfo> queue = new SynchronousQueue<>();
                    topicSubscribers.put("record" + sn, queue);
                    RecordInfo recordInfo = queue.poll(userSetting.getRecordInfoTimeout(), TimeUnit.MILLISECONDS);
                    if (recordInfo != null) {
                        callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), recordInfo);
                    }else {
                        callback.run(ErrorCode.ERROR100.getCode(), ErrorCode.ERROR100.getMsg(), recordInfo);
                    }
                } catch (InterruptedException e) {
                    callback.run(ErrorCode.ERROR100.getCode(), e.getMessage(), null);
                } finally {
                    this.topicSubscribers.remove("record" + sn);
                }

            }, (eventResult -> {
                callback.run(ErrorCode.ERROR100.getCode(), "Failed to query video, status: " +  eventResult.statusCode + ", message: " + eventResult.msg, null);
            }));
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[Command sending failed] Query video: {}", e.getMessage());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Command sending failed: " +  e.getMessage());
        }
    }

    @Override
    public void queryRecordInfo(CommonGBChannel channel, String startTime, String endTime, ErrorCallback<RecordInfo> callback) {
        if (channel.getDataType() != ChannelDataType.GB28181){
            // Only supports national standard voice calls
            log.warn("[INFO news] Non-national standard equipment, channelID： {}", channel.getGbId());
            callback.run(ErrorCode.ERROR100.getCode(), "Non-national standard equipment", null);
            return;
        }
        Device device = deviceMapper.query(channel.getDataDeviceId());
        if (device == null) {
            log.warn("[on demand] Channel not found{}device information", channel);
            callback.run(ErrorCode.ERROR100.getCode(), "Device does not exist", null);
            return;
        }
        DeviceChannel deviceChannel = getOneForSourceById(channel.getGbId());
        queryRecordInfo(device, deviceChannel, startTime, endTime, callback);

    }

    @Override
    public Map<String, DeviceChannel> getAllForMobilePosition(List<DeviceMobilePosition> mobilePositionList) {
        return channelMapper.getAllForMobilePosition(mobilePositionList.get(0).getDevice().getId(), mobilePositionList);
    }

    @Override
    @Async
    @Transactional
    public void asyncBatchChannelPosition(Collection<DeviceChannel> channels) {
        // Update channel location information in batches
        int limitCount = 500;
        List<DeviceChannel> channelList = new ArrayList<>(channels);
        if (!channelList.isEmpty()) {
            for (int i = 0; i < channelList.size(); i += limitCount) {
                int end = Math.min(i + limitCount, channelList.size());
                List<DeviceChannel> batchList = channelList.subList(i, end);
                channelMapper.batchUpdatePosition(batchList);
            }
        }
    }
}
