package com.genersoft.iot.vmp.jt1078.bean;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Schema(description = "route properties")
public class JTRouteAttribute {

    @Schema(description = "Whether to enable the judgment rules of start time and end time, false: no; true: yes")
    private boolean ruleForTimeLimit;

    @Schema(description = "Whether to alert the driver when entering the area, false: no; true: yes")
    private boolean ruleForAlarmToDriverWhenEnter;

    @Schema(description = "Whether to alert the platform when entering the area, false: no; true: yes")
    private boolean ruleForAlarmToPlatformWhenEnter;

    @Schema(description = "Whether to alert the driver when leaving the area, false: no; true: yes")
    private boolean ruleForAlarmToDriverWhenExit;

    @Schema(description = "Whether to alarm the platform when leaving the area, false: no; true: yes")
    private boolean ruleForAlarmToPlatformWhenExit;

    public ByteBuf encode(){
        ByteBuf byteBuf = Unpooled.buffer();
        short content = 0;
        if (ruleForTimeLimit) {
            content |= 1;
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
        byteBuf.writeShort((short)(content & 0xffff));
        return byteBuf;
    }

    public static JTRouteAttribute decode(int attributeInt) {
        JTRouteAttribute attribute = new JTRouteAttribute();
        attribute.setRuleForTimeLimit((attributeInt & 1) == 1);
        attribute.setRuleForAlarmToDriverWhenEnter((attributeInt >> 2 & 1) == 1);
        attribute.setRuleForAlarmToPlatformWhenEnter((attributeInt >> 3 & 1) == 1);
        attribute.setRuleForAlarmToDriverWhenExit((attributeInt >> 4 & 1) == 1);
        attribute.setRuleForAlarmToPlatformWhenExit((attributeInt >> 5 & 1) == 1);
        return attribute;
    }

    @Override
    public String toString() {
        return "JTRouteAttribute{" +
                "ruleForTimeLimit=" + ruleForTimeLimit +
                ", ruleForAlarmToDriverWhenEnter=" + ruleForAlarmToDriverWhenEnter +
                ", ruleForAlarmToPlatformWhenEnter=" + ruleForAlarmToPlatformWhenEnter +
                ", ruleForAlarmToDriverWhenExit=" + ruleForAlarmToDriverWhenExit +
                ", ruleForAlarmToPlatformWhenExit=" + ruleForAlarmToPlatformWhenExit +
                '}';
    }
}
