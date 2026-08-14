package com.genersoft.iot.vmp.gb28181.controller.bean;

import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Streamlined channel information display, mainly used to display the list when selecting a channel
 */
@Schema(description = "Streamlined channel information display")
public class ChannelReduce {

    /**
     * deviceChannelThe database is automatically incrementedID
     */
    @Schema(description = "deviceChannelThe database is automatically incrementedID")
    private int id;

    /**
     * channelid
     */
    @Schema(description = "Channel national standard number")
    private String channelId;

    /**
     * Equipmentid
     */
    @Schema(description = "Equipment national standard number")
    private String deviceId;

    /**
     * Channel name
     */
    @Schema(description = "Channel name")
    private String name;

    /**
     * Manufacturer
     */
    @Schema(description = "Manufacturer")
    private String manufacturer;

    /**
     * wanaddress
     */
    @Schema(description = "wanaddress")
    private String  hostAddress;

    /**
     * Number of child nodes
     */
    @Schema(description = "Number of child nodes")
    private int  subCount;

    /**
     * platformId
     */
    @Schema(description = "Platform superior national standard number")
    private String  platformId;

    /**
     * DirectoryId
     */
    @Schema(description = "Catalog national standard number")
    private String  catalogId;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }

    public int getSubCount() {
        return subCount;
    }

    public void setSubCount(int subCount) {
        this.subCount = subCount;
    }

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public String getCatalogId() {
        return catalogId;
    }

    public void setCatalogId(String catalogId) {
        this.catalogId = catalogId;
    }
}
