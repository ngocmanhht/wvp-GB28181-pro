package com.genersoft.iot.vmp.jt1078.bean;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Schema(description = "Area properties")
public class JTAreaAttribute {

    @Schema(description = "Whether to enable the judgment rules of start time and end time, false: no; true: yes")
    private boolean ruleForTimeLimit;

    @Schema(description = "Whether to enable the judgment rules for maximum speed, overspeed duration and nighttime maximum speed, false: no; true: yes")
    private boolean ruleForSpeedLimit;

    @Schema(description = "Whether to alert the driver when entering the area, false: no; true: yes")
    private boolean ruleForAlarmToDriverWhenEnter;

    @Schema(description = "Whether to alert the platform when entering the area, false: no; true: yes")
    private boolean ruleForAlarmToPlatformWhenEnter;

    @Schema(description = "Whether to alert the driver when leaving the area, false: no; true: yes")
    private boolean ruleForAlarmToDriverWhenExit;

    @Schema(description = "Whether to alarm the platform when leaving the area, false: no; true: yes")
    private boolean ruleForAlarmToPlatformWhenExit;

    @Schema(description = "false：North latitude; true: south latitude")
    private boolean southLatitude;

    @Schema(description = "false：East longitude; true: west longitude")
    private boolean westLongitude;

    @Schema(description = "false：Allow opening the door; true: prohibit opening the door")
    private boolean prohibitOpeningDoors;

    @Schema(description = "false：Turn on the communication module when entering the area; true: turn off the communication module when entering the area")
    private boolean ruleForTurnOffCommunicationWhenEnter;

    @Schema(description = "false：Do not collect GNSS detailed positioning data when entering the area; true: Collect GNSS detailed positioning data when entering the area")
    private boolean ruleForGnssWhenEnter;

    public ByteBuf encode(){
        ByteBuf byteBuf = Unpooled.buffer();
        short content = 0 ;
        if (ruleForTimeLimit) {
            content |= 1;
        }
        if (ruleForSpeedLimit) {
            content |= (1 << 1);
        }
        if (ruleForAlarmToDriverWhenEnter) {
            content |= (1 << 2);
        }
        if (ruleForAlarmToPlatformWhenEnter) {
            content |= (1 << 3);
        }
        if (ruleForAlarmToDriverWhenExit) {
            content |= (1 << 4);
        }
        if (ruleForAlarmToPlatformWhenExit) {
            content |= (1 << 5);
        }
        if (southLatitude) {
            content |= (1 << 6);
        }
        if (westLongitude) {
            content |= (byte) (1 << 7);
        }
        if (prohibitOpeningDoors) {
            content |= (1 << (0 + 8));
        }
        if (ruleForTurnOffCommunicationWhenEnter) {
            content |= (1 << (1 + 8));
        }
        if (ruleForGnssWhenEnter) {
            content |= (1 << (2 + 8));
        }
        byteBuf.writeShort((short)(content & 0xffff));
        return byteBuf;
    }

    public static JTAreaAttribute decode(int attributeInt) {
        JTAreaAttribute attribute = new JTAreaAttribute();
        attribute.setRuleForTimeLimit((attributeInt & 1) == 1);
        attribute.setRuleForSpeedLimit((attributeInt >> 1 & 1) == 1);
        attribute.setRuleForAlarmToDriverWhenEnter((attributeInt >> 2 & 1) == 1);
        attribute.setRuleForAlarmToPlatformWhenEnter((attributeInt >> 3 & 1) == 1);
        attribute.setRuleForAlarmToDriverWhenExit((attributeInt >> 4 & 1) == 1);
        attribute.setRuleForAlarmToPlatformWhenExit((attributeInt >> 5 & 1) == 1);
        attribute.setSouthLatitude((attributeInt >> 6 & 1) == 1);
        attribute.setWestLongitude((attributeInt >> 7 & 1) == 1);
        attribute.setProhibitOpeningDoors((attributeInt >> 8 & 1) == 1);
        attribute.setRuleForTurnOffCommunicationWhenEnter((attributeInt >> 9 & 1) == 1);
        attribute.setRuleForGnssWhenEnter((attributeInt >> 10 & 1) == 1);
        return attribute;
    }

}
