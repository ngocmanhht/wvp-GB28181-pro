package com.genersoft.iot.vmp.service.bean;

import com.genersoft.iot.vmp.gb28181.bean.MobilePosition;
import com.genersoft.iot.vmp.utils.DateUtil;
import lombok.Data;

@Data
public class GPSMsgInfo {

    /**
     * Channel national standardID
     */
    private String id;

    /**
     * channelID
     */
    private Integer channelId;

    /**
     *
     */
    private String app;

    /**
     * longitude (Required)
     */
    private double lng;

    /**
     * Latitude (Required)
     */
    private double lat;

    /**
     * speed, unit:km/h (Optional)
     */
    private Double speed;

    /**
     * Generate notification time, time format： 2020-01-14T14:32:12
     */
    private String time;

    /**
     * Direction, the value is the clockwise angle between the current camera direction and true north, the value range is 0°~360°, unit:(°)(Optional)
     */
    private Double direction;

    /**
     * Altitude, unit:m(Optional)
     */
    private Double altitude;

    private boolean stored;

    public static GPSMsgInfo getInstance(MobilePosition mobilePosition) {
        GPSMsgInfo gpsMsgInfo = new GPSMsgInfo();
        gpsMsgInfo.setChannelId(mobilePosition.getChannelId());
        gpsMsgInfo.setAltitude(mobilePosition.getAltitude());
        gpsMsgInfo.setLng(mobilePosition.getLongitude());
        gpsMsgInfo.setLat(mobilePosition.getLatitude());
        gpsMsgInfo.setSpeed(mobilePosition.getSpeed());
        gpsMsgInfo.setDirection(mobilePosition.getDirection());
        gpsMsgInfo.setTime(DateUtil.timestampMsTo_yyyy_MM_dd_HH_mm_ss(mobilePosition.getTimestamp()));
        return gpsMsgInfo;
    }
}
