package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.dao.CommonGBChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.GroupMapper;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.gb28181.service.IGroupService;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.util.*;

/**
 * Regional management
 */
@Service
@Slf4j
public class GroupServiceImpl implements IGroupService {

    @Autowired
    private GroupMapper groupManager;

    @Autowired
    private CommonGBChannelMapper commonGBChannelMapper;

    @Autowired
    private IGbChannelService gbChannelService;

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void add(Group group) {
        Assert.notNull(group, "Parameter cannot beNULL");
        Assert.notNull(group.getDeviceId(), "The group number cannot beNULL");
        Assert.isTrue(group.getDeviceId().trim().length() == 20, "The group number must be 20 digits");
        Assert.notNull(group.getName(), "The group name cannot beNULL");

        GbCode gbCode = GbCode.decode(group.getDeviceId());
        Assert.notNull(gbCode, "The group number does not meet the national standard definition");

        // Query what already exists in the database.
        List<Group> groupListInDb = groupManager.queryInGroupListByDeviceId(Lists.newArrayList(group));
        if (!ObjectUtils.isEmpty(groupListInDb)){
            throw new ControllerException(ErrorCode.ERROR100.getCode(), String.format("The node number %s already exists", group.getDeviceId()));
        }

        if ("215".equals(gbCode.getTypeCode())){
            // Add business group
            addBusinessGroup(group);
        }else {
            Assert.isTrue("216".equals(gbCode.getTypeCode()), "Device number when creating a virtual organization11-13bits should be used216");
            // Add virtual organization
            addGroup(group);
        }
    }

    private void addGroup(Group group) {
        // Create a virtual organization
        Assert.notNull(group.getBusinessGroup(), "The business group it belongs to does not exist");
        Group businessGroup = groupManager.queryBusinessGroup(group.getBusinessGroup());
        Assert.notNull(businessGroup, "The business group it belongs to does not exist");
        if (!ObjectUtils.isEmpty(group.getParentDeviceId())) {
            Group parentGroup = groupManager.queryOneByDeviceId(group.getParentDeviceId(), group.getBusinessGroup());
            Assert.notNull(parentGroup, "The parent group it belongs to does not exist");
        }else {
            group.setParentDeviceId(null);
        }
        group.setCreateTime(DateUtil.getNow());
        group.setUpdateTime(DateUtil.getNow());
        groupManager.add(group);
    }

    private void addBusinessGroup(Group group) {
        group.setBusinessGroup(group.getDeviceId());
        group.setCreateTime(DateUtil.getNow());
        group.setUpdateTime(DateUtil.getNow());
        groupManager.addBusinessGroup(group);
    }

    @Override
    public List<Group> queryAllChildren(Integer id) {
        List<Group> children = groupManager.getChildren(id);
        if (ObjectUtils.isEmpty(children)) {
            return children;
        }
        for (int i = 0; i < children.size(); i++) {
            children.addAll(queryAllChildren(children.get(i).getId()));
        }
        return children;
    }

    @Override
    @Transactional
    public void update(Group group) {
        Assert.isTrue(group.getId()> 0, "Updates must carry groupsID");
        Assert.notNull(group.getDeviceId(), "The number cannot beNULL");
        Assert.notNull(group.getBusinessGroup(), "Business grouping cannot beNULL");
        Group groupInDb = groupManager.queryOne(group.getId());
        Assert.notNull(groupInDb, "Group does not exist");

        // Query what already exists in the database.
        List<Group> groupListInDb = groupManager.queryInGroupListByDeviceId(Lists.newArrayList(group));
        if (!ObjectUtils.isEmpty(groupListInDb) && groupListInDb.get(0).getId() != group.getId()){
            throw new ControllerException(ErrorCode.ERROR100.getCode(), String.format("The node number %s already exists", group.getDeviceId()));
        }

        group.setName(group.getName());
        group.setUpdateTime(DateUtil.getNow());
        groupManager.update(group);
        // Modify its child nodes
        if (!group.getDeviceId().equals(groupInDb.getDeviceId())
                || !group.getBusinessGroup().equals(groupInDb.getBusinessGroup())) {
            List<Group> groupList = queryAllChildren(groupInDb.getId());
            if (!groupList.isEmpty()) {
               int result =  groupManager.updateChild(groupInDb.getId(), group);
               if (result > 0) {
                   for (Group chjildGroup : groupList) {
                       chjildGroup.setParentDeviceId(group.getDeviceId());
                       chjildGroup.setBusinessGroup(group.getBusinessGroup());
                       // Send notification of change information
                       CommonGBChannel channel = CommonGBChannel.build(chjildGroup);
                       try {
                           // sendcatalog
                           eventPublisher.channelEventPublishForUpdate(channel, null);
                       }catch (Exception e) {
                           log.warn("[business grouping/virtual organization changes] Sending failed，{}", group.getDeviceId(), e);
                       }
                   }
               }
            }
        }
        // Send notification of change information
        CommonGBChannel channel = CommonGBChannel.build(group);
        try {
            // sendcatalog
            eventPublisher.channelEventPublishForUpdate(channel, null);
        }catch (Exception e) {
            log.warn("[business grouping/virtual organization changes] Sending failed，{}", group.getDeviceId(), e);
        }

        // Due to the number change, too much content needs to be processed and a large number of messages may be sent, so the current update only supports renaming.
        GbCode decode = GbCode.decode(group.getDeviceId());
        if (!groupInDb.getDeviceId().equals(group.getDeviceId())) {
            if (decode.getTypeCode().equals("215")) {
                // Business grouping changes. All business groups under it need to be modified
                gbChannelService.updateBusinessGroup(groupInDb.getDeviceId(), group.getDeviceId());
            }else {
                // To modify the virtual organization, you need to modify the child nodes under it to the parent node.ID
                gbChannelService.updateParentIdGroup(groupInDb.getDeviceId(), group.getDeviceId());
            }
        }
    }

