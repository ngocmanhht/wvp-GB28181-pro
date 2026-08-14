package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.CivilCodePo;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Region;
import com.genersoft.iot.vmp.gb28181.bean.RegionTree;
import com.genersoft.iot.vmp.gb28181.dao.CommonGBChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.RegionMapper;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.gb28181.service.IRegionService;
import com.genersoft.iot.vmp.utils.CivilCodeUtil;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
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
public class RegionServiceImpl implements IRegionService {

    @Autowired
    private RegionMapper regionMapper;

    @Autowired
    private CommonGBChannelMapper commonGBChannelMapper;

    @Autowired
    private IGbChannelService gbChannelService;

    @Autowired
    private EventPublisher eventPublisher;

    @Override
    public void add(Region region) {
        Assert.hasLength(region.getName(), "name must exist");
        Assert.hasLength(region.getDeviceId(), "The national standard number must exist");
        if (ObjectUtils.isEmpty(region.getParentDeviceId()) || ObjectUtils.isEmpty(region.getParentDeviceId().trim())) {
            region.setParentDeviceId(null);
        }
        region.setCreateTime(DateUtil.getNow());
        region.setUpdateTime(DateUtil.getNow());
        try {
            regionMapper.add(region);
        }catch (DuplicateKeyException e){
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "This administrative division already exists");
        }

    }

    @Override
    @Transactional
    public boolean deleteByDeviceId(Integer regionDeviceId) {
        Region region = regionMapper.queryOne(regionDeviceId);
        // Get all child nodes
        List<Region> allChildren = getAllChildren(regionDeviceId);
        allChildren.add(region);
        // Set the civilCode of the channel using these nodes tonull,
        gbChannelService.removeCivilCode(allChildren);
        regionMapper.batchDelete(allChildren);
        return true;
    }

    private List<Region> getAllChildren(Integer deviceId) {
        if (deviceId == null) {
            return new ArrayList<>();
        }
        List<Region> children = regionMapper.getChildren(deviceId);
        if (ObjectUtils.isEmpty(children)) {
            return children;
        }
        List<Region> regions = new ArrayList<>(children);
        for (Region region : children) {
            if (region.getDeviceId().length() < 8) {
                regions.addAll(getAllChildren(region.getId()));
            }
        }
        return regions;
    }

    @Override
    public PageInfo<Region> query(String query, int page, int count) {
        PageHelper.startPage(page, count);
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }
        List<Region> regionList =  regionMapper.query(query, null);
        return new PageInfo<>(regionList);
    }

    @Override
    @Transactional
    public void update(Region region) {
        Assert.notNull(region.getDeviceId(), "The number cannot beNULL");
        Assert.notNull(region.getName(), "Name cannot beNULL");
        Region regionInDb = regionMapper.queryOne(region.getId());
        Assert.notNull(regionInDb, "The administrative division to be updated does not exist in the database");
        if (!regionInDb.getDeviceId().equals(region.getDeviceId())) {
            Region regionNewInDb = regionMapper.queryByDeviceId(region.getDeviceId());
            Assert.isNull(regionNewInDb, "This administrative division already exists");
            // When the number changes, all channels assigned to this administrative division are updated and the data is sent.
            gbChannelService.updateCivilCode(regionInDb.getDeviceId(), region.getDeviceId());
            // Child node information update
            regionMapper.updateChild(region.getId(), region.getDeviceId());
        }
        regionMapper.update(region);
        // Send change notifications
        try {
            // sendcatalog
            eventPublisher.channelEventPublishForUpdate(CommonGBChannel.build(region), null);
        }catch (Exception e) {
            log.warn("[Administrative division changes] Sending failed，{}", region.getDeviceId(), e);
        }
    }

    @Override
    public List<Region> getAllChild(String parent) {
        List<Region> allChild = CivilCodeUtil.INSTANCE.getAllChild(parent);
        Collections.sort(allChild);
        return allChild;
    }

    @Override
    public Region queryRegionByDeviceId(String regionDeviceId) {
        return null;
    }

    @Override
    public List<RegionTree> queryForTree(Integer parent, Boolean hasChannel) {
        List<RegionTree> regionList = regionMapper.queryForTree(parent);
        if (parent != null && hasChannel != null && hasChannel) {
            Region parentRegion = regionMapper.queryOne(parent);
            if (parentRegion != null) {
                List<RegionTree> channelList = commonGBChannelMapper.queryForRegionTreeByCivilCode(parentRegion.getDeviceId());
                regionList.addAll(channelList);
            }
        }
        return regionList;
    }

    @Override
    public void syncFromChannel() {
        // Get the uninitialized administrative division node
        List<String> civilCodeList = regionMapper.getUninitializedCivilCode();
        if (civilCodeList.isEmpty()) {
            return;
        }
        List<Region> regionList = new ArrayList<>();
        // Collect the parent nodes of nodes to verify which nodes' parent nodes do not exist, so that they can be stored together.
        Map<String, Region> regionMapForVerification = new HashMap<>();
        civilCodeList.forEach(civilCode->{
            CivilCodePo civilCodePo = CivilCodeUtil.INSTANCE.getCivilCodePo(civilCode);
            if (civilCodePo != null) {
                Region region = Region.getInstance(civilCodePo);
                regionList.add(region);
                // Get all parent nodes
                List<CivilCodePo> civilCodePoList = CivilCodeUtil.INSTANCE.getAllParentCode(civilCode);
                if (!civilCodePoList.isEmpty()) {
                    for (CivilCodePo codePo : civilCodePoList) {
                        regionMapForVerification.put(codePo.getCode(), Region.getInstance(codePo));
                    }
                }
            }
        });
        if (regionList.isEmpty()){
            return;
        }
        if (!regionMapForVerification.isEmpty()) {
            // Query what already exists in the database.
            List<String> civilCodesInDb = regionMapper.queryInList(regionMapForVerification.keySet());
            if (!civilCodesInDb.isEmpty()) {
                for (String code : civilCodesInDb) {
                    regionMapForVerification.remove(code);
                }
            }
        }
        for (Region region : regionList) {
            regionMapForVerification.put(region.getDeviceId(), region);
        }

        regionMapper.batchAdd(new ArrayList<>(regionMapForVerification.values()));
    }

    @Override
    public boolean delete(int id) {
        return regionMapper.delete(id) > 0;
    }

    @Override
    @Transactional
    public boolean batchAdd(List<Region> regionList) {
        if (regionList== null || regionList.isEmpty()) {
            return false;
        }
        Map<String, Region> regionMapForVerification = new HashMap<>();
        for (Region region : regionList) {
            regionMapForVerification.put(region.getDeviceId(), region);
        }
        // Query what already exists in the database.
        List<Region> regionListInDb = regionMapper.queryInRegionListByDeviceId(regionList);
        if (!regionListInDb.isEmpty()) {
            for (Region region : regionListInDb) {
                regionMapForVerification.remove(region.getDeviceId());
            }
        }
        if (!regionMapForVerification.isEmpty()) {
            List<Region> regions = new ArrayList<>(regionMapForVerification.values());
            regionMapper.batchAdd(regions);
            regionMapper.updateParentId(regions);
        }

        return true;
    }

    @Override
    public List<Region> getPath(String deviceId) {
        Region region = regionMapper.queryByDeviceId(deviceId);
        if (region == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Administrative divisions do not exist");
        }
        List<Region> allParent = getAllParent(region);
        List<Region> regionList = new LinkedList<>(allParent);
        regionList.add(region);
        return regionList;
    }


    private List<Region> getAllParent(Region region) {
        if (region.getParentId() == null) {
            return new ArrayList<>();
        }

        Region parent = regionMapper.queryByDeviceId(region.getParentDeviceId());
        if (parent == null) {
            return new ArrayList<>();
        }
        List<Region> allParent = getAllParent(parent);
        allParent.add(parent);
        return allParent;
    }

    @Override
    public String getDescription(String civilCode) {

        CivilCodePo civilCodePo = CivilCodeUtil.INSTANCE.getCivilCodePo(civilCode);
        Assert.notNull(civilCodePo, String.format("Node %s was not found", civilCode));
        StringBuilder sb = new StringBuilder();
        sb.append(civilCodePo.getName());
        List<CivilCodePo> civilCodePoList = CivilCodeUtil.INSTANCE.getAllParentCode(civilCode);
        if (civilCodePoList.isEmpty()) {
            return sb.toString();
        }
        for (int i = 0; i < civilCodePoList.size(); i++) {
            CivilCodePo item = civilCodePoList.get(i);
            sb.insert(0, item.getName());
            if (i != civilCodePoList.size() - 1) {
                sb.insert(0, "/");
            }
        }
        return sb.toString();
    }

    @Override
    @Transactional
    public void addByCivilCode(String civilCode) {
        CivilCodePo civilCodePo = CivilCodeUtil.INSTANCE.getCivilCodePo(civilCode);
        // Query whether this node already exists
        Assert.notNull(civilCodePo, String.format("Node %s was not found", civilCode));
        List<CivilCodePo> civilCodePoList = CivilCodeUtil.INSTANCE.getAllParentCode(civilCode);
        civilCodePoList.add(civilCodePo);

        Set<String> civilCodeSet = regionMapper.queryInCivilCodePoList(civilCodePoList);
        if (!civilCodeSet.isEmpty()) {
            civilCodePoList.removeIf(item ->  civilCodeSet.contains(item.getCode()));
        }
        if (civilCodePoList.isEmpty()) {
            return;
        }
        int parentId = -1;
        for (int i = civilCodePoList.size() - 1; i > -1; i--) {
            CivilCodePo codePo =  civilCodePoList.get(i);

            Region region = new Region();
            region.setDeviceId(codePo.getCode());
            region.setParentDeviceId(codePo.getParentCode());
            region.setName(civilCodePo.getName());
            region.setCreateTime(DateUtil.getNow());
            region.setUpdateTime(DateUtil.getNow());
            if (parentId == -1 && codePo.getParentCode() != null) {
                Region parentRegion = regionMapper.queryByDeviceId(codePo.getParentCode());
                if (parentRegion == null){
                    log.error(String.format("Administrative division %sy already exists, but the query is wrong", codePo.getParentCode()));
                    throw new ControllerException(ErrorCode.ERROR100.getCode(), String.format("Administrative division %sy already exists, but the query is wrong", codePo.getParentCode()));
                }
                region.setParentId(parentRegion.getId());
            }else {
                region.setParentId(parentId);
            }
            regionMapper.add(region);
            parentId = region.getId();
        }
    }

    @Override
    public PageInfo<Region> queryList(int page, int count, String query) {
        PageHelper.startPage(page, count);
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }
        List<Region> all = regionMapper.query(query, null);
        return new PageInfo<>(all);
    }
}
