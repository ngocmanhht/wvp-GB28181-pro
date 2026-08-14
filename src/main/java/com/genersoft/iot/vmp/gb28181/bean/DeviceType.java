package com.genersoft.iot.vmp.gb28181.bean;

import org.jetbrains.annotations.NotNull;

public class DeviceType implements Comparable<DeviceType>{

    /**
     * No.
     */
    private String name;

    /**
     * Name
     */
    private String code;

    /**
     * attributed name
     */
    private String ownerName;
    public static DeviceType getInstance(DeviceTypeEnum typeEnum) {
        DeviceType deviceType = new DeviceType();
        deviceType.setName(typeEnum.getName());
        deviceType.setCode(typeEnum.getCode());
        deviceType.setOwnerName(typeEnum.getOwnerName());
        return deviceType;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    @Override
    public int compareTo(@NotNull DeviceType deviceType) {
        return Integer.compare(Integer.parseInt(this.code), Integer.parseInt(deviceType.getCode()));
    }
}
