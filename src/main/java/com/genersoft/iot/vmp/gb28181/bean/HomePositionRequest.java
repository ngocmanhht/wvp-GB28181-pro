package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.gb28181.utils.MessageElement;

/**
 * Device information query response
 *
 * @author Y.G
 * @version 1.0
 * @date 2022/6/28 14:55
 */
public class HomePositionRequest {
    /**
     * serial number
     */
    @MessageElement("SN")
    private String sn;

    @MessageElement("DeviceID")
    private String deviceId;

    @MessageElement(value = "HomePosition")
    private HomePosition homePosition;


    /**
     * Basic parameters
     */
    public static class HomePosition {
        /**
         * Play window length pixel value
         */
        @MessageElement("Enabled")
        protected String enabled;
        /**
         * Play window width pixel value
         */
        @MessageElement("ResetTime")
        protected String resetTime;
        /**
         * The horizontal axis coordinate pixel value of the center of the pull box
         */
        @MessageElement("PresetIndex")
        protected String presetIndex;

        public String getEnabled() {
            return enabled;
        }

        public void setEnabled(String enabled) {
            this.enabled = enabled;
        }

        public String getResetTime() {
            return resetTime;
        }

        public void setResetTime(String resetTime) {
            this.resetTime = resetTime;
        }

        public String getPresetIndex() {
            return presetIndex;
        }

        public void setPresetIndex(String presetIndex) {
            this.presetIndex = presetIndex;
        }
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public HomePosition getHomePosition() {
        return homePosition;
    }

    public void setHomePosition(HomePosition homePosition) {
        this.homePosition = homePosition;
    }
}
