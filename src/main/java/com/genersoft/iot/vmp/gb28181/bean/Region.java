package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.common.CivilCodePo;
import com.genersoft.iot.vmp.utils.CivilCodeUtil;
import com.genersoft.iot.vmp.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

/**
 * area
 */
@Data
@Schema(description = "area")
public class Region implements Comparable<Region>{
    /**
     * Database auto-incrementID
     */
    @Schema(description = "Database auto-incrementID")
    private int id;

    /**
     * Regional national standard number
     */
    @Schema(description = "Regional national standard number")
    private String deviceId;

    /**
     * area name
     */
    @Schema(description = "area name")
    private String name;

    /**
     * Parent regional national standardID
     */
    @Schema(description = "parent regionID")
    private Integer parentId;

    /**
     * Parent regional national standardID
     */
    @Schema(description = "Parent regional national standardID")
    private String parentDeviceId;

    /**
     * creation time
     */
    @Schema(description = "creation time")
    private String createTime;

    /**
     * Update time
     */
    @Schema(description = "Update time")
    private String updateTime;

    public static Region getInstance(String commonRegionDeviceId, String commonRegionName, String commonRegionParentId) {
        Region region = new Region();
        region.setDeviceId(commonRegionDeviceId);
        region.setName(commonRegionName);
        region.setParentDeviceId(commonRegionParentId);
        region.setCreateTime(DateUtil.getNow());
        region.setUpdateTime(DateUtil.getNow());
        return region;
    }

    public static Region getInstance(CivilCodePo civilCodePo) {
        Region region = new Region();
        region.setName(civilCodePo.getName());
        region.setDeviceId(civilCodePo.getCode());
        if (civilCodePo.getCode().length() > 2) {
            region.setParentDeviceId(civilCodePo.getParentCode());
        }
        region.setCreateTime(DateUtil.getNow());
        region.setUpdateTime(DateUtil.getNow());
        return region;
    }

    public static Region getInstance(DeviceChannel channel) {
        Region region = new Region();
        region.setName(channel.getName());
        region.setDeviceId(channel.getDeviceId());
        CivilCodePo parentCode = CivilCodeUtil.INSTANCE.getParentCode(channel.getDeviceId());
        if (parentCode != null) {
            region.setParentDeviceId(parentCode.getCode());
        }
        region.setCreateTime(DateUtil.getNow());
        region.setUpdateTime(DateUtil.getNow());
        return region;
    }

    @Override
    public int compareTo(@NotNull Region region) {
        return Integer.compare(Integer.parseInt(this.deviceId), Integer.parseInt(region.getDeviceId()));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (this == obj)
            return true;
        if (obj instanceof Region) {
            Region region = (Region) obj;

            // Return only when the value of each attribute is consistent.true
            if (region.getId() == this.id) {
                return true;
            }
        }
        return false;
    }

    /**
     * Rewrite the hashcode method and compare the value of each attribute only when the returned hashCode is the same.
     */
    @Override
    public int hashCode() {
        return id;
    }
}
