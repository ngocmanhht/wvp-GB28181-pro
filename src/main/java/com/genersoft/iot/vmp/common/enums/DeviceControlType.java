package com.genersoft.iot.vmp.common.enums;

import org.dom4j.Element;
import org.springframework.util.ObjectUtils;


/**
 * @author gaofuwang
 * @date 2023/01/18/ 10:09:00
 * @since 1.0
 */
public enum DeviceControlType {

    /**
     * PTZ control
     * Up, down, left, right, preset, scan, auxiliary functions, cruise
     */
    PTZ("PTZCmd","PTZ control"),
    /**
     * remote start
     */
    TELE_BOOT("TeleBoot","remote start"),
    /**
     * Video control
     */
    RECORD("RecordCmd","Video control"),
    /**
     * Arm and disarm
     */
    GUARD("GuardCmd","Arm and disarm"),
    /**
     * Alarm control
     */
    ALARM("AlarmCmd","Alarm control"),
    /**
     * Force keyframe
     */
    I_FRAME("IFameCmd","Force keyframe"),
    /**
     * Scroll down to enlarge
     */
    DRAG_ZOOM_IN("DragZoomIn","Scroll down to enlarge"),
    /**
     * Zoom out
     */
    DRAG_ZOOM_OUT("DragZoomOut","Zoom out"),
    /**
     * guard position
     */
    HOME_POSITION("HomePosition","guard position");

    private final String val;

    private final String desc;

    DeviceControlType(String val, String desc) {
        this.val = val;
        this.desc = desc;
    }

    public String getVal() {
        return val;
    }

    public String getDesc() {
        return desc;
    }

    public static DeviceControlType typeOf(Element rootElement) {
        for (DeviceControlType item : DeviceControlType.values()) {
            if (!ObjectUtils.isEmpty(rootElement.element(item.val)) || !ObjectUtils.isEmpty(rootElement.elements(item.val))) {
                return item;
            }
        }
        return null;
    }
}
