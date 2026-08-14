package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.gb28181.utils.XmlUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dom4j.Element;

/**
 * Basic configuration
 */
@Data
@Schema(description = "Basic configuration")
public class BasicParam implements DeviceConfigAware {

    @Schema(description = "EquipmentID")
    private String deviceId;

    @Schema(description = "Channel ID, if you set the device configuration directly to the same as the device ID, you can")
    private String channelId;

    @Schema(description = "Name")
    private String name;

    @Schema(description = "Registration expiration time")
    private String expiration;

    @Schema(description = "heartbeat interval")
    private Integer heartBeatInterval;

    @Schema(description = "Number of heartbeat timeouts")
    private Integer heartBeatCount;

    @Schema(description = "Positioning feature support. value:0-Not supported;1-Support GPS positioning;2-Support Beidou positioning(Optional, the default value is0)，" +
            "Used to accept configuration query results, invalid in basic configuration")
    private Integer positionCapability;

    @Schema(description = "longitude(Optional)，Used to accept configuration query results, invalid in basic configuration")
    private Double longitude;

    @Schema(description = "Latitude(Optional)，Used to accept configuration query results, invalid in basic configuration")
    private Double latitude;

    public static BasicParam getInstance(String name, String expiration, Integer heartBeatInterval, Integer heartBeatCount) {
        BasicParam basicParam = new BasicParam();
        basicParam.setName(name);
        basicParam.setExpiration(expiration);
        basicParam.setHeartBeatInterval(heartBeatInterval);
        basicParam.setHeartBeatCount(heartBeatCount);
        return basicParam;
    }

    @Override
    public String configType() {
        return "BasicParam";
    }

    @Override
    public void fromXml(Element element) {
        setName(XmlUtil.getText(element, "Name"));
        setExpiration(XmlUtil.getText(element, "Expiration"));
        setHeartBeatInterval(XmlUtil.getInteger(element, "HeartBeatInterval"));
        setHeartBeatCount(XmlUtil.getInteger(element, "HeartBeatCount"));
        setPositionCapability(XmlUtil.getInteger(element, "PositionCapability"));
        setLongitude(XmlUtil.getDouble(element, "Longitude"));
        setLatitude(XmlUtil.getDouble(element, "Latitude"));
    }
}
