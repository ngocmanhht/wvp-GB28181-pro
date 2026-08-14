package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.controller.bean.ChannelListForRpcParam;
import com.genersoft.iot.vmp.gb28181.dao.*;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.channel.ChannelEvent;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.gb28181.service.IPlatformChannelService;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.service.redisMsg.IRedisRpcService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * @author lin
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformChannelServiceImpl implements IPlatformChannelService {

    private static final int BATCH_SIZE = 500;

    private final PlatformChannelMapper platformChannelMapper;

    private final EventPublisher eventPublisher;

    private final GroupMapper groupMapper;

    private final RegionMapper regionMapper;

    private final CommonGBChannelMapper commonGBChannelMapper;

    private final PlatformMapper platformMapper;

    private final ISIPCommanderForPlatform sipCommanderForPlatform;

    private final SubscribeHolder subscribeHolder;

    private final UserSetting userSetting;

    private final IRedisRpcService redisRpcService;


    // Monitor channel information changes
    @EventListener
    public void onApplicationEvent(ChannelEvent event) {
        if (event.getChannels().isEmpty()) {
            log.info("[National standard cascade-Handle channel change events] Channel number is empty");
            return;
        }
        String deviceIds = event.getChannels().stream().map(CommonGBChannel::getGbDeviceId).collect(Collectors.joining(","));
        log.info("[National standard cascade-Handle channel change events] Type： {}, channel: {}", event.getMessageType(), deviceIds);
        // Get the platform associated with the channel
        List<Platform> allPlatform = platformMapper.queryByServerId(userSetting.getServerId());
        if (allPlatform.isEmpty()) {
            log.info("[National standard cascade-Handle channel change events] There is no current service responsible for the platform");
            return;
        }
        // Get the subscription used
        List<String> platforms = subscribeHolder.getAllCatalogSubscribePlatform(allPlatform);

        Map<String, List<Platform>> platformMap = new HashMap<>();
        Map<String, CommonGBChannel> channelMap = new HashMap<>();
        if (platforms.isEmpty()) {
            log.info("[National standard cascade-Handle channel change events] Catalog subscription without associated platform");
            return;
        }
        for (CommonGBChannel deviceChannel : event.getChannels()) {
            List<Platform> parentPlatformsForGB = queryPlatFormListByChannelDeviceId(
                    deviceChannel.getGbId(), platforms);
            platformMap.put(deviceChannel.getGbDeviceId(), parentPlatformsForGB);
            channelMap.put(deviceChannel.getGbDeviceId(), deviceChannel);
        }
        if (platformMap.isEmpty()) {
            log.info("[National standard cascade-Handle channel change events] There are no associated channels for the platforms where subscription is enabled.： {}", deviceIds);
            return;
        }
        switch (event.getMessageType()) {
            case ON:
            case OFF:
            case DEL:
                for (String serverGbId : platformMap.keySet()) {
                    List<Platform> platformList = platformMap.get(serverGbId);
                    if (platformList != null && !platformList.isEmpty()) {
                        for (Platform platform : platformList) {
                            SubscribeInfo subscribeInfo = subscribeHolder.getCatalogSubscribe(platform.getServerGBId());
                            if (subscribeInfo == null) {
                                continue;
                            }
                            log.info("[Catalogevent: {}]platform：{}，influence channel{}", event.getMessageType(), platform.getServerGBId(), serverGbId);
                            List<CommonGBChannel> deviceChannelList = new ArrayList<>();
                            CommonGBChannel deviceChannel = new CommonGBChannel();
                            deviceChannel.setGbDeviceId(serverGbId);
                            deviceChannelList.add(deviceChannel);
                            try {
                                sipCommanderForPlatform.sendNotifyForCatalogOther(event.getMessageType().name(), platform, deviceChannelList, subscribeInfo, null);
                            } catch (InvalidArgumentException | ParseException | NoSuchFieldException | SipException |
                                     IllegalAccessException e) {
                                log.error("[Command sending failed] National Standard Cascade Catalog Notice: {}", e.getMessage());
                            }
                        }
                    }else {
                        log.info("[Catalogevent: {}] There is no superior platform that needs to be notified： {}", event.getMessageType(), serverGbId);
                    }
                }
                break;
            case VLOST:
                break;
            case DEFECT:
                break;
            case ADD:
            case UPDATE:
                for (String gbId : platformMap.keySet()) {
                    List<Platform> parentPlatforms = platformMap.get(gbId);
                    if (parentPlatforms != null && !parentPlatforms.isEmpty()) {
                        for (Platform platform : parentPlatforms) {
                            SubscribeInfo subscribeInfo = subscribeHolder.getCatalogSubscribe(platform.getServerGBId());
                            if (subscribeInfo == null) {
                                continue;
                            }
                            log.info("[Catalogevent: {}]platform：{}，influence channel{}", event.getMessageType(), platform.getServerGBId(), gbId);
                            List<CommonGBChannel> channelList = new ArrayList<>();
                            CommonGBChannel deviceChannel = channelMap.get(gbId);
                            channelList.add(deviceChannel);
                            try {
                                sipCommanderForPlatform.sendNotifyForCatalogAddOrUpdate(event.getMessageType().name(), platform, channelList, subscribeInfo, null);
                            } catch (InvalidArgumentException | ParseException | NoSuchFieldException |
                                     SipException | IllegalAccessException e) {
                                log.error("[Command sending failed] National Standard Cascade Catalog Notice: {}", e.getMessage());
                            }
                        }
                    }
                }
                break;
            default:
                break;
        }
    }

    @EventListener
    public void onApplicationEvent(CatalogEvent event) {
        String deviceIds = event.getChannels().stream().map(CommonGBChannel::getGbDeviceId).collect(Collectors.joining(","));
        log.info("[Catalogevent: {}] channel： {}", event.getType(), deviceIds);
        Platform platform = event.getPlatform();
        if (platform == null || platform.getServerGBId() == null) {
            log.info("[Catalogevent: {}] Missing channel or abnormal channel data： {}", event.getType(), deviceIds);
            return;
        }
        SubscribeInfo subscribe = subscribeHolder.getCatalogSubscribe(platform.getServerGBId());
        if (subscribe == null) {
            log.info("[Catalogevent: {}] The platform is not subscribed to the catalog, cancel sending.： {}", event.getType(), deviceIds);
            return;
        }
        switch (event.getType()) {
            case CatalogEvent.ON:
            case CatalogEvent.OFF:
            case CatalogEvent.DEL:
                List<CommonGBChannel> channels = new ArrayList<>();
                if (event.getChannels() != null) {
                    channels.addAll(event.getChannels());
                }
                if (!channels.isEmpty()) {
                    log.info("[Catalogevent: {}]platform：{}，influence channel{}", event.getType(), platform.getServerGBId(), deviceIds);
                    try {
                        sipCommanderForPlatform.sendNotifyForCatalogOther(event.getType(), platform, channels, subscribe, null);
                    } catch (InvalidArgumentException | ParseException | NoSuchFieldException | SipException |
                             IllegalAccessException e) {
                        log.error("[Command sending failed] National Standard Cascade Catalog Notice: {}", e.getMessage());
                    }
                }
                break;
            case CatalogEvent.VLOST:
                break;
            case CatalogEvent.DEFECT:
                break;
            case CatalogEvent.ADD:
            case CatalogEvent.UPDATE:
                List<CommonGBChannel> deviceChannelList = new ArrayList<>();
                if (event.getChannels() != null) {
                    deviceChannelList.addAll(event.getChannels());
                }
                if (!deviceChannelList.isEmpty()) {
                    log.info("[Catalogevent: {}]platform：{}，influence channel{}", event.getType(), platform.getServerGBId(), deviceIds);
                    try {
                        sipCommanderForPlatform.sendNotifyForCatalogAddOrUpdate(event.getType(), platform, deviceChannelList, subscribe, null);
                    } catch (InvalidArgumentException | ParseException | NoSuchFieldException | SipException |
                             IllegalAccessException e) {
                        log.error("[Command sending failed] National Standard Cascade Catalog Notice: {}", e.getMessage());
                    }
                }
                break;
            default:
                break;
        }
    }


    @Override
    public PageInfo<PlatformChannel> queryChannelList(int page, int count, String query, Integer channelType, Boolean online, Integer platformId, Boolean hasShare) {
        PageHelper.startPage(page, count);
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }
        List<PlatformChannel> all = platformChannelMapper.queryForPlatformForWebList(platformId, query, channelType, online, hasShare);
        return new PageInfo<>(all);
    }

    /**
     * Get the unshared items in the group used by the channel
     */
    @Transactional
    public Set<Group> getGroupNotShareByChannelList(List<CommonGBChannel> channelList, Integer platformId) {
        // Get unshared nodes in the group
        Set<Group> groupList = groupMapper.queryNotShareGroupForPlatformByChannelList(channelList, platformId);
        // Get all parent nodes of these nodes
        if (groupList.isEmpty()) {
            return new HashSet<>();
        }
        Set<Group> allGroup = getAllGroup(groupList);
        allGroup.addAll(groupList);
        // Get all unshared nodes
        return groupMapper.queryNotShareGroupForPlatformByGroupList(allGroup, platformId);
    }

    /**
     * Get the unshared items in the group used by the channel
     */
    private Set<Region> getRegionNotShareByChannelList(List<CommonGBChannel> channelList, Integer platformId) {
        // Get unshared nodes in the group
        Set<Region> regionSet = regionMapper.queryNotShareRegionForPlatformByChannelList(channelList, platformId);
        // Get all parent nodes of these nodes
        if (regionSet.isEmpty()) {
            return new HashSet<>();
        }
        Set<Region> allRegion = getAllRegion(regionSet);
        allRegion.addAll(regionSet);
        // Get all unshared nodes
        return regionMapper.queryNotShareRegionForPlatformByRegionList(allRegion, platformId);
    }

    /**
     * Remove empty shares and return removed groups
     */
    @Transactional
    public Set<Group> deleteEmptyGroup(Set<Group> groupSet, Integer platformId) {
        Iterator<Group> iterator = groupSet.iterator();
        while (iterator.hasNext()) {
            Group group = iterator.next();
            // groupSet It is a group used directly by the current channel. If there are no sub-groups and other channels, it can be removed.
            // Get grouped child nodes
            Set<Group> children = platformChannelMapper.queryShareChildrenGroup(group.getId(), platformId);
            if (!children.isEmpty()) {
                iterator.remove();
                continue;
            }
            // Get the channel associated with the group
            List<CommonGBChannel> channelList = commonGBChannelMapper.queryShareChannelByParentId(group.getDeviceId(), platformId);
            if (!channelList.isEmpty()) {
                iterator.remove();
                continue;
            }
            platformChannelMapper.removePlatformGroupById(group.getId(), platformId);
        }
        // If it is empty, it means there are no channels to process.
        if (groupSet.isEmpty()) {
            return new HashSet<>();
        }
        Set<Group> parent =  platformChannelMapper.queryShareParentGroupByGroupSet(groupSet, platformId);
        if (parent.isEmpty()) {
            return groupSet;
        }else {
            Set<Group> parentGroupSet = deleteEmptyGroup(parent, platformId);
            groupSet.addAll(parentGroupSet);
            return groupSet;
        }
    }

    /**
     * Remove empty shares and return removed administrative divisions
     */
    private Set<Region> deleteEmptyRegion(Set<Region> regionSet, Integer platformId) {
        Iterator<Region> iterator = regionSet.iterator();
        while (iterator.hasNext()) {
            Region region = iterator.next();
            // groupSet It is a group used directly by the current channel. If there are no sub-groups and other channels, it can be removed.
            // Get grouped child nodes
            Set<Region> children = platformChannelMapper.queryShareChildrenRegion(region.getDeviceId(), platformId);
            if (!children.isEmpty()) {
                iterator.remove();
                continue;
            }
            // Get the channel associated with the group
            List<CommonGBChannel> channelList = commonGBChannelMapper.queryShareChannelByCivilCode(region.getDeviceId(), platformId);
            if (!channelList.isEmpty()) {
                iterator.remove();
                continue;
            }
            platformChannelMapper.removePlatformRegionById(region.getId(), platformId);
        }
        // If it is empty, it means there are no channels to process.
        if (regionSet.isEmpty()) {
            return new HashSet<>();
        }
        Set<Region> parent =  platformChannelMapper.queryShareParentRegionByRegionSet(regionSet, platformId);
        if (parent.isEmpty()) {
            return regionSet;
        }else {
            Set<Region> parentGroupSet = deleteEmptyRegion(parent, platformId);
            regionSet.addAll(parentGroupSet);
            return regionSet;
        }
    }

    private Set<Group> getAllGroup(Set<Group> groupList ) {
        if (groupList.isEmpty()) {
            return new HashSet<>();
        }
        Set<Group> channelList = groupMapper.queryParentInChannelList(groupList);
        if (channelList.isEmpty()) {
            return channelList;
        }
        Set<Group> allParentRegion = getAllGroup(channelList);
        channelList.addAll(allParentRegion);
        return channelList;
    }

    private Set<Region> getAllRegion(Set<Region> regionSet ) {
        if (regionSet.isEmpty()) {
            return new HashSet<>();
        }

        Set<Region> channelList = regionMapper.queryParentInChannelList(regionSet);
        if (channelList.isEmpty()) {
            return channelList;
        }
        Set<Region> allParentRegion = getAllRegion(channelList);
        channelList.addAll(allParentRegion);
        return channelList;
    }

    @Override
    @Transactional
    public int addAllChannel(Integer platformId) {
        List<CommonGBChannel> channelListNotShare = platformChannelMapper.queryNotShare(platformId, null);
        Assert.notEmpty(channelListNotShare, "All channels are shared");
        return addChannelList(platformId, channelListNotShare);
    }

    @Override
    @Transactional
    public int addChannels(Integer platformId, List<Integer> channelIds) {
        List<CommonGBChannel> channelListNotShare = platformChannelMapper.queryNotShare(platformId, channelIds);
        Assert.notEmpty(channelListNotShare, "Channel is shared");
        return addChannelList(platformId, channelListNotShare);
    }

    @Transactional
    public int addChannelList(Integer platformId, List<CommonGBChannel> channelList) {
        Platform platform = platformMapper.query(platformId);
        Assert.notNull(platform, "Platform does not exist");
        String channelDeviceIds = channelList.stream().map(CommonGBChannel::getGbDeviceId).collect(Collectors.joining(","));

        log.info("[shared channel] platform：{}， channel：{}", platform.getServerGBId(), channelDeviceIds);
        if (!userSetting.getServerId().equals(platform.getServerId())) {

            List<Integer> channelIdList = channelList.stream().map(CommonGBChannel::getGbId).toList();
            int result = redisRpcService.addPlatformChannelList(platform.getServerId(), new ChannelListForRpcParam(channelIdList, platformId));
            if (result > 0) {
                log.info("[Cross-platform-shared channel] success, platform：{}， channel：{}", platform.getServerGBId(), channelDeviceIds);
            }else {
                log.info("[Cross-platform-shared channel] failed, platform：{}， channel：{}", platform.getServerGBId(), channelDeviceIds);
            }
            return result;
        }
        int result = platformChannelMapper.addChannels(platformId, channelList);
        if (result > 0) {
            // Check whether the administrative division information related to the channel is shared. If not, add it.
            // Determine whether the platform is polite and push administrative divisions
            if (platform.getCatalogWithRegion() != 0) {
                Set<Region> regionListNotShare =  getRegionNotShareByChannelList(channelList, platformId);
                if (!regionListNotShare.isEmpty()) {
                    int addGroupResult = platformChannelMapper.addPlatformRegion(new ArrayList<>(regionListNotShare), platformId);
                    if (addGroupResult > 0) {
                        for (Region region : regionListNotShare) {
                            // When sorting group information, the top layer needs to be sorted last.
                            channelList.addFirst(CommonGBChannel.build(region));
                        }
                    }
                }
            }

            if (platform.getCatalogWithGroup() != 0) {
                // Check whether the grouping information related to the channel is shared. If not, add it.
                Set<Group> groupListNotShare =  getGroupNotShareByChannelList(channelList, platformId);
                if (!groupListNotShare.isEmpty()) {
                    int addGroupResult = platformChannelMapper.addPlatformGroup(new ArrayList<>(groupListNotShare), platformId);
                    if (addGroupResult > 0) {
                        for (Group group : groupListNotShare) {
                            // When sorting group information, the top layer needs to be sorted last.
                            channelList.addFirst(CommonGBChannel.build(group));
                        }
                    }
                }
            }
            // Send message
            try {
                // sendcatalog
                eventPublisher.catalogEventPublish(platform, channelList, CatalogEvent.ADD);
            } catch (Exception e) {
                log.warn("[associated channel] Failed to send, quantity：{}", channelList.size(), e);
            }
        }
        return result;
    }

    @Override
    public int removeAllChannel(Integer platformId) {
        Platform platform = platformMapper.query(platformId);
        if (platform == null) {
            return 0;
        }
        log.info("[Cancel shared channel] platform：{}， Channel: All", platform.getServerGBId());
        if (!userSetting.getServerId().equals(platform.getServerId())) {

            int result = redisRpcService.removeAllPlatformChannel(platform.getServerId(), platformId);
            if (result > 0) {
                log.info("[Cross-platform-Cancel shared channel] success, platform：{}， Channel: All", platform.getServerGBId());
            }else {
                log.info("[Cross-platform-Cancel shared channel] failed, platform：{}， Channel: All", platform.getServerGBId());
            }
            return result;
        }
        List<CommonGBChannel> channelListShare = platformChannelMapper.queryShare(platformId,  null);
        Assert.notEmpty(channelListShare, "No channels are shared");
        return removeChannelsFromDb(platform, platformId, channelListShare);
    }

    @Override
    @Transactional
    public void addChannelByDevice(Integer platformId, List<Integer> deviceIds) {
        List<Integer> channelList = commonGBChannelMapper.queryByGbDeviceIdsForIds(ChannelDataType.GB28181, deviceIds);
        addChannels(platformId, channelList);
    }

    @Override
    @Transactional
    public void removeChannelByDevice(Integer platformId, List<Integer> deviceIds) {
        List<Integer> channelList = commonGBChannelMapper.queryByGbDeviceIdsForIds(ChannelDataType.GB28181, deviceIds);
        removeChannels(platformId, channelList);
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }

    private int removeChannelsFromDb(Platform platform, Integer platformId, List<CommonGBChannel> channelList) {
        List<List<CommonGBChannel>> batches = partition(channelList, BATCH_SIZE);
        int totalResult = 0;
        for (List<CommonGBChannel> batch : batches) {
            totalResult += platformChannelMapper.removeChannelsWithPlatform(platformId, batch);
        }
        if (totalResult > 0) {
            Set<Region> regionSet = regionMapper.queryByChannelList(channelList);
            Set<Region> deleteRegion = deleteEmptyRegion(regionSet, platformId);
            if (!deleteRegion.isEmpty()) {
                for (Region region : deleteRegion) {
                    channelList.add(0, CommonGBChannel.build(region));
                }
            }
            Set<Group> groupSet = groupMapper.queryByChannelList(channelList);
            Set<Group> deleteGroup = deleteEmptyGroup(groupSet, platformId);
            if (!deleteGroup.isEmpty()) {
                for (Group group : deleteGroup) {
                    channelList.add(0, CommonGBChannel.build(group));
                }
            }
            try {
                eventPublisher.catalogEventPublish(platform, channelList, CatalogEvent.DEL);
            } catch (Exception e) {
                log.warn("[Cancel shared channel] Failed to send, quantity：{}", channelList.size(), e);
            }
        }
        return totalResult;
    }

    @Transactional
    public int removeChannelList(Integer platformId, List<CommonGBChannel> channelList) {
        Platform platform = platformMapper.query(platformId);
        if (platform == null) {
            log.info("[Remove associated channel] platform{}Not found", platformId);
            return 0;
        }
        String channelDeviceIds = channelList.stream().map(CommonGBChannel::getGbDeviceId).collect(Collectors.joining(","));
        log.info("[Cancel shared channel] platform：{}， channel： {}", platform.getServerGBId(), channelDeviceIds);
        if (!userSetting.getServerId().equals(platform.getServerId())) {
            List<Integer> channelIds = channelList.stream().map(CommonGBChannel::getGbId).toList();
            int result = redisRpcService.removePlatformChannelList(platform.getServerId(), new ChannelListForRpcParam(channelIds, platformId));
            if (result > 0) {
                log.info("[Cross-platform-Cancel shared channel] success, platform：{}， channel： {}", platform.getServerGBId(), channelDeviceIds);
            }else {
                log.info("[Cross-platform-Cancel shared channel] failed, platform：{}， channel： {}", platform.getServerGBId(), channelDeviceIds);
            }
            return result;
        }
        int result = removeChannelsFromDb(platform, platformId, channelList);
        if (result <= 0) {
            String deviceIds = channelList.stream().map(CommonGBChannel::getGbDeviceId).collect(Collectors.joining(","));
            log.info("[Cancel shared channel] platform{}Unassociated channel： {}", platformId, deviceIds);
            return 0;
        }
        return result;
    }

    @Override
    @Transactional
    public int removeChannels(Integer platformId, List<Integer> channelIds) {
        List<CommonGBChannel> channelList = platformChannelMapper.queryShare(platformId, channelIds);
        if (channelList.isEmpty()) {
            log.info("[Remove channel] Channel list is empty");
            return 0;
        }
        return removeChannelList(platformId, channelList);
    }

    @Override
    @Transactional
    public void removeChannels(List<Integer> ids) {
        List<Platform> platformList = platformChannelMapper.queryPlatFormListByChannelList(ids);
        if (platformList.isEmpty()) {
            log.info("[Remove multiple channels] The platform associated with the channel was not found");
            return;
        }

        for (Platform platform : platformList) {
            removeChannels(platform.getId(), ids);
        }
    }

    @Override
    @Transactional
    public void removeChannel(int channelId) {
        List<Platform> platformList = platformChannelMapper.queryPlatFormListByChannelId(channelId);
        if (platformList.isEmpty()) {
            log.info("[Remove multiple channels] No channel found：{} Associated platforms", channelId);
            return;
        }
        for (Platform platform : platformList) {
            ArrayList<Integer> ids = new ArrayList<>();
            ids.add(channelId);
            removeChannels(platform.getId(), ids);
        }
    }

    @Override
    public List<CommonGBChannel> queryByPlatform(Platform platform) {
        if (platform == null) {
            log.info("[Query the platform to which the channel belongs] The platform parameters areNULL");
            return null;
        }
        List<CommonGBChannel> commonGBChannelList = commonGBChannelMapper.queryWithPlatform(platform.getId());
        if (commonGBChannelList.isEmpty()) {
            return new ArrayList<>();
        }
        List<CommonGBChannel> channelList = new ArrayList<>();
        // Whether to include platform information
        if (platform.getCatalogWithPlatform() > 0) {
            CommonGBChannel channel = CommonGBChannel.build(platform);
            channelList.add(channel);
        }
        // Related administrative division information
        if (platform.getCatalogWithRegion() > 0) {
            // Query the administrative division information of the associated platform
            List<CommonGBChannel> regionChannelList = regionMapper.queryByPlatform(platform.getId());
            if (!regionChannelList.isEmpty()) {
                channelList.addAll(regionChannelList);
            }
        }
        if (platform.getCatalogWithGroup() > 0) {
            // Associated grouping information
            List<CommonGBChannel> groupChannelList =  groupMapper.queryForPlatform(platform.getId());
            if (!groupChannelList.isEmpty()) {
                channelList.addAll(groupChannelList);
            }
        }

        channelList.addAll(commonGBChannelList);
        return channelList;
    }

    @Override
    public void pushChannel(Integer platformId) {
        Platform platform = platformMapper.query(platformId);
        Assert.notNull(platform, "Platform does not exist");
        if (!userSetting.getServerId().equals(platform.getServerId())) {
            boolean result = redisRpcService.pushPlatformChannel(platform.getServerId(), platformId);
            if (result) {
                log.info("[Cross-platform-Active push channel] success, platform：{}", platform.getServerGBId());
            }else {
                log.info("[Cross-platform-Active push channel] failed, platform：{}", platform.getServerGBId());
            }
            return;
        }

        List<CommonGBChannel> channelList = queryByPlatform(platform);
        if (channelList.isEmpty()){
            log.info("[push channel] platform：{} No channel information found", platform.getServerGBId());
            return;
        }
        SubscribeInfo subscribeInfo = SubscribeInfo.buildSimulated(platform.getServerGBId(), platform.getServerIp());

        try {
            sipCommanderForPlatform.sendNotifyForCatalogAddOrUpdate(CatalogEvent.ADD, platform, channelList, subscribeInfo, null);
        } catch (InvalidArgumentException | ParseException | NoSuchFieldException |
                 SipException | IllegalAccessException e) {
            log.error("[Command sending failed] National Standard Cascade Catalog Notice: {}", e.getMessage());
        }
    }

    @Override
    public void updateCustomChannel(PlatformChannel channel) {
        Platform platform = platformMapper.query(channel.getPlatformId());
        Assert.notNull(platform, "Platform does not exist");
        log.info("[National standard cascade-Custom shared channel] platform：{}， channel：{}", platform.getServerGBId(), channel);
        if (!userSetting.getServerId().equals(platform.getServerId())) {
            boolean result = redisRpcService.updateCustomPlatformChannel(platform.getServerId(), channel);
            if (result) {
                log.info("[National standard cascade-Custom shared channel] success, platform：{}， channel：{}", platform.getServerGBId(), channel);
            }else {
                log.info("[National standard cascade-Custom shared channel] failed, platform：{}， channel：{}", platform.getServerGBId(), channel);
            }
            return;
        }

        platformChannelMapper.updateCustomChannel(channel);

        CommonGBChannel commonGBChannel = platformChannelMapper.queryShareChannel(channel.getPlatformId(), channel.getGbId());
        // Send message
        try {
            // sendcatalog
            eventPublisher.catalogEventPublish(platform, commonGBChannel, CatalogEvent.UPDATE);
        } catch (Exception e) {
            log.warn("[National standard cascade-Custom shared channel] Sending failed, platformID： {}， channel： {}（{}）", channel.getPlatformId(),
                    channel.getGbName(), channel.getId(), e);
        }
    }

    @Override
    @Transactional
    public void checkGroupRemove(List<CommonGBChannel> channelList, List<Group> groupList) {

        List<Integer> channelIds = new ArrayList<>();
        channelList.stream().forEach(commonGBChannel -> {
            channelIds.add(commonGBChannel.getGbId());
        });
        // Get the platforms associated with these channels
        List<Platform> platformList = platformChannelMapper.queryPlatFormListByChannelList(channelIds);
        if (platformList.isEmpty()) {
            String deviceIds = channelList.stream().map(CommonGBChannel::getGbDeviceId).collect(Collectors.joining(","));
            log.info("[Get the platforms associated with these channels] The platform associated with the channel was not found. The channel is as follows {}", deviceIds);
            return;
        }
        for (Platform platform : platformList) {
            Set<Group> groupSet;
            if (groupList == null || groupList.isEmpty()) {
                groupSet = platformChannelMapper.queryShareGroup(platform.getId());
            }else {
                groupSet = new HashSet<>(groupList);
            }
            // Clear empty groups and send messages
            Set<Group> deleteGroup = deleteEmptyGroup(groupSet, platform.getId());

            List<CommonGBChannel> channelListForEvent = new ArrayList<>();
            if (!deleteGroup.isEmpty()) {
                for (Group group : deleteGroup) {
                    channelListForEvent.add(0, CommonGBChannel.build(group));
                }
            }
            // Send message
            try {
                // sendcatalog
                eventPublisher.catalogEventPublish(platform, channelListForEvent, CatalogEvent.DEL);
            } catch (Exception e) {
                log.warn("[Remove associated channel] Failed to send, quantity：{}", channelList.size(), e);
            }
        }
    }

    @Override
    @Transactional
    public void checkRegionRemove(List<CommonGBChannel> channelList, List<Region> regionList) {
        List<Integer> channelIds = new ArrayList<>();
        channelList.stream().forEach(commonGBChannel -> {
            channelIds.add(commonGBChannel.getGbId());
        });
        // Get the platforms associated with these channels
        List<Platform> platformList = platformChannelMapper.queryPlatFormListByChannelList(channelIds);
        if (platformList.isEmpty()) {
            String deviceIds = channelList.stream().map(CommonGBChannel::getGbDeviceId).collect(Collectors.joining(","));
            log.info("[Get the platforms associated with these channels] The platform associated with the channel was not found. The channel is as follows {}", deviceIds);
            return;
        }
        for (Platform platform : platformList) {
            Set<Region> regionSet;
            if (regionList == null || regionList.isEmpty()) {
                regionSet = platformChannelMapper.queryShareRegion(platform.getId());
            }else {
                regionSet = new HashSet<>(regionList);
            }
            // Clear empty groups and send messages
            Set<Region> deleteRegion = deleteEmptyRegion(regionSet, platform.getId());

            List<CommonGBChannel> channelListForEvent = new ArrayList<>();
            if (!deleteRegion.isEmpty()) {
                for (Region region : deleteRegion) {
                    channelListForEvent.add(0, CommonGBChannel.build(region));
                }
            }
            // Send message
            try {
                // sendcatalog
                eventPublisher.catalogEventPublish(platform, channelListForEvent, CatalogEvent.DEL);
            } catch (Exception e) {
                log.warn("[Remove associated channel] Failed to send, quantity：{}", channelList.size(), e);
            }
        }
    }

    @Override
    @Transactional
    public void checkGroupAdd(List<CommonGBChannel> channelList) {
        List<Integer> channelIds = new ArrayList<>();
        channelList.stream().forEach(commonGBChannel -> {
            channelIds.add(commonGBChannel.getGbId());
        });
        List<Platform> platformList = platformChannelMapper.queryPlatFormListByChannelList(channelIds);
        if (platformList.isEmpty()) {
            String deviceIds = channelList.stream().map(CommonGBChannel::getGbDeviceId).collect(Collectors.joining(","));
            log.info("[Get the platforms associated with these channels] The platform associated with the channel was not found. The channel is as follows {}", deviceIds);
            return;
        }
        for (Platform platform : platformList) {

            Set<Group> addGroup =  getGroupNotShareByChannelList(channelList, platform.getId());

            List<CommonGBChannel> channelListForEvent = new ArrayList<>();
            if (!addGroup.isEmpty()) {
                for (Group group : addGroup) {
                    channelListForEvent.add(0, CommonGBChannel.build(group));
                }
                platformChannelMapper.addPlatformGroup(addGroup, platform.getId());
                // Send message
                try {
                    // sendcatalog
                    eventPublisher.catalogEventPublish(platform, channelListForEvent, CatalogEvent.ADD);
                } catch (Exception e) {
                    log.warn("[Remove associated channel] Failed to send, quantity：{}", channelList.size(), e);
                }
            }
        }
    }

    @Override
    public void checkRegionAdd(List<CommonGBChannel> channelList) {
        List<Integer> channelIds = new ArrayList<>();
        channelList.stream().forEach(commonGBChannel -> {
            channelIds.add(commonGBChannel.getGbId());
        });
        List<Platform> platformList = platformChannelMapper.queryPlatFormListByChannelList(channelIds);
        if (platformList.isEmpty()) {
            String deviceIds = channelList.stream().map(CommonGBChannel::getGbDeviceId).collect(Collectors.joining(","));
            log.info("[Get the platforms associated with these channels] The platform associated with the channel was not found. The channel is as follows {}", deviceIds);
            return;
        }
        for (Platform platform : platformList) {

            Set<Region> addRegion =  getRegionNotShareByChannelList(channelList, platform.getId());
            List<CommonGBChannel> channelListForEvent = new ArrayList<>();
            if (!addRegion.isEmpty()) {
                for (Region region : addRegion) {
                    channelListForEvent.add(0, CommonGBChannel.build(region));
                }
                platformChannelMapper.addPlatformRegion(new ArrayList<>(addRegion), platform.getId());
                // Send message
                try {
                    // sendcatalog
                    eventPublisher.catalogEventPublish(platform, channelListForEvent, CatalogEvent.ADD);
                } catch (Exception e) {
                    log.warn("[Remove associated channel] Failed to send, quantity：{}", channelList.size(), e);
                }
            }
        }
    }

    @Override
    public List<Platform> queryPlatFormListByChannelDeviceId(Integer channelId, List<String> platforms) {
        return platformChannelMapper.queryPlatFormListForGBWithGBId(channelId, platforms);
    }

    @Override
    public CommonGBChannel queryChannelByPlatformIdAndChannelId(Integer platformId, Integer channelId) {
        return platformChannelMapper.queryShareChannel(platformId, channelId);
    }

    @Override
    public List<CommonGBChannel> queryChannelByPlatformIdAndChannelIds(Integer platformId, List<Integer> channelIds) {
        return platformChannelMapper.queryShare(platformId, channelIds);
    }

    @Override
    public List<Platform> queryByPlatformBySharChannelId(String channelDeviceId) {
        List<CommonGBChannel> commonGBChannels = commonGBChannelMapper.queryByDeviceId(channelDeviceId);
        ArrayList<Integer> ids = new ArrayList<>();
        for (CommonGBChannel commonGBChannel : commonGBChannels) {
            ids.add(commonGBChannel.getGbId());
        }
        return platformChannelMapper.queryPlatFormListByChannelList(ids);
    }

    @Override
    public void notifyMobilePosition(List<MobilePosition> mobilePositionList) {

        List<Platform> allPlatforms = platformMapper.queryServerIdsWithEnableAndServer(userSetting.getServerId());
        // Get the subscription used
        Map<Integer, Platform> platformMap = subscribeHolder.getAllMobilePositionSubscribePlatform(allPlatforms);
        if (platformMap.isEmpty()) {
            return;
        }

        // YesmobilePositionListInternal channelId classification
        Map<Integer, List<MobilePosition>> channelIdMap = mobilePositionList.stream().collect(Collectors.groupingBy(MobilePosition::getChannelId));

        List<ShareGBChannel> shareGBChannels = platformChannelMapper.queryShareChannelInPlatformsAndChannelIds(platformMap.values(), channelIdMap.keySet());
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (ShareGBChannel shareGBChannel : shareGBChannels) {
                List<MobilePosition> mobilePositions = channelIdMap.get(shareGBChannel.getGbId());
                if (mobilePositions == null || mobilePositions.isEmpty()) {
                    continue;
                }
                executor.submit(() -> {
                    Platform platform = platformMap.get(shareGBChannel.getPlatformId());
                    if (platform == null) {
                        log.info("[Query platform] platformID：{} Not found", shareGBChannel.getPlatformId());
                        return;
                    }
                    SubscribeInfo subscribe = subscribeHolder.getMobilePositionSubscribe(platform.getServerGBId());
                    if (subscribe == null) {
                        log.info("[Query subscription] platform：{} No mobile location subscription found", platform.getServerGBId());
                        return;
                    }
                    for (MobilePosition mobilePosition : mobilePositions) {
                        try {
                            GPSMsgInfo gpsMsgInfo = GPSMsgInfo.getInstance(mobilePosition);
                            // Get channel number
                            CommonGBChannel commonGBChannel = queryChannelByPlatformIdAndChannelId(platform.getId(), mobilePosition.getChannelId());
                            sipCommanderForPlatform.sendNotifyMobilePosition(platform, gpsMsgInfo, commonGBChannel,
                                    subscribe);
                        } catch (InvalidArgumentException | ParseException | NoSuchFieldException | SipException |
                                 IllegalAccessException e) {
                            log.error("[Command sending failed] National Standard Cascade Catalog Notice: {}", e.getMessage());
                        }
                    }
                });
            }
        }
    }
}
