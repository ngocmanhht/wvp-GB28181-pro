package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.gb28181.utils.NumericUtil;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.utils.DateUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Element;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.getText;

/**
 * @description: Move locationbean
 * @author: lawrencehj
 * @date: 2021January 23
 */

@Slf4j
@Data
public class MobilePosition {

    /**
     * Channel database auto-incrementId
     */
    private Integer channelId;

    /**
     * Channel national standard number
     */
    private String channelDeviceId;

    /**
     * Notification time
     */
    private long timestamp;

    /**
     * longitude
     */
    private double longitude;

    /**
     * Latitude
     */
    private double latitude;

    /**
     * altitude
     */
    private double altitude;

    /**
     * speed
     */
    private double speed;

    /**
     * direction
     */
    private double direction;

    /**
     * creation time
     */
    private String createTime;

    public static List<MobilePosition> decode(Element rootElementAfterCharset) {

        List<MobilePosition> mobilePositions = new ArrayList<>();

        MobilePosition mobilePosition = new MobilePosition();
        mobilePosition.setCreateTime(DateUtil.getNow());

        String channelId = getText(rootElementAfterCharset, "DeviceID");

        mobilePosition.setChannelDeviceId(channelId);
        String time = getText(rootElementAfterCharset, "Time");
        if (ObjectUtils.isEmpty(time)){
            mobilePosition.setTimestamp(System.currentTimeMillis());
        }else {
            Long timestamp = SipUtils.parseTimeForTimestamp(time);
            if(timestamp == null) {
                log.warn("Failed to parse mobile location time：{}， Use current time", time);
                mobilePosition.setTimestamp(System.currentTimeMillis());
            }else {
                mobilePosition.setTimestamp(timestamp);
            }
        }
        mobilePosition.setLongitude(Double.parseDouble(getText(rootElementAfterCharset, "Longitude")));
        mobilePosition.setLatitude(Double.parseDouble(getText(rootElementAfterCharset, "Latitude")));
        if (NumericUtil.isDouble(getText(rootElementAfterCharset, "Speed"))) {
            mobilePosition.setSpeed(Double.parseDouble(getText(rootElementAfterCharset, "Speed")));
        } else {
            mobilePosition.setSpeed(0.0);
        }
        if (NumericUtil.isDouble(getText(rootElementAfterCharset, "Direction"))) {
            mobilePosition.setDirection(Double.parseDouble(getText(rootElementAfterCharset, "Direction")));
        } else {
            mobilePosition.setDirection(0.0);
        }
        if (NumericUtil.isDouble(getText(rootElementAfterCharset, "Altitude"))) {
            mobilePosition.setAltitude(Double.parseDouble(getText(rootElementAfterCharset, "Altitude")));
        } else {
            mobilePosition.setAltitude(0.0);
        }

        mobilePositions.add(mobilePosition);

        return mobilePositions;
    }

    @Override
    public String toString() {
        return "MobilePosition{" +
                ", channelId=" + channelId +
                ", channelDeviceId='" + channelDeviceId + '\'' +
                ", longitude=" + longitude +
                ", latitude=" + latitude +
                ", altitude=" + altitude +
                ", speed=" + speed +
                ", direction=" + direction +
                ", createTime='" + createTime + '\'' +
                '}';
    }
}
