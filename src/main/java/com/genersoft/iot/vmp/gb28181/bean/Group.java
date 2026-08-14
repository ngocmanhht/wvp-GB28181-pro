package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

/**
 * business grouping
 */
@Data
@Schema(description = "business grouping")
public class Group implements Comparable<Group>{
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
     * parent groupID
     */
    @Schema(description = "parent groupID")
    private Integer parentId;

    /**
     * Parent regional national standardID
     */
    @Schema(description = "Parent regional national standardID")
    private String parentDeviceId;

    /**
     * The national standard number of the business group to which it belongs
     */
    @Schema(description = "The national standard number of the business group to which it belongs")
    private String businessGroup;

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

    /**
     * Administrative division
     */
    @Schema(description = "Administrative division")
    private String civilCode;

    /**
     * Alias
     */
    @Schema(description = "Alias. This alias is a unique value and can be used to connect to a third party and store the other party'sID")
    private String alias;

    public static Group getInstance(DeviceChannel channel) {
        GbCode gbCode = GbCode.decode(channel.getDeviceId());
        if (gbCode == null || (!gbCode.getTypeCode().equals("215") && !gbCode.getTypeCode().equals("216"))) {
            return null;
        }
        Group group = new Group();
        group.setName(channel.getName());
        group.setDeviceId(channel.getDeviceId());
        group.setCreateTime(DateUtil.getNow());
        group.setUpdateTime(DateUtil.getNow());
        if (gbCode.getTypeCode().equals("215")) {
            group.setBusinessGroup(channel.getDeviceId());
        }else if (gbCode.getTypeCode().equals("216")) {
            group.setBusinessGroup(channel.getBusinessGroupId());
            group.setParentDeviceId(channel.getParentId());
        }
        if (group.getBusinessGroup() == null) {
            return null;
        }
        return group;
    }

    @Override
    public int compareTo(@NotNull Group region) {
        return Integer.compare(Integer.parseInt(this.deviceId), Integer.parseInt(region.getDeviceId()));
    }
}