    @Override
    public Group queryGroupByDeviceId(String regionDeviceId) {
        return groupManager.queryOneByOnlyDeviceId(regionDeviceId);
    }

    @Override
    public List<GroupTree> queryForTree(String query, Integer parentId, Boolean hasChannel) {

        List<GroupTree> groupTrees = groupManager.queryForTree(query, parentId);
        if (parentId == null) {
            return groupTrees;
        }
        // Query the channels contained
        Group parentGroup = groupManager.queryOne(parentId);
        if (parentGroup != null && hasChannel != null && hasChannel) {
            List<GroupTree> groupTreesForChannel = commonGBChannelMapper.queryForGroupTreeByParentId(query, parentGroup.getDeviceId());
            if (!ObjectUtils.isEmpty(groupTreesForChannel)) {
                groupTrees.addAll(groupTreesForChannel);
            }
        }
        return groupTrees;
    }

    @Override
    @Transactional
    public boolean delete(int id) {
        Group group = groupManager.queryOne(id);
        Assert.notNull(group, "Group does not exist");
        List<Group> groupListForDelete = new ArrayList<>();
        GbCode gbCode = GbCode.decode(group.getDeviceId());
        if (gbCode.getTypeCode().equals("215")) {
            List<Group> groupList = groupManager.queryByBusinessGroup(group.getDeviceId());
            if (!groupList.isEmpty()) {
                groupListForDelete.addAll(groupList);
            }
            // business grouping
            gbChannelService.removeParentIdByBusinessGroup(group.getDeviceId());
        }else {
            List<Group> groupList = queryAllChildren(group.getId());
            if (!groupList.isEmpty()) {
                groupListForDelete.addAll(groupList);
            }
            groupListForDelete.add(group);
            gbChannelService.removeParentIdByGroupList(groupListForDelete);
        }
        groupManager.batchDelete(groupListForDelete);

        for (Group groupForDelete : groupListForDelete) {
            // Delete the group information associated with the platform. Send notifications simultaneously
            List<Platform> platformList = groupManager.queryForPlatformByGroupId(groupForDelete.getId());
            if ( !platformList.isEmpty()) {
                groupManager.deletePlatformGroup(groupForDelete.getId());
                // Send notification of change information
                CommonGBChannel channel = CommonGBChannel.build(groupForDelete);
                for (Platform platform : platformList) {
                    try {
                        // sendcatalog
                        eventPublisher.catalogEventPublish(platform, channel, CatalogEvent.DEL);
                    }catch (Exception e) {
                        log.warn("[business grouping/Virtual organization deletion] Sending failed，{}", groupForDelete.getDeviceId(), e);
                    }
                }
            }
        }
        return true;
    }

    @Override
    @Transactional
    public boolean batchAdd(List<Group> groupList) {
        if (groupList== null || groupList.isEmpty()) {
            return false;
        }
        Map<String, Group> groupMapForVerification = new HashMap<>();
        for (Group group : groupList) {
            groupMapForVerification.put(group.getDeviceId(), group);
        }
        // Query what already exists in the database.
        List<Group> groupListInDb = groupManager.queryInGroupListByDeviceId(groupList);
        if (!groupListInDb.isEmpty()) {
            for (Group group : groupListInDb) {
                groupMapForVerification.remove(group.getDeviceId());
            }
        }
        if (!groupMapForVerification.isEmpty()) {
            List<Group> groupListForAdd = new ArrayList<>(groupMapForVerification.values());
            groupManager.batchAdd(groupListForAdd);
            // Update grouping relationship
            groupManager.updateParentId(groupListForAdd);
            groupManager.updateParentIdWithBusinessGroup(groupListForAdd);
        }

        return true;
    }

    @Override
    public List<Group> getPath(String deviceId, String businessGroup) {
        Group businessGroupInDb = groupManager.queryBusinessGroup(businessGroup);
        if (businessGroupInDb == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Business group does not exist");
        }
        Group group = groupManager.queryOneByDeviceId(deviceId, businessGroup);
        if (group == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Virtual organization does not exist");
        }
        List<Group> allParent = getAllParent(group);
        List<Group> groupList = new LinkedList<>(allParent);
        groupList.add(group);
        return groupList;
    }

    private List<Group> getAllParent(Group group) {
        if (group.getParentId() == null || group.getBusinessGroup() == null) {
            return new ArrayList<>();
        }

        Group parent = groupManager.queryOneByDeviceId(group.getParentDeviceId(), group.getBusinessGroup());
        if (parent == null) {
            return new ArrayList<>();
        }
        List<Group> allParent = getAllParent(parent);
        allParent.add(parent);
        return allParent;
    }

    @Override
    public PageInfo<Group> queryList(Integer page, Integer count, String query) {
        PageHelper.startPage(page, count);
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }
        List<Group> all = groupManager.query(query, null, null);
        return new PageInfo<>(all);
    }

    @Override
    public Group queryGroupByAlias(String groupAlias) {
        return groupManager.queryGroupByAlias(groupAlias);
    }

    @Override
    public Map<String, Group> queryGroupByAliasMap() {
        return groupManager.queryGroupByAliasMap();
    }

    @Override
    @Transactional
    public void saveByAlias(Collection<Group> groups) {
        // Clear alias data
        groupManager.deleteHasAlias();
        // Write new data
        groupManager.batchAdd(new ArrayList<>(groups));
        // Repair lost dataparentID
        groupManager.fixParentId();
    }
}
